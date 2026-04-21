package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.dto.TeamChatDtos;
import com.campus.learningspace.entity.TeamMessage;
import com.campus.learningspace.service.TeamChatService;
import com.campus.learningspace.ws.TeamChatWsHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/team-chat")
@CrossOrigin
public class TeamChatController {
    @Autowired
    private TeamChatService teamChatService;
    @Autowired
    private TeamChatWsHub teamChatWsHub;

    @GetMapping("/{teamId}/messages")
    public Result<TeamChatDtos.MessagePage> getMessages(@PathVariable Long teamId,
                                                        @RequestParam Long userId,
                                                        @RequestParam(required = false) Long beforeId,
                                                        @RequestParam(required = false, defaultValue = "100") Integer size) {
        try {
            return Result.success(teamChatService.getHistory(teamId, userId, beforeId, size));
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @GetMapping("/unread-summary")
    public Result<TeamChatDtos.UnreadSummary> unreadSummary(@RequestParam Long userId) {
        return Result.success(teamChatService.getUnreadSummary(userId));
    }

    @PostMapping("/{teamId}/send")
    public Result<TeamChatDtos.MessageItem> sendMessage(@PathVariable Long teamId,
                                                        @RequestBody TeamChatDtos.SendMessageRequest request) {
        if (request == null || request.getUserId() == null) {
            return Result.error(400, "缺少 userId");
        }
        try {
            TeamMessage message = teamChatService.sendMessage(
                    teamId,
                    request.getUserId(),
                    request.getType(),
                    request.getContent(),
                    request.getFileName(),
                    request.getFileSize(),
                    request.getClientMsgId()
            );
            TeamChatDtos.MessageItem item = teamChatService.toMessageItem(message);
            teamChatWsHub.broadcastMessage(teamId, item);
            return Result.success(item);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{teamId}/messages/{messageId}/recall")
    public Result<TeamChatDtos.MessageItem> recallMessage(@PathVariable Long teamId,
                                                          @PathVariable Long messageId,
                                                          @RequestBody TeamChatDtos.RecallMessageRequest request) {
        if (request == null || request.getUserId() == null) {
            return Result.error(400, "缺少 userId");
        }
        try {
            TeamMessage recalled = teamChatService.recallMessage(teamId, messageId, request.getUserId());
            TeamChatDtos.MessageItem item = teamChatService.toMessageItem(recalled);
            teamChatWsHub.broadcastRecall(teamId, item);
            return Result.success(item);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{teamId}/read")
    public Result<Boolean> markRead(@PathVariable Long teamId,
                                    @RequestBody TeamChatDtos.MarkReadRequest request) {
        if (request == null || request.getUserId() == null) {
            return Result.error(400, "缺少 userId");
        }
        try {
            teamChatService.markRead(teamId, request.getUserId(), request.getLastReadMessageId());
            teamChatWsHub.broadcastRead(teamId, request.getUserId(), request.getLastReadMessageId());
            return Result.success(true);
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @GetMapping("/{teamId}/meta")
    public Result<Map<String, Object>> chatMeta(@PathVariable Long teamId, @RequestParam Long userId) {
        boolean member = teamChatService.isMember(teamId, userId);
        return member ? Result.success(Map.of("teamId", teamId, "member", true))
                : Result.error(403, "你不是该小组成员");
    }

    @PostMapping("/{teamId}/upload")
    public Result<Map<String, Object>> upload(@PathVariable Long teamId,
                                              @RequestParam Long userId,
                                              @RequestParam("file") MultipartFile file) {
        if (!teamChatService.isMember(teamId, userId)) {
            return Result.error(403, "你不是该小组成员");
        }
        if (file == null || file.isEmpty()) return Result.error(400, "文件不能为空");
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) ext = originalName.substring(dot).toLowerCase();
        boolean image = ext.matches("\\.(jpg|jpeg|png|gif|webp)$");
        boolean fileAllowed = ext.matches("\\.(doc|docx|xls|xlsx|ppt|pptx|pdf|txt)$");
        long size = file.getSize();
        if (image) {
            if (size > 10L * 1024 * 1024) return Result.error(400, "图片不能超过10MB");
        } else {
            if (!fileAllowed) return Result.error(400, "文件类型不支持");
            if (size > 20L * 1024 * 1024) return Result.error(400, "文件不能超过20MB");
        }
        try {
            Path root = Paths.get("uploads", "team-chat");
            Files.createDirectories(root);
            String savedName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = root.resolve(savedName);
            file.transferTo(target);
            String accessUrl = "/api/team-chat/file/" + savedName;
            return Result.success(Map.of(
                    "url", accessUrl,
                    "fileName", originalName,
                    "fileSize", size,
                    "isImage", image
            ));
        } catch (IOException e) {
            return Result.error(500, "上传失败");
        }
    }

    @GetMapping("/file/{name}")
    public ResponseEntity<byte[]> file(@PathVariable String name) throws IOException {
        Path filePath = Paths.get("uploads", "team-chat", name);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        String mime = Files.probeContentType(filePath);
        if (mime == null) mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        byte[] bytes = Files.readAllBytes(filePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                .body(bytes);
    }
}
