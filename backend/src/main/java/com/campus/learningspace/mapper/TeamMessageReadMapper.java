package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.TeamMessageRead;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TeamMessageReadMapper extends BaseMapper<TeamMessageRead> {

    @Select("""
            SELECT *
            FROM team_message_read
            WHERE user_id = #{userId}
              AND deleted = 0
            """)
    List<TeamMessageRead> selectByUserId(@Param("userId") Long userId);
}
