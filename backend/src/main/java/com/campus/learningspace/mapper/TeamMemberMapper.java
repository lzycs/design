package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamMemberVO;
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
}
