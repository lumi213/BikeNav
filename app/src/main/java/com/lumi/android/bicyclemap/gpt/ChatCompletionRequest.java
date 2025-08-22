package com.lumi.android.bicyclemap.gpt;

import java.util.ArrayList;
import java.util.List;

public class ChatCompletionRequest {
    public String model;
    public List<Message> messages = new ArrayList<>(); // ✅ 기본 초기화
    public Double temperature = 0.2;
    public Integer max_tokens = 600;

    public ChatCompletionRequest() {
        this.messages = new ArrayList<>();   // ✅ 꼭 초기화
    }

    public static class Message {
        public String role;     // "system" | "user"
        public String content;
        public Message(String role, String content){ this.role=role; this.content=content; }
    }
}
