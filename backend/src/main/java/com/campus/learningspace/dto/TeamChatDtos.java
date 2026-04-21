package com.campus.learningspace.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class TeamChatDtos {
    @Data
    public static class SendMessageRequest {
        private Long userId;
        /**
         * 1-文本 2-图片 3-文件 4-计划卡片 5-预约卡片
         */
        private Integer type;
        private String content;
        private String fileName;
        private Long fileSize;
        private String clientMsgId;
    }

    @Data
    public static class MarkReadRequest {
        private Long userId;
        private Long lastReadMessageId;
    }

    @Data
    public static class MessageItem {
        private Long id;
        private Long teamRequestId;
        private Long senderId;
        private String senderName;
        private Integer type;
        private String content;
        private String fileName;
        private Long fileSize;
        private String clientMsgId;
        private Integer status;
        private Integer recalled;
        private Integer canRecall;
        private LocalDateTime createTime;
    }

    @Data
    public static class RecallMessageRequest {
        private Long userId;
    }

    @Data
    public static class MessagePage {
        private List<MessageItem> list;
        private Long nextCursor;
        private boolean hasMore;
    }

    @Data
    public static class UnreadTeamItem {
        private Long teamRequestId;
        private String teamTitle;
        private Long unreadCount;
        private String lastMessagePreview;
        private LocalDateTime lastMessageTime;
    }

    @Data
    public static class UnreadSummary {
        private Long totalUnread;
        private List<UnreadTeamItem> teamUnreadList;
    }
}
