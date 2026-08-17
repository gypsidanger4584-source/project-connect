package com.example.login_new.alogic;

public class ChatMessage {
    private final String text;
    private final boolean sentByUser;
    private final String time;
    private final String title; // 칭호 (ex: 입문자)

    public ChatMessage(String text, boolean sentByUser, String time, String title) {
        this.text = text;
        this.sentByUser = sentByUser;
        this.time = time;
        this.title = title;
    }

    public String getText() { return text; }
    public boolean isSentByUser() { return sentByUser; }
    public String getTime() { return time; }
    public String getTitle() { return title; }
}
