package com.example.login_new.alogic;

import java.util.List;

public class ChatRequest {

    @com.google.gson.annotations.SerializedName("model")
    private String model;

    @com.google.gson.annotations.SerializedName("messages")
    private List<Message> messages;

    // ✅ 추가: 토큰 수 고정 옵션
    @com.google.gson.annotations.SerializedName("max_tokens")
    private int maxTokens;

    // ✅ 기존 생성자 (기본값 150)
    public ChatRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
        this.maxTokens = 200;  // 기본값
    }

    public ChatRequest(String model, List<Message> messages, int maxTokens) {
        this.model = model;
        this.messages = messages;
        this.maxTokens = maxTokens;
    }

    public String getModel() { return model; }
    public List<Message> getMessages() { return messages; }
    public int getMaxTokens() { return maxTokens; }
}
