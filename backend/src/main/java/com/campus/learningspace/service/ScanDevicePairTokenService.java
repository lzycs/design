package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.ScanDevicePairToken;
import com.campus.learningspace.mapper.ScanDevicePairTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ScanDevicePairTokenService extends ServiceImpl<ScanDevicePairTokenMapper, ScanDevicePairToken> {

    public ScanDevicePairToken generateToken(int expireMinutes) {
        LocalDateTime now = LocalDateTime.now();
        ScanDevicePairToken t = new ScanDevicePairToken();
        t.setToken(UUID.randomUUID().toString().replace("-", ""));
        t.setExpiredTime(now.plusMinutes(Math.max(1, expireMinutes)));
        t.setUsedStatus(0);
        t.setDeleted(0);
        t.setCreateTime(now);
        t.setUpdateTime(now);
        save(t);
        return t;
    }

    public ScanDevicePairToken getValidUnused(String token) {
        if (token == null || token.isBlank()) return null;
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ScanDevicePairToken> qw = new LambdaQueryWrapper<>();
        qw.eq(ScanDevicePairToken::getToken, token)
                .eq(ScanDevicePairToken::getDeleted, 0)
                .eq(ScanDevicePairToken::getUsedStatus, 0)
                .gt(ScanDevicePairToken::getExpiredTime, now);
        return getOne(qw);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean redeemToken(String token, String deviceUid) {
        if (deviceUid == null || deviceUid.isBlank()) return false;
        ScanDevicePairToken t = getValidUnused(token);
        if (t == null) return false;

        t.setUsedStatus(1);
        t.setUsedTime(LocalDateTime.now());
        t.setRedeemedDeviceUid(deviceUid);
        return updateById(t);
    }
}

