package com.example.login_new.alogic;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏠 StudyRoom
 * Firestore에 저장되는 스터디룸 정보 모델 클래스.
 * - Firestore 직렬화 호환
 * - Timestamp, welcomeMessage 포함
 * - Firestore 필드명(open) 정확히 매칭
 */
@IgnoreExtraProperties
public class StudyRoom {

    private String roomId;         // Firestore 문서 ID
    private String roomName;       // 방 이름
    private String topic;          // 학습 주제
    private String category;       // 카테고리
    private String startDate;      // 시작일
    private String endDate;        // 종료일
    private int currentPeople;     // 현재 인원
    private int maxPeople;         // 최대 인원
    private int problemCount;      // 등록된 문제 개수
    private boolean open;          // Firestore와 동일
    private String ownerUid;       // 방장 UID
    private String creatorName;    // 방장 이름
    private String welcomeMessage; // 환영 메시지
    private Timestamp createdAt;   // 생성 시각
    private Timestamp updatedAt;   // 마지막 수정 시각

    // ⭐⭐ [추가됨] 내가 참여중인지 체크하기 위해
    private List<String> participants;

    // ⭐⭐ [추가됨] RoomCard 펼침 상태 저장
    private boolean expanded = false;


    public StudyRoom() {
        this.roomName = "스터디룸";
        this.topic = "";
        this.category = "기타";
        this.startDate = "";
        this.endDate = "";
        this.currentPeople = 0;
        this.maxPeople = 10;
        this.problemCount = 0;
        this.open = true;
        this.ownerUid = "";
        this.creatorName = "익명";
        this.welcomeMessage = "";
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();

        // ⭐ 기본값 추가
        this.participants = new ArrayList<>();
        this.expanded = false;
    }

    // ---------- Getter / Setter ----------
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = safeString(roomName, "스터디룸"); }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = safeString(topic, ""); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = safeString(category, "기타"); }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = safeString(startDate, ""); }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = safeString(endDate, ""); }

    public int getCurrentPeople() { return currentPeople; }
    public void setCurrentPeople(int currentPeople) { this.currentPeople = currentPeople; }

    public int getMaxPeople() { return maxPeople; }
    public void setMaxPeople(int maxPeople) { this.maxPeople = maxPeople; }

    public int getProblemCount() { return problemCount; }
    public void setProblemCount(int problemCount) { this.problemCount = problemCount; }

    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = safeString(ownerUid, ""); }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = safeString(creatorName, "익명"); }

    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = safeString(welcomeMessage, ""); }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // ⭐⭐ 추가 : 참여자 UID 목록
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) {
        this.participants = participants != null ? participants : new ArrayList<>();
    }

    // ⭐⭐ 추가 : 카드 펼침 여부
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }


    @Exclude
    public boolean isValid() {
        return roomName != null && !roomName.trim().isEmpty();
    }

    private static String safeString(Object value, String fallback) {
        if (value == null) return fallback;
        if (value instanceof String s) return s.isEmpty() ? fallback : s;
        return String.valueOf(value);
    }
}
