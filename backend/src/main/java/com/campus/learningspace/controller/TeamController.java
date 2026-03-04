package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.service.TeamMemberService;
import com.campus.learningspace.service.TeamRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/team")
@CrossOrigin
public class TeamController {

    @Autowired
    private TeamRequestService teamRequestService;

    @Autowired
    private TeamMemberService teamMemberService;

    @GetMapping("/request/{id}")
    public Result<TeamRequest> getRequestById(@PathVariable Long id) {
        return Result.success(teamRequestService.getById(id));
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
        request.setId(null);
        if (request.getStatus() == null) {
            request.setStatus(1); // 招募中
        }
        if (request.getCurrentCount() == null) {
            request.setCurrentCount(1);
        }
        teamRequestService.save(request);
        return Result.success(request);
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
    public Result<List<TeamRequest>> getUserTeams(@PathVariable Long userId) {
        List<TeamMember> memberships = teamMemberService.getUserTeams(userId);
        Set<Long> requestIds = memberships.stream()
                .map(TeamMember::getTeamRequestId)
                .collect(Collectors.toSet());
        if (requestIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<TeamRequest> requests = teamRequestService.listByIds(requestIds);
        return Result.success(requests);
    }
}

