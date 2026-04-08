package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.ScanDevice;
import com.campus.learningspace.entity.ScanDevicePairToken;
import com.campus.learningspace.service.AdminAuthService;
import com.campus.learningspace.service.AdminPermissionService;
import com.campus.learningspace.service.ScanDevicePairTokenService;
import com.campus.learningspace.service.ScanDeviceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/scan-devices")
@CrossOrigin
public class AdminScanDeviceController {

    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final ScanDeviceService scanDeviceService;
    private final ScanDevicePairTokenService scanDevicePairTokenService;

    public AdminScanDeviceController(AdminAuthService adminAuthService,
                                      AdminPermissionService adminPermissionService,
                                      ScanDeviceService scanDeviceService,
                                      ScanDevicePairTokenService scanDevicePairTokenService) {
        this.adminAuthService = adminAuthService;
        this.adminPermissionService = adminPermissionService;
        this.scanDeviceService = scanDeviceService;
        this.scanDevicePairTokenService = scanDevicePairTokenService;
    }

    /**
     * 生成一次性设备配对 token（前端编码二维码展示给设备端扫码）
     */
    @PostMapping("/pair-token")
    public Result<Map<String, Object>> generatePairToken(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) return Result.error(403, "没有权限");

        // 默认 token 有效期 10 分钟（配对完成后即失效）
        int expireMinutes = 10;
        ScanDevicePairToken t = scanDevicePairTokenService.generateToken(expireMinutes);

        return Result.success(Map.of(
                "token", t.getToken(),
                "expiredTime", t.getExpiredTime()
        ));
    }

    @GetMapping
    public Result<List<ScanDevice>> list(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) return Result.error(403, "没有权限");

        List<ScanDevice> list = scanDeviceService.lambdaQuery()
                .eq(ScanDevice::getDeleted, 0)
                .list();
        return Result.success(list);
    }

    /**
     * 启用/禁用授权设备
     * body: { enabled, deviceName? }
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateDevice(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) return Result.error(403, "没有权限");

        if (id == null) return Result.error(400, "设备ID不能为空");
        ScanDevice exist = scanDeviceService.getById(id);
        if (exist == null || (exist.getDeleted() != null && exist.getDeleted() == 1)) return Result.error(404, "设备不存在");

        Object enabledObj = body.get("enabled");
        if (enabledObj != null) {
            exist.setEnabled(Integer.parseInt(enabledObj.toString()));
        }
        Object nameObj = body.get("deviceName");
        if (nameObj != null) {
            exist.setDeviceName(String.valueOf(nameObj));
        }

        return Result.success(scanDeviceService.updateById(exist));
    }

    /**
     * 删除授权设备（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteDevice(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                        @PathVariable Long id) {
        var session = adminAuthService.getSession(token);
        if (session == null) return Result.error(401, "未登录或登录已过期");
        if (!adminPermissionService.hasPermission(session, "base:manage")) return Result.error(403, "没有权限");

        if (id == null) return Result.error(400, "设备ID不能为空");
        ScanDevice exist = scanDeviceService.getById(id);
        if (exist == null || (exist.getDeleted() != null && exist.getDeleted() == 1)) return Result.error(404, "设备不存在");

        return Result.success(scanDeviceService.removeById(id));
    }
}

