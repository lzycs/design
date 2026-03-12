package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.StudyPlan;
import com.campus.learningspace.entity.StudyPlanVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StudyPlanMapper extends BaseMapper<StudyPlan> {

    /**
     * 共享学习计划：用户参与的小组内的所有计划（含小组名、研讨室名）
     */
    @Select("""
            SELECT sp.id, sp.team_request_id AS teamRequestId, sp.user_id AS userId,
                   sp.reservation_id AS reservationId, sp.title, sp.description,
                   sp.plan_date AS planDate, sp.start_time AS startTime, sp.end_time AS endTime,
                   sp.key_time_nodes AS keyTimeNodes, sp.status, sp.create_time AS createTime, sp.update_time AS updateTime,
                   tr.title AS teamTitle,
                   c.name AS classroomName,
                   r.reservation_date AS reservationDate, r.start_time AS reservationStartTime, r.end_time AS reservationEndTime
            FROM study_plan sp
            INNER JOIN team_member tm ON sp.team_request_id = tm.team_request_id AND tm.deleted = 0 AND tm.user_id = #{userId}
            LEFT JOIN team_request tr ON sp.team_request_id = tr.id AND tr.deleted = 0
            LEFT JOIN reservation r ON sp.reservation_id = r.id AND r.deleted = 0
            LEFT JOIN classroom c ON r.classroom_id = c.id AND c.deleted = 0
            WHERE sp.deleted = 0
            ORDER BY sp.plan_date DESC, sp.start_time DESC
            """)
    List<StudyPlanVO> selectSharedPlans(@Param("userId") Long userId);

    /**
     * 按小组查询学习计划（含研讨室信息、关键节点）
     */
    @Select("""
            SELECT sp.id, sp.team_request_id AS teamRequestId, sp.user_id AS userId,
                   sp.reservation_id AS reservationId, sp.title, sp.description,
                   sp.plan_date AS planDate, sp.start_time AS startTime, sp.end_time AS endTime,
                   sp.key_time_nodes AS keyTimeNodes, sp.status, sp.create_time AS createTime, sp.update_time AS updateTime,
                   tr.title AS teamTitle,
                   c.name AS classroomName,
                   r.reservation_date AS reservationDate, r.start_time AS reservationStartTime, r.end_time AS reservationEndTime
            FROM study_plan sp
            LEFT JOIN team_request tr ON sp.team_request_id = tr.id AND tr.deleted = 0
            LEFT JOIN reservation r ON sp.reservation_id = r.id AND r.deleted = 0
            LEFT JOIN classroom c ON r.classroom_id = c.id AND c.deleted = 0
            WHERE sp.deleted = 0 AND sp.team_request_id = #{teamRequestId}
            ORDER BY sp.plan_date DESC, sp.start_time DESC
            """)
    List<StudyPlanVO> selectPlansByTeam(@Param("teamRequestId") Long teamRequestId);

    /**
     * 按预约查询学习计划（一个预约通常关联一个学习计划）
     */
    @Select("""
            SELECT sp.id, sp.team_request_id AS teamRequestId, sp.user_id AS userId,
                   sp.reservation_id AS reservationId, sp.title, sp.description,
                   sp.plan_date AS planDate, sp.start_time AS startTime, sp.end_time AS endTime,
                   sp.key_time_nodes AS keyTimeNodes, sp.status, sp.create_time AS createTime, sp.update_time AS updateTime,
                   tr.title AS teamTitle,
                   c.name AS classroomName,
                   r.reservation_date AS reservationDate, r.start_time AS reservationStartTime, r.end_time AS reservationEndTime
            FROM study_plan sp
            LEFT JOIN team_request tr ON sp.team_request_id = tr.id AND tr.deleted = 0
            LEFT JOIN reservation r ON sp.reservation_id = r.id AND r.deleted = 0
            LEFT JOIN classroom c ON r.classroom_id = c.id AND c.deleted = 0
            WHERE sp.deleted = 0 AND sp.reservation_id = #{reservationId}
            ORDER BY sp.plan_date DESC, sp.start_time DESC
            """)
    List<StudyPlanVO> selectPlansByReservation(@Param("reservationId") Long reservationId);
}
