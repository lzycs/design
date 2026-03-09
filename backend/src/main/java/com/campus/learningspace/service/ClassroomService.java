package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.entity.ClassroomDetailVO;
import com.campus.learningspace.mapper.ClassroomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassroomService extends ServiceImpl<ClassroomMapper, Classroom> {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ClassroomFeedbackService classroomFeedbackService;

    public List<Classroom> getClassroomsByBuilding(Long buildingId) {
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Classroom::getBuildingId, buildingId)
                .eq(Classroom::getStatus, 1);
        return list(wrapper);
    }

    public List<Classroom> getAvailableClassrooms(Integer type) {
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Classroom::getStatus, 1);
        if (type != null) {
            wrapper.eq(Classroom::getType, type);
        }
        return list(wrapper);
    }

    /**
     * 教室详情：含热度星级（按签到次数排名前20%为5星，依次降序）、综合评分（历史评分均值）
     */
    public ClassroomDetailVO getDetail(Long classroomId) {
        Classroom classroom = getById(classroomId);
        if (classroom == null) return null;

        ClassroomDetailVO vo = new ClassroomDetailVO();
        vo.setClassroom(classroom);

        long checkinCount = reservationService.countCheckinByClassroomId(classroomId);
        vo.setCheckinCount(checkinCount);

        List<Classroom> all = list(new LambdaQueryWrapper<Classroom>().eq(Classroom::getStatus, 1));
        List<Long> sortedByCheckin = all.stream()
                .map(Classroom::getId)
                .sorted(Comparator.<Long>comparingLong(id -> reservationService.countCheckinByClassroomId(id)).reversed())
                .collect(Collectors.toList());
        int n = sortedByCheckin.size();
        int rank = 1;
        for (int i = 0; i < n; i++) {
            if (sortedByCheckin.get(i).equals(classroomId)) {
                rank = i + 1;
                break;
            }
        }
        int stars = 5;
        if (n > 0) {
            double percentile = (rank - 1) * 1.0 / n;
            int level = (int) (percentile * 5);
            stars = Math.max(1, 5 - level);
        }
        vo.setPopularityStars(stars);

        java.math.BigDecimal avg = classroomFeedbackService.getAverageScoreByClassroomId(classroomId);
        vo.setAverageScore(avg);
        vo.setTotalReviews(classroomFeedbackService.countByClassroomId(classroomId));

        return vo;
    }
}
