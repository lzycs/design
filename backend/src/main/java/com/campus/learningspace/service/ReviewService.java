package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Review;
import com.campus.learningspace.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReviewService extends ServiceImpl<ReviewMapper, Review> {

    public List<Review> getUserReviews(Long userId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId)
                .orderByDesc(Review::getCreateTime);
        return list(wrapper);
    }

    public List<Review> getClassroomReviews(Long classroomId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getClassroomId, classroomId)
                .eq(Review::getStatus, 1); // 已展示
        return list(wrapper);
    }

    /** 该教室历史评分的平均值（综合评分），无评价时返回 null */
    public BigDecimal getAverageScoreByClassroomId(Long classroomId) {
        if (classroomId == null) return null;
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<>();
        w.eq(Review::getClassroomId, classroomId).eq(Review::getStatus, 1);
        List<Review> list = list(w);
        if (list == null || list.isEmpty()) return null;
        double sum = 0;
        for (Review r : list) {
            if (r.getScore() != null) sum += r.getScore();
        }
        return BigDecimal.valueOf(sum / list.size()).setScale(2, RoundingMode.HALF_UP);
    }

    /** 该教室历史评价条数 */
    public int countByClassroomId(Long classroomId) {
        if (classroomId == null) return 0;
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<>();
        w.eq(Review::getClassroomId, classroomId).eq(Review::getStatus, 1);
        return (int) count(w);
    }
}

