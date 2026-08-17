package com.example.login_new.alogic;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Map;

@IgnoreExtraProperties
public class Participant {

    private String uid;
    private String name;
    private String title;
    private String profileUrl;
    private int profileResId;

    // 🔥 기존 isActive → active 로 변경 (Firestore 매핑 문제 해결)
    private boolean active;

    private int score;
    private Timestamp joinedAt;

    // 🔥 내가 푼 문제 목록
    private Map<String, Boolean> solved;

    // Firestore 역직렬화용 기본 생성자
    public Participant() {}

    // ========================= Getter/Setter ===============================

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }

    public int getProfileResId() { return profileResId; }
    public void setProfileResId(int profileResId) { this.profileResId = profileResId; }

    // =========================
    // 🔥 Firestore + Adapter → 둘 다 정상 작동하는 active 필드
    // =========================
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // =========================

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    public Map<String, Boolean> getSolved() { return solved; }
    public void setSolved(Map<String, Boolean> solved) { this.solved = solved; }
}
