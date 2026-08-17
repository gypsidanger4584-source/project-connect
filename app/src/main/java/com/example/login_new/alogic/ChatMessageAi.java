package com.example.login_new.alogic;

public class ChatMessageAi {
    private String role;
    private String content;
    private String time;

    public ChatMessageAi(String role, String content, String time) {
        this.role = role;
        this.content = content;
        this.time = time;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
    public String getTime() { return time; }
}
