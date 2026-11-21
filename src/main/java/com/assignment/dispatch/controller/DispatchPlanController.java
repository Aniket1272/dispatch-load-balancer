package com.assignment.dispatch.controller;

import com.assignment.dispatch.dto.DispatchPlanResponse;
import com.assignment.dispatch.service.DispatchPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchPlanController {

    private final DispatchPlanService planService;

    public DispatchPlanController(DispatchPlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/plan")
    public ResponseEntity<DispatchPlanResponse> getPlan() {
        return ResponseEntity.ok(planService.generatePlan());
    }
}
