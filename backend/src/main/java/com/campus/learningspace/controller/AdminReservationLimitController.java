package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.dto.ReservationLimitUpdateDTO;
import com.campus.learningspace.dto.ReservationLimitVO;
import com.campus.learningspace.service.AdminAuthService;
import com.campus.learningspace.service.AdminPermissionService;
import com.campus.learningspace.service.ReservationLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reservation-limits")
@CrossOrigin
public class AdminReservationLimitController {

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private AdminPermissionService adminPermissionService;

    @Autowired
    private ReservationLimitService reservationLimitService;

    @GetMapping
    public Result<ReservationLimitVO> get(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        var session = adminAuthService.getSession(token);
        if (session == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        return Result.success(reservationLimitService.getEffectiveLimits());
    }

    @PutMapping
    public Result<ReservationLimitVO> update(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody ReservationLimitUpdateDTO dto) {
        var session = adminAuthService.getSession(token);
        if (session == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        return Result.success(reservationLimitService.updateLimits(dto));
    }
}
