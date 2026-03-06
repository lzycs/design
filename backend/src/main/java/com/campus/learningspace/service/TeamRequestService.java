package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.entity.TeamRequestVO;
import com.campus.learningspace.mapper.TeamRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamRequestService extends ServiceImpl<TeamRequestMapper, TeamRequest> {

    @Autowired
    private TeamMemberService teamMemberService;

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

    public List<TeamRequestVO> getUserTeamVOList(Long userId) {
        return baseMapper.selectUserTeamVOList(userId);
    }

    public TeamRequestVO getTeamRequestVOById(Long id) {
        return baseMapper.selectTeamRequestVOById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public TeamRequest createRequestWithCreator(TeamRequest request) {
        request.setId(null);
        if (request.getStatus() == null) {
            request.setStatus(1); // 招募中
        }
        if (request.getCurrentCount() == null) {
            request.setCurrentCount(1);
        }
        save(request);
        if (request.getId() != null && request.getUserId() != null) {
            TeamMember creator = new TeamMember();
            creator.setTeamRequestId(request.getId());
            creator.setUserId(request.getUserId());
            creator.setRole(1); // 创建者
            teamMemberService.save(creator);
        }
        return request;
    }
}

