package com.campus.learningspace.entity;

import lombok.Data;

import java.util.List;

@Data
public class BuildingScheduleRowVO {
    private Long classroomId;
    private String classroomName;
    private String roomNumber;
    private Integer floor;
    private List<CourseScheduleCellVO> cells;
}
