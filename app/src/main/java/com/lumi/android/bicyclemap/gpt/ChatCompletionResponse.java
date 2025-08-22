package com.lumi.android.bicyclemap.gpt;

import java.util.List;

public class ChatCompletionResponse {
    public List<Choice> choices;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String role;
        public String content; // JSON string
    }
}
