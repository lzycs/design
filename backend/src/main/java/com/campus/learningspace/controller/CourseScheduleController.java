package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.ClassroomScheduleVO;
import com.campus.learningspace.service.CourseScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-schedule")
@CrossOrigin
public class CourseScheduleController {
    private final CourseScheduleService courseScheduleService;

    public CourseScheduleController(CourseScheduleService courseScheduleService) {
        this.courseScheduleService = courseScheduleService;
    }

    @GetMapping("/building/{buildingId}/day")
    public Result<Map<String, Object>> getBuildingDay(@PathVariable Long buildingId,
                                                      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                      @RequestParam(required = false) Integer floor,
                                                      @RequestParam(required = false) String roomKeyword,
                                                      @RequestParam(required = false) String courseKeyword,
                                                      @RequestParam(required = false) String timeRange) {
        return Result.success(courseScheduleService.getBuildingDaySchedule(buildingId, date, floor, roomKeyword, courseKeyword, timeRange));
    }

    @GetMapping("/building/{buildingId}/week")
    public Result<Map<String, Object>> getBuildingWeek(@PathVariable Long buildingId,
                                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                       @RequestParam(required = false) Integer floor) {
        return Result.success(courseScheduleService.getBuildingWeekSchedule(buildingId, date, floor));
    }

    @GetMapping("/building/{buildingId}/month")
    public Result<Map<String, Object>> getBuildingMonth(@PathVariable Long buildingId,
                                                        @RequestParam Integer year,
                                                        @RequestParam Integer month) {
        return Result.success(courseScheduleService.getBuildingMonthSchedule(buildingId, year, month));
    }

    @GetMapping("/classroom/{classroomId}")
    public Result<ClassroomScheduleVO> getClassroomSchedule(@PathVariable Long classroomId,
                                                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        ClassroomScheduleVO vo = courseScheduleService.getClassroomSchedule(classroomId, date);
        if (vo == null) {
            return Result.error(404, "教室不存在");
        }
        return Result.success(vo);
    }

    @GetMapping("/classroom/{classroomId}/slot-status")
    public Result<List<Map<String, Object>>> getClassroomSlotStatus(@PathVariable Long classroomId,
                                                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(courseScheduleService.getClassroomSlotStatusMaps(classroomId, date));
    }
}
