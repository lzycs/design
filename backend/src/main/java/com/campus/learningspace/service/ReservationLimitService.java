package com.campus.learningspace.service;

import com.campus.learningspace.common.ReservationRuleException;
import com.campus.learningspace.config.ReservationProperties;
import com.campus.learningspace.dto.ReservationLimitUpdateDTO;
import com.campus.learningspace.dto.ReservationLimitVO;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.entity.ReservationLimitConfig;
import com.campus.learningspace.mapper.ReservationLimitConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ReservationLimitService {

    @Autowired
    private ReservationLimitConfigMapper reservationLimitConfigMapper;

    @Autowired
    private ReservationProperties reservationProperties;

    public ReservationLimitVO getEffectiveLimits() {
        ReservationLimitConfig row = reservationLimitConfigMapper.selectById(1);
        if (row == null || row.getMaxPerWeek() == null || row.getMaxDurationMinutes() == null) {
            return new ReservationLimitVO(
                    reservationProperties.getMaxPerWeek(),
                    reservationProperties.getMaxDurationMinutes());
        }
        return new ReservationLimitVO(row.getMaxPerWeek(), row.getMaxDurationMinutes());
    }

    /**
     * 新建预约前校验：仅保留单次时长限制，不再限制每周预约次数
     */
    public void assertNewReservationAllowed(Reservation r) {
        if (r.getUserId() == null) {
            throw new ReservationRuleException("用户信息无效，无法预约");
        }
        ReservationLimitVO limits = getEffectiveLimits();
        int durationMin = resolveDurationMinutes(r);
        if (durationMin < 1) {
            throw new ReservationRuleException("预约时长无效");
        }
        if (durationMin > limits.getMaxDurationMinutes()) {
            int max = limits.getMaxDurationMinutes();
            if (max % 60 == 0) {
                throw new ReservationRuleException("单次预约最长 " + (max / 60) + " 小时");
            }
            throw new ReservationRuleException("单次预约最长 " + max + " 分钟");
        }
        LocalDate resDate = r.getReservationDate();
        if (resDate == null) {
            throw new ReservationRuleException("请选择预约日期");
        }
    }

    private static int resolveDurationMinutes(Reservation r) {
        if (r.getDuration() != null && r.getDuration() > 0) {
            return r.getDuration();
        }
        if (r.getStartTime() != null && r.getEndTime() != null) {
            int start = r.getStartTime().toSecondOfDay() / 60;
            int end = r.getEndTime().toSecondOfDay() / 60;
            return end - start;
        }
        return 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReservationLimitVO updateLimits(ReservationLimitUpdateDTO dto) {
        if (dto.getMaxPerWeek() == null || dto.getMaxDurationMinutes() == null) {
            throw new ReservationRuleException("请填写完整的上限配置");
        }
        int mw = dto.getMaxPerWeek();
        int md = dto.getMaxDurationMinutes();
        if (mw < 1 || mw > 200) {
            throw new ReservationRuleException("每周预约次数须在 1～200 之间");
        }
        if (md < 15 || md > 24 * 60) {
            throw new ReservationRuleException("单次最长时长须在 15～1440 分钟之间");
        }
        ReservationLimitConfig row = reservationLimitConfigMapper.selectById(1);
        if (row == null) {
            row = new ReservationLimitConfig();
            row.setId(1);
            row.setMaxPerWeek(mw);
            row.setMaxDurationMinutes(md);
            reservationLimitConfigMapper.insert(row);
        } else {
            row.setMaxPerWeek(mw);
            row.setMaxDurationMinutes(md);
            reservationLimitConfigMapper.updateById(row);
        }
        return new ReservationLimitVO(mw, md);
    }
}
