package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.dto.ReservationLimitVO;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.entity.ReservationVO;
import com.campus.learningspace.service.CourseScheduleService;
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
    @Autowired
    private CourseScheduleService courseScheduleService;

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
        return Result.success(courseScheduleService.getClassroomSlotStatusMaps(classroomId, date));
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

    /**
     * 生成预约二维码（用户端触发，用于设备端扫码核销）
     * POST /api/reservation/{id}/qrcode
     */
    @PostMapping("/{id}/qrcode")
    public Result<Map<String, Object>> generateQrcode(@PathVariable Long id) {
        // 默认有效期 10 分钟
        String code = reservationService.ensureReservationQrcode(id, 10);
        if (code == null) {
            return Result.error(400, "无法生成二维码（预约可能已不是待扫码状态）");
        }
        return Result.success(Map.of(
                "code", code,
                "ok", true
        ));
    }

    /**
     * 获取二维码状态（用户端轮询）
     * GET /api/reservation/qrcode/{code}/status
     */
    @GetMapping("/qrcode/{code}/status")
    public Result<Map<String, Object>> getQrcodeStatus(@PathVariable String code) {
        return Result.success(reservationService.getReservationQrcodeStatus(code));
    }

    /**
     * 设备端扫码核销
     * POST /api/reservation/qrcode/{code}/scan
     * body: { deviceUid }
     */
    @PostMapping("/qrcode/{code}/scan")
    public Result<Map<String, Object>> scanQrcode(@PathVariable String code, @RequestBody Map<String, Object> body) {
        Object deviceUidObj = body == null ? null : body.get("deviceUid");
        String deviceUid = deviceUidObj == null ? null : String.valueOf(deviceUidObj);
        return Result.success(reservationService.scanReservationQrcode(code, deviceUid));
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
