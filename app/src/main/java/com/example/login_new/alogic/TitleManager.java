package com.example.login_new.alogic;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * 🔹 TitleManager
 *  - 점수 관리 및 칭호 자동 업데이트 담당
 *  - 어디서든 호출 가능 (정적 or 인스턴스)
 */
public class TitleManager {

    private static final int SCORE_PER_TITLE = 5; // 🔹 5점마다 칭호 업그레이드
    private static final String[] TITLES = {
            "연습생 코더", "도전하는 학습자", "기초 다지는 초보", "알고리즘 탐험가", "문제 풀이 입문자",
            "꾸준한 해결사", "성장하는 개발자", "코드 연구생", "집중력 마스터", "실전 코더",
            "문제 해결 전문가", "논리의 장인", "버그 헌터", "알고리즘 설계자", "코드 공학자",
            "최적화 전문가", "시스템 분석가", "기술 마법사", "패턴 인식가", "프로그래밍 마스터",
            "연성대학교의 별 ⭐", "계산의 신", "지식 탐험가", "코드 조련사", "천재적인 분석가",
            "디버깅 마왕", "로직 설계 장인", "알고리즘 박사", "창의력 괴물", "코드 혁신가",
            "연성의 영웅", "알고리즘의 전설", "데이터 마스터", "논리의 제왕", "프로그래밍의 현자",
            "문제 해결 귀재", "디지털 장군", "코드의 왕좌", "AI의 동반자", "궁극의 개발자",
            "연성의 수호자", "천상계 코더", "문제의 신", "코딩 현자", "전설적인 두뇌",
            "코드의 신화", "완벽한 논리체", "알고리즘 절대자", "디지털 신성", "연성의 창조주 👑"
    };

    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;

    public TitleManager() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    // ------------------------------------------------------------
    /** ✅ 문제 정답 맞췄을 때 점수 +N */
    public void addScore(int addAmount) {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    long currentScore = 0;
                    if (doc.exists() && doc.contains("score")) {
                        Long scoreValue = doc.getLong("score");
                        currentScore = (scoreValue != null) ? scoreValue : 0;
                    }

                    long newScore = Math.max(0, currentScore + addAmount); // 🔹 음수 방지
                    String newTitle = titleForScore(newScore);

                    Map<String, Object> update = new HashMap<>();
                    update.put("score", newScore);
                    update.put("title", newTitle);

                    db.collection("users").document(currentUser.getUid())
                            .set(update, SetOptions.merge()) // 🔹 문서가 없을 때 자동 생성
                            .addOnSuccessListener(aVoid ->
                                    System.out.println("✅ 점수 업데이트 완료: " + newScore + "점 (" + newTitle + ")"))
                            .addOnFailureListener(e ->
                                    System.err.println("❌ 점수 업데이트 실패: " + e.getMessage()));
                });
    }

    // ------------------------------------------------------------
    /** ✅ 점수에 따른 칭호 반환 (정적 호출용) */
    @NonNull
    public static String titleForScore(long score) {
        int index = (int) (score / SCORE_PER_TITLE);
        if (index < 0) index = 0;
        if (index >= TITLES.length) index = TITLES.length - 1;
        return TITLES[index];
    }

    /** ✅ alias (호환성 유지용) — getTitleForScore()도 동일 동작 */
    @NonNull
    public static String getTitleForScore(long score) {
        return titleForScore(score);
    }

    // ------------------------------------------------------------
    /** ✅ 유저 점수 직접 설정 */
    public void setScore(long score) {
        if (currentUser == null) return;
        String title = titleForScore(score);
        Map<String, Object> update = new HashMap<>();
        update.put("score", score);
        update.put("title", title);
        db.collection("users").document(currentUser.getUid())
                .set(update, SetOptions.merge());
    }

    /** ✅ 현재 칭호 강제 변경 */
    public void setTitle(String title) {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid())
                .update("title", title);
    }
}
