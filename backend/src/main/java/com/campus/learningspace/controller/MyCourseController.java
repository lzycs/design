package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.MyCourseItemVO;
import com.campus.learningspace.service.MyCourseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/my-courses")
@CrossOrigin
public class MyCourseController {
    private final MyCourseService myCourseService;

    public MyCourseController(MyCourseService myCourseService) {
        this.myCourseService = myCourseService;
    }

    @GetMapping("/day")
    public Result<List<MyCourseItemVO>> getDayCourses(@RequestParam Long userId,
                                                      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(myCourseService.getDayCourses(userId, date, keyword));
    }

    @GetMapping("/week")
    public Result<Map<String, Object>> getWeekCourses(@RequestParam Long userId,
                                                      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                      @RequestParam(required = false) String keyword) {
        return Result.success(myCourseService.getWeekCourses(userId, date, keyword));
    }

    @PutMapping("/{courseId}/note")
    public Result<Boolean> updateNote(@PathVariable Long courseId, @RequestBody Map<String, Object> body) {
        Object userIdObj = body == null ? null : body.get("userId");
        if (userIdObj == null) return Result.error(400, "缺少 userId");
        Long userId = Long.valueOf(userIdObj.toString());
        Integer isStarred = body.get("isStarred") == null ? null : Integer.valueOf(body.get("isStarred").toString());
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        Integer remind = body.get("remindBeforeMinutes") == null ? null : Integer.valueOf(body.get("remindBeforeMinutes").toString());
        return Result.success(myCourseService.updateNote(userId, courseId, isStarred, note, remind));
    }
}
