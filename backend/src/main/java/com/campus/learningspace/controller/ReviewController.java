package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Review;
import com.campus.learningspace.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@CrossOrigin
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/{id}")
    public Result<Review> getById(@PathVariable Long id) {
        return Result.success(reviewService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public Result<List<Review>> getUserReviews(@PathVariable Long userId) {
        return Result.success(reviewService.getUserReviews(userId));
    }

    @GetMapping("/classroom/{classroomId}")
    public Result<List<Review>> getClassroomReviews(@PathVariable Long classroomId) {
        return Result.success(reviewService.getClassroomReviews(classroomId));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody Review review) {
        review.setId(null);
        if (review.getStatus() == null) {
            review.setStatus(1); // 默认直接展示
        }
        return Result.success(reviewService.save(review));
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody Review payload) {
        Review existing = reviewService.getById(id);
        if (existing == null) {
            return Result.error(404, "评价不存在");
        }
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus());
        }
        return Result.success(reviewService.updateById(existing));
    }
}

