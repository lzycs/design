package com.campus.learningspace.ws;

import com.campus.learningspace.dto.TeamChatDtos;
import com.campus.learningspace.entity.TeamMessage;
import com.campus.learningspace.service.TeamChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class TeamChatWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private TeamChatService teamChatService;
    @Autowired
    private TeamChatWsHub teamChatWsHub;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // no-op
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());
        String action = root.path("action").asText("");
        Long userId = root.path("userId").isMissingNode() ? null : root.path("userId").asLong();
        Long teamId = root.path("teamId").isMissingNode() ? null : root.path("teamId").asLong();
        if (userId == null || teamId == null) {
            session.sendMessage(new TextMessage("{\"event\":\"error\",\"message\":\"缺少 userId 或 teamId\"}"));
            return;
        }
        if (!teamChatService.isMember(teamId, userId)) {
            session.sendMessage(new TextMessage("{\"event\":\"error\",\"message\":\"你不是该小组成员\"}"));
            return;
        }
        if ("subscribe".equals(action)) {
            teamChatWsHub.subscribe(session, teamId);
            session.sendMessage(new TextMessage("{\"event\":\"subscribed\",\"teamId\":" + teamId + "}"));
            return;
        }
        if ("send".equals(action)) {
            String content = root.path("content").asText("");
            String clientMsgId = root.path("clientMsgId").asText(null);
            Integer type = root.path("type").isMissingNode() ? 1 : root.path("type").asInt(1);
            String fileName = root.path("fileName").isMissingNode() ? null : root.path("fileName").asText(null);
            Long fileSize = root.path("fileSize").isMissingNode() ? null : root.path("fileSize").asLong();
            try {
                TeamMessage saved = teamChatService.sendMessage(teamId, userId, type, content, fileName, fileSize, clientMsgId);
                TeamChatDtos.MessageItem item = teamChatService.toMessageItem(saved);
                teamChatWsHub.broadcastMessage(teamId, item);
            } catch (IllegalArgumentException e) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("event", "error", "message", e.getMessage()))));
            }
            return;
        }
        if ("recall".equals(action)) {
            Long messageId = root.path("messageId").asLong(0L);
            try {
                TeamMessage recalled = teamChatService.recallMessage(teamId, messageId, userId);
                TeamChatDtos.MessageItem item = teamChatService.toMessageItem(recalled);
                teamChatWsHub.broadcastRecall(teamId, item);
            } catch (IllegalArgumentException e) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("event", "error", "message", e.getMessage()))));
            }
            return;
        }
        if ("read".equals(action)) {
            Long lastReadMessageId = root.path("lastReadMessageId").asLong(0L);
            try {
                teamChatService.markRead(teamId, userId, lastReadMessageId);
                teamChatWsHub.broadcastRead(teamId, userId, lastReadMessageId);
            } catch (IllegalArgumentException e) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("event", "error", "message", e.getMessage()))));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        teamChatWsHub.unsubscribeAll(session);
    }
}
