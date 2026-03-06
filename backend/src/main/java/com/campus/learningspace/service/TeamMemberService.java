package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamMemberVO;
import com.campus.learningspace.mapper.TeamMemberMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamMemberService extends ServiceImpl<TeamMemberMapper, TeamMember> {

    public List<TeamMember> getUserTeams(Long userId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getUserId, userId);
        return list(wrapper);
    }

    public List<TeamMember> getTeamMembers(Long teamRequestId) {
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamRequestId, teamRequestId);
        return list(wrapper);
    }

    public List<TeamMemberVO> getTeamMemberVOList(Long teamRequestId) {
        return baseMapper.selectMemberVOListByTeam(teamRequestId);
    }
}

