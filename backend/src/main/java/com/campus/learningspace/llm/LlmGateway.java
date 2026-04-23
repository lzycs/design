package com.campus.learningspace.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmGateway {
    private final List<LlmClient> clients;
    private final String provider;

    public LlmGateway(List<LlmClient> clients, @Value("${app.llm.provider:mvp_stub}") String provider) {
        this.clients = clients;
        this.provider = provider == null ? "mvp_stub" : provider.trim();
    }

    public String provider() {
        return provider;
    }

    public String chatText(String scene, List<LlmMessage> messages) {
        LlmClient picked = null;
        if (clients != null) {
            for (LlmClient c : clients) {
                if (c != null && provider.equalsIgnoreCase(c.providerKey())) {
                    picked = c;
                    break;
                }
            }
        }
        if (picked == null && clients != null && !clients.isEmpty()) {
            picked = clients.get(0);
        }
        if (picked == null) return "LLM 未配置（当前无可用 provider）";
        return picked.chatText(scene, messages);
    }
}

