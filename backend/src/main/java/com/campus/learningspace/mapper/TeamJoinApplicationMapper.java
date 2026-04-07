package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.TeamJoinApplication;
import com.campus.learningspace.entity.TeamJoinApplicationVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TeamJoinApplicationMapper extends BaseMapper<TeamJoinApplication> {

    /**
     * 查询某个小组的待审核申请列表（含申请人姓名、小组标题）
     */
    @Select("""
            SELECT a.id, a.team_request_id AS teamRequestId, a.applicant_id AS applicantId,
                   a.reason, a.status, a.create_time AS createTime,
                   a.reviewer_id AS reviewerId, a.review_time AS reviewTime, a.reject_reason AS rejectReason,
                   u.real_name AS applicantName, u.student_id AS applicantStudentId,
                   ur.real_name AS reviewerName,
                   t.title AS teamTitle
            FROM team_join_application a
            LEFT JOIN `user` u ON a.applicant_id = u.id AND u.deleted = 0
            LEFT JOIN `user` ur ON a.reviewer_id = ur.id AND ur.deleted = 0
            LEFT JOIN team_request t ON a.team_request_id = t.id AND t.deleted = 0
            WHERE a.deleted = 0 AND a.team_request_id = #{teamRequestId} AND a.status = 0
            ORDER BY a.create_time ASC
            """)
    List<TeamJoinApplicationVO> selectPendingByTeam(@Param("teamRequestId") Long teamRequestId);

    /**
     * 查询某用户作为组长的所有待审核申请（跨小组）
     */
    @Select("""
            SELECT a.id, a.team_request_id AS teamRequestId, a.applicant_id AS applicantId,
                   a.reason, a.status, a.create_time AS createTime,
                   a.reviewer_id AS reviewerId, a.review_time AS reviewTime, a.reject_reason AS rejectReason,
                   u.real_name AS applicantName, u.student_id AS applicantStudentId,
                   ur.real_name AS reviewerName,
                   t.title AS teamTitle
            FROM team_join_application a
            INNER JOIN team_member m ON a.team_request_id = m.team_request_id
                AND m.user_id = #{leaderId} AND m.role = 1 AND m.deleted = 0
            LEFT JOIN `user` u ON a.applicant_id = u.id AND u.deleted = 0
            LEFT JOIN `user` ur ON a.reviewer_id = ur.id AND ur.deleted = 0
            LEFT JOIN team_request t ON a.team_request_id = t.id AND t.deleted = 0
            WHERE a.deleted = 0 AND a.status = 0
            ORDER BY a.create_time ASC
            """)
    List<TeamJoinApplicationVO> selectPendingByLeader(@Param("leaderId") Long leaderId);

    /**
     * 查询某用户提交的申请结果（已通过/已拒绝），用于成员侧红点
     */
    @Select("""
            SELECT a.id, a.team_request_id AS teamRequestId, a.applicant_id AS applicantId,
                   a.reason, a.status, a.create_time AS createTime,
                   a.reviewer_id AS reviewerId, a.review_time AS reviewTime, a.reject_reason AS rejectReason,
                   u.real_name AS applicantName, u.student_id AS applicantStudentId,
                   ur.real_name AS reviewerName,
                   t.title AS teamTitle
            FROM team_join_application a
            LEFT JOIN `user` u ON a.applicant_id = u.id AND u.deleted = 0
            LEFT JOIN `user` ur ON a.reviewer_id = ur.id AND ur.deleted = 0
            LEFT JOIN team_request t ON a.team_request_id = t.id AND t.deleted = 0
            WHERE a.deleted = 0 AND a.applicant_id = #{applicantId} AND a.status IN (1, 2)
            ORDER BY a.create_time DESC
            """)
    List<TeamJoinApplicationVO> selectResultsByApplicant(@Param("applicantId") Long applicantId);

    @Select("""
            SELECT a.id, a.team_request_id AS teamRequestId, a.applicant_id AS applicantId,
                   a.reason, a.status, a.create_time AS createTime,
                   a.reviewer_id AS reviewerId, a.review_time AS reviewTime, a.reject_reason AS rejectReason,
                   u.real_name AS applicantName, u.student_id AS applicantStudentId,
                   ur.real_name AS reviewerName,
                   t.title AS teamTitle
            FROM team_join_application a
            LEFT JOIN `user` u ON a.applicant_id = u.id AND u.deleted = 0
            LEFT JOIN `user` ur ON a.reviewer_id = ur.id AND ur.deleted = 0
            LEFT JOIN team_request t ON a.team_request_id = t.id AND t.deleted = 0
            WHERE a.deleted = 0 AND a.applicant_id = #{applicantId} AND a.status = 0
            ORDER BY a.create_time DESC
            """)
    List<TeamJoinApplicationVO> selectPendingByApplicant(@Param("applicantId") Long applicantId);

    /**
     * 统计组长有多少待审核申请
     */
    @Select("""
            SELECT COUNT(*)
            FROM team_join_application a
            INNER JOIN team_member m ON a.team_request_id = m.team_request_id
                AND m.user_id = #{leaderId} AND m.role = 1 AND m.deleted = 0
            WHERE a.deleted = 0 AND a.status = 0
            """)
    int countPendingForLeader(@Param("leaderId") Long leaderId);

    /**
     * 统计某用户有多少审核结果未读（已通过或已拒绝）
     */
    @Select("""
            SELECT COUNT(*)
            FROM team_join_application a
            WHERE a.deleted = 0 AND a.applicant_id = #{applicantId} AND a.status IN (1, 2)
            """)
    int countResultsForApplicant(@Param("applicantId") Long applicantId);
}
