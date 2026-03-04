package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.StudyPlan;
import com.campus.learningspace.service.StudyPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-plan")
@CrossOrigin
public class StudyPlanController {

    @Autowired
    private StudyPlanService studyPlanService;

    @GetMapping("/{id}")
    public Result<StudyPlan> getById(@PathVariable Long id) {
        return Result.success(studyPlanService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public Result<List<StudyPlan>> getUserPlans(@PathVariable Long userId) {
        return Result.success(studyPlanService.getUserPlans(userId));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody StudyPlan plan) {
        plan.setId(null);
        if (plan.getStatus() == null) {
            plan.setStatus(1); // 进行中
        }
        return Result.success(studyPlanService.save(plan));
    }
}

