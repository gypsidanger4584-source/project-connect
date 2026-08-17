package com.example.login_new.alogic;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * ✅ MemoEntity
 * Room 테이블 구조와 완벽하게 맞는 버전 (문자열 ID, null 안전)
 */
@Entity(
        tableName = "memos",
        foreignKeys = @ForeignKey(
                entity = LectureEntity.class,
                parentColumns = "id",
                childColumns = "lectureId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("lectureId")}
)
public class MemoEntity {

    @PrimaryKey
    @NonNull
    public String id = "";  // 반드시 기본값 "" 줘야 null 에러 방지

    public String lectureId;
    public String text;
    public Long createdAt;
    public Long expiryAt;
    public boolean isImportant;

    public MemoEntity() {
        // Room에서 사용되는 기본 생성자
    }

    public MemoEntity(@NonNull String id, String lectureId, String text,
                      Long createdAt, Long expiryAt, boolean isImportant) {
        this.id = id;
        this.lectureId = lectureId;
        this.text = text;
        this.createdAt = createdAt;
        this.expiryAt = expiryAt;
        this.isImportant = isImportant;
    }
}
