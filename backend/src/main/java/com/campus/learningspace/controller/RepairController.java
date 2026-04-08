package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Repair;
import com.campus.learningspace.entity.RepairVO;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.service.RepairService;
import com.campus.learningspace.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair")
@CrossOrigin
public class RepairController {

    @Autowired
    private RepairService repairService;
    @Autowired
    private ReservationService reservationService;

    @GetMapping("/{id}")
    public Result<RepairVO> getById(@PathVariable Long id) {
        RepairVO vo = repairService.getRepairVOById(id);
        return vo != null ? Result.success(vo) : Result.error(404, "报修工单不存在");
    }

    @GetMapping("/user/{userId}")
    public Result<List<RepairVO>> getUserRepairs(@PathVariable Long userId) {
        return Result.success(repairService.getUserRepairVOList(userId));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody Repair repair) {
        repair.setId(null);
        if (repair.getResourceType() == null) {
            repair.setResourceType(1); // 1-教室
        }
        if (repair.getStatus() == null) {
            repair.setStatus(1); // 1-待处理
        }
        // 报修约束：仅允许报修本人“已签到/已完成”过的教室
        if (repair.getResourceType() != null && repair.getResourceType() == 1) {
            if (repair.getUserId() == null || repair.getClassroomId() == null) {
                return Result.error(400, "缺少用户或教室信息");
            }
            long signedCount = reservationService.lambdaQuery()
                    .eq(Reservation::getUserId, repair.getUserId())
                    .eq(Reservation::getResourceType, 1)
                    .eq(Reservation::getClassroomId, repair.getClassroomId())
                    .in(Reservation::getStatus, 2, 3)
                    .eq(Reservation::getDeleted, 0)
                    .count();
            if (signedCount == 0) {
                return Result.error(400, "仅可报修已签到过的教室");
            }
        }
        return Result.success(repairService.save(repair));
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody Repair payload) {
        Repair existing = repairService.getById(id);
        if (existing == null) {
            return Result.error(404, "报修工单不存在");
        }
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus());
        }
        existing.setHandleRemark(payload.getHandleRemark());
        existing.setHandlerId(payload.getHandlerId());
        existing.setHandleTime(payload.getHandleTime());
        return Result.success(repairService.updateById(existing));
    }
}

