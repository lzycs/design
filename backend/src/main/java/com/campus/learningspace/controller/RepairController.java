package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Repair;
import com.campus.learningspace.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair")
@CrossOrigin
public class RepairController {

    @Autowired
    private RepairService repairService;

    @GetMapping("/{id}")
    public Result<Repair> getById(@PathVariable Long id) {
        return Result.success(repairService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public Result<List<Repair>> getUserRepairs(@PathVariable Long userId) {
        return Result.success(repairService.getUserRepairs(userId));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody Repair repair) {
        repair.setId(null);
        // 默认状态: 1-待处理
        if (repair.getStatus() == null) {
            repair.setStatus(1);
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

