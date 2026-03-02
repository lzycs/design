package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Review;
import com.campus.learningspace.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

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
}

