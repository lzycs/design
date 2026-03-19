package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.AdminCourseVO;
import com.campus.learningspace.entity.AdminOperationLog;
import com.campus.learningspace.entity.Course;
import com.campus.learningspace.mapper.CourseMapper;
import com.campus.learningspace.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
@CrossOrigin
public class AdminCourseController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final CourseService courseService;
    private final ClassroomService classroomService;
    private final CourseMapper courseMapper;
    private final AdminOperationLogService adminOperationLogService;

    public AdminCourseController(AdminAuthService adminAuthService,
                                 AdminPermissionService adminPermissionService,
                                 CourseService courseService,
                                 ClassroomService classroomService,
                                 CourseMapper courseMapper,
                                 AdminOperationLogService adminOperationLogService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.courseService = courseService;
        this.classroomService = classroomService;
        this.courseMapper = courseMapper;
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public Result<List<AdminCourseVO>> list(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                             @RequestParam(required = false) String keyword) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }

        List<AdminCourseVO> all = courseMapper.selectAdminCourseList();
        if (keyword == null || keyword.trim().isEmpty()) return Result.success(all);

        String kw = keyword.trim().toLowerCase();
        List<AdminCourseVO> filtered = new ArrayList<>();
        for (AdminCourseVO vo : all) {
            String location = vo.getLocation() == null ? "" : vo.getLocation().toLowerCase();
            String courseName = vo.getCourseName() == null ? "" : vo.getCourseName().toLowerCase();
            String teacherName = vo.getTeacherName() == null ? "" : vo.getTeacherName().toLowerCase();
            if (location.contains(kw) || courseName.contains(kw) || teacherName.contains(kw)) {
                filtered.add(vo);
            }
        }
        return Result.success(filtered);
    }

    @PostMapping
    public Result<Boolean> create(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @RequestBody Course course) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        if (course == null) return Result.error(400, "课程数据不能为空");

        if (course.getClassroomId() == null) return Result.error(400, "所属教室不能为空");
        if (course.getCourseName() == null || course.getCourseName().trim().isEmpty()) return Result.error(400, "课程名称不能为空");
        if (course.getTeacherName() == null || course.getTeacherName().trim().isEmpty()) return Result.error(400, "授课老师不能为空");
        if (course.getWeekDay() == null) return Result.error(400, "星期不能为空");
        if (course.getStartTime() == null || course.getEndTime() == null) return Result.error(400, "上课时间不能为空");
        if (course.getStartWeek() == null || course.getEndWeek() == null) return Result.error(400, "周次不能为空");

        var classroom = classroomService.getById(course.getClassroomId());
        if (classroom == null || (classroom.getDeleted() != null && classroom.getDeleted() == 1)) {
            return Result.error(400, "所属教室不存在");
        }

        course.setId(null);
        if (course.getDeleted() == null) course.setDeleted(0);

        long exists = courseService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Course>() {{
            eq(Course::getDeleted, 0);
            eq(Course::getClassroomId, course.getClassroomId());
            eq(Course::getCourseName, course.getCourseName());
            eq(Course::getTeacherName, course.getTeacherName());
            eq(Course::getWeekDay, course.getWeekDay());
            eq(Course::getStartTime, course.getStartTime());
            eq(Course::getEndTime, course.getEndTime());
            eq(Course::getStartWeek, course.getStartWeek());
            eq(Course::getEndWeek, course.getEndWeek());
        }});
        if (exists > 0) return Result.error(400, "该课程已存在，请勿重复");

        boolean ok = courseService.save(course);
        if (ok) {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("COURSE_CREATE");
            log.setDetail("courseName=" + course.getCourseName() + ", classroomId=" + course.getClassroomId());
            adminOperationLogService.save(log);
        }
        return Result.success(ok);
    }

    @PutMapping
    public Result<Boolean> update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @RequestBody Course course) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        if (course == null || course.getId() == null) return Result.error(400, "课程ID不能为空");

        Course old = courseService.getById(course.getId());
        if (old == null || (old.getDeleted() != null && old.getDeleted() == 1)) {
            return Result.error(404, "课程不存在");
        }

        if (course.getClassroomId() == null) course.setClassroomId(old.getClassroomId());
        var classroom = classroomService.getById(course.getClassroomId());
        if (classroom == null || (classroom.getDeleted() != null && classroom.getDeleted() == 1)) {
            return Result.error(400, "所属教室不存在");
        }

        // 兜底：避免 updateById 写入 null
        if (course.getCourseName() == null) course.setCourseName(old.getCourseName());
        if (course.getTeacherName() == null) course.setTeacherName(old.getTeacherName());
        if (course.getWeekDay() == null) course.setWeekDay(old.getWeekDay());
        if (course.getStartTime() == null) course.setStartTime(old.getStartTime());
        if (course.getEndTime() == null) course.setEndTime(old.getEndTime());
        if (course.getStartWeek() == null) course.setStartWeek(old.getStartWeek());
        if (course.getEndWeek() == null) course.setEndWeek(old.getEndWeek());
        if (course.getDeleted() == null) course.setDeleted(old.getDeleted());

        long exists = courseService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Course>() {{
            eq(Course::getDeleted, 0);
            eq(Course::getClassroomId, course.getClassroomId());
            eq(Course::getCourseName, course.getCourseName());
            eq(Course::getTeacherName, course.getTeacherName());
            eq(Course::getWeekDay, course.getWeekDay());
            eq(Course::getStartTime, course.getStartTime());
            eq(Course::getEndTime, course.getEndTime());
            eq(Course::getStartWeek, course.getStartWeek());
            eq(Course::getEndWeek, course.getEndWeek());
            ne(Course::getId, course.getId());
        }});
        if (exists > 0) return Result.error(400, "该课程已存在，请勿重复");

        boolean ok = courseService.updateById(course);
        if (ok) {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("COURSE_UPDATE");
            log.setDetail("courseId=" + course.getId() + ", courseName=" + course.getCourseName());
            adminOperationLogService.save(log);
        }
        return Result.success(ok);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @PathVariable Long id) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        boolean ok = courseService.removeById(id);
        if (ok) {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("COURSE_DELETE");
            log.setDetail("courseId=" + id);
            adminOperationLogService.save(log);
        }
        return Result.success(ok);
    }
}

