package com.example.login_new.alogic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// ⭐️ Room Entity로 선언하고 테이블 이름을 지정합니다.
@Entity(tableName = "memo_table")
public class Memo {

    // ⭐️ 기본 키: Room은 기본 키를 non-null로 요구합니다.
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id = UUID.randomUUID().toString(); // 기본값으로 UUID 할당

    // ⭐️ 외래 키 역할: 이 메모가 어떤 LectureItem에 속하는지 연결 (필수)
    @NonNull
    @ColumnInfo(name = "lectureId")
    private String lectureId; // 외부에서 반드시 설정해줘야 함.

    @ColumnInfo(name = "text")
    private String text;

    // ⭐️ ColumnInfo를 사용하여 데이터베이스 컬럼명을 명시
    @ColumnInfo(name = "createdAtMs")
    private long createdAtMs; // epoch millis (created time)

    @Nullable
    @ColumnInfo(name = "expireAtMs")
    private Long expireAtMs;  // epoch millis (nullable)

    // ⭐️ 추가: 만료 기간을 '사용자 지정'으로 설정했을 때 일수를 저장
    @Nullable
    @ColumnInfo(name = "customDays")
    private Integer customDays;

    // ⭐️ 추가: 중요 메모 표시용 필드
    @ColumnInfo(name = "isImportant")
    private boolean isImportant = false;


    // 이 필드는 UI 상태 관리용이며 DB에 저장하지 않습니다.
    @Ignore // ⭐️ @Ignore 추가
    private boolean isExpanded = false;

    // 하루(1일) 단위 밀리초
    public static final long DAY_MS = TimeUnit.DAYS.toMillis(1);

    // ⭐️ Room이 객체 생성을 위해 필요로 하는 기본 생성자
    public Memo() {
        this.lectureId = "";
        this.text = "";
    }

    // ⭐️ 필수 필드를 포함한 생성자 (TimetableActivity에서 새 메모 생성 시 사용)
    public Memo(@NonNull String id, @NonNull String lectureId, String text, long createdAtMs) {
        this.id = id;
        this.lectureId = lectureId;
        this.text = text;
        this.createdAtMs = createdAtMs;
        this.expireAtMs = null;
        this.customDays = null;
        this.isImportant = false;
    }

    // ⭐️ 복사 생성자 (TimetableActivity에서 메모 수정 시 임시 복사본 생성용)
    public Memo(Memo original) {
        this.id = original.id;
        this.lectureId = original.lectureId;
        this.text = original.text;
        this.createdAtMs = original.createdAtMs;
        this.expireAtMs = original.expireAtMs;
        this.customDays = original.customDays;
        this.isImportant = original.isImportant;
        this.isExpanded = original.isExpanded;
    }

    // ⭐️ Room이 객체 생성을 위해 필요로 하는 모든 필드를 포함한 생성자
    // 이 생성자를 Room이 사용합니다.
    public Memo(@NonNull String id, @NonNull String lectureId, String text,
                long createdAtMs, @Nullable Long expireAtMs, @Nullable Integer customDays,
                boolean isImportant) {
        this.id = id;
        this.lectureId = lectureId;
        this.text = text;
        this.createdAtMs = createdAtMs;
        this.expireAtMs = expireAtMs;
        this.customDays = customDays;
        this.isImportant = isImportant;
    }


    // ----------------------
    // 헬퍼 메서드 (로직 유지 및 업데이트)
    // ----------------------

    /** null=만료 없음, 1..30일 → expireAtMs로 환산하여 저장 (사용하지 않음, TimetableActivity에서 직접 계산) */
    // @Deprecated
    // public void setExpireDays(@Nullable Integer days) { ... }

    /** 저장된 expireAtMs로부터 '몇 일짜리'인지 계산. 없으면 null */
    @Nullable
    public Integer getExpireDays() {
        if (expireAtMs == null) return null;
        long delta = expireAtMs - createdAtMs;
        // delta가 0이거나 음수여도 1일 미만이면 0으로 처리하고 싶다면:
        if (delta < 0) return 0;

        // ⭐️ DAY_MS로 나눈 일수 반환 (정확한 일수 계산은 TimeZone에 따라 복잡하므로, 단순 밀리초 차이로 근사)
        return (int) (delta / DAY_MS);
    }

    /** 리스트 표시용: "MM/dd (E)" */
    public String formatDateLabel() {
        if (createdAtMs == 0) return "";
        return new SimpleDateFormat("M/d (E)", Locale.KOREA)
                .format(new Date(createdAtMs));
    }

    // ----------------------
    // Getter/Setter (Room 접근용)
    // ----------------------

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull public String getLectureId() { return lectureId; }
    public void setLectureId(@NonNull String lectureId) { this.lectureId = lectureId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getCreatedAtMs() { return createdAtMs; }
    public void setCreatedAtMs(long createdAtMs) { this.createdAtMs = createdAtMs; }

    @Nullable public Long getExpireAtMs() { return expireAtMs; }
    public void setExpireAtMs(@Nullable Long expireAtMs) { this.expireAtMs = expireAtMs; }

    // ⭐️ customDays Getter/Setter
    @Nullable public Integer getCustomDays() { return customDays; }
    public void setCustomDays(@Nullable Integer customDays) { this.customDays = customDays; }

    // ⭐️ isImportant Getter/Setter
    public boolean isImportant() { return isImportant; }
    public void setImportant(boolean important) { isImportant = important; }

    // ⭐️ isExpanded Getter/Setter
    public boolean isExpanded(){return  isExpanded;}
    public void setExpanded(boolean expanded){ isExpanded = expanded;}
}