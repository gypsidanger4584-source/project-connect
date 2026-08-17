package com.example.login_new.alogic;

public class Message {
    @com.google.gson.annotations.SerializedName("role")
    private String role;

    @com.google.gson.annotations.SerializedName("content")
    private String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
}