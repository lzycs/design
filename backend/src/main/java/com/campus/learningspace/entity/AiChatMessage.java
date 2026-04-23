package com.campus.learningspace.entity;

import lombok.Data;

@Data
public class AiChatMessage {
    private String role; // system | user | assistant
    private String content;
}
