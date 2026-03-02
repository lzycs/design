package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.mapper.TeamRequestMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamRequestService extends ServiceImpl<TeamRequestMapper, TeamRequest> {

    public List<TeamRequest> getActiveRequests() {
        LambdaQueryWrapper<TeamRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamRequest::getStatus, 1) // 招募中
                .orderByDesc(TeamRequest::getCreateTime);
        return list(wrapper);
    }

    public List<TeamRequest> getUserRequests(Long userId) {
        LambdaQueryWrapper<TeamRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamRequest::getUserId, userId)
                .orderByDesc(TeamRequest::getCreateTime);
        return list(wrapper);
    }
}

