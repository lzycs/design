package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.entity.ReservationVO;
import com.campus.learningspace.entity.TimeSlot;
import com.campus.learningspace.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService extends ServiceImpl<ReservationMapper, Reservation> {

    @Autowired
    private TimeSlotService timeSlotService;

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
}
