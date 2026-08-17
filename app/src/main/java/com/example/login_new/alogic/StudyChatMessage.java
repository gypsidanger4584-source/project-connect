package com.example.login_new.alogic;

import java.util.Date;

/**
 * 스터디 채팅방 전용 메시지 모델
 *  - StudyChatActivity / StudyChatAdapter 에서만 사용
 *  - 랜덤채팅용 ChatMessage 와는 완전히 별개
 */
public class StudyChatMessage {

    private final String text;         // 메시지 내용
    private final boolean sentByUser;  // 내가 보낸 메시지인지
    private final String time;         // "오전 10:23" 같은 표시용 시간
    private final String senderId;     // 유저 ID / 닉네임 (senderId)
    private final String senderUid;    // Firebase UID
    private final String title;        // 칭호 (입문자 등)
    private final boolean isSystem;    // 시스템 메시지인지 여부
    private final Date timestamp;      // 정렬용 원본 시간

    public StudyChatMessage(String text,
                            boolean sentByUser,
                            String time,
                            String senderId,
                            String senderUid,
                            String title,
                            boolean isSystem,
                            Date timestamp) {
        this.text = text;
        this.sentByUser = sentByUser;
        this.time = time;
        this.senderId = senderId;
        this.senderUid = senderUid;
        this.title = title;
        this.isSystem = isSystem;
        this.timestamp = timestamp;
    }

    public String getText()       { return text; }
    public boolean isSentByUser() { return sentByUser; }
    public String getTime()       { return time; }
    public String getSenderId()   { return senderId; }
    public String getSenderUid()  { return senderUid; }
    public String getTitle()      { return title; }
    public boolean isSystem()     { return isSystem; }
    public Date getTimestamp()    { return timestamp; }
}
