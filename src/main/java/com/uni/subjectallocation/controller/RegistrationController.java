package com.uni.subjectallocation.controller;

import com.uni.subjectallocation.dto.CourseInfo;
import com.uni.subjectallocation.service.AllocationService;
import com.uni.subjectallocation.service.SlotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/registration")
public class RegistrationController {

    private final SlotService slotService;
    private final AllocationService allocationService;

    public RegistrationController(SlotService slotService, AllocationService allocationService) {
        this.slotService = slotService;
        this.allocationService = allocationService;
    }

    @GetMapping("/slots")
    public String showSlotSelection(@RequestParam(defaultValue = "11926") Long studentId, Model model) {
        String enrolledSlot = allocationService.getEnrolledSlot(studentId);
        
        Map<String, List<CourseInfo>> slots = slotService.getAvailableCoursesBySlot();
        
        model.addAttribute("slots", slots);
        model.addAttribute("enrolledSlot", enrolledSlot);
        model.addAttribute("currentStudentId", studentId);
        
        return "slot-selection";
    }

    @PostMapping("/allocate")
    public String allocateSlot(
            @RequestParam String slot,
            @RequestParam Long studentId,
            RedirectAttributes redirectAttributes) {
        try {
            String result = allocationService.allocateSlot(studentId, slot);
            if ("Slot Full".equals(result)) {
                redirectAttributes.addFlashAttribute("message", result);
                redirectAttributes.addFlashAttribute("messageType", "error");
            } else {
                redirectAttributes.addFlashAttribute("message", result);
                redirectAttributes.addFlashAttribute("messageType", "success");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/registration/result";
    }

    @GetMapping("/result")
    public String showResult() {
        return "result";
    }
}
