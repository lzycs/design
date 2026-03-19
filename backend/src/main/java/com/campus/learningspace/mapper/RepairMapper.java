package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.Repair;
import com.campus.learningspace.entity.RepairVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepairMapper extends BaseMapper<Repair> {

    @Select("""
            SELECT
              r.id,
              r.user_id       AS userId,
              r.classroom_id  AS classroomId,
              r.title,
              r.description,
              r.type,
              r.priority,
              r.status,
              r.create_time   AS createTime,
              r.handle_time   AS handleTime,
              r.handle_remark AS handleRemark,
              r.handler_id    AS handlerId,
              CONCAT(IFNULL(b.name, ''), '-', IFNULL(c.room_number, '')) AS location,
              hu.real_name    AS handlerName,
              hu.phone        AS handlerPhone
            FROM repair r
            LEFT JOIN classroom c ON r.classroom_id = c.id AND c.deleted = 0
            LEFT JOIN building b ON c.building_id = b.id AND b.deleted = 0
            LEFT JOIN `user` hu ON r.handler_id = hu.id AND hu.deleted = 0
            WHERE r.deleted = 0 AND r.user_id = #{userId}
            ORDER BY r.create_time DESC
            """)
    List<RepairVO> selectUserRepairVOList(@Param("userId") Long userId);

    @Select("""
            SELECT
              r.id,
              r.user_id       AS userId,
              r.classroom_id  AS classroomId,
              r.title,
              r.description,
              r.type,
              r.priority,
              r.status,
              r.create_time   AS createTime,
              r.handle_time   AS handleTime,
              r.handle_remark AS handleRemark,
              r.handler_id    AS handlerId,
              CONCAT(IFNULL(b.name, ''), '-', IFNULL(c.room_number, '')) AS location,
              hu.real_name    AS handlerName,
              hu.phone        AS handlerPhone
            FROM repair r
            LEFT JOIN classroom c ON r.classroom_id = c.id AND c.deleted = 0
            LEFT JOIN building b ON c.building_id = b.id AND b.deleted = 0
            LEFT JOIN `user` hu ON r.handler_id = hu.id AND hu.deleted = 0
            WHERE r.deleted = 0 AND r.id = #{id}
            """)
    RepairVO selectRepairVOById(@Param("id") Long id);

    @Select("""
        SELECT
          r.id,
          r.user_id       AS userId,
          ru.real_name    AS requesterName,
          ru.phone        AS requesterPhone,
          r.classroom_id  AS classroomId,
          r.title,
          r.description,
          r.type,
          r.priority,
          r.status,
          r.create_time   AS createTime,
          r.handle_time   AS handleTime,
          r.handle_remark AS handleRemark,
          r.handler_id    AS handlerId,
          hu.real_name    AS handlerName,
          hu.phone        AS handlerPhone,
          CONCAT(IFNULL(b.name, ''), '-', IFNULL(c.room_number, '')) AS location
        FROM repair r
        LEFT JOIN classroom c ON r.classroom_id = c.id AND c.deleted = 0
        LEFT JOIN building b ON c.building_id = b.id AND b.deleted = 0
        LEFT JOIN `user` ru ON r.user_id = ru.id AND ru.deleted = 0
        LEFT JOIN `user` hu ON r.handler_id = hu.id AND hu.deleted = 0
        WHERE r.deleted = 0
        ORDER BY r.create_time DESC
    """)
    List<com.campus.learningspace.entity.AdminRepairVO> selectAdminRepairList();
}
