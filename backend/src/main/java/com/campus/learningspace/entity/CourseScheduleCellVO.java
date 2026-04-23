package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalTime;

@Data
public class CourseScheduleCellVO {
    private Long slotId;
    private String label;
    private LocalTime startTime;
    private LocalTime endTime;
    /**
     * course_occupied | reservation_occupied | available
     */
    private String finalStatus;
    private String courseName;
    private String teacherName;
}
