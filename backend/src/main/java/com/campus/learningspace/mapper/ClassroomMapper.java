package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.entity.AdminClassroomVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClassroomMapper extends BaseMapper<Classroom> {

    @Select("""
        SELECT
          c.id,
          c.building_id   AS buildingId,
          b.name          AS buildingName,
          c.name,
          c.room_number   AS roomNumber,
          c.floor,
          c.type,
          c.capacity,
          c.equipment,
          c.status,
          CASE WHEN c.status = 1 THEN 1 ELSE 0 END AS isAvailable,
          c.latitude,
          c.longitude
        FROM classroom c
        LEFT JOIN building b ON c.building_id = b.id AND b.deleted = 0
        WHERE c.deleted = 0
        ORDER BY c.id DESC
    """)
    List<AdminClassroomVO> selectAdminClassroomList();
}
