/**
 * 
 */
package com.leave.request.controller;

import java.lang.ProcessBuilder.Redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.leave.request.dto.MyTask;
import com.leave.request.dto.RequestApprovalDto;
import com.leave.request.model.LeaveRequest;
import com.leave.request.service.MyTaskService;
import com.leave.request.service.RequestService;
import com.leave.request.util.SecurityUtil;
import com.leave.request.validator.RequestValidator;

/**
 * @author Eraine
 *
 */
@Controller
public class RequestController {

	private final static Logger logger = LoggerFactory.getLogger(RequestController.class);
	
	@Autowired
	private RequestValidator validator;

	@Autowired
	private RequestService requestService;

	@Autowired
	private MyTaskService myTaskService;

	@GetMapping("/request")
	public String request(Model model) {
		model.addAttribute("requestForm", new LeaveRequest());
		return "request";
	}

	@PostMapping("/request")
	public String processRequest(@ModelAttribute("requestForm") LeaveRequest leaveRequest, BindingResult bindingResult,
			Model model) {
		validator.validate(leaveRequest, bindingResult);

		if (bindingResult.hasErrors()) {
			return "request";
		}

		requestService.save(leaveRequest);
		requestService.submit(leaveRequest);

		model.addAttribute("success", true);

		return "request";
	}

	@GetMapping("/view/{id}")
	public String requestView(@PathVariable("id") Long id, Model model) {
		LeaveRequest leaveRequest = requestService.findById(id);

		model.addAttribute("leaveRequest", leaveRequest);

		return "request-view";
	}

	@GetMapping("/review/{id}")
	public String manageEmployeeLeavesReview(@PathVariable("id") String taskId, RedirectAttributes redirectAttributes,
			Model model) {
		MyTask myTask = myTaskService.findTaskByTaskId(taskId);

		LeaveRequest leaveRequest = requestService.findById((Long) myTask.getProcessVariables().get("leaveId"));

		if (SecurityUtil.getUsername().equals(leaveRequest.getCreateBy())) {
			redirectAttributes.addFlashAttribute("error", "You are not authorized to view that page!");
			return "redirect:/home";
		}

		model.addAttribute("leaveRequest", leaveRequest);
		model.addAttribute("taskId", myTask.getId());
		RequestApprovalDto dto = new RequestApprovalDto(taskId, String.valueOf(leaveRequest.getId()));
		model.addAttribute("requestApprovalForm", dto);

		return "request-review";
	}

	@PostMapping(value = "/review/submit", params = "action=approve")
	public String processApproveRequest(@ModelAttribute("requestApprovalForm") RequestApprovalDto requestApprovalDto,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		logger.info("+++approve request+++");
		logger.info("+++form: " + requestApprovalDto.toString());
		
		redirectAttributes.addFlashAttribute("requestReviewed", "Done! Request has been approved.");
		
		return "redirect:/home";
	}
	
	@PostMapping(value = "/review/submit", params = "action=reject")
	public String processRejectRequest(@ModelAttribute("requestApprovalForm") RequestApprovalDto requestApprovalDto,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		logger.info("+++reject request+++");
		
		redirectAttributes.addFlashAttribute("requestReviewed", "Done! Request has been rejected.");
		return "request-review";
	}

}