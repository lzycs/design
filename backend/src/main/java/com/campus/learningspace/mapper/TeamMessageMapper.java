package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.TeamMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TeamMessageMapper extends BaseMapper<TeamMessage> {
    @Select("""
            SELECT id, team_request_id AS teamRequestId, sender_id AS senderId, sender_name AS senderName,
                   type, content, file_name AS fileName, file_size AS fileSize, client_msg_id AS clientMsgId,
                   status, recalled, create_time AS createTime, update_time AS updateTime, deleted
            FROM team_message
            WHERE team_request_id = #{teamRequestId}
              AND deleted = 0
              AND (#{beforeId} IS NULL OR id < #{beforeId})
            ORDER BY id DESC
            LIMIT #{size}
            """)
    java.util.List<TeamMessage> selectHistory(@Param("teamRequestId") Long teamRequestId,
                                              @Param("beforeId") Long beforeId,
                                              @Param("size") Integer size);

    @Select("""
            SELECT COUNT(*)
            FROM team_message
            WHERE team_request_id = #{teamRequestId}
              AND deleted = 0
              AND id > #{lastReadMessageId}
            """)
    Long countUnreadByTeam(@Param("teamRequestId") Long teamRequestId,
                           @Param("lastReadMessageId") Long lastReadMessageId);
}

