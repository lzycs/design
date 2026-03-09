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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeamRequestService extends ServiceImpl<TeamRequestMapper, TeamRequest> {

    @Autowired
    private TeamMemberService teamMemberService;

    public List<TeamRequest> getActiveRequests() {
        LambdaQueryWrapper<TeamRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TeamRequest::getStatus, 1, 2) // 招募中/已满员（广场也要展示）
                .orderByDesc(TeamRequest::getCreateTime);
        return list(wrapper);
    }

    public List<TeamRequestVO> getActiveRequestVOList() {
        return baseMapper.selectActiveTeamVOList();
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

    /**
     * 加入小组：校验是否存在/是否已加入/是否满员，并同步更新当前人数与状态
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(Long requestId, Long userId) {
        if (requestId == null || userId == null) {
            return false;
        }
        TeamRequest req = getById(requestId);
        if (req == null) {
            return false;
        }
        // 已关闭的小组不允许加入
        if (req.getStatus() != null && req.getStatus() == 0) {
            return false;
        }

        // 是否已加入（利用唯一索引前先做查询，避免抛 500）
        LambdaQueryWrapper<TeamMember> existsW = new LambdaQueryWrapper<>();
        existsW.eq(TeamMember::getTeamRequestId, requestId)
                .eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getDeleted, 0);
        if (teamMemberService.count(existsW) > 0) {
            return false;
        }

        int expected = req.getExpectedCount() != null ? req.getExpectedCount() : 0;
        int current = req.getCurrentCount() != null ? req.getCurrentCount() : 0;
        if (expected > 0 && current >= expected) {
            // 已满员
            req.setStatus(2);
            updateById(req);
            return false;
        }

        TeamMember m = new TeamMember();
        m.setId(null);
        m.setTeamRequestId(requestId);
        m.setUserId(userId);
        m.setRole(2);
        m.setJoinTime(LocalDateTime.now());
        boolean saved = teamMemberService.save(m);
        if (!saved) {
            return false;
        }

        int newCurrent = current + 1;
        req.setCurrentCount(newCurrent);
        if (expected > 0 && newCurrent >= expected) {
            req.setStatus(2);
        } else {
            req.setStatus(1);
        }
        updateById(req);
        return true;
    }
}

