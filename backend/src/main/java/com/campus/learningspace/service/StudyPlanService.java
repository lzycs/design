package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.common.ReservationRuleException;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.dto.CreateStudyPlanDTO;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.entity.StudyPlan;
import com.campus.learningspace.entity.StudyPlanVO;
import com.campus.learningspace.mapper.StudyPlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class StudyPlanService extends ServiceImpl<StudyPlanMapper, StudyPlan> {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ClassroomService classroomService;

    public List<StudyPlan> getUserPlans(Long userId) {
        LambdaQueryWrapper<StudyPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyPlan::getUserId, userId);
        return list(wrapper);
    }

    /** 共享学习计划：用户参与的小组内的所有计划 */
    public List<StudyPlanVO> getSharedPlans(Long userId) {
        return baseMapper.selectSharedPlans(userId);
    }

    /** 按小组查询学习计划 */
    public List<StudyPlanVO> getPlansByTeam(Long teamRequestId) {
        return baseMapper.selectPlansByTeam(teamRequestId);
    }

    /** 按预约查询学习计划 */
    public List<StudyPlanVO> getPlansByReservation(Long reservationId) {
        return baseMapper.selectPlansByReservation(reservationId);
    }

    /**
     * 创建学习计划，可选同时创建研讨室预约并关联
     */
    @Transactional(rollbackFor = Exception.class)
    public StudyPlan createWithReservation(CreateStudyPlanDTO dto) {
        Long reservationId = null;
        if (dto.getClassroomId() != null && dto.getReservationDate() != null
                && dto.getResStartTime() != null && dto.getResEndTime() != null
                && dto.getUserId() != null) {
            Reservation r = buildSeminarReservation(dto);
            reservationService.save(r);
            reservationId = r.getId();
        }

        StudyPlan plan = new StudyPlan();
        plan.setTeamRequestId(dto.getTeamRequestId());
        plan.setUserId(dto.getUserId());
        plan.setTitle(dto.getTitle());
        plan.setDescription(dto.getDescription());
        plan.setPlanDate(dto.getPlanDate() != null ? LocalDate.parse(dto.getPlanDate()) : null);
        if (dto.getStartTime() != null) {
            String st = dto.getStartTime().length() > 5 ? dto.getStartTime() : dto.getStartTime() + ":00";
            plan.setStartTime(LocalTime.parse(st));
        }
        if (dto.getEndTime() != null) {
            String et = dto.getEndTime().length() > 5 ? dto.getEndTime() : dto.getEndTime() + ":00";
            plan.setEndTime(LocalTime.parse(et));
        }
        plan.setReservationId(reservationId);
        plan.setKeyTimeNodes(dto.getKeyTimeNodes());
        plan.setStatus(1);
        save(plan);
        return plan;
    }

    /**
     * 更新学习计划（关联研讨室等），可选创建新预约并关联
     */
    @Transactional(rollbackFor = Exception.class)
    public StudyPlan updateWithReservation(Long id, CreateStudyPlanDTO dto) {
        StudyPlan plan = getById(id);
        if (plan == null) return null;

        Long reservationId = plan.getReservationId();
        if (dto.getClassroomId() != null && dto.getReservationDate() != null
                && dto.getResStartTime() != null && dto.getResEndTime() != null
                && dto.getUserId() != null) {
            Reservation r = buildSeminarReservation(dto);
            reservationService.save(r);
            reservationId = r.getId();
        }

        if (dto.getDescription() != null) plan.setDescription(dto.getDescription());
        if (dto.getPlanDate() != null) plan.setPlanDate(LocalDate.parse(dto.getPlanDate()));
        if (dto.getStartTime() != null) {
            String st = dto.getStartTime().length() > 5 ? dto.getStartTime() : dto.getStartTime() + ":00";
            plan.setStartTime(LocalTime.parse(st));
        }
        if (dto.getEndTime() != null) {
            String et = dto.getEndTime().length() > 5 ? dto.getEndTime() : dto.getEndTime() + ":00";
            plan.setEndTime(LocalTime.parse(et));
        }
        if (dto.getKeyTimeNodes() != null) plan.setKeyTimeNodes(dto.getKeyTimeNodes());
        plan.setReservationId(reservationId);
        updateById(plan);
        return plan;
    }

    /**
     * 学习计划模块下的研讨室预约约束：
     * 1) 仅允许预约研讨室(type=2)
     * 2) 仅允许今天、明天、后天（未来三天内）
     * 3) 同一研讨室同一时段禁止重复预约
     */
    private Reservation buildSeminarReservation(CreateStudyPlanDTO dto) {
        Classroom classroom = classroomService.getById(dto.getClassroomId());
        if (classroom == null || classroom.getStatus() == null || classroom.getStatus() != 1) {
            throw new ReservationRuleException("研讨室不存在或不可预约");
        }
        if (classroom.getType() == null || classroom.getType() != 2) {
            throw new ReservationRuleException("学习计划仅支持预约研讨室");
        }

        LocalDate reservationDate = LocalDate.parse(dto.getReservationDate());
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(2);
        if (reservationDate.isBefore(today) || reservationDate.isAfter(maxDate)) {
            throw new ReservationRuleException("仅支持预约未来三天内的研讨室时段");
        }

        String st = dto.getResStartTime().length() > 5 ? dto.getResStartTime() : dto.getResStartTime() + ":00";
        String et = dto.getResEndTime().length() > 5 ? dto.getResEndTime() : dto.getResEndTime() + ":00";
        LocalTime start = LocalTime.parse(st);
        LocalTime end = LocalTime.parse(et);
        if (!end.isAfter(start)) {
            throw new ReservationRuleException("预约结束时间必须晚于开始时间");
        }

        boolean hasConflict = reservationService.checkTimeConflict(dto.getClassroomId(), reservationDate, start, end, null);
        if (hasConflict) {
            throw new ReservationRuleException("该研讨室时段已被占用，请选择其他时段");
        }

        Reservation r = new Reservation();
        r.setUserId(dto.getUserId());
        r.setResourceType(1);
        r.setClassroomId(dto.getClassroomId());
        r.setReservationDate(reservationDate);
        r.setStartTime(start);
        r.setEndTime(end);
        int dur = end.toSecondOfDay() / 60 - start.toSecondOfDay() / 60;
        r.setDuration(dur);
        r.setPurpose("共享学习计划-研讨室");
        // 待扫码确认，扫码成功后状态会变为已签到（预约成功）
        r.setStatus(1);
        return r;
    }
}

