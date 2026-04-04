package com.campus.learningspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationLimitVO {
    private int maxPerWeek;
    private int maxDurationMinutes;
}
