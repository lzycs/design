package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.StudyPlan;
import com.campus.learningspace.mapper.StudyPlanMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudyPlanService extends ServiceImpl<StudyPlanMapper, StudyPlan> {

    public List<StudyPlan> getUserPlans(Long userId) {
        LambdaQueryWrapper<StudyPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyPlan::getUserId, userId);
        return list(wrapper);
    }
}

