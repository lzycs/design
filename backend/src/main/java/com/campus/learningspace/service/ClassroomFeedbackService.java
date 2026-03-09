package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.ClassroomFeedback;
import com.campus.learningspace.mapper.ClassroomFeedbackMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * 计算某教室的综合评分（环境 + 设备的平均值），无评价时返回 null
     */
    public BigDecimal getAverageScoreByClassroomId(Long classroomId) {
        if (classroomId == null) {
            return null;
        }
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getClassroomId, classroomId)
                .eq(ClassroomFeedback::getStatus, 2);
        List<ClassroomFeedback> list = list(wrapper);
        if (list == null || list.isEmpty()) {
            return null;
        }
        int scoreSum = 0;
        int scoreCount = 0;
        for (ClassroomFeedback fb : list) {
            if (fb.getEnvScore() != null) {
                scoreSum += fb.getEnvScore();
                scoreCount++;
            }
            if (fb.getEquipScore() != null) {
                scoreSum += fb.getEquipScore();
                scoreCount++;
            }
        }
        if (scoreCount == 0) {
            return null;
        }
        return BigDecimal.valueOf((double) scoreSum / scoreCount).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 统计某教室的评价条数（已评价）
     */
    public int countByClassroomId(Long classroomId) {
        if (classroomId == null) {
            return 0;
        }
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getClassroomId, classroomId)
                .eq(ClassroomFeedback::getStatus, 2);
        return (int) count(wrapper);
    }
}

