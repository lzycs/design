package com.campus.learningspace.llm;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MvpStubLlmClient implements LlmClient {
    @Override
    public String providerKey() {
        return "mvp_stub";
    }

    @Override
    public String chatText(String scene, List<LlmMessage> messages) {
        String lastUser = "";
        if (messages != null) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                LlmMessage m = messages.get(i);
                if (m != null && "user".equalsIgnoreCase(m.getRole())) {
                    lastUser = m.getContent() == null ? "" : m.getContent().trim();
                    break;
                }
            }
        }
        return "MVP 模式（可切换真实大模型）：\n\n"
                + "问题：" + (lastUser.isBlank() ? "（未提供）" : lastUser) + "\n\n"
                + "回答结构建议：\n"
                + "1) 概念与适用场景\n"
                + "2) 核心步骤/伪代码\n"
                + "3) 常见易错点\n"
                + "4) 2 个练习题（含提示）";
    }
}

