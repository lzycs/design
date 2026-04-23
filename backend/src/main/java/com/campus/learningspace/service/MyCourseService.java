package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.learningspace.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MyCourseService {
    private final UserCourseEnrollmentService userCourseEnrollmentService;
    private final UserCourseNoteService userCourseNoteService;
    private final CourseService courseService;
    private final ClassroomService classroomService;
    private final BuildingService buildingService;

    @Value("${app.course-schedule.term-start-date:2026-02-24}")
    private String termStartDate;

    public MyCourseService(UserCourseEnrollmentService userCourseEnrollmentService,
                           UserCourseNoteService userCourseNoteService,
                           CourseService courseService,
                           ClassroomService classroomService,
                           BuildingService buildingService) {
        this.userCourseEnrollmentService = userCourseEnrollmentService;
        this.userCourseNoteService = userCourseNoteService;
        this.courseService = courseService;
        this.classroomService = classroomService;
        this.buildingService = buildingService;
    }

    public List<MyCourseItemVO> getDayCourses(Long userId, LocalDate date, String keyword) {
        int weekDay = date.getDayOfWeek().getValue();
        int teachingWeek = resolveTeachingWeek(date);
        Set<Long> enrolledCourseIds = getEnrolledCourseIds(userId);
        if (enrolledCourseIds.isEmpty()) return List.of();

        List<Course> courses = courseService.list(new LambdaQueryWrapper<Course>()
                .in(Course::getId, enrolledCourseIds)
                .eq(Course::getWeekDay, weekDay)
                .le(Course::getStartWeek, teachingWeek)
                .ge(Course::getEndWeek, teachingWeek)
                .eq(Course::getDeleted, 0)
                .orderByAsc(Course::getStartTime));
        return toVoList(userId, courses, date, teachingWeek, keyword);
    }

    public Map<String, Object> getWeekCourses(Long userId, LocalDate anyDateInWeek, String keyword) {
        LocalDate monday = anyDateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            days.add(Map.of(
                    "date", d,
                    "weekDay", d.getDayOfWeek().getValue(),
                    "courses", getDayCourses(userId, d, keyword)
            ));
        }
        return Map.of("startDate", monday, "days", days);
    }

    public boolean updateNote(Long userId, Long courseId, Integer isStarred, String note, Integer remindBeforeMinutes) {
        UserCourseNote existed = userCourseNoteService.lambdaQuery()
                .eq(UserCourseNote::getUserId, userId)
                .eq(UserCourseNote::getCourseId, courseId)
                .eq(UserCourseNote::getDeleted, 0)
                .one();
        if (existed == null) {
            UserCourseNote n = new UserCourseNote();
            n.setUserId(userId);
            n.setCourseId(courseId);
            n.setIsStarred(isStarred == null ? 0 : isStarred);
            n.setNote(note);
            n.setRemindBeforeMinutes(remindBeforeMinutes);
            return userCourseNoteService.save(n);
        }
        existed.setIsStarred(isStarred == null ? existed.getIsStarred() : isStarred);
        existed.setNote(note);
        existed.setRemindBeforeMinutes(remindBeforeMinutes);
        return userCourseNoteService.updateById(existed);
    }

    private List<MyCourseItemVO> toVoList(Long userId, List<Course> courses, LocalDate date, int teachingWeek, String keyword) {
        if (courses.isEmpty()) return List.of();
        Set<Long> classroomIds = courses.stream().map(Course::getClassroomId).collect(Collectors.toSet());
        List<Classroom> classrooms = classroomService.listByIds(classroomIds);
        Map<Long, Classroom> classroomMap = classrooms.stream().collect(Collectors.toMap(Classroom::getId, c -> c));
        Set<Long> buildingIds = classrooms.stream().map(Classroom::getBuildingId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Building> buildingMap = buildingService.listByIds(buildingIds).stream()
                .collect(Collectors.toMap(Building::getId, b -> b));
        Map<Long, UserCourseNote> noteMap = userCourseNoteService.lambdaQuery()
                .eq(UserCourseNote::getUserId, userId)
                .eq(UserCourseNote::getDeleted, 0)
                .in(UserCourseNote::getCourseId, courses.stream().map(Course::getId).toList())
                .list()
                .stream().collect(Collectors.toMap(UserCourseNote::getCourseId, n -> n));

        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<MyCourseItemVO> result = new ArrayList<>();
        for (Course c : courses) {
            Classroom classroom = classroomMap.get(c.getClassroomId());
            Building building = classroom == null ? null : buildingMap.get(classroom.getBuildingId());
            if (!kw.isEmpty()) {
                String t = (c.getCourseName() + "|" + c.getTeacherName() + "|" +
                        (classroom == null ? "" : classroom.getRoomNumber()) + "|" +
                        (building == null ? "" : building.getName())).toLowerCase(Locale.ROOT);
                if (!t.contains(kw)) continue;
            }
            UserCourseNote n = noteMap.get(c.getId());
            MyCourseItemVO vo = new MyCourseItemVO();
            vo.setCourseId(c.getId());
            vo.setCourseName(c.getCourseName());
            vo.setTeacherName(c.getTeacherName());
            vo.setWeekDay(c.getWeekDay());
            vo.setTeachingWeek(teachingWeek);
            vo.setDate(date);
            vo.setStartTime(c.getStartTime());
            vo.setEndTime(c.getEndTime());
            vo.setClassroomId(classroom == null ? null : classroom.getId());
            vo.setClassroomName(classroom == null ? null : classroom.getName());
            vo.setRoomNumber(classroom == null ? null : classroom.getRoomNumber());
            vo.setBuildingName(building == null ? null : building.getName());
            vo.setIsStarred(n == null ? 0 : n.getIsStarred());
            vo.setNote(n == null ? null : n.getNote());
            vo.setRemindBeforeMinutes(n == null ? null : n.getRemindBeforeMinutes());
            result.add(vo);
        }
        return result;
    }

    private Set<Long> getEnrolledCourseIds(Long userId) {
        return userCourseEnrollmentService.lambdaQuery()
                .eq(UserCourseEnrollment::getUserId, userId)
                .eq(UserCourseEnrollment::getDeleted, 0)
                .list()
                .stream()
                .map(UserCourseEnrollment::getCourseId)
                .collect(Collectors.toSet());
    }

    private int resolveTeachingWeek(LocalDate date) {
        LocalDate termStart;
        try {
            termStart = LocalDate.parse(termStartDate);
        } catch (Exception e) {
            termStart = LocalDate.of(2026, 2, 24);
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(termStart, date);
        return (int) Math.max(1, days / 7 + 1);
    }
}
