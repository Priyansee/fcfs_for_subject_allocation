package com.uni.subjectallocation.controller;

import com.uni.subjectallocation.dto.CourseInfo;
import com.uni.subjectallocation.dto.request.SlotAllocationRequest;
import com.uni.subjectallocation.exception.AlreadyEnrolledException;
import com.uni.subjectallocation.exception.SlotFullException;
import com.uni.subjectallocation.exception.SlotNotFoundException;
import com.uni.subjectallocation.service.AllocationService;
import com.uni.subjectallocation.service.SlotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slots")
public class SlotController {

    private final SlotService slotService;
    private final AllocationService allocationService;

    public SlotController(SlotService slotService, AllocationService allocationService) {
        this.slotService = slotService;
        this.allocationService = allocationService;
    }

    @GetMapping
    public Map<String, List<CourseInfo>> getSlots() {
        return slotService.getAvailableCoursesBySlot();
    }

    @PostMapping("/allocate")
    public ResponseEntity<String> allocateSlot(
            @RequestBody SlotAllocationRequest request,
            @RequestHeader("X-Student-Id") Long studentId) {
        try {
            String result = allocationService.allocateSlot(studentId, request.getSlot());
            return ResponseEntity.ok(result);
        } catch (SlotNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Slot Not Found");
        } catch (SlotFullException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Slot Full");
        } catch (AlreadyEnrolledException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Already Enrolled");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }
}
