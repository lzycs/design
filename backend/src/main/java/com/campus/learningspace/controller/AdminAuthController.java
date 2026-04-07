package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.AdminLoginResponse;
import com.campus.learningspace.entity.AdminMenu;
import com.campus.learningspace.entity.AdminOverviewVO;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.entity.ClassroomFeedback;
import com.campus.learningspace.entity.Repair;
import com.campus.learningspace.service.AdminAuthService;
import com.campus.learningspace.service.AdminPermissionService;
import com.campus.learningspace.service.ClassroomFeedbackService;
import com.campus.learningspace.service.ClassroomService;
import com.campus.learningspace.service.RepairService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final RepairService repairService;
    private final ClassroomFeedbackService classroomFeedbackService;
    private final ClassroomService classroomService;

    public AdminAuthController(AdminAuthService adminAuthService,
                                AdminPermissionService adminPermissionService,
                                RepairService repairService,
                                ClassroomFeedbackService classroomFeedbackService,
                                ClassroomService classroomService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.repairService = repairService;
        this.classroomFeedbackService = classroomFeedbackService;
        this.classroomService = classroomService;
    }

    public static class AdminLoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@RequestBody AdminLoginRequest req) {
        AdminLoginResponse resp = adminAuthService.login(req.getUsername(), req.getPassword());
        if (resp == null) {
            return Result.error(401, "用户名或密码错误，或账号不具备管理员权限");
        }
        return Result.success(resp);
    }

    @GetMapping("/menus")
    public Result<List<AdminMenu>> menus(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        return Result.success(adminPermissionService.listMenus(session));
    }

    @GetMapping("/overview")
    public Result<AdminOverviewVO> overview(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");

        AdminOverviewVO vo = new AdminOverviewVO();

        if (adminPermissionService.hasPermission(session, "repair:process")) {
            LambdaQueryWrapper<Repair> w = new LambdaQueryWrapper<>();
            w.eq(Repair::getStatus, 1);
            w.eq(Repair::getDeleted, 0);
            long pending = repairService.count(w);
            vo.setPendingRepairCount((int) pending);
        } else {
            vo.setPendingRepairCount(0);
        }

        if (adminPermissionService.hasPermission(session, "review:audit")) {
            LambdaQueryWrapper<ClassroomFeedback> w = new LambdaQueryWrapper<>();
            w.eq(ClassroomFeedback::getStatus, ClassroomFeedback.STATUS_PENDING_AUDIT);
            w.eq(ClassroomFeedback::getDeleted, 0);
            long pending = classroomFeedbackService.count(w);
            vo.setPendingReviewCount((int) pending);
        } else {
            vo.setPendingReviewCount(0);
        }

        if (adminPermissionService.hasPermission(session, "base:manage")) {
            LambdaQueryWrapper<Classroom> w = new LambdaQueryWrapper<>();
            w.eq(Classroom::getStatus, 1);
            w.eq(Classroom::getDeleted, 0);
            long totalClassrooms = classroomService.count(w);
            vo.setClassroomTotal((int) totalClassrooms);
        } else {
            vo.setClassroomTotal(0);
        }

        return Result.success(vo);
    }
}

