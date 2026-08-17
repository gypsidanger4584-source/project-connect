package com.example.login_new.alogic;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.login_new.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class StudyProblemDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvAuthor, tvInfo, tvOptionsCount, tvSolvedInfo, tvScore;
    private ProgressBar progressBar;
    private Button btnSolve;

    private FirebaseFirestore db;
    private String roomId, problemId;

    private int solvedCount = 0;
    private int correctCount = 0;
    private ListenerRegistration solveListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_problem_detail);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupProgressBar();

        // 🔙 뒤로가기
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // ✅ 인텐트 데이터
        roomId = getIntent().getStringExtra("roomId");
        problemId = getIntent().getStringExtra("problemId");

        if (roomId == null || problemId == null) {
            toast("문제 정보를 불러올 수 없습니다.");
            finish();
            return;
        }

        loadProblemDetails();
        listenSolveStatsRealtime();

        btnSolve.setOnClickListener(v -> {
            btnSolve.setEnabled(false);
            new Handler().postDelayed(() -> btnSolve.setEnabled(true), 1000);

            Intent intent = new Intent(this, StudyProblemSolveActivity.class);
            intent.putExtra("roomId", roomId);
            intent.putExtra("problemId", problemId);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    // ------------------------------------------------------------
    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvInfo = findViewById(R.id.tvInfo);
        tvOptionsCount = findViewById(R.id.tvOptionsCount);
        tvSolvedInfo = findViewById(R.id.tvSolvedInfo);
        tvScore = findViewById(R.id.tvScore);
        progressBar = findViewById(R.id.progressBarRate);
        btnSolve = findViewById(R.id.btnSolve);
    }

    /** ✅ ProgressBar 색상 세팅 (SDK 호환) */
    private void setupProgressBar() {
        int color = getColor(R.color.purple_500);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ColorFilter filter = new BlendModeColorFilter(color, BlendMode.SRC_IN);
            progressBar.getProgressDrawable().setColorFilter(filter);
        } else {
            DrawableCompat.setTint(progressBar.getProgressDrawable(), color);
            progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    // ------------------------------------------------------------
    /** 🔹 문제 기본 정보 로드 */
    private void loadProblemDetails() {
        db.collection("studyRooms").document(roomId)
                .collection("problems").document(problemId)
                .get()
                .addOnSuccessListener(this::updateProblemUI)
                .addOnFailureListener(e ->
                        toast("문제 불러오기 실패: " + e.getMessage()));
    }

    /** 🔹 UI 업데이트 */
    private void updateProblemUI(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            toast("문제 데이터를 찾을 수 없습니다.");
            return;
        }

        String title = safeString(doc.get("title"), "제목 없음");
        String creator = safeString(doc.get("creatorName"), "익명");
        String level = safeString(doc.get("level"), "보통");
        String category = safeString(doc.get("category"), "기타");
        String time = safeString(doc.get("timeAgo"), "방금 전");
        int scoreValue = safeInt(doc.get("score"), 5);
        List<String> options = (List<String>) doc.get("options");
        int optionCount = (options != null) ? options.size() : 0;

        tvTitle.setText(title);
        tvAuthor.setText("출제자: " + creator);
        tvInfo.setText("[" + category + "]  " + level + " · " + time);
        tvOptionsCount.setText("보기 개수: " + optionCount + "개");
        tvScore.setText("획득 점수: " + scoreValue + "점");
    }

    // ------------------------------------------------------------
    /** 🔹 정답률 실시간 리스너 */
    private void listenSolveStatsRealtime() {
        solveListener = db.collection("studyRooms").document(roomId)
                .collection("problems").document(problemId)
                .collection("solvedUsers")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    solvedCount = snapshots.size();
                    correctCount = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Boolean correct = doc.getBoolean("correct");
                        if (Boolean.TRUE.equals(correct)) correctCount++;
                    }

                    updateStatsUI();
                });
    }

    /** 🔹 통계 UI 업데이트 */
    private void updateStatsUI() {
        if (solvedCount == 0) {
            tvSolvedInfo.setText("아직 아무도 풀지 않았습니다.");
            progressBar.setProgress(0);
            return;
        }

        int rate = (int) Math.round((correctCount * 100.0) / solvedCount);
        rate = Math.max(0, Math.min(rate, 100)); // 0~100 사이로 제한

        tvSolvedInfo.setText("풀이자 수: " + solvedCount + "명  •  정답률: " + rate + "%");
        progressBar.setProgress(rate);
    }

    // ------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (solveListener != null) solveListener.remove();
    }

    // ------------------------------------------------------------
    private static String safeString(Object value, String fallback) {
        if (value == null) return fallback;
        if (value instanceof String s) return s.isEmpty() ? fallback : s;
        return String.valueOf(value);
    }

    private static int safeInt(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
