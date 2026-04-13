package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.StudyPlan;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamMessage;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.entity.TeamRequestVO;
import com.campus.learningspace.entity.TeamJoinApplication;
import com.campus.learningspace.mapper.TeamJoinApplicationMapper;
import com.campus.learningspace.mapper.TeamMemberMapper;
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
    @Autowired
    private TeamMessageService teamMessageService;
    @Autowired
    private StudyPlanService studyPlanService;
    @Autowired
    private TeamJoinApplicationMapper teamJoinApplicationMapper;
    @Autowired
    private TeamMemberMapper teamMemberMapper;

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

        int expected = req.getExpectedCount() != null ? req.getExpectedCount() : 0;
        int current = req.getCurrentCount() != null ? req.getCurrentCount() : 0;
        if (expected > 0 && current >= expected) {
            // 已满员
            req.setStatus(2);
            updateById(req);
            return false;
        }

        // 是否已加入 / 是否存在历史记录（team_member 有唯一键 uk_team_user，会导致 deleted=1 时再次插入抛 500）
        // 注意：MyBatis-Plus 逻辑删除会默认过滤 deleted=1，所以这里用 mapper 自定义 SQL 查“包含 deleted=1”的记录
        TeamMember anyExist = teamMemberMapper.selectAnyByTeamAndUser(requestId, userId);
        if (anyExist != null) {
            if (anyExist.getDeleted() != null && anyExist.getDeleted() == 0) {
                // 已是成员
                return false;
            }
            // 只有创建者才允许保留 role=1；普通重新加入一律按成员处理
            Integer role = (anyExist.getRole() != null && anyExist.getRole() == 1) ? 1 : 2;
            LocalDateTime now = LocalDateTime.now();
            // 复活逻辑删除记录：不能用 updateById（会带 deleted=0 条件导致更新不到 deleted=1 的行）
            int revivedRows = teamMemberMapper.reviveDeletedMemberById(anyExist.getId(), role, now);
            if (revivedRows <= 0) {
                return false;
            }

            // current_count 可能因历史数据不一致而偏小：这里仍按 +1 处理
            int newCurrentRevive = current + 1;
            req.setCurrentCount(newCurrentRevive);
            if (expected > 0 && newCurrentRevive >= expected) {
                req.setStatus(2);
            } else {
                req.setStatus(1);
            }
            updateById(req);
            return true;
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

    /**
     * 退出小组：仅成员可退出，组长不可退出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(Long requestId, Long userId) {
        if (requestId == null || userId == null) {
            return false;
        }
        TeamRequest req = getById(requestId);
        if (req == null) {
            return false;
        }

        // 组长不能退出
        if (teamMemberService.isLeader(requestId, userId) || (req.getUserId() != null && req.getUserId().equals(userId))) {
            return false;
        }

        // 必须是当前成员
        LambdaQueryWrapper<TeamMember> memberW = new LambdaQueryWrapper<>();
        memberW.eq(TeamMember::getTeamRequestId, requestId)
                .eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getDeleted, 0);
        TeamMember exist = teamMemberService.getOne(memberW);
        if (exist == null) {
            return false;
        }

        // 逻辑删除成员关系
        boolean removed = teamMemberService.removeById(exist.getId());
        if (!removed) {
            return false;
        }

        int current = req.getCurrentCount() != null ? req.getCurrentCount() : 1;
        int newCurrent = Math.max(1, current - 1);
        req.setCurrentCount(newCurrent);

        int expected = req.getExpectedCount() != null ? req.getExpectedCount() : 0;
        if (req.getStatus() != null && req.getStatus() == 2) {
            // 由已满员恢复为招募中
            if (expected <= 0 || newCurrent < expected) {
                req.setStatus(1);
            }
        }
        updateById(req);
        return true;
    }

    /**
     * 删除小组：仅组长（创建者）可操作，执行级联逻辑删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeamByLeader(Long requestId, Long userId) {
        if (requestId == null || userId == null) return false;
        TeamRequest req = getById(requestId);
        if (req == null || (req.getDeleted() != null && req.getDeleted() == 1)) return false;

        boolean isLeader = (req.getUserId() != null && req.getUserId().equals(userId))
                || teamMemberService.isLeader(requestId, userId);
        if (!isLeader) return false;

        // 1) 小组成员逻辑删除
        LambdaQueryWrapper<TeamMember> memberW = new LambdaQueryWrapper<>();
        memberW.eq(TeamMember::getTeamRequestId, requestId)
                .eq(TeamMember::getDeleted, 0);
        teamMemberService.remove(memberW);

        // 2) 加入申请逻辑删除
        teamJoinApplicationMapper.delete(new LambdaQueryWrapper<TeamJoinApplication>()
                .eq(TeamJoinApplication::getTeamRequestId, requestId)
                .eq(TeamJoinApplication::getDeleted, 0));

        // 3) 学习计划逻辑删除
        LambdaQueryWrapper<StudyPlan> planW = new LambdaQueryWrapper<>();
        planW.eq(StudyPlan::getTeamRequestId, requestId)
                .eq(StudyPlan::getDeleted, 0);
        studyPlanService.remove(planW);

        // 4) 历史消息逻辑删除（即使前端已下线聊天，也保持数据一致）
        LambdaQueryWrapper<TeamMessage> msgW = new LambdaQueryWrapper<>();
        msgW.eq(TeamMessage::getTeamRequestId, requestId)
                .eq(TeamMessage::getDeleted, 0);
        teamMessageService.remove(msgW);

        // 5) 小组逻辑删除
        return removeById(requestId);
    }
}

