package com.campus.learningspace.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约列表展示对象：在 Reservation 基础上补充资源名称等展示字段。
 * 用于“我的预约”等需要直接展示“教学楼-教室号/图书馆-座位号”的场景。
 */
@Data
public class ReservationVO {
    private Long id;
    private Long userId;
    private Integer resourceType;
    private Long classroomId;
    private Long librarySeatId;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer duration;
    private String purpose;
    private Integer status;
    private String qrcode;
    private LocalDateTime qrcodeExpireTime;
    private LocalDateTime qrcodeScanTime;
    private String qrcodeScanDeviceUid;
    private Integer qrcodeScanStatus;
    private LocalDateTime checkinTime;
    private BigDecimal checkinLatitude;
    private BigDecimal checkinLongitude;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 资源展示名称：
     * - 教室：教学楼名称-教室号
     * - 图书馆座位：图书馆名称-座位号
     */
    private String resourceName;

    /** 冗余字段：便于前端按需展示 */
    private String buildingName;
    private String classroomRoomNumber;
    private String libraryName;
    private String seatLabel;
}

