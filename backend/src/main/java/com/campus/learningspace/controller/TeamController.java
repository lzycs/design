package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.TeamJoinApplicationVO;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamMemberVO;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.entity.TeamRequestVO;
import com.campus.learningspace.entity.TeamMessage;
import com.campus.learningspace.service.TeamJoinApplicationService;
import com.campus.learningspace.service.TeamMemberService;
import com.campus.learningspace.service.TeamRequestService;
import com.campus.learningspace.service.TeamMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team")
@CrossOrigin
public class TeamController {

    @Autowired
    private TeamRequestService teamRequestService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private TeamMessageService teamMessageService;

    @Autowired
    private TeamJoinApplicationService teamJoinApplicationService;

    @GetMapping("/request/{id}")
    public Result<TeamRequestVO> getRequestById(@PathVariable Long id) {
        TeamRequestVO vo = teamRequestService.getTeamRequestVOById(id);
        return vo != null ? Result.success(vo) : Result.error(404, "协作不存在");
    }

    @GetMapping("/requests/active")
    public Result<List<TeamRequestVO>> getActiveRequests() {
        return Result.success(teamRequestService.getActiveRequestVOList());
    }

    @GetMapping("/requests/user/{userId}")
    public Result<List<TeamRequest>> getUserRequests(@PathVariable Long userId) {
        return Result.success(teamRequestService.getUserRequests(userId));
    }

    @PostMapping("/request")
    public Result<TeamRequest> createRequest(@RequestBody TeamRequest request) {
        teamRequestService.createRequestWithCreator(request);
        return Result.success(request);
    }

    @PutMapping("/request/{id}/status")
    public Result<Boolean> updateRequestStatus(@PathVariable Long id, @RequestBody TeamRequest payload) {
        TeamRequest existing = teamRequestService.getById(id);
        if (existing == null) {
            return Result.error(404, "协作不存在");
        }
        if (payload == null || payload.getUserId() == null) {
            return Result.error(400, "缺少 userId");
        }
        // 仅组长有权限标记完成/变更状态
        boolean isLeader = teamMemberService.isLeader(id, payload.getUserId());
        if (!isLeader) {
            return Result.error(403, "仅组长可标记完成");
        }
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus()); // 0-已关闭(已完成) 1-招募中 2-已满员
            teamRequestService.updateById(existing);
        }
        return Result.success(true);
    }

    @PostMapping("/request/{requestId}/join")
    public Result<Boolean> joinTeam(@PathVariable Long requestId, @RequestBody TeamMember member) {
        if (member == null || member.getUserId() == null) {
            return Result.error(400, "缺少 userId");
        }
        boolean ok = teamRequestService.joinTeam(requestId, member.getUserId());
        return ok ? Result.success(true) : Result.error(400, "加入失败（可能已加入或已满员）");
    }

    @GetMapping("/user/{userId}")
    public Result<List<TeamRequestVO>> getUserTeams(@PathVariable Long userId) {
        return Result.success(teamRequestService.getUserTeamVOList(userId));
    }

    @GetMapping("/request/{requestId}/members")
    public Result<List<TeamMemberVO>> getMembers(@PathVariable Long requestId) {
        return Result.success(teamMemberService.getTeamMemberVOList(requestId));
    }

    @GetMapping("/request/{requestId}/messages")
    public Result<List<TeamMessage>> getMessages(@PathVariable Long requestId,
                                                 @RequestParam(required = false) Integer limit) {
        return Result.success(teamMessageService.getMessagesByTeam(requestId, limit));
    }

    @PostMapping("/request/{requestId}/messages")
    public Result<TeamMessage> sendMessage(@PathVariable Long requestId, @RequestBody TeamMessage message) {
        message.setId(null);
        message.setTeamRequestId(requestId);
        if (message.getType() == null) {
            message.setType(1);
        }
        teamMessageService.save(message);
        return Result.success(message);
    }

    // ===== 加入申请相关接口 =====

    /**
     * 提交加入申请
     * POST /api/team/request/{requestId}/apply
     * body: { applicantId, reason }
     */
    @PostMapping("/request/{requestId}/apply")
    public Result<Boolean> applyToJoin(@PathVariable Long requestId,
                                       @RequestBody Map<String, Object> body) {
        Object applicantIdObj = body.get("applicantId");
        if (applicantIdObj == null) {
            return Result.error(400, "缺少 applicantId");
        }
        Long applicantId = Long.valueOf(applicantIdObj.toString());
        String reason = body.getOrDefault("reason", "").toString();
        boolean ok = teamJoinApplicationService.applyToJoin(requestId, applicantId, reason);
        return ok ? Result.success(true) : Result.error(400, "已存在待审核申请或申请失败");
    }

    /**
     * 组长审核申请
     * POST /api/team/application/{applicationId}/review
     * body: { leaderId, approve }
     */
    @PostMapping("/application/{applicationId}/review")
    public Result<Boolean> reviewApplication(@PathVariable Long applicationId,
                                             @RequestBody Map<String, Object> body) {
        Object leaderIdObj = body.get("leaderId");
        if (leaderIdObj == null) {
            return Result.error(400, "缺少 leaderId");
        }
        Long leaderId = Long.valueOf(leaderIdObj.toString());
        boolean approve = Boolean.parseBoolean(body.getOrDefault("approve", "false").toString());
        boolean ok = teamJoinApplicationService.review(applicationId, leaderId, approve);
        return ok ? Result.success(true) : Result.error(400, "审核失败（权限不足或申请已处理）");
    }

    /**
     * 获取组长的待审核申请列表
     * GET /api/team/applications/pending?leaderId=xxx
     */
    @GetMapping("/applications/pending")
    public Result<List<TeamJoinApplicationVO>> getPendingApplications(
            @RequestParam Long leaderId) {
        return Result.success(teamJoinApplicationService.getPendingForLeader(leaderId));
    }

    /**
     * 获取成员的申请结果列表
     * GET /api/team/applications/results?applicantId=xxx
     */
    @GetMapping("/applications/results")
    public Result<List<TeamJoinApplicationVO>> getApplicationResults(
            @RequestParam Long applicantId) {
        return Result.success(teamJoinApplicationService.getResultsForApplicant(applicantId));
    }

    /**
     * 获取红点数量（组长：待审核数；成员：有审核结果数）
     * GET /api/team/badge?userId=xxx
     */
    @GetMapping("/badge")
    public Result<Map<String, Integer>> getBadge(@RequestParam Long userId) {
        int pendingCount = teamJoinApplicationService.countPendingForLeader(userId);
        int resultCount = teamJoinApplicationService.countResultsForApplicant(userId);
        return Result.success(Map.of(
                "pendingCount", pendingCount,
                "resultCount", resultCount,
                "total", pendingCount + resultCount
        ));
    }
}

