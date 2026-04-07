package com.campus.learningspace.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.entity.ClassroomFeedback;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.service.ClassroomFeedbackService;
import com.campus.learningspace.service.ClassroomService;
import com.campus.learningspace.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin
public class ClassroomFeedbackController {

    @Autowired
    private ClassroomFeedbackService feedbackService;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private ReservationService reservationService;

    /**
     * 待评价列表
     */
    @GetMapping("/pending")
    public Result<List<PendingFeedbackVO>> getPending(@RequestParam Long userId) {
        syncPendingFromReservations(userId);
        List<ClassroomFeedback> list = feedbackService.getPendingByUser(userId);
        List<PendingFeedbackVO> result = new ArrayList<>();
        for (ClassroomFeedback fb : list) {
            PendingFeedbackVO vo = new PendingFeedbackVO();
            vo.setId(fb.getId());
            vo.setUserId(fb.getUserId());
            vo.setClassroomId(fb.getClassroomId());
            Classroom classroom = classroomService.getById(fb.getClassroomId());
            vo.setClassroomName(classroom != null ? classroom.getName() : "");
            vo.setUsedStartTime(fb.getUsedStartTime());
            vo.setUsedEndTime(fb.getUsedEndTime());
            vo.setStatus(fb.getStatus());
            result.add(vo);
        }
        return Result.success(result);
    }

    /**
     * 已评价列表（含待审核/审核通过/审核驳回）
     */
    @GetMapping
    public Result<List<FeedbackVO>> getEvaluated(@RequestParam Long userId) {
        List<ClassroomFeedback> list = feedbackService.getEvaluatedByUser(userId);
        List<FeedbackVO> result = new ArrayList<>();
        for (ClassroomFeedback fb : list) {
            result.add(toFeedbackVO(fb));
        }
        return Result.success(result);
    }

    /**
     * 教室审核通过评价列表
     */
    @GetMapping("/classroom/{classroomId}/approved")
    public Result<List<FeedbackVO>> getApprovedByClassroom(@PathVariable Long classroomId) {
        List<ClassroomFeedback> list = feedbackService.getApprovedByClassroomId(classroomId);
        List<FeedbackVO> result = new ArrayList<>();
        for (ClassroomFeedback fb : list) {
            result.add(toFeedbackVO(fb));
        }
        return Result.success(result);
    }

    /**
     * 更新/提交评价（环境评分 + 设备评分 + 内容）
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateFeedback(@PathVariable Long id, @RequestBody ClassroomFeedback payload) {
        ClassroomFeedback existing = feedbackService.getById(id);
        if (existing == null) {
            return Result.error(404, "反馈记录不存在");
        }
        Integer env = payload.getEnvScore();
        Integer equip = payload.getEquipScore();
        if (env == null || equip == null) {
            return Result.error(400, "请完成整体环境和设备设施的评分");
        }
        if (env < 1 || env > 5 || equip < 1 || equip > 5) {
            return Result.error(400, "评分范围应为 1-5");
        }
        existing.setEnvScore(env);
        existing.setEquipScore(equip);
        existing.setContent(payload.getContent());
        existing.setStatus(ClassroomFeedback.STATUS_PENDING_AUDIT);
        if (existing.getUsedEndTime() == null) {
            existing.setUsedEndTime(LocalDateTime.now());
        }
        return Result.success(feedbackService.updateById(existing));
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteFeedback(@PathVariable Long id) {
        return Result.success(feedbackService.removeById(id));
    }

    /**
     * 根据预约记录自动生成“待评价”数据
     * 规则：用户教室预约（resourceType=1）且状态为已签到/已完成（2/3），且未生成过反馈记录，则插入一条 status=1 的记录。
     */
    private void syncPendingFromReservations(Long userId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
                .eq(Reservation::getResourceType, 1)
                .in(Reservation::getStatus, 2, 3)
                .orderByDesc(Reservation::getReservationDate)
                .orderByDesc(Reservation::getStartTime);

        List<Reservation> reservations = reservationService.list(wrapper);
        for (Reservation r : reservations) {
            if (r.getId() == null || r.getClassroomId() == null) {
                continue;
            }

            LambdaQueryWrapper<ClassroomFeedback> existsWrapper = new LambdaQueryWrapper<>();
            existsWrapper.eq(ClassroomFeedback::getUserId, userId)
                    .eq(ClassroomFeedback::getReservationId, r.getId());
            if (feedbackService.count(existsWrapper) > 0) {
                continue;
            }

            ClassroomFeedback fb = new ClassroomFeedback();
            fb.setUserId(userId);
            fb.setClassroomId(r.getClassroomId());
            fb.setReservationId(r.getId());
            fb.setStatus(ClassroomFeedback.STATUS_PENDING_EVALUATION);

            LocalDate date = r.getReservationDate();
            LocalTime start = r.getStartTime();
            LocalTime end = r.getEndTime();
            if (date != null && start != null) {
                fb.setUsedStartTime(LocalDateTime.of(date, start));
            }
            if (date != null && end != null) {
                fb.setUsedEndTime(LocalDateTime.of(date, end));
            }

            feedbackService.save(fb);
        }
    }

    private FeedbackVO toFeedbackVO(ClassroomFeedback fb) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(fb.getId());
        vo.setUserId(fb.getUserId());
        vo.setClassroomId(fb.getClassroomId());
        Classroom classroom = classroomService.getById(fb.getClassroomId());
        vo.setClassroomName(classroom != null ? classroom.getName() : "");
        vo.setEnvScore(fb.getEnvScore() != null ? fb.getEnvScore() : 0);
        vo.setEquipScore(fb.getEquipScore() != null ? fb.getEquipScore() : 0);
        vo.setContent(fb.getContent());
        vo.setStatus(fb.getStatus());
        vo.setCreatedAt(fb.getCreateTime());
        vo.setUpdatedAt(fb.getUpdateTime());
        vo.setAverageScore(calcAverageScore(fb.getEnvScore(), fb.getEquipScore()));
        return vo;
    }

    private BigDecimal calcAverageScore(Integer envScore, Integer equipScore) {
        if (envScore == null && equipScore == null) {
            return null;
        }
        int total = 0;
        int count = 0;
        if (envScore != null) {
            total += envScore;
            count++;
        }
        if (equipScore != null) {
            total += equipScore;
            count++;
        }
        return count == 0 ? null : BigDecimal.valueOf((double) total / count).setScale(1, RoundingMode.HALF_UP);
    }

    // ================== VO 定义 ==================

    public static class PendingFeedbackVO {
        private Long id;
        private Long userId;
        private Long classroomId;
        private String classroomName;
        private LocalDateTime usedStartTime;
        private LocalDateTime usedEndTime;
        private Integer status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getClassroomId() {
            return classroomId;
        }

        public void setClassroomId(Long classroomId) {
            this.classroomId = classroomId;
        }

        public String getClassroomName() {
            return classroomName;
        }

        public void setClassroomName(String classroomName) {
            this.classroomName = classroomName;
        }

        public LocalDateTime getUsedStartTime() {
            return usedStartTime;
        }

        public void setUsedStartTime(LocalDateTime usedStartTime) {
            this.usedStartTime = usedStartTime;
        }

        public LocalDateTime getUsedEndTime() {
            return usedEndTime;
        }

        public void setUsedEndTime(LocalDateTime usedEndTime) {
            this.usedEndTime = usedEndTime;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    public static class FeedbackVO {
        private Long id;
        private Long userId;
        private Long classroomId;
        private String classroomName;
        private Integer envScore;
        private Integer equipScore;
        private String content;
        private Integer status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private BigDecimal averageScore;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getClassroomId() {
            return classroomId;
        }

        public void setClassroomId(Long classroomId) {
            this.classroomId = classroomId;
        }

        public String getClassroomName() {
            return classroomName;
        }

        public void setClassroomName(String classroomName) {
            this.classroomName = classroomName;
        }

        public Integer getEnvScore() {
            return envScore;
        }

        public void setEnvScore(Integer envScore) {
            this.envScore = envScore;
        }

        public Integer getEquipScore() {
            return equipScore;
        }

        public void setEquipScore(Integer equipScore) {
            this.equipScore = equipScore;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public BigDecimal getAverageScore() {
            return averageScore;
        }

        public void setAverageScore(BigDecimal averageScore) {
            this.averageScore = averageScore;
        }
    }
}
