package com.campus.learningspace.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.AdminBuildingVO;
import com.campus.learningspace.entity.Building;
import com.campus.learningspace.service.AdminAuthService;
import com.campus.learningspace.service.AdminPermissionService;
import com.campus.learningspace.service.AdminOperationLogService;
import com.campus.learningspace.service.BuildingService;
import com.campus.learningspace.entity.AdminOperationLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/buildings")
@CrossOrigin
public class AdminBuildingController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final BuildingService buildingService;
    private final AdminOperationLogService adminOperationLogService;

    public AdminBuildingController(AdminAuthService adminAuthService,
                                    AdminPermissionService adminPermissionService,
                                    BuildingService buildingService,
                                    AdminOperationLogService adminOperationLogService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.buildingService = buildingService;
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public Result<List<AdminBuildingVO>> list(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                                @RequestParam(required = false) String keyword) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }

        List<Building> buildings;
        if (keyword == null || keyword.trim().isEmpty()) {
            buildings = buildingService.list();
        } else {
            String kw = keyword.trim();
            LambdaQueryWrapper<Building> w = new LambdaQueryWrapper<>();
            w.eq(Building::getDeleted, 0)
                    .and(x -> x.like(Building::getName, kw).or().like(Building::getBuildingNumber, kw).or().like(Building::getAddress, kw));
            buildings = buildingService.list(w);
        }

        // 简单转换：AdminBuildingVO 只暴露后台所需字段
        List<AdminBuildingVO> res = buildings.stream().map(b -> {
            AdminBuildingVO vo = new AdminBuildingVO();
            vo.setId(b.getId());
            vo.setName(b.getName());
            vo.setBuildingNumber(b.getBuildingNumber());
            vo.setLocation(b.getAddress());
            vo.setStatus(b.getStatus());
            vo.setCreateTime(b.getCreateTime());
            return vo;
        }).toList();

        return Result.success(res);
    }

    @PostMapping
    public Result<Boolean> create(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @RequestBody Building building) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        if (building == null) return Result.error(400, "教学楼数据不能为空");

        building.setId(null);
        if (building.getBuildingNumber() == null || building.getBuildingNumber().trim().isEmpty()) {
            return Result.error(400, "楼栋号不能为空");
        }
        if (building.getStatus() == null) building.setStatus(1);
        if (building.getFloorCount() == null) building.setFloorCount(1);
        if (building.getDescription() == null) building.setDescription("");
        if (building.getLatitude() == null) building.setLatitude(null);
        if (building.getLongitude() == null) building.setLongitude(null);
        building.setDeleted(0);

        long exists = buildingService.count(new LambdaQueryWrapper<Building>() {{
            eq(Building::getDeleted, 0);
            eq(Building::getBuildingNumber, building.getBuildingNumber());
        }});
        if (exists > 0) {
            return Result.error(400, "楼栋号已存在，请勿重复");
        }

        boolean ok = buildingService.save(building);
        if (ok) {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("BUILDING_CREATE");
            log.setDetail("buildingNumber=" + building.getBuildingNumber() + ", name=" + building.getName());
            adminOperationLogService.save(log);
        }
        return Result.success(ok);
    }

    @PutMapping
    public Result<Boolean> update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @RequestBody Building building) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) {
            return Result.error(403, "没有基础数据管理权限");
        }
        if (building == null || building.getId() == null) return Result.error(400, "教学楼ID不能为空");

        Building old = buildingService.getById(building.getId());
        if (old == null || (old.getDeleted() != null && old.getDeleted() == 1)) {
            return Result.error(404, "教学楼不存在");
        }

        if (building.getBuildingNumber() == null || building.getBuildingNumber().trim().isEmpty()) {
            building.setBuildingNumber(old.getBuildingNumber());
        }
        if (building.getStatus() == null) building.setStatus(old.getStatus());
        if (building.getFloorCount() == null) building.setFloorCount(old.getFloorCount());
        if (building.getDescription() == null) building.setDescription(old.getDescription());
        if (building.getAddress() == null) building.setAddress(old.getAddress());
        if (building.getName() == null) building.setName(old.getName());
        if (building.getLatitude() == null) building.setLatitude(old.getLatitude());
        if (building.getLongitude() == null) building.setLongitude(old.getLongitude());
        building.setDeleted(old.getDeleted());

        long exists = buildingService.count(new LambdaQueryWrapper<Building>() {{
            eq(Building::getDeleted, 0);
            eq(Building::getBuildingNumber, building.getBuildingNumber());
            ne(Building::getId, building.getId());
        }});
        if (exists > 0) {
            return Result.error(400, "楼栋号已存在，请勿重复");
        }

        boolean ok = buildingService.updateById(building);
        if (ok) {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("BUILDING_UPDATE");
            log.setDetail("buildingId=" + building.getId() + ", buildingNumber=" + building.getBuildingNumber());
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
        if (id == null) return Result.error(400, "教学楼ID不能为空");

        boolean ok = buildingService.removeById(id);
        if (ok) {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(session.adminUserId());
            log.setAdminRoleId(session.adminRoleId());
            log.setAction("BUILDING_DELETE");
            log.setDetail("buildingId=" + id);
            adminOperationLogService.save(log);
        }
        return Result.success(ok);
    }
}

