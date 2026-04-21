package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.learningspace.dto.TeamChatDtos;
import com.campus.learningspace.entity.TeamMember;
import com.campus.learningspace.entity.TeamMessage;
import com.campus.learningspace.entity.TeamMessageRead;
import com.campus.learningspace.entity.TeamRequest;
import com.campus.learningspace.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class TeamChatService {
    @Autowired
    private TeamMemberService teamMemberService;
    @Autowired
    private TeamMessageService teamMessageService;
    @Autowired
    private TeamMessageReadService teamMessageReadService;
    @Autowired
    private TeamRequestService teamRequestService;
    @Autowired
    private UserService userService;

    public boolean isMember(Long teamRequestId, Long userId) {
        if (teamRequestId == null || userId == null) return false;
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMember::getTeamRequestId, teamRequestId)
                .eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getDeleted, 0);
        return teamMemberService.count(wrapper) > 0;
    }

    public TeamChatDtos.MessagePage getHistory(Long teamRequestId, Long userId, Long beforeId, Integer size) {
        if (!isMember(teamRequestId, userId)) {
            throw new IllegalArgumentException("你不是该小组成员");
        }
        int pageSize = Math.min(Math.max(size == null ? 100 : size, 1), 100);
        List<TeamMessage> rows = teamMessageService.getBaseMapper().selectHistory(teamRequestId, beforeId, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }
        rows.sort(Comparator.comparing(TeamMessage::getId));

        List<TeamChatDtos.MessageItem> items = rows.stream().map(this::toMessageItem).toList();
        TeamChatDtos.MessagePage page = new TeamChatDtos.MessagePage();
        page.setList(items);
        page.setHasMore(hasMore);
        page.setNextCursor(items.isEmpty() ? null : items.get(0).getId());
        return page;
    }

    @Transactional(rollbackFor = Exception.class)
    public TeamMessage sendMessage(Long teamRequestId, Long userId, Integer type, String content, String fileName, Long fileSize, String clientMsgId) {
        if (!isMember(teamRequestId, userId)) {
            throw new IllegalArgumentException("你不是该小组成员");
        }
        String text = content == null ? "" : content.trim();
        int msgType = type == null ? 1 : type;
        if (msgType == 1 && text.isEmpty()) throw new IllegalArgumentException("消息内容不能为空");
        if (text.length() > 2000) throw new IllegalArgumentException("消息内容过长");

        if (clientMsgId != null && !clientMsgId.isBlank()) {
            LambdaQueryWrapper<TeamMessage> dedupW = new LambdaQueryWrapper<>();
            dedupW.eq(TeamMessage::getTeamRequestId, teamRequestId)
                    .eq(TeamMessage::getSenderId, userId)
                    .eq(TeamMessage::getClientMsgId, clientMsgId)
                    .eq(TeamMessage::getDeleted, 0)
                    .last("LIMIT 1");
            TeamMessage existing = teamMessageService.getOne(dedupW);
            if (existing != null) return existing;
        }

        TeamRequest team = teamRequestService.getById(teamRequestId);
        if (team == null || Objects.equals(team.getDeleted(), 1)) {
            throw new IllegalArgumentException("小组不存在");
        }
        TeamMessage message = new TeamMessage();
        message.setTeamRequestId(teamRequestId);
        message.setSenderId(userId);
        message.setSenderName(resolveSenderName(userId));
        message.setType(msgType);
        message.setContent(text);
        message.setFileName(fileName);
        message.setFileSize(fileSize);
        message.setClientMsgId(clientMsgId);
        message.setStatus(1);
        message.setRecalled(0);
        teamMessageService.save(message);
        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public TeamMessage recallMessage(Long teamRequestId, Long messageId, Long userId) {
        if (!isMember(teamRequestId, userId)) {
            throw new IllegalArgumentException("你不是该小组成员");
        }
        TeamMessage message = teamMessageService.getById(messageId);
        if (message == null || !Objects.equals(message.getTeamRequestId(), teamRequestId) || Objects.equals(message.getDeleted(), 1)) {
            throw new IllegalArgumentException("消息不存在");
        }
        if (!Objects.equals(message.getSenderId(), userId)) {
            throw new IllegalArgumentException("仅发送者可撤回");
        }
        if (Objects.equals(message.getRecalled(), 1)) return message;
        LocalDateTime deadline = message.getCreateTime() == null ? LocalDateTime.MIN : message.getCreateTime().plusMinutes(5);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalArgumentException("仅支持撤回5分钟内的消息");
        }
        message.setRecalled(1);
        message.setStatus(2);
        message.setType(1);
        message.setContent("[消息已撤回]");
        message.setFileName(null);
        message.setFileSize(null);
        teamMessageService.updateById(message);
        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long teamRequestId, Long userId, Long lastReadMessageId) {
        if (!isMember(teamRequestId, userId)) {
            throw new IllegalArgumentException("你不是该小组成员");
        }
        long cursor = lastReadMessageId == null ? 0L : Math.max(0L, lastReadMessageId);
        LambdaQueryWrapper<TeamMessageRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamMessageRead::getTeamRequestId, teamRequestId)
                .eq(TeamMessageRead::getUserId, userId)
                .eq(TeamMessageRead::getDeleted, 0)
                .last("LIMIT 1");
        TeamMessageRead read = teamMessageReadService.getOne(wrapper);
        if (read == null) {
            read = new TeamMessageRead();
            read.setTeamRequestId(teamRequestId);
            read.setUserId(userId);
            read.setLastReadMessageId(cursor);
            read.setLastReadTime(LocalDateTime.now());
            teamMessageReadService.save(read);
            return;
        }
        if (read.getLastReadMessageId() == null || cursor > read.getLastReadMessageId()) {
            read.setLastReadMessageId(cursor);
        }
        read.setLastReadTime(LocalDateTime.now());
        teamMessageReadService.updateById(read);
    }

    public TeamChatDtos.UnreadSummary getUnreadSummary(Long userId) {
        List<TeamMember> memberships = teamMemberService.getUserTeams(userId).stream()
                .filter(m -> m.getDeleted() == null || m.getDeleted() == 0)
                .toList();
        List<TeamChatDtos.UnreadTeamItem> result = new ArrayList<>();
        long total = 0L;
        for (TeamMember member : memberships) {
            Long teamId = member.getTeamRequestId();
            if (teamId == null) continue;
            Long lastRead = 0L;
            LambdaQueryWrapper<TeamMessageRead> readW = new LambdaQueryWrapper<>();
            readW.eq(TeamMessageRead::getTeamRequestId, teamId)
                    .eq(TeamMessageRead::getUserId, userId)
                    .eq(TeamMessageRead::getDeleted, 0)
                    .last("LIMIT 1");
            TeamMessageRead read = teamMessageReadService.getOne(readW);
            if (read != null && read.getLastReadMessageId() != null) {
                lastRead = read.getLastReadMessageId();
            }
            Long unread = teamMessageService.getBaseMapper().countUnreadByTeam(teamId, lastRead);
            if (unread == null || unread <= 0) continue;

            TeamRequest team = teamRequestService.getById(teamId);
            LambdaQueryWrapper<TeamMessage> latestW = new LambdaQueryWrapper<>();
            latestW.eq(TeamMessage::getTeamRequestId, teamId)
                    .eq(TeamMessage::getDeleted, 0)
                    .orderByDesc(TeamMessage::getId)
                    .last("LIMIT 1");
            TeamMessage latest = teamMessageService.getOne(latestW);
            TeamChatDtos.UnreadTeamItem item = new TeamChatDtos.UnreadTeamItem();
            item.setTeamRequestId(teamId);
            item.setTeamTitle(team == null ? "小组" : team.getTitle());
            item.setUnreadCount(unread);
            item.setLastMessagePreview(latest == null ? "" : latest.getContent());
            item.setLastMessageTime(latest == null ? null : latest.getCreateTime());
            result.add(item);
            total += unread;
        }
        result.sort(Comparator.comparing(TeamChatDtos.UnreadTeamItem::getLastMessageTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        TeamChatDtos.UnreadSummary summary = new TeamChatDtos.UnreadSummary();
        summary.setTotalUnread(total);
        summary.setTeamUnreadList(result);
        return summary;
    }

    public TeamChatDtos.MessageItem toMessageItem(TeamMessage message) {
        TeamChatDtos.MessageItem item = new TeamChatDtos.MessageItem();
        item.setId(message.getId());
        item.setTeamRequestId(message.getTeamRequestId());
        item.setSenderId(message.getSenderId());
        item.setSenderName(message.getSenderName());
        item.setType(message.getType());
        item.setContent(message.getContent());
        item.setFileName(message.getFileName());
        item.setFileSize(message.getFileSize());
        item.setClientMsgId(message.getClientMsgId());
        item.setStatus(message.getStatus());
        item.setRecalled(message.getRecalled());
        boolean canRecall = message.getSenderId() != null
                && message.getCreateTime() != null
                && !Objects.equals(message.getRecalled(), 1)
                && LocalDateTime.now().isBefore(message.getCreateTime().plusMinutes(5));
        item.setCanRecall(canRecall ? 1 : 0);
        item.setCreateTime(message.getCreateTime());
        return item;
    }

    private String resolveSenderName(Long userId) {
        User user = userService.getById(userId);
        if (user == null) return "用户 " + userId;
        if (user.getRealName() != null && !user.getRealName().isBlank()) return user.getRealName();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        return "用户 " + userId;
    }
}
