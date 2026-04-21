package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.AdminOperationLog;
import com.campus.learningspace.entity.ResourceMarketItem;
import com.campus.learningspace.service.AdminAuthService;
import com.campus.learningspace.service.AdminOperationLogService;
import com.campus.learningspace.service.AdminPermissionService;
import com.campus.learningspace.service.ResourceMarketItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/market-items")
@CrossOrigin
public class AdminMarketAuditController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final ResourceMarketItemService resourceMarketItemService;
    private final AdminOperationLogService adminOperationLogService;

    public AdminMarketAuditController(AdminAuthService adminAuthService,
                                      AdminPermissionService adminPermissionService,
                                      ResourceMarketItemService resourceMarketItemService,
                                      AdminOperationLogService adminOperationLogService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.resourceMarketItemService = resourceMarketItemService;
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public Result<List<ResourceMarketItem>> listPending(
            @RequestHeader(value = "X-Admin-Token", required = false) String token
    ) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "review:audit")) {
            return Result.error(403, "没有资源审核权限");
        }
        return Result.success(resourceMarketItemService.listPendingAudit());
    }

    @PutMapping("/{id}/audit")
    public Result<Boolean> audit(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "review:audit")) {
            return Result.error(403, "没有资源审核权限");
        }

        boolean approve = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approve", "false")));
        String remark = String.valueOf(body.getOrDefault("remark", ""));
        resourceMarketItemService.audit(id, session.adminUserId(), approve, remark);

        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(session.adminUserId());
        log.setAdminRoleId(session.adminRoleId());
        log.setAction("RESOURCE_MARKET_AUDIT");
        log.setDetail("itemId=" + id + ", approve=" + approve + ", remark=" + remark);
        adminOperationLogService.save(log);

        return Result.success(true);
    }
}
