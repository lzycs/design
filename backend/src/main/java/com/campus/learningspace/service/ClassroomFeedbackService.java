package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.ClassroomFeedback;
import com.campus.learningspace.mapper.ClassroomFeedbackMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassroomFeedbackService extends ServiceImpl<ClassroomFeedbackMapper, ClassroomFeedback> {

    /**
     * 获取用户待评价列表
     */
    public List<ClassroomFeedback> getPendingByUser(Long userId) {
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getUserId, userId)
                .eq(ClassroomFeedback::getStatus, 1)
                .orderByDesc(ClassroomFeedback::getUsedStartTime)
                .orderByDesc(ClassroomFeedback::getCreateTime);
        return list(wrapper);
    }

    /**
     * 获取用户已评价列表
     */
    public List<ClassroomFeedback> getEvaluatedByUser(Long userId) {
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getUserId, userId)
                .eq(ClassroomFeedback::getStatus, 2)
                .orderByDesc(ClassroomFeedback::getCreateTime);
        return list(wrapper);
    }
}

