package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.ScanDevice;
import com.campus.learningspace.mapper.ScanDeviceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ScanDeviceService extends ServiceImpl<ScanDeviceMapper, ScanDevice> {

    /**
     * 根据 deviceUid 获取是否存在且启用
     */
    public boolean isEnabled(String deviceUid) {
        if (deviceUid == null || deviceUid.isBlank()) return false;
        LambdaQueryWrapper<ScanDevice> qw = new LambdaQueryWrapper<>();
        qw.eq(ScanDevice::getDeviceUid, deviceUid)
                .eq(ScanDevice::getDeleted, 0)
                .eq(ScanDevice::getEnabled, 1);
        return count(qw) > 0;
    }

    /**
     * 获取或创建授权设备（默认启用）
     */
    @Transactional(rollbackFor = Exception.class)
    public ScanDevice getOrCreateEnabled(String deviceUid, String deviceName) {
        LambdaQueryWrapper<ScanDevice> qw = new LambdaQueryWrapper<>();
        qw.eq(ScanDevice::getDeviceUid, deviceUid)
                .eq(ScanDevice::getDeleted, 0);
        ScanDevice exist = getOne(qw);
        if (exist != null) {
            exist.setEnabled(1);
            if (deviceName != null && !deviceName.isBlank()) {
                exist.setDeviceName(deviceName);
            }
            updateById(exist);
            return exist;
        }
        ScanDevice d = new ScanDevice();
        d.setDeviceUid(deviceUid);
        d.setDeviceName(deviceName == null ? "" : deviceName);
        d.setEnabled(1);
        d.setDeleted(0);
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        save(d);
        return d;
    }
}

