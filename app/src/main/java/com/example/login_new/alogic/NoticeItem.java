package com.example.login_new.alogic;

import com.google.firebase.Timestamp;

/**
 * ✅ 공지사항 데이터 모델
 * - Firestore "posts" 컬렉션에서 가져온 공지 데이터 표현
 * - id: 문서 ID
 * - title: 공지 제목
 * - content: 본문 내용
 * - timestamp: 등록 시간
 */
public class NoticeItem {
    private String id;
    private String title;
    private String content;
    private Timestamp timestamp;

    // Firestore용 기본 생성자 (없으면 자동 매핑 시 에러 발생 가능)
    public NoticeItem() {}

    public NoticeItem(String id, String title, String content, Timestamp timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
