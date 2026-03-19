package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.TeamJoinApplication;
import com.campus.learningspace.entity.TeamJoinApplicationVO;
import com.campus.learningspace.mapper.TeamJoinApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamJoinApplicationService extends ServiceImpl<TeamJoinApplicationMapper, TeamJoinApplication> {

    @Autowired
    private TeamRequestService teamRequestService;

    /**
     * 提交加入申请（同一人对同一小组只能有一条待审核申请）
     */
    public boolean applyToJoin(Long teamRequestId, Long applicantId, String reason) {
        // 已存在待审核申请则不重复提交
        LambdaQueryWrapper<TeamJoinApplication> qw = new LambdaQueryWrapper<>();
        qw.eq(TeamJoinApplication::getTeamRequestId, teamRequestId)
          .eq(TeamJoinApplication::getApplicantId, applicantId)
          .eq(TeamJoinApplication::getStatus, 0)
          .eq(TeamJoinApplication::getDeleted, 0);
        if (count(qw) > 0) {
            return false;
        }
        TeamJoinApplication app = new TeamJoinApplication();
        app.setTeamRequestId(teamRequestId);
        app.setApplicantId(applicantId);
        app.setReason(reason);
        app.setStatus(0);
        return save(app);
    }

    /**
     * 组长审核申请：approve=true 通过，false 拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean review(Long applicationId, Long leaderId, boolean approve) {
        TeamJoinApplication app = getById(applicationId);
        if (app == null || app.getStatus() != 0) {
            return false;
        }
        // 校验 leaderId 是否为该小组组长
        boolean isLeader = baseMapper.selectPendingByLeader(leaderId)
                .stream().anyMatch(v -> v.getId().equals(applicationId));
        if (!isLeader) {
            return false;
        }
        app.setStatus(approve ? 1 : 2);
        updateById(app);

        if (approve) {
            // 通过则自动加入小组
            teamRequestService.joinTeam(app.getTeamRequestId(), app.getApplicantId());
        }
        return true;
    }

    /** 组长的待审核申请列表 */
    public List<TeamJoinApplicationVO> getPendingForLeader(Long leaderId) {
        return baseMapper.selectPendingByLeader(leaderId);
    }

    /** 成员的申请结果列表 */
    public List<TeamJoinApplicationVO> getResultsForApplicant(Long applicantId) {
        return baseMapper.selectResultsByApplicant(applicantId);
    }

    /** 组长未处理申请数量（用于红点） */
    public int countPendingForLeader(Long leaderId) {
        return baseMapper.countPendingForLeader(leaderId);
    }

    /** 成员审核结果数量（用于红点） */
    public int countResultsForApplicant(Long applicantId) {
        return baseMapper.countResultsForApplicant(applicantId);
    }
}
