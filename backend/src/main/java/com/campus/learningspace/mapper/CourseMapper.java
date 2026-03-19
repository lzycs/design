package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.Course;
import com.campus.learningspace.entity.AdminCourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    @Select("""
        SELECT
          co.id AS id,
          co.classroom_id AS classroomId,
          CONCAT(IFNULL(b.name, ''), '-', IFNULL(c.room_number, '')) AS location,
          co.course_name AS courseName,
          co.teacher_name AS teacherName,
          co.week_day AS weekDay,
          co.start_time AS startTime,
          co.end_time AS endTime,
          co.start_week AS startWeek,
          co.end_week AS endWeek
        FROM course co
        LEFT JOIN classroom c ON co.classroom_id = c.id AND c.deleted = 0
        LEFT JOIN building b ON c.building_id = b.id AND b.deleted = 0
        WHERE co.deleted = 0
        ORDER BY co.week_day ASC, co.start_time ASC, co.id DESC
    """)
    List<AdminCourseVO> selectAdminCourseList();
}
