package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.Review;
import com.campus.learningspace.entity.AdminReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("""
        SELECT
          rv.id,
          rv.user_id        AS userId,
          ru.real_name     AS reviewerName,
          rv.classroom_id  AS classroomId,
          CONCAT(IFNULL(b.name, ''), '-', IFNULL(c.room_number, '')) AS location,
          rv.score,
          rv.content,
          rv.tags,
          rv.status,
          rv.create_time    AS createTime
        FROM review rv
        LEFT JOIN classroom c ON rv.classroom_id = c.id AND c.deleted = 0
        LEFT JOIN building b ON c.building_id = b.id AND b.deleted = 0
        LEFT JOIN `user` ru ON rv.user_id = ru.id AND ru.deleted = 0
        WHERE rv.deleted = 0
        ORDER BY rv.create_time DESC
    """)
    List<AdminReviewVO> selectAdminReviewList();
}
