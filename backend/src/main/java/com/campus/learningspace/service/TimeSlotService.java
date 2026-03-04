package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.TimeSlot;
import com.campus.learningspace.mapper.TimeSlotMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimeSlotService extends ServiceImpl<TimeSlotMapper, TimeSlot> {

    public List<TimeSlot> getActiveSlots() {
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(TimeSlot::getSortOrder);
        return list(wrapper);
    }
}

