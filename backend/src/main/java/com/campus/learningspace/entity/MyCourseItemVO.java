package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MyCourseItemVO {
    private Long courseId;
    private String courseName;
    private String teacherName;
    private Integer weekDay;
    private Integer teachingWeek;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long classroomId;
    private String classroomName;
    private String roomNumber;
    private String buildingName;
    private Integer isStarred;
    private String note;
    private Integer remindBeforeMinutes;
}
