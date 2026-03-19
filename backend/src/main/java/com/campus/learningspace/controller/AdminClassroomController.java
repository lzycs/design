package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.AdminClassroomVO;
import com.campus.learningspace.entity.AdminOperationLog;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.service.*;
import com.campus.learningspace.mapper.ClassroomMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/classrooms")
@CrossOrigin
public class AdminClassroomController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final ClassroomService classroomService;
    private final BuildingService buildingService;
    private final ClassroomMapper classroomMapper;
    private final AdminOperationLogService adminOperationLogService;

    public AdminClassroomController(AdminAuthService adminAuthService,
                                     AdminPermissionService adminPermissionService,
                                     ClassroomService classroomService,
                                     BuildingService buildingService,
                                     ClassroomMapper classroomMapper,
                                     AdminOperationLogService adminOperationLogService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.classroomService = classroomService;
        this.buildingService = buildingService;
        this.classroomMapper = classroomMapper;
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public Result<List<AdminClassroomVO>> list(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                @RequestParam(required = false) Long buildingId,
                                                @RequestParam(required = false) Integer status) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }

        List<AdminClassroomVO> all = classroomMapper.selectAdminClassroomList();
        List<AdminClassroomVO> filtered = new ArrayList<>();
        for (AdminClassroomVO vo : all) {
            if (buildingId != null && vo.getBuildingId() != null && !buildingId.equals(vo.getBuildingId())) continue;
            if (status != null && vo.getStatus() != null && !status.equals(vo.getStatus())) continue;
            filtered.add(vo);
        }
        return Result.success(filtered);
    }

    @PostMapping
    public Result<Boolean> create(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @RequestBody Classroom classroom) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        if (classroom == null) return Result.error(400, "教室数据不能为空");
        if (classroom.getBuildingId() == null) return Result.error(400, "所属教学楼不能为空");
        if (classroom.getRoomNumber() == null || classroom.getRoomNumber().trim().isEmpty()) {
            return Result.error(400, "教室号不能为空");
        }
        if (classroom.getCapacity() == null) return Result.error(400, "座位数不能为空");

        var b = buildingService.getById(classroom.getBuildingId());
        if (b == null || (b.getDeleted() != null && b.getDeleted() == 1)) {
            return Result.error(400, "所属教学楼不存在");
        }

        long exists = classroomService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Classroom>() {{
            eq(Classroom::getDeleted, 0);
            eq(Classroom::getBuildingId, classroom.getBuildingId());
            eq(Classroom::getRoomNumber, classroom.getRoomNumber());
        }});
        if (exists > 0) return Result.error(400, "教室号已存在，请勿重复");

        // 兜底：DB 非空字段默认值
        classroom.setId(null);
        if (classroom.getName() == null) classroom.setName(classroom.getRoomNumber());
        if (classroom.getFloor() == null) classroom.setFloor(1);
        if (classroom.getType() == null) classroom.setType(1);
        if (classroom.getEquipment() == null) classroom.setEquipment("");
        if (classroom.getStatus() == null) classroom.setStatus(1);
        if (classroom.getDeleted() == null) classroom.setDeleted(0);

        boolean ok = classroomService.save(classroom);

        if (ok) {
            com.campus.learningspace.entity.AdminOperationLog log = new com.campus.learningspace.entity.AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("CLASSROOM_CREATE");
            log.setDetail("classroom=" + classroom.getRoomNumber() + ", buildingId=" + classroom.getBuildingId());
            adminOperationLogService.save(log);
        }

        return Result.success(ok);
    }

    @PutMapping
    public Result<Boolean> update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @RequestBody Classroom classroom) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        if (classroom == null || classroom.getId() == null) return Result.error(400, "教室ID不能为空");

        Classroom old = classroomService.getById(classroom.getId());
        if (old == null || (old.getDeleted() != null && old.getDeleted() == 1)) {
            return Result.error(404, "教室不存在");
        }

        if (classroom.getBuildingId() == null) classroom.setBuildingId(old.getBuildingId());
        if (classroom.getRoomNumber() == null || classroom.getRoomNumber().trim().isEmpty()) classroom.setRoomNumber(old.getRoomNumber());

        var b = buildingService.getById(classroom.getBuildingId());
        if (b == null || (b.getDeleted() != null && b.getDeleted() == 1)) {
            return Result.error(400, "所属教学楼不存在");
        }

        long exists = classroomService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Classroom>() {{
            eq(Classroom::getDeleted, 0);
            eq(Classroom::getBuildingId, classroom.getBuildingId());
            eq(Classroom::getRoomNumber, classroom.getRoomNumber());
            ne(Classroom::getId, classroom.getId());
        }});
        if (exists > 0) return Result.error(400, "教室号已存在，请勿重复");

        // 补全空值，避免 updateById 写入 null（字段非空）
        if (classroom.getName() == null) classroom.setName(old.getName());
        if (classroom.getFloor() == null) classroom.setFloor(old.getFloor());
        if (classroom.getType() == null) classroom.setType(old.getType());
        if (classroom.getEquipment() == null) classroom.setEquipment(old.getEquipment());
        if (classroom.getCapacity() == null) classroom.setCapacity(old.getCapacity());
        if (classroom.getStatus() == null) classroom.setStatus(old.getStatus());
        if (classroom.getDeleted() == null) classroom.setDeleted(old.getDeleted());
        if (classroom.getCheckinRadius() == null) classroom.setCheckinRadius(old.getCheckinRadius());
        if (classroom.getLatitude() == null) classroom.setLatitude(old.getLatitude());
        if (classroom.getLongitude() == null) classroom.setLongitude(old.getLongitude());

        boolean ok = classroomService.updateById(classroom);
        if (ok) {
            com.campus.learningspace.entity.AdminOperationLog log = new com.campus.learningspace.entity.AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("CLASSROOM_UPDATE");
            log.setDetail("classroomId=" + classroom.getId() + ", roomNumber=" + classroom.getRoomNumber());
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

        boolean ok = classroomService.removeById(id);
        if (ok) {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("CLASSROOM_DELETE");
            log.setDetail("classroomId=" + id);
            adminOperationLogService.save(log);
        }
        return Result.success(ok);
    }
}

