package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.*;
import com.campus.learningspace.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/repairs")
@CrossOrigin
public class AdminRepairController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final RepairService repairService;
    private final com.campus.learningspace.mapper.RepairMapper repairMapper;
    private final NotificationService notificationService;
    private final AdminOperationLogService adminOperationLogService;

    public AdminRepairController(AdminAuthService adminAuthService,
                                  AdminPermissionService adminPermissionService,
                                  RepairService repairService,
                                  com.campus.learningspace.mapper.RepairMapper repairMapper,
                                  NotificationService notificationService,
                                  AdminOperationLogService adminOperationLogService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.repairService = repairService;
        this.repairMapper = repairMapper;
        this.notificationService = notificationService;
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public Result<List<AdminRepairVO>> list(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String keyword) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "repair:process")) {
            return Result.error(403, "没有报修处理权限");
        }

        List<AdminRepairVO> all = repairMapper.selectAdminRepairList();
        List<AdminRepairVO> filtered = new ArrayList<>();
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        for (AdminRepairVO vo : all) {
            if (status != null && vo.getStatus() != null && !status.equals(vo.getStatus())) continue;
            if (kw != null && !kw.isEmpty()) {
                String id = vo.getId() == null ? "" : String.valueOf(vo.getId());
                String location = vo.getLocation() == null ? "" : vo.getLocation().toLowerCase();
                String requester = vo.getRequesterName() == null ? "" : vo.getRequesterName().toLowerCase();
                if (!id.contains(kw) && !location.contains(kw) && !requester.contains(kw)) continue;
            }
            filtered.add(vo);
        }
        return Result.success(filtered);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                          @PathVariable Long id,
                                          @RequestBody AdminRepairStatusUpdateRequest req) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "repair:process")) {
            return Result.error(403, "没有报修处理权限");
        }
        if (req == null || req.getStatus() == null) {
            return Result.error(400, "状态不能为空");
        }

        Repair existing = repairService.getById(id);
        if (existing == null || existing.getDeleted() != null && existing.getDeleted() == 1) {
            return Result.error(404, "报修工单不存在");
        }

        Integer oldStatus = existing.getStatus();
        existing.setStatus(req.getStatus());
        existing.setHandlerId(session.adminUserId());
        existing.setHandleRemark(req.getHandleRemark());

        // 只要进入处理/完成阶段，就记录处理时间
        if (req.getStatus() != null && req.getStatus() >= 2) {
            existing.setHandleTime(LocalDateTime.now());
        }

        boolean ok = repairService.updateById(existing);
        if (!ok) return Result.success(false);
        // 报修状态变更后，同步教室实时状态到预约侧展示
        if (existing.getResourceType() != null && existing.getResourceType() == 1) {
            repairService.syncClassroomRealtimeStatusByRepair(existing.getId(), existing.getClassroomId(), existing.getStatus());
        }

        String statusName = switch (req.getStatus()) {
            case 1 -> "待处理";
            case 2 -> "处理中";
            case 3 -> "已修复";
            case 4 -> "已驳回";
            default -> "状态更新";
        };

        // 状态变化通知报修人（type=3-报修通知，relatedId=repairId）
        Notification n = new Notification();
        n.setUserId(existing.getUserId());
        n.setType(3);
        n.setTitle("报修进度更新");
        n.setContent("您的报修工单 " + existing.getId() + " 已更新为：" + statusName);
        n.setRelatedId(existing.getId());
        n.setIsRead(0);
        n.setDeleted(0);
        notificationService.save(n);

        // 操作日志
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(session.adminUserId());
        log.setAdminRoleId(session.adminRoleId());
        log.setAction("REPAIR_STATUS_UPDATE");
        log.setDetail("repairId=" + existing.getId() + ", oldStatus=" + oldStatus + ", newStatus=" + req.getStatus()
                + ", handleRemark=" + (req.getHandleRemark() == null ? "" : req.getHandleRemark()));
        adminOperationLogService.save(log);

        return Result.success(true);
    }
}

