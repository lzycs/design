package com.campus.learningspace.ws;

import com.campus.learningspace.dto.TeamChatDtos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TeamChatWsHub {
    private final Map<Long, Set<WebSocketSession>> teamSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> sessionTeams = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    public void subscribe(WebSocketSession session, Long teamId) {
        teamSessions.computeIfAbsent(teamId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionTeams.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet()).add(teamId);
    }

    public void unsubscribeAll(WebSocketSession session) {
        Set<Long> teams = sessionTeams.remove(session.getId());
        if (teams == null) return;
        for (Long teamId : teams) {
            Set<WebSocketSession> sessions = teamSessions.get(teamId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    teamSessions.remove(teamId);
                }
            }
        }
    }

    public void broadcastMessage(Long teamId, TeamChatDtos.MessageItem messageItem) {
        Map<String, Object> payload = Map.of(
                "event", "message",
                "teamId", teamId,
                "data", messageItem
        );
        broadcast(teamId, payload);
    }

    public void broadcastRead(Long teamId, Long userId, Long lastReadMessageId) {
        Map<String, Object> payload = Map.of(
                "event", "read",
                "teamId", teamId,
                "userId", userId,
                "lastReadMessageId", lastReadMessageId == null ? 0L : lastReadMessageId
        );
        broadcast(teamId, payload);
    }

    public void broadcastRecall(Long teamId, TeamChatDtos.MessageItem messageItem) {
        Map<String, Object> payload = Map.of(
                "event", "recall",
                "teamId", teamId,
                "data", messageItem
        );
        broadcast(teamId, payload);
    }

    private void broadcast(Long teamId, Map<String, Object> payload) {
        Set<WebSocketSession> sessions = teamSessions.get(teamId);
        if (sessions == null || sessions.isEmpty()) return;
        String text;
        try {
            text = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;
            try {
                session.sendMessage(new TextMessage(text));
            } catch (Exception ignored) {
            }
        }
    }
}
