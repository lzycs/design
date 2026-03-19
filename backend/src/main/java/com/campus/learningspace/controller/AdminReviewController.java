package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.AdminOperationLog;
import com.campus.learningspace.entity.AdminReviewAuditRequest;
import com.campus.learningspace.entity.AdminReviewVO;
import com.campus.learningspace.entity.Review;
import com.campus.learningspace.mapper.ReviewMapper;
import com.campus.learningspace.service.AdminAuthService;
import com.campus.learningspace.service.AdminOperationLogService;
import com.campus.learningspace.service.AdminPermissionService;
import com.campus.learningspace.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@CrossOrigin
public class AdminReviewController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final AdminOperationLogService adminOperationLogService;

    public AdminReviewController(AdminAuthService adminAuthService,
                                  AdminPermissionService adminPermissionService,
                                  ReviewService reviewService,
                                  ReviewMapper reviewMapper,
                                  AdminOperationLogService adminOperationLogService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
        this.adminOperationLogService = adminOperationLogService;
    }

    @GetMapping
    public Result<List<AdminReviewVO>> list(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String keyword) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "review:audit")) {
            return Result.error(403, "没有评价审核权限");
        }

        List<AdminReviewVO> all = reviewMapper.selectAdminReviewList();
        List<AdminReviewVO> filtered = new ArrayList<>();
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        for (AdminReviewVO vo : all) {
            if (status != null && vo.getStatus() != null && !status.equals(vo.getStatus())) continue;
            if (kw != null && !kw.isEmpty()) {
                String id = vo.getReviewerName() == null ? "" : vo.getReviewerName().toLowerCase();
                String location = vo.getLocation() == null ? "" : vo.getLocation().toLowerCase();
                String content = vo.getContent() == null ? "" : vo.getContent().toLowerCase();
                if (!id.contains(kw) && !location.contains(kw) && !content.contains(kw)) continue;
            }
            filtered.add(vo);
        }
        return Result.success(filtered);
    }

    @PutMapping("/{id}/audit")
    public Result<Boolean> audit(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                    @PathVariable Long id,
                                    @RequestBody AdminReviewAuditRequest req) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "review:audit")) {
            return Result.error(403, "没有评价审核权限");
        }
        if (req == null || req.getApprove() == null) return Result.error(400, "approve不能为空");

        Review existing = reviewService.getById(id);
        if (existing == null || existing.getDeleted() != null && existing.getDeleted() == 1) {
            return Result.error(404, "评价不存在");
        }

        Integer oldStatus = existing.getStatus();
        existing.setStatus(req.getApprove() ? 1 : 2);
        boolean ok = reviewService.updateById(existing);
        if (!ok) return Result.success(false);

        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(session.adminUserId());
        log.setAdminRoleId(session.adminRoleId());
        log.setAction("REVIEW_AUDIT");
        log.setDetail("reviewId=" + existing.getId() + ", oldStatus=" + oldStatus + ", newStatus=" + existing.getStatus()
                + ", remark=" + (req.getRemark() == null ? "" : req.getRemark()));
        adminOperationLogService.save(log);

        return Result.success(true);
    }
}

