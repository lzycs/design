package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
            Reservation r = new Reservation();
            r.setUserId(dto.getUserId());
            r.setResourceType(1);
            r.setClassroomId(dto.getClassroomId());
            r.setReservationDate(LocalDate.parse(dto.getReservationDate()));
            String st = dto.getResStartTime().length() > 5 ? dto.getResStartTime() : dto.getResStartTime() + ":00";
            String et = dto.getResEndTime().length() > 5 ? dto.getResEndTime() : dto.getResEndTime() + ":00";
            r.setStartTime(LocalTime.parse(st));
            r.setEndTime(LocalTime.parse(et));
            int dur = r.getEndTime().toSecondOfDay() / 60 - r.getStartTime().toSecondOfDay() / 60;
            r.setDuration(dur);
            r.setPurpose("共享学习计划-研讨室");
            r.setStatus(1);
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
            Reservation r = new Reservation();
            r.setUserId(dto.getUserId());
            r.setResourceType(1);
            r.setClassroomId(dto.getClassroomId());
            r.setReservationDate(LocalDate.parse(dto.getReservationDate()));
            String st = dto.getResStartTime().length() > 5 ? dto.getResStartTime() : dto.getResStartTime() + ":00";
            String et = dto.getResEndTime().length() > 5 ? dto.getResEndTime() : dto.getResEndTime() + ":00";
            r.setStartTime(LocalTime.parse(st));
            r.setEndTime(LocalTime.parse(et));
            int dur = r.getEndTime().toSecondOfDay() / 60 - r.getStartTime().toSecondOfDay() / 60;
            r.setDuration(dur);
            r.setPurpose("共享学习计划-研讨室");
            r.setStatus(1);
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
}

