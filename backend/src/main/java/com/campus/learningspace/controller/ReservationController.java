package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.dto.ReservationLimitVO;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.entity.ReservationVO;
import com.campus.learningspace.service.ReservationLimitService;
import com.campus.learningspace.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservation")
@CrossOrigin
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationLimitService reservationLimitService;

    /**
     * 当前预约规则（每人每周次数上限、单次最长分钟数），供用户端展示
     */
    @GetMapping("/limits")
    public Result<ReservationLimitVO> getReservationLimits() {
        return Result.success(reservationLimitService.getEffectiveLimits());
    }

    @GetMapping("/user/{userId}")
    public Result<List<ReservationVO>> getUserReservations(@PathVariable Long userId) {
        return Result.success(reservationService.getUserReservations(userId));
    }

    @GetMapping("/classroom/{classroomId}")
    public Result<List<Reservation>> getClassroomReservations(
            @PathVariable Long classroomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(reservationService.getClassroomReservations(classroomId, date));
    }

    /**
     * 获取某教室在指定日期的标准时间段预约状态（可用于前端时间段展示）
     */
    @GetMapping("/classroom/{classroomId}/slots")
    public Result<List<Map<String, Object>>> getClassroomSlotStatuses(
            @PathVariable Long classroomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(reservationService.getClassroomSlotStatuses(classroomId, date));
    }

    @GetMapping("/{id:\\d+}")
    public Result<Reservation> getById(@PathVariable Long id) {
        return Result.success(reservationService.getById(id));
    }

    /**
     * 扫码签到：前端模拟扫码成功后调用该接口，将预约状态更新为已签到
     */
    @PostMapping("/{id}/checkin")
    public Result<Boolean> checkin(@PathVariable Long id) {
        return Result.success(reservationService.checkin(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Reservation reservation) {
        return Result.success(reservationService.save(reservation));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Reservation reservation) {
        return Result.success(reservationService.updateById(reservation));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(reservationService.removeById(id));
    }
}
