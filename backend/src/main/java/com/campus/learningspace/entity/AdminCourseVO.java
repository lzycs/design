package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalTime;

/**
 * 管理端：课程表展示对象（包含教室位置、时间、周次等）
 */
@Data
public class AdminCourseVO {
    private Long id;
    private Long classroomId;
    private Long buildingId;
    private String buildingName;
    private Integer floor;
    private String roomNumber;
    private String location;

    private String courseName;
    private String teacherName;

    private Integer weekDay;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer startWeek;
    private Integer endWeek;
}

