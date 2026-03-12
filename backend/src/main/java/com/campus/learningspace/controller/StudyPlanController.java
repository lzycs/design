package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.dto.CreateStudyPlanDTO;
import com.campus.learningspace.entity.StudyPlan;
import com.campus.learningspace.entity.StudyPlanVO;
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

    /** 共享学习计划：用户参与的小组内的所有计划 */
    @GetMapping("/shared/{userId}")
    public Result<List<StudyPlanVO>> getSharedPlans(@PathVariable Long userId) {
        return Result.success(studyPlanService.getSharedPlans(userId));
    }

    /** 小组学习计划：按 teamRequestId 查询 */
    @GetMapping("/team/{teamRequestId}")
    public Result<List<StudyPlanVO>> getTeamPlans(@PathVariable Long teamRequestId) {
        return Result.success(studyPlanService.getPlansByTeam(teamRequestId));
    }

    /** 按预约查询学习计划 */
    @GetMapping("/by-reservation/{reservationId}")
    public Result<List<StudyPlanVO>> getPlansByReservation(@PathVariable Long reservationId) {
        return Result.success(studyPlanService.getPlansByReservation(reservationId));
    }

    /** 创建学习计划，可选同时创建研讨室预约并关联 */
    @PostMapping("/create-with-reservation")
    public Result<StudyPlan> createWithReservation(@RequestBody CreateStudyPlanDTO dto) {
        return Result.success(studyPlanService.createWithReservation(dto));
    }

    /** 更新学习计划（关联研讨室等） */
    @PutMapping("/{id}")
    public Result<StudyPlan> update(@PathVariable Long id, @RequestBody CreateStudyPlanDTO dto) {
        return Result.success(studyPlanService.updateWithReservation(id, dto));
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

