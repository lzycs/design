package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamMemberVO;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.entity.TeamRequestVO;
import com.campus.learningspace.entity.TeamMessage;
import com.campus.learningspace.service.TeamMemberService;
import com.campus.learningspace.service.TeamRequestService;
import com.campus.learningspace.service.TeamMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/request/{id}")
    public Result<TeamRequestVO> getRequestById(@PathVariable Long id) {
        TeamRequestVO vo = teamRequestService.getTeamRequestVOById(id);
        return vo != null ? Result.success(vo) : Result.error(404, "协作不存在");
    }

    @GetMapping("/requests/active")
    public Result<List<TeamRequest>> getActiveRequests() {
        return Result.success(teamRequestService.getActiveRequests());
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
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus()); // 0-已关闭(已完成) 1-招募中 2-已满员
            teamRequestService.updateById(existing);
        }
        return Result.success(true);
    }

    @PostMapping("/request/{requestId}/join")
    public Result<Boolean> joinTeam(@PathVariable Long requestId, @RequestBody TeamMember member) {
        member.setId(null);
        member.setTeamRequestId(requestId);
        if (member.getRole() == null) {
            member.setRole(2); // 成员
        }
        return Result.success(teamMemberService.save(member));
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
}

