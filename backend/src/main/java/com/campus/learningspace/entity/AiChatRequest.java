package com.campus.learningspace.entity;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    private Long userId;
    private String scene; // qa | plan | recommend
    private List<AiChatMessage> messages;
}
