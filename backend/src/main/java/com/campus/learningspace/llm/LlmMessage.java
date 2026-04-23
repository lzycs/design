package com.campus.learningspace.llm;

import lombok.Data;

@Data
public class LlmMessage {
    private String role; // system/user/assistant
    private String content;
}

