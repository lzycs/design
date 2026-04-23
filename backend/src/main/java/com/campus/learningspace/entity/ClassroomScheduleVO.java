package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClassroomScheduleVO {
    private Long classroomId;
    private Long buildingId;
    private String classroomName;
    private String roomNumber;
    private LocalDate date;
    private String view;
    private List<CourseScheduleCellVO> slots;
}
