package com.campus.learningspace.dto;

import lombok.Data;

@Data
public class ReservationLimitUpdateDTO {
    private Integer maxPerWeek;
    private Integer maxDurationMinutes;
}
