package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.entity.ClassroomDetailVO;
import com.campus.learningspace.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classroom")
@CrossOrigin
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @GetMapping("/list")
    public Result<List<Classroom>> list() {
        return Result.success(classroomService.list());
    }

    @GetMapping("/building/{buildingId}")
    public Result<List<Classroom>> getByBuildingId(@PathVariable Long buildingId) {
        return Result.success(classroomService.getClassroomsByBuilding(buildingId));
    }

    @GetMapping("/available")
    public Result<List<Classroom>> getAvailable(@RequestParam(required = false) Integer type) {
        return Result.success(classroomService.getAvailableClassrooms(type));
    }

    @GetMapping("/{id}")
    public Result<Classroom> getById(@PathVariable Long id) {
        return Result.success(classroomService.getById(id));
    }

    /**
     * 教室详情（含热度星级、综合评分均值、签到次数）
     */
    @GetMapping("/{id}/detail")
    public Result<ClassroomDetailVO> getDetail(@PathVariable Long id) {
        ClassroomDetailVO vo = classroomService.getDetail(id);
        return vo != null ? Result.success(vo) : Result.error("教室不存在");
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Classroom classroom) {
        return Result.success(classroomService.save(classroom));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Classroom classroom) {
        return Result.success(classroomService.updateById(classroom));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(classroomService.removeById(id));
    }
}
