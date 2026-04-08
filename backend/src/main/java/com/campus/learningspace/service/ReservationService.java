package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.entity.ReservationVO;
import com.campus.learningspace.entity.TimeSlot;
import com.campus.learningspace.service.ScanDeviceService;
import com.campus.learningspace.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService extends ServiceImpl<ReservationMapper, Reservation> {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private ReservationLimitService reservationLimitService;

    @Autowired
    private ScanDeviceService scanDeviceService;

    @Override
    public boolean save(Reservation entity) {
        if (entity != null && entity.getId() == null) {
            reservationLimitService.assertNewReservationAllowed(entity);
        }
        return super.save(entity);
    }

    /**
     * 用户预约列表（含资源名称等展示字段）
     */
    public List<ReservationVO> getUserReservations(Long userId) {
        return baseMapper.selectUserReservations(userId);
    }

    public List<Reservation> getClassroomReservations(Long classroomId, LocalDate date) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getClassroomId, classroomId)
                .eq(Reservation::getReservationDate, date)
                .in(Reservation::getStatus, 1, 2, 3);
        return list(wrapper);
    }

    /**
     * 按时间段返回某个教室在指定日期的预约状态
     */
    public List<Map<String, Object>> getClassroomSlotStatuses(Long classroomId, LocalDate date) {
        List<TimeSlot> slots = timeSlotService.getActiveSlots();
        return slots.stream().map(slot -> {
            Map<String, Object> map = new HashMap<>();
            map.put("label", slot.getLabel());
            map.put("startTime", slot.getStartTime());
            map.put("endTime", slot.getEndTime());

            LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Reservation::getResourceType, 1)
                    .eq(Reservation::getClassroomId, classroomId)
                    .eq(Reservation::getReservationDate, date)
                    .in(Reservation::getStatus, 1, 2, 3)
                    .and(w -> w.lt(Reservation::getStartTime, slot.getEndTime())
                            .gt(Reservation::getEndTime, slot.getStartTime()));
            boolean occupied = count(wrapper) > 0;
            map.put("status", occupied ? "occupied" : "available");
            return map;
        }).toList();
    }

    public boolean checkTimeConflict(Long classroomId, LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeReservationId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getClassroomId, classroomId)
                .eq(Reservation::getReservationDate, date)
                .in(Reservation::getStatus, 1, 2, 3)
                .and(w -> w.lt(Reservation::getStartTime, endTime)
                        .gt(Reservation::getEndTime, startTime));
        if (excludeReservationId != null) {
            wrapper.ne(Reservation::getId, excludeReservationId);
        }
        return count(wrapper) > 0;
    }

    /**
     * 扫码签到：将预约状态设置为已签到，并记录签到时间
     */
    public boolean checkin(Long id) {
        Reservation reservation = getById(id);
        if (reservation == null) {
            return false;
        }
        // 仅待签到状态允许签到
        Integer status = reservation.getStatus();
        if (status != null && status != 1) {
            return false;
        }
        reservation.setStatus(2); // 已签到
        reservation.setCheckinTime(LocalDateTime.now());
        return updateById(reservation);
    }

    /**
     * 统计某教室的签到次数（预约状态为已签到或已完成的条数，resource_type=1）
     */
    public long countCheckinByClassroomId(Long classroomId) {
        if (classroomId == null) return 0;
        LambdaQueryWrapper<Reservation> w = new LambdaQueryWrapper<>();
        w.eq(Reservation::getResourceType, 1)
                .eq(Reservation::getClassroomId, classroomId)
                .in(Reservation::getStatus, 2, 3);
        return count(w);
    }

    /**
     * 生成/复用预约二维码（用于用户端展示，设备端扫码后进行核销）
     */
    public String ensureReservationQrcode(Long reservationId, int expireMinutes) {
        if (reservationId == null) return null;
        Reservation r = getById(reservationId);
        if (r == null || (r.getDeleted() != null && r.getDeleted() == 1)) return null;
        Integer status = r.getStatus();
        // 仅待签到状态允许生成二维码
        if (status == null || status != 1) return null;

        LocalDateTime now = LocalDateTime.now();
        if (r.getQrcode() != null && !r.getQrcode().isBlank()
                && r.getQrcodeExpireTime() != null
                && r.getQrcodeExpireTime().isAfter(now)
                && (r.getQrcodeScanStatus() == null || r.getQrcodeScanStatus() == 0)) {
            return r.getQrcode();
        }

        String code = UUID.randomUUID().toString().replace("-", "");
        r.setQrcode(code);
        r.setQrcodeExpireTime(now.plusMinutes(Math.max(1, expireMinutes)));
        r.setQrcodeScanStatus(0);
        r.setQrcodeScanTime(null);
        r.setQrcodeScanDeviceUid(null);
        updateById(r);
        return code;
    }

    /**
     * 获取预约二维码状态（给用户端轮询）
     */
    public Map<String, Object> getReservationQrcodeStatus(String code) {
        Map<String, Object> m = new HashMap<>();
        if (code == null || code.isBlank()) {
            m.put("ok", false);
            m.put("message", "二维码无效");
            return m;
        }

        Reservation r = lambdaQuery().eq(Reservation::getQrcode, code)
                .eq(Reservation::getDeleted, 0)
                .one();
        if (r == null) {
            m.put("ok", false);
            m.put("message", "二维码无效");
            return m;
        }

        LocalDateTime now = LocalDateTime.now();
        Integer status = r.getStatus();
        if (status != null && status == 1) {
            // 仍处于待扫码确认
            if (r.getQrcodeExpireTime() != null && !r.getQrcodeExpireTime().isAfter(now)) {
                m.put("ok", false);
                m.put("message", "二维码已过期");
                m.put("expired", true);
                return m;
            }

            int scanStatus = r.getQrcodeScanStatus() == null ? 0 : r.getQrcodeScanStatus();
            if (scanStatus == 1) {
                m.put("ok", true);
                m.put("message", "预约成功");
                m.put("success", true);
            } else if (scanStatus == 2) {
                m.put("ok", false);
                m.put("message", "非授权设备禁止扫码");
            } else if (scanStatus == 3) {
                m.put("ok", false);
                m.put("message", "二维码已过期");
                m.put("expired", true);
            } else if (scanStatus == 4) {
                m.put("ok", false);
                m.put("message", "二维码无效/已使用");
            } else {
                m.put("ok", true);
                m.put("message", "等待扫码确认");
            }
            return m;
        }

        if (status != null && status == 2) {
            m.put("ok", true);
            m.put("message", "预约成功");
            m.put("success", true);
            return m;
        }

        m.put("ok", false);
        m.put("message", "二维码无效/已使用");
        return m;
    }

    /**
     * 设备端扫码预约二维码核销
     */
    public Map<String, Object> scanReservationQrcode(String code, String deviceUid) {
        if (code == null || code.isBlank()) return Map.of("ok", false, "message", "二维码无效");
        if (deviceUid == null || deviceUid.isBlank()) return Map.of("ok", false, "message", "缺少 deviceUid");

        Reservation r = lambdaQuery().eq(Reservation::getQrcode, code)
                .eq(Reservation::getDeleted, 0)
                .one();
        if (r == null) return Map.of("ok", false, "message", "二维码无效");

        LocalDateTime now = LocalDateTime.now();
        Integer status = r.getStatus();
        if (status == null || status != 1) {
            return Map.of("ok", false, "message", "二维码无效/已使用");
        }

        // 过期校验
        if (r.getQrcodeExpireTime() != null && !r.getQrcodeExpireTime().isAfter(now)) {
            r.setQrcodeScanStatus(3);
            r.setQrcodeScanTime(now);
            r.setQrcodeScanDeviceUid(deviceUid);
            updateById(r);
            return Map.of("ok", false, "message", "二维码已过期");
        }

        // 设备授权校验
        if (!scanDeviceService.isEnabled(deviceUid)) {
            r.setQrcodeScanStatus(2);
            r.setQrcodeScanTime(now);
            r.setQrcodeScanDeviceUid(deviceUid);
            updateById(r);
            return Map.of("ok", false, "message", "非授权设备禁止扫码");
        }

        // 成功核销
        r.setStatus(2);
        r.setCheckinTime(now);
        r.setQrcodeScanStatus(1);
        r.setQrcodeScanTime(now);
        r.setQrcodeScanDeviceUid(deviceUid);
        updateById(r);
        return Map.of("ok", true, "message", "预约成功", "success", true);
    }
}
