package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.mapper.ReservationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservationService extends ServiceImpl<ReservationMapper, Reservation> {

    public List<Reservation> getUserReservations(Long userId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreateTime);
        return list(wrapper);
    }

    public List<Reservation> getClassroomReservations(Long classroomId, LocalDate date) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getClassroomId, classroomId)
                .eq(Reservation::getReservationDate, date)
                .in(Reservation::getStatus, 1, 2, 3);
        return list(wrapper);
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
}
