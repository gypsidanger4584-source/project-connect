package com.example.login_new.alogic;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.*;

public class StudyProblemCreateActivity extends AppCompatActivity {

    private EditText editQuizTitle, editQuestion;
    private LinearLayout optionContainer;
    private TextView tvAddOption, tvRoomInfo;
    private MaterialButton btnSubmitQuiz;
    private AutoCompleteTextView dropdownDifficulty;

    private MaterialRadioButton lastCheckedButton = null;
    private int optionCount = 0;
    private static final int MAX_OPTIONS = 8;
    private int nextViewId = 1000;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String roomName;
    private String roomId;
    private String userName = "익명";

    private TitleManager titleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_problem_create);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        titleManager = new TitleManager();

        initViews();
        setupDifficulty();

        for (int i = 0; i < 4; i++) addNewOptionRow();

        Intent intent = getIntent();
        roomName = intent.getStringExtra("roomName");
        roomId = intent.getStringExtra("roomId");

        if (TextUtils.isEmpty(roomName)) roomName = "알 수 없는 방";
        tvRoomInfo.setText(roomName + "에 문제 출제 (+1점)");

        loadUserName();

        if (TextUtils.isEmpty(roomId)) {
            btnSubmitQuiz.setEnabled(false);
            fetchRoomId(roomName, () -> {
                btnSubmitQuiz.setEnabled(true);
                toast("roomId 로드 완료 ✅");
            });
        }

        tvAddOption.setOnClickListener(v -> addNewOptionRow());
        btnSubmitQuiz.setOnClickListener(v -> submitQuiz());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        editQuizTitle = findViewById(R.id.editQuizTitle);
        editQuestion = findViewById(R.id.editQuestion);
        optionContainer = findViewById(R.id.optionContainer);
        tvAddOption = findViewById(R.id.tvAddOption);
        btnSubmitQuiz = findViewById(R.id.btnSubmitQuiz);
        dropdownDifficulty = findViewById(R.id.dropdownDifficulty);
        tvRoomInfo = findViewById(R.id.tvRoomInfo);
    }

    /** 🔹 난이도 드롭다운 */
    private void setupDifficulty() {
        String[] levels = {"쉬움", "보통", "어려움"};
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, levels);
        dropdownDifficulty.setAdapter(adapter);
        dropdownDifficulty.setText("보통", false);
        dropdownDifficulty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        dropdownDifficulty.setOnClickListener(v -> dropdownDifficulty.showDropDown());
    }

    /** 🔹 선택지 추가 */
    private void addNewOptionRow() {
        if (optionCount >= MAX_OPTIONS) {
            toast("선택지는 최대 " + MAX_OPTIONS + "개까지 가능합니다.");
            return;
        }

        optionCount++;
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, dp(8), 0, dp(8));

        MaterialRadioButton rb = new MaterialRadioButton(this);
        rb.setId(nextViewId++);
        rb.setUseMaterialThemeColors(true);

        rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && lastCheckedButton != rb) {
                if (lastCheckedButton != null) lastCheckedButton.setChecked(false);
                lastCheckedButton = rb;
            } else if (!isChecked && lastCheckedButton == rb) {
                lastCheckedButton = null;
            }
        });

        EditText et = new EditText(this);
        et.setHint("선택지 " + optionCount);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        et.setTextColor(getColor(android.R.color.black));
        et.setBackground(null);
        et.setMaxLines(3);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        et.setPadding(dp(12), dp(6), dp(12), dp(6));

        LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rbLp.rightMargin = dp(12);
        rb.setLayoutParams(rbLp);

        line.addView(rb);
        line.addView(et);
        line.setOnClickListener(v -> rb.performClick());
        optionContainer.addView(line);
    }

    /** 🔹 문제 등록 */
    private void submitQuiz() {
        String title = text(editQuizTitle);
        String question = text(editQuestion);
        String difficulty = text(dropdownDifficulty);

        if (TextUtils.isEmpty(title)) { toast("문제 제목을 입력하세요."); return; }
        if (TextUtils.isEmpty(question)) { toast("문제 내용을 입력하세요."); return; }
        if (lastCheckedButton == null) { toast("정답을 선택하세요."); return; }

        if (TextUtils.isEmpty(roomId)) {
            toast("⚠️ roomId가 없습니다. 잠시 후 다시 시도하세요.");
            Log.e("🔥DEBUG", "roomId == null, 문제 저장 중단됨");
            return;
        }

        saveProblemToFirestore(title, question, difficulty);
    }

    /** 🔹 Firestore 업로드 */
    private void saveProblemToFirestore(String title, String question, String level) {
        List<String> options = new ArrayList<>();
        int answerIndex = -1;

        for (int i = 0; i < optionContainer.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) optionContainer.getChildAt(i);
            MaterialRadioButton rb = (MaterialRadioButton) row.getChildAt(0);
            EditText et = (EditText) row.getChildAt(1);

            String optText = et.getText().toString().trim();
            if (!optText.isEmpty()) {
                options.add(optText);
                if (rb.isChecked()) answerIndex = options.size() - 1;
            }
        }

        if (options.isEmpty()) { toast("최소 하나의 선택지를 입력하세요."); return; }
        if (answerIndex == -1) { toast("정답을 선택하세요."); return; }

        Map<String, Object> problem = new HashMap<>();
        problem.put("title", title);
        problem.put("question", question);
        problem.put("creatorUid", currentUser != null ? currentUser.getUid() : "unknown");
        problem.put("creatorName", userName);
        problem.put("level", level);
        problem.put("category", "기타");
        problem.put("options", options);
        problem.put("answerIndex", answerIndex);
        problem.put("createdAt", FieldValue.serverTimestamp());  // ✅ 자동 타임스탬프
        problem.put("score", 1);

        Log.d("🔥DEBUG", "문제 업로드 경로: studyRooms/" + roomId + "/problems");

        db.collection("studyRooms").document(roomId)
                .collection("problems")
                .add(problem)
                .addOnSuccessListener(ref -> {
                    // ✅ 문제 개수 실시간 증가
                    db.collection("studyRooms").document(roomId)
                            .update("problemCount", FieldValue.increment(1))
                            .addOnSuccessListener(v -> Log.d("✅ 문제수 갱신", "problemCount +1 완료"))
                            .addOnFailureListener(e -> Log.e("⚠️ 문제수 갱신 실패", e.getMessage()));

                    // ✅ 선택사항: createdAt 정렬을 위한 serverTimestamp 적용
                    db.collection("studyRooms").document(roomId)
                            .collection("problems")
                            .orderBy("createdAt", Query.Direction.DESCENDING);

                    titleManager.addScore(1);
                    toast("문제가 등록되었습니다 🎉 (+1점)");
                    finish();
                })
                .addOnFailureListener(e -> toast("문제 등록 실패: " + e.getMessage()));
    }

    /** 🔹 roomId 자동 로드 */
    private void fetchRoomId(String roomName, @Nullable Runnable onComplete) {
        db.collection("studyRooms")
                .whereEqualTo("roomName", roomName)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        roomId = query.getDocuments().get(0).getId();
                        Log.d("🔥DEBUG", "roomId 로드 완료: " + roomId);
                    } else {
                        toast("⚠️ 방 정보를 찾을 수 없습니다.");
                    }
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    toast("❌ 방 정보를 불러오지 못했습니다.");
                    if (onComplete != null) onComplete.run();
                });
    }

    private void loadUserName() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) userName = safeString(doc.get("userId"), "익명");
                });
    }

    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
    private String text(TextView tv) { return tv.getText().toString().trim(); }
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

    }

