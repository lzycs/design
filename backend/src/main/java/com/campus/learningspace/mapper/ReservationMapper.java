package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.entity.ReservationVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface ReservationMapper extends BaseMapper<Reservation> {
    @Select("""
            SELECT
              r.id,
              r.user_id           AS userId,
              r.resource_type     AS resourceType,
              r.classroom_id      AS classroomId,
              r.library_seat_id   AS librarySeatId,
              r.reservation_date  AS reservationDate,
              r.start_time        AS startTime,
              r.end_time          AS endTime,
              r.duration,
              r.purpose,
              r.status,
              r.qrcode,
              r.checkin_time      AS checkinTime,
              r.checkin_latitude  AS checkinLatitude,
              r.checkin_longitude AS checkinLongitude,
              r.create_time       AS createTime,
              r.update_time       AS updateTime,
              b.name              AS buildingName,
              c.room_number       AS classroomRoomNumber,
              l.name              AS libraryName,
              ls.seat_label       AS seatLabel,
              CASE
                WHEN r.resource_type = 1 THEN CONCAT(IFNULL(b.name, ''), '-', IFNULL(c.room_number, ''))
                WHEN r.resource_type = 2 THEN CONCAT(IFNULL(l.name, ''), '-', IFNULL(ls.seat_label, ''))
                ELSE CONCAT('预约#', r.id)
              END AS resourceName
            FROM reservation r
            LEFT JOIN classroom c ON r.classroom_id = c.id AND c.deleted = 0
            LEFT JOIN building b ON c.building_id = b.id AND b.deleted = 0
            LEFT JOIN library_seat ls ON r.library_seat_id = ls.id AND ls.deleted = 0
            LEFT JOIN library l ON ls.library_id = l.id AND l.deleted = 0
            WHERE r.deleted = 0 AND r.user_id = #{userId}
            ORDER BY r.create_time DESC
            """)
    List<ReservationVO> selectUserReservations(@Param("userId") Long userId);

    /**
     * 统计用户在日期区间内「有效」预约条数（待签到/已签到/已完成），用于每周次数上限
     */
    @Select("""
            SELECT COUNT(*)
            FROM reservation r
            WHERE r.deleted = 0
              AND r.user_id = #{userId}
              AND r.reservation_date >= #{startDate}
              AND r.reservation_date <= #{endDate}
              AND r.status IN (1, 2, 3)
            """)
    long countUserActiveReservationsBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
