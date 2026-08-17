package com.example.login_new.alogic;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 🧩 StudyProblemItem
 * Firestore 문제 데이터를 표현하는 모델 클래스.
 * - RecyclerView, 상세화면, Firestore 연동 모두에 사용됨.
 * - ✅ 풀었는지 여부(solved) + 정답 여부(correctSolved)까지 관리
 * - ⭐ creatorUid 추가됨 (내가 만든 문제인지 확인용)
 */
@IgnoreExtraProperties
public class StudyProblemItem {

    private String id;            // Firestore 문서 ID (problemId)
    private String title;         // 문제 제목
    private String author;        // 작성자 이름
    private String timeAgo;       // 작성된 시간 (ex: "2시간 전")
    private String difficulty;    // 난이도 (쉬움 / 보통 / 어려움)
    private String category;      // 카테고리
    private int score;            // 점수

    // ⭐⭐ 추가된 필드 ⭐⭐
    private String creatorUid = "";   // ← 작성자 UID (내 문제인지 구분용)

    // 기존 필드
    private boolean solved;
    private boolean correctSolved;

    /**
     * Firestore 기본 생성자
     */
    public StudyProblemItem() {
        this.id = "";
        this.title = "제목 없음";
        this.author = "익명";
        this.timeAgo = "";
        this.difficulty = "보통";
        this.category = "기타";
        this.score = 5;
        this.solved = false;
        this.correctSolved = false;

        // ⭐ 기본값 지정
        this.creatorUid = "";
    }

    /**
     * 전체 필드 생성자
     */
    public StudyProblemItem(String id, String title, String author,
                            String timeAgo, String difficulty, String category) {
        this(id, title, author, timeAgo, difficulty, category, 5, false, false);
    }

    public StudyProblemItem(String id, String title, String author,
                            String timeAgo, String difficulty, String category,
                            int score, boolean solved) {
        this(id, title, author, timeAgo, difficulty, category, score, solved, false);
    }

    public StudyProblemItem(String id, String title, String author,
                            String timeAgo, String difficulty, String category,
                            int score, boolean solved, boolean correctSolved) {

        this.id = safeString(id, "");
        this.title = safeString(title, "제목 없음");
        this.author = safeString(author, "익명");
        this.timeAgo = safeString(timeAgo, "");
        this.difficulty = safeString(difficulty, "보통");
        this.category = safeString(category, "기타");
        this.score = (score > 0 ? score : 5);
        this.solved = solved;
        this.correctSolved = correctSolved;

        // ⭐ 생성자에 creatorUid 기본값 넣기
        this.creatorUid = "";
    }

    // ------------------------------------------------------------
    // Getter
    // ------------------------------------------------------------
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getCategory() {
        return category;
    }

    public int getScore() {
        return score;
    }

    public boolean isSolved() {
        return solved;
    }

    public boolean isCorrectSolved() {
        return correctSolved;
    }

    // ⭐⭐ 추가된 getter ⭐⭐
    public String getCreatorUid() {
        return creatorUid;
    }

    // ------------------------------------------------------------
    // Setter
    // ------------------------------------------------------------
    public void setId(String id) {
        this.id = safeString(id, "");
    }

    public void setTitle(String title) {
        this.title = safeString(title, "제목 없음");
    }

    public void setAuthor(String author) {
        this.author = safeString(author, "익명");
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = safeString(timeAgo, "");
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = safeString(difficulty, "보통");
    }

    public void setCategory(String category) {
        this.category = safeString(category, "기타");
    }

    public void setScore(int score) {
        this.score = (score > 0 ? score : 5);
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public void setCorrectSolved(boolean correctSolved) {
        this.correctSolved = correctSolved;
    }

    // ⭐⭐ 추가된 setter ⭐⭐
    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    // ------------------------------------------------------------
    // Firestore 전송용 Map
    // ------------------------------------------------------------
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("title", title);
        map.put("author", author);
        map.put("timeAgo", timeAgo);
        map.put("difficulty", difficulty);
        map.put("category", category);
        map.put("score", score);
        map.put("solved", solved);
        map.put("correctSolved", correctSolved);

        // ⭐⭐ Firestore에도 creatorUid 저장 ⭐⭐
        map.put("creatorUid", creatorUid);

        return map;
    }

    // ------------------------------------------------------------
    // 유효성 체크
    // ------------------------------------------------------------
    @Exclude
    public boolean isValid() {
        return title != null && !title.trim().isEmpty()
                && difficulty != null && !difficulty.trim().isEmpty();
    }

    // ------------------------------------------------------------
    // 안전 문자열 변환
    // ------------------------------------------------------------
    private String safeString(Object value, String fallback) {
        if (value == null) return fallback;

        // ❌ Java 17 문법
        // if (value instanceof String s) return s.isEmpty() ? fallback : s;

        // ✅ Java 11 호환
        if (value instanceof String) {
            String s = (String) value;
            return s.isEmpty() ? fallback : s;
        }

        return String.valueOf(value);
    }


    // ------------------------------------------------------------
    // 디버깅용 toString
    // ------------------------------------------------------------
    @NonNull
    @Override
    public String toString() {
        return "Problem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", level='" + difficulty + '\'' +
                ", category='" + category + '\'' +
                ", score=" + score +
                ", solved=" + solved +
                ", correctSolved=" + correctSolved +
                ", creatorUid='" + creatorUid + '\'' +
                '}';
    }
}
