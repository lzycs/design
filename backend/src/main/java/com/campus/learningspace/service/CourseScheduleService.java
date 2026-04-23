package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.learningspace.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseScheduleService {
    private final ClassroomService classroomService;
    private final CourseService courseService;
    private final ReservationService reservationService;
    private final TimeSlotService timeSlotService;

    @Value("${app.course-schedule.term-start-date:2026-02-24}")
    private String termStartDate;

    public CourseScheduleService(ClassroomService classroomService,
                                 CourseService courseService,
                                 ReservationService reservationService,
                                 TimeSlotService timeSlotService) {
        this.classroomService = classroomService;
        this.courseService = courseService;
        this.reservationService = reservationService;
        this.timeSlotService = timeSlotService;
    }

    public Map<String, Object> getBuildingDaySchedule(Long buildingId,
                                                      LocalDate date,
                                                      Integer floor,
                                                      String roomKeyword,
                                                      String courseKeyword,
                                                      String timeRange) {
        List<Classroom> classrooms = listBuildingClassrooms(buildingId, floor, roomKeyword);
        List<TimeSlot> slots = timeSlotService.getActiveSlots();
        Map<Long, List<Course>> courseMap = listCoursesByClassrooms(classrooms, date);
        Map<Long, List<Reservation>> reservationMap = listReservationsByClassrooms(classrooms, date);

        List<BuildingScheduleRowVO> rows = new ArrayList<>();
        for (Classroom classroom : classrooms) {
            BuildingScheduleRowVO row = new BuildingScheduleRowVO();
            row.setClassroomId(classroom.getId());
            row.setClassroomName(classroom.getName());
            row.setRoomNumber(classroom.getRoomNumber());
            row.setFloor(classroom.getFloor());
            row.setCells(buildCells(slots,
                    courseMap.getOrDefault(classroom.getId(), List.of()),
                    reservationMap.getOrDefault(classroom.getId(), List.of()),
                    false));
            rows.add(row);
        }
        rows = applyCellFilters(rows, courseKeyword, timeRange);

        Map<String, Object> res = new HashMap<>();
        res.put("view", "day");
        res.put("date", date);
        res.put("buildingId", buildingId);
        res.put("teachingWeek", resolveTeachingWeek(date));
        res.put("rows", rows);
        return res;
    }

    public Map<String, Object> getBuildingWeekSchedule(Long buildingId, LocalDate date, Integer floor) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            days.add(getBuildingDaySchedule(buildingId, d, floor, null, null, null));
        }
        return Map.of("view", "week", "buildingId", buildingId, "startDate", monday, "days", days);
    }

    public Map<String, Object> getBuildingMonthSchedule(Long buildingId, int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate end = first.withDayOfMonth(first.lengthOfMonth());
        List<Classroom> classrooms = listBuildingClassrooms(buildingId, null, null);
        List<Map<String, Object>> days = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(end); d = d.plusDays(1)) {
            Map<Long, List<Course>> courseMap = listCoursesByClassrooms(classrooms, d);
            long occupied = classrooms.stream().filter(c -> !courseMap.getOrDefault(c.getId(), List.of()).isEmpty()).count();
            days.add(Map.of(
                    "date", d,
                    "teachingWeek", resolveTeachingWeek(d),
                    "occupiedClassroomCount", occupied,
                    "totalClassroomCount", classrooms.size()
            ));
        }
        return Map.of("view", "month", "buildingId", buildingId, "month", String.format("%04d-%02d", year, month), "days", days);
    }

    public ClassroomScheduleVO getClassroomSchedule(Long classroomId, LocalDate date) {
        Classroom classroom = classroomService.getById(classroomId);
        if (classroom == null) return null;
        List<TimeSlot> slots = timeSlotService.getActiveSlots();
        List<Course> courses = listCoursesByClassrooms(List.of(classroom), date).getOrDefault(classroomId, List.of());
        List<Reservation> reservations = listReservationsByClassrooms(List.of(classroom), date).getOrDefault(classroomId, List.of());

        ClassroomScheduleVO vo = new ClassroomScheduleVO();
        vo.setClassroomId(classroom.getId());
        vo.setBuildingId(classroom.getBuildingId());
        vo.setClassroomName(classroom.getName());
        vo.setRoomNumber(classroom.getRoomNumber());
        vo.setDate(date);
        vo.setView("day");
        vo.setSlots(buildCells(slots, courses, reservations, true));
        return vo;
    }

    public List<Map<String, Object>> getClassroomSlotStatusMaps(Long classroomId, LocalDate date) {
        ClassroomScheduleVO schedule = getClassroomSchedule(classroomId, date);
        if (schedule == null) return List.of();
        return schedule.getSlots().stream().map(cell -> {
            Map<String, Object> m = new HashMap<>();
            m.put("slotId", cell.getSlotId());
            m.put("label", cell.getLabel());
            m.put("startTime", cell.getStartTime());
            m.put("endTime", cell.getEndTime());
            m.put("status", cell.getFinalStatus());
            m.put("courseName", cell.getCourseName());
            m.put("teacherName", maskTeacherName(cell.getTeacherName()));
            return m;
        }).toList();
    }

    private List<BuildingScheduleRowVO> applyCellFilters(List<BuildingScheduleRowVO> rows, String courseKeyword, String timeRange) {
        return rows.stream().filter(row -> {
            boolean courseHit = true;
            if (courseKeyword != null && !courseKeyword.isBlank()) {
                String kw = courseKeyword.trim().toLowerCase(Locale.ROOT);
                courseHit = row.getCells().stream().anyMatch(cell -> cell.getCourseName() != null
                        && cell.getCourseName().toLowerCase(Locale.ROOT).contains(kw));
            }
            boolean timeHit = true;
            if (timeRange != null && !timeRange.isBlank()) {
                timeHit = row.getCells().stream().anyMatch(cell -> timeRange.equals(cell.getLabel()));
            }
            return courseHit && timeHit;
        }).collect(Collectors.toList());
    }

    private List<CourseScheduleCellVO> buildCells(List<TimeSlot> slots,
                                                  List<Course> courses,
                                                  List<Reservation> reservations,
                                                  boolean withCourseDetail) {
        List<CourseScheduleCellVO> cells = new ArrayList<>();
        for (TimeSlot slot : slots) {
            CourseScheduleCellVO cell = new CourseScheduleCellVO();
            cell.setSlotId(slot.getId());
            cell.setLabel(slot.getLabel());
            cell.setStartTime(slot.getStartTime());
            cell.setEndTime(slot.getEndTime());

            Course hitCourse = courses.stream().filter(c -> overlaps(slot.getStartTime(), slot.getEndTime(), c.getStartTime(), c.getEndTime()))
                    .findFirst().orElse(null);
            boolean reservationHit = reservations.stream().anyMatch(r -> overlaps(slot.getStartTime(), slot.getEndTime(), r.getStartTime(), r.getEndTime()));

            if (hitCourse != null) {
                cell.setFinalStatus("course_occupied");
                // 楼栋维度和教室维度都返回课程名称，前端可直接展示具体课程名
                cell.setCourseName(hitCourse.getCourseName());
                if (withCourseDetail) {
                    cell.setTeacherName(hitCourse.getTeacherName());
                }
            } else if (reservationHit) {
                cell.setFinalStatus("reservation_occupied");
            } else {
                cell.setFinalStatus("available");
            }
            cells.add(cell);
        }
        return cells;
    }

    private Map<Long, List<Course>> listCoursesByClassrooms(List<Classroom> classrooms, LocalDate date) {
        if (classrooms.isEmpty()) return Map.of();
        Set<Long> ids = classrooms.stream().map(Classroom::getId).collect(Collectors.toSet());
        int weekDay = date.getDayOfWeek().getValue();
        int teachingWeek = resolveTeachingWeek(date);
        List<Course> courses = courseService.list(new LambdaQueryWrapper<Course>()
                .in(Course::getClassroomId, ids)
                .eq(Course::getWeekDay, weekDay)
                .le(Course::getStartWeek, teachingWeek)
                .ge(Course::getEndWeek, teachingWeek)
                .eq(Course::getDeleted, 0));
        return courses.stream().collect(Collectors.groupingBy(Course::getClassroomId));
    }

    private Map<Long, List<Reservation>> listReservationsByClassrooms(List<Classroom> classrooms, LocalDate date) {
        if (classrooms.isEmpty()) return Map.of();
        Set<Long> ids = classrooms.stream().map(Classroom::getId).collect(Collectors.toSet());
        List<Reservation> reservations = reservationService.list(new LambdaQueryWrapper<Reservation>()
                .in(Reservation::getClassroomId, ids)
                .eq(Reservation::getResourceType, 1)
                .eq(Reservation::getReservationDate, date)
                .in(Reservation::getStatus, 1, 2, 3)
                .eq(Reservation::getDeleted, 0));
        return reservations.stream().collect(Collectors.groupingBy(Reservation::getClassroomId));
    }

    private List<Classroom> listBuildingClassrooms(Long buildingId, Integer floor, String roomKeyword) {
        LambdaQueryWrapper<Classroom> q = new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getBuildingId, buildingId)
                .eq(Classroom::getDeleted, 0)
                .eq(Classroom::getStatus, 1)
                .orderByAsc(Classroom::getFloor)
                .orderByAsc(Classroom::getRoomNumber);
        if (floor != null) {
            q.eq(Classroom::getFloor, floor);
        }
        if (roomKeyword != null && !roomKeyword.isBlank()) {
            q.and(w -> w.like(Classroom::getName, roomKeyword.trim())
                    .or().like(Classroom::getRoomNumber, roomKeyword.trim()));
        }
        return classroomService.list(q);
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    private int resolveTeachingWeek(LocalDate date) {
        LocalDate termStart = parseTermStartDate();
        long days = java.time.temporal.ChronoUnit.DAYS.between(termStart, date);
        return (int) Math.max(1, days / 7 + 1);
    }

    private LocalDate parseTermStartDate() {
        try {
            return LocalDate.parse(termStartDate);
        } catch (Exception e) {
            return LocalDate.of(2026, 2, 24);
        }
    }

    private String maskTeacherName(String teacherName) {
        if (teacherName == null || teacherName.isBlank()) return teacherName;
        if (teacherName.length() <= 1) return teacherName;
        return teacherName.substring(0, 1) + "*";
    }
}
