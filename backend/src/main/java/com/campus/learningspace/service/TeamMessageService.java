package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.TeamMessage;
import com.campus.learningspace.mapper.TeamMessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamMessageService extends ServiceImpl<TeamMessageMapper, TeamMessage> {

    public List<TeamMessage> getMessagesByTeam(Long teamRequestId, Integer limit) {
        LambdaQueryWrapper<TeamMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMessage::getTeamRequestId, teamRequestId)
                .orderByDesc(TeamMessage::getCreateTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        List<TeamMessage> list = list(wrapper);
        // 前端按时间正序展示
        list.sort((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()));
        return list;
    }
}

