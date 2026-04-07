package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.AdminReviewVO;
import com.campus.learningspace.entity.ClassroomFeedback;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.entity.User;
import com.campus.learningspace.mapper.ClassroomFeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClassroomFeedbackService extends ServiceImpl<ClassroomFeedbackMapper, ClassroomFeedback> {

    @Autowired
    @Lazy
    private ClassroomService classroomService;

    @Autowired
    private UserService userService;

    /**
     * 获取用户待评价列表
     */
    public List<ClassroomFeedback> getPendingByUser(Long userId) {
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getUserId, userId)
                .eq(ClassroomFeedback::getStatus, ClassroomFeedback.STATUS_PENDING_EVALUATION)
                .orderByDesc(ClassroomFeedback::getUsedStartTime)
                .orderByDesc(ClassroomFeedback::getCreateTime);
        return list(wrapper);
    }

    /**
     * 获取用户已评价列表（含待审核/已通过/已驳回）
     */
    public List<ClassroomFeedback> getEvaluatedByUser(Long userId) {
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getUserId, userId)
                .in(ClassroomFeedback::getStatus,
                        ClassroomFeedback.STATUS_PENDING_AUDIT,
                        ClassroomFeedback.STATUS_APPROVED,
                        ClassroomFeedback.STATUS_REJECTED)
                .orderByDesc(ClassroomFeedback::getUpdateTime)
                .orderByDesc(ClassroomFeedback::getCreateTime);
        return list(wrapper);
    }

    /**
     * 获取教室审核通过的评价列表
     */
    public List<ClassroomFeedback> getApprovedByClassroomId(Long classroomId) {
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getClassroomId, classroomId)
                .eq(ClassroomFeedback::getStatus, ClassroomFeedback.STATUS_APPROVED)
                .orderByDesc(ClassroomFeedback::getUpdateTime)
                .orderByDesc(ClassroomFeedback::getCreateTime);
        return list(wrapper);
    }

    /**
     * 管理端评价审核列表
     */
    public List<AdminReviewVO> getAdminReviewList() {
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ClassroomFeedback::getStatus,
                        ClassroomFeedback.STATUS_PENDING_AUDIT,
                        ClassroomFeedback.STATUS_APPROVED,
                        ClassroomFeedback.STATUS_REJECTED)
                .orderByDesc(ClassroomFeedback::getUpdateTime)
                .orderByDesc(ClassroomFeedback::getCreateTime);
        List<ClassroomFeedback> list = list(wrapper);
        List<AdminReviewVO> result = new ArrayList<>();
        for (ClassroomFeedback feedback : list) {
            AdminReviewVO vo = new AdminReviewVO();
            vo.setId(feedback.getId());
            vo.setUserId(feedback.getUserId());
            User user = feedback.getUserId() == null ? null : userService.getById(feedback.getUserId());
            vo.setReviewerName(user != null ? user.getRealName() : "");
            vo.setClassroomId(feedback.getClassroomId());
            Classroom classroom = feedback.getClassroomId() == null ? null : classroomService.getById(feedback.getClassroomId());
            vo.setLocation(classroom != null ? classroom.getName() : "");
            vo.setScore(calcAverageScore(feedback));
            vo.setContent(feedback.getContent());
            vo.setStatus(toAdminStatus(feedback.getStatus()));
            vo.setCreateTime(feedback.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 计算某教室的综合评分（环境 + 设备的平均值），无评价时返回 null
     */
    public BigDecimal getAverageScoreByClassroomId(Long classroomId) {
        if (classroomId == null) {
            return null;
        }
        List<ClassroomFeedback> list = getApprovedByClassroomId(classroomId);
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
     * 统计某教室审核通过的评价条数
     */
    public int countByClassroomId(Long classroomId) {
        if (classroomId == null) {
            return 0;
        }
        LambdaQueryWrapper<ClassroomFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassroomFeedback::getClassroomId, classroomId)
                .eq(ClassroomFeedback::getStatus, ClassroomFeedback.STATUS_APPROVED);
        return (int) count(wrapper);
    }

    private Integer calcAverageScore(ClassroomFeedback feedback) {
        int total = 0;
        int count = 0;
        if (feedback.getEnvScore() != null) {
            total += feedback.getEnvScore();
            count++;
        }
        if (feedback.getEquipScore() != null) {
            total += feedback.getEquipScore();
            count++;
        }
        if (count == 0) {
            return null;
        }
        return BigDecimal.valueOf((double) total / count).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private Integer toAdminStatus(Integer feedbackStatus) {
        if (feedbackStatus == null) {
            return null;
        }
        return switch (feedbackStatus) {
            case ClassroomFeedback.STATUS_PENDING_AUDIT -> 0;
            case ClassroomFeedback.STATUS_APPROVED -> 1;
            case ClassroomFeedback.STATUS_REJECTED -> 2;
            default -> null;
        };
    }
}
