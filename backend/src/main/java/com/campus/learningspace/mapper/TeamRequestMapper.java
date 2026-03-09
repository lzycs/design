package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.entity.TeamRequestVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TeamRequestMapper extends BaseMapper<TeamRequest> {

    /**
     * 查询用户参与的所有协作（含发起人姓名），用于「我的协作」列表
     */
    @Select("""
            SELECT t.id, t.user_id AS userId, t.title, t.description, t.tags,
                   t.expected_count AS expectedCount, t.current_count AS currentCount,
                   t.start_time AS startTime, t.end_time AS endTime,
                   t.status, t.create_time AS createTime, t.update_time AS updateTime,
                   u.real_name AS creatorName
            FROM team_request t
            INNER JOIN team_member m ON t.id = m.team_request_id AND m.deleted = 0 AND m.user_id = #{userId}
            LEFT JOIN `user` u ON t.user_id = u.id AND u.deleted = 0
            WHERE t.deleted = 0
            ORDER BY t.update_time DESC
            """)
    List<TeamRequestVO> selectUserTeamVOList(@Param("userId") Long userId);

    @Select("""
            SELECT t.id, t.user_id AS userId, t.title, t.description, t.tags,
                   t.expected_count AS expectedCount, t.current_count AS currentCount,
                   t.start_time AS startTime, t.end_time AS endTime,
                   t.status, t.create_time AS createTime, t.update_time AS updateTime,
                   u.real_name AS creatorName
            FROM team_request t
            LEFT JOIN `user` u ON t.user_id = u.id AND u.deleted = 0
            WHERE t.deleted = 0 AND t.id = #{id}
            """)
    TeamRequestVO selectTeamRequestVOById(@Param("id") Long id);

    /**
     * 协作广场：查询可展示的组队（招募中 + 已满员），含发起人姓名
     */
    @Select("""
            SELECT t.id, t.user_id AS userId, t.title, t.description, t.tags,
                   t.expected_count AS expectedCount, t.current_count AS currentCount,
                   t.start_time AS startTime, t.end_time AS endTime,
                   t.status, t.create_time AS createTime, t.update_time AS updateTime,
                   u.real_name AS creatorName
            FROM team_request t
            LEFT JOIN `user` u ON t.user_id = u.id AND u.deleted = 0
            WHERE t.deleted = 0 AND t.status IN (1, 2)
            ORDER BY t.create_time DESC
            """)
    List<TeamRequestVO> selectActiveTeamVOList();
}
