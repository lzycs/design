package com.campus.learningspace.llm;

import java.util.List;

public interface LlmClient {
    String providerKey();
    String chatText(String scene, List<LlmMessage> messages);
}

