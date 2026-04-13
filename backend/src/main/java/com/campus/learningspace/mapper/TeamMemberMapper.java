package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamMemberVO;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    @Select("""
            SELECT m.id, m.team_request_id AS teamRequestId, m.user_id AS userId,
                   m.role, m.join_time AS joinTime, u.real_name AS memberName
            FROM team_member m
            LEFT JOIN `user` u ON m.user_id = u.id AND u.deleted = 0
            WHERE m.deleted = 0 AND m.team_request_id = #{teamRequestId}
            ORDER BY m.role ASC, m.join_time ASC
            """)
    List<TeamMemberVO> selectMemberVOListByTeam(@Param("teamRequestId") Long teamRequestId);

    /**
     * 查询成员关系（包含逻辑删除记录）。
     * 注意：这里用自定义 SQL 绕过 MyBatis-Plus 的逻辑删除过滤，避免 uk_team_user 冲突导致 500。
     */
    @Select("""
            SELECT *
            FROM team_member
            WHERE team_request_id = #{teamRequestId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    TeamMember selectAnyByTeamAndUser(@Param("teamRequestId") Long teamRequestId, @Param("userId") Long userId);

    /**
     * 复活逻辑删除的成员关系（绕过 MyBatis-Plus 逻辑删除 where 条件）。
     * 返回影响行数：1 表示成功复活；0 表示记录不存在或并非 deleted=1。
     */
    @Update("""
            UPDATE team_member
            SET deleted = 0,
                role = #{role},
                join_time = #{joinTime},
                update_time = NOW()
            WHERE id = #{id}
              AND deleted = 1
            """)
    int reviveDeletedMemberById(@Param("id") Long id, @Param("role") Integer role, @Param("joinTime") java.time.LocalDateTime joinTime);
}
