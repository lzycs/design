package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.ScanDevice;
import com.campus.learningspace.entity.ScanDevicePairToken;
import com.campus.learningspace.service.ScanDevicePairTokenService;
import com.campus.learningspace.service.ScanDeviceService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scan-devices")
@CrossOrigin
public class ScanDeviceAuthorizeController {

    private final ScanDevicePairTokenService scanDevicePairTokenService;
    private final ScanDeviceService scanDeviceService;

    public ScanDeviceAuthorizeController(ScanDevicePairTokenService scanDevicePairTokenService,
                                         ScanDeviceService scanDeviceService) {
        this.scanDevicePairTokenService = scanDevicePairTokenService;
        this.scanDeviceService = scanDeviceService;
    }

    /**
     * 设备扫码配对 token 后兑换授权：绑定 deviceUid
     * body: { token, deviceUid, deviceName? }
     */
    @PostMapping("/authorize")
    public Result<Boolean> authorize(@RequestBody Map<String, Object> body) {
        if (body == null) return Result.error(400, "参数不能为空");
        Object tokenObj = body.get("token");
        Object deviceUidObj = body.get("deviceUid");
        if (tokenObj == null) return Result.error(400, "缺少 token");
        if (deviceUidObj == null) return Result.error(400, "缺少 deviceUid");

        String token = String.valueOf(tokenObj);
        String deviceUid = String.valueOf(deviceUidObj);
        String deviceName = body.get("deviceName") == null ? "" : String.valueOf(body.get("deviceName"));

        boolean ok = scanDevicePairTokenService.redeemToken(token, deviceUid);
        if (!ok) {
            return Result.error(400, "配对 token 无效或已过期/已使用");
        }

        ScanDevice d = scanDeviceService.getOrCreateEnabled(deviceUid, deviceName);
        return Result.success(d != null);
    }
}

