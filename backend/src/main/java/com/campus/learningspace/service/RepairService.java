package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Repair;
import com.campus.learningspace.entity.RepairVO;
import com.campus.learningspace.mapper.RepairMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepairService extends ServiceImpl<RepairMapper, Repair> {

    public List<RepairVO> getUserRepairVOList(Long userId) {
        return baseMapper.selectUserRepairVOList(userId);
    }

    public RepairVO getRepairVOById(Long id) {
        return baseMapper.selectRepairVOById(id);
    }

    public List<Repair> getUserRepairs(Long userId) {
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Repair::getUserId, userId)
                .orderByDesc(Repair::getCreateTime);
        return list(wrapper);
    }

    public List<Repair> getPendingRepairs() {
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Repair::getStatus, 1, 2); // 1-待处理, 2-处理中
        return list(wrapper);
    }
}

