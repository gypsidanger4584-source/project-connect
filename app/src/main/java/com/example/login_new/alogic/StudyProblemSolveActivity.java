package com.example.login_new.alogic;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class StudyProblemSolveActivity extends AppCompatActivity {

    private TextView tvAuthorName, tvPostDate, tvQuestion, tvTitle, tvSolvedNotice;
    private RadioGroup radioGroup;
    private Button btnSubmitAnswer, btnPrevProblem, btnNextProblem;
    private ImageButton btnBack;
    private Button btnDeleteProblem;   // ← 수정된 부분!

    private ImageView ivTrophy;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TitleManager titleManager;

    private String roomId;
    private String problemId;
    private String creatorUid;

    private List<DocumentSnapshot> problemList = new ArrayList<>();
    private int currentIndex = -1;

    private int correctAnswerIndex = -1;
    private int problemScore = 1;
    private boolean alreadySolved = false;
    private int userSelectedIndex = -1;
    private boolean isMyProblem = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_problem_solve);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        titleManager = new TitleManager();

        initViews();

        roomId = getIntent().getStringExtra("roomId");
        problemId = getIntent().getStringExtra("problemId");

        if (roomId == null || currentUser == null) {
            toast("문제 정보를 불러올 수 없습니다.");
            finish();
            return;
        }

        loadAllProblems();

        btnSubmitAnswer.setOnClickListener(v -> checkAnswer());
        btnBack.setOnClickListener(v -> finish());
        btnPrevProblem.setOnClickListener(v -> moveToPreviousProblem());
        btnNextProblem.setOnClickListener(v -> moveToNextProblem());
        btnDeleteProblem.setOnClickListener(v -> showDeleteDialog());
    }

    private void initViews() {
        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvPostDate = findViewById(R.id.tvPostDate);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvTitle = findViewById(R.id.tvTitle);
        radioGroup = findViewById(R.id.radioGroupOptions);
        btnSubmitAnswer = findViewById(R.id.btnSubmitAnswer);
        btnBack = findViewById(R.id.btnBack);
        ivTrophy = findViewById(R.id.ivTrophy);
        tvSolvedNotice = findViewById(R.id.tvSolvedNotice);
        btnPrevProblem = findViewById(R.id.btnPrevProblem);
        btnNextProblem = findViewById(R.id.btnNextProblem);
        btnDeleteProblem = findViewById(R.id.btnDeleteProblem);
    }

    /** 🔹 전체 문제 로드 */
    private void loadAllProblems() {
        db.collection("studyRooms").document(roomId)
                .collection("problems")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {

                    problemList = snapshot.getDocuments();

                    if (problemList.isEmpty()) {
                        toast("문제가 없습니다.");
                        finish();
                        return;
                    }

                    int foundIndex = 0;
                    if (problemId != null) {
                        for (int i = 0; i < problemList.size(); i++) {
                            if (problemList.get(i).getId().equals(problemId)) {
                                foundIndex = i;
                                break;
                            }
                        }
                    }

                    currentIndex = foundIndex;
                    problemId = problemList.get(currentIndex).getId();
                    showProblemAt(currentIndex);
                })
                .addOnFailureListener(e -> toast("문제 불러오기 실패: " + e.getMessage()));
    }

    /** 🔹 문제 표시 */
    private void showProblemAt(int index) {
        if (index < 0 || index >= problemList.size()) return;

        currentIndex = index;
        DocumentSnapshot doc = problemList.get(currentIndex);
        problemId = doc.getId();

        alreadySolved = false;
        userSelectedIndex = -1;
        correctAnswerIndex = -1;

        ivTrophy.setVisibility(View.GONE);
        tvSolvedNotice.setVisibility(View.GONE);
        btnSubmitAnswer.setVisibility(View.VISIBLE);

        // 작성자 UID 저장
        creatorUid = safeString(doc.get("creatorUid"), "");
        isMyProblem = creatorUid.equals(currentUser.getUid());

        checkIfAlreadySolvedThenUpdateUI(doc);

        updateUI(doc);

        btnPrevProblem.setEnabled(currentIndex > 0);
        btnNextProblem.setEnabled(currentIndex < problemList.size() - 1);

        btnDeleteProblem.setVisibility(isMyProblem ? View.VISIBLE : View.GONE);
    }

    /** 🔹 이미 푼 문제인지 확인 후 UI 업데이트 */
    private void checkIfAlreadySolvedThenUpdateUI(DocumentSnapshot doc) {
        db.collection("studyRooms").document(roomId)
                .collection("problems").document(problemId)
                .collection("solvedUsers").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(solved -> {
                    if (solved.exists()) {
                        alreadySolved = true;
                        Long idx = solved.getLong("answerIndex");
                        if (idx != null) userSelectedIndex = idx.intValue();

                        tvSolvedNotice.setVisibility(View.VISIBLE);
                        tvSolvedNotice.setText("✓ 이미 푼 문제입니다.");
                        btnSubmitAnswer.setVisibility(View.GONE);
                    }
                    updateUI(doc);
                });
    }

    /** 🔹 UI 업데이트 */
    private void updateUI(DocumentSnapshot doc) {

        String creator = safeString(doc.get("creatorName"), "익명");
        String creatorUid = safeString(doc.get("creatorUid"), "");
        boolean isMyProblem = creatorUid.equals(currentUser.getUid());

        tvAuthorName.setText(creator);
        tvPostDate.setText(safeString(doc.get("timeAgo"), ""));
        tvTitle.setText("[" + safeString(doc.get("category"), "") + "] "
                + safeString(doc.get("level"), "") + " · "
                + safeString(doc.get("title"), "제목 없음"));

        tvQuestion.setText((currentIndex + 1) + ". "
                + safeString(doc.get("question"), "내용 없음"));

        problemScore = safeInt(doc.get("score"), 1);
        Long answerIndex = doc.getLong("answerIndex");
        correctAnswerIndex = answerIndex != null ? answerIndex.intValue() : -1;

        // 선택지 그리기
        radioGroup.removeAllViews();
        List<String> options = (List<String>) doc.get("options");
        if (options != null) {
            char opt = 'A';
            for (String o : options) {
                RadioButton rb = new RadioButton(this);
                rb.setText(opt + ". " + o);
                rb.setTextSize(16);
                rb.setId(ViewIdGenerator.generateViewId());
                radioGroup.addView(rb);
                opt++;
            }
        }

        // 🔥 본인이 만든 문제 — 풀기만 막고 삭제는 가능해야 함
        if (isMyProblem) {
            tvSolvedNotice.setVisibility(View.VISIBLE);
            tvSolvedNotice.setText("🚫 본인이 만든 문제는 풀 수 없습니다.");

            btnSubmitAnswer.setVisibility(View.GONE);  // 제출 버튼 숨김
            disableAllOptions();                      // 선택지 비활성화

            // ❗ 삭제 버튼은 보이게 유지
            btnDeleteProblem.setVisibility(View.VISIBLE);

            // ❗ return 하지 말기! → 아래 UI 업데이트 계속 진행됨
        }


        // 🔥 이미 푼 문제 UI 처리
        if (alreadySolved && userSelectedIndex != -1) {
            RadioButton rb = (RadioButton) radioGroup.getChildAt(userSelectedIndex);
            if (rb != null) rb.setChecked(true);

            if (correctAnswerIndex != -1) {
                RadioButton correctRb = (RadioButton) radioGroup.getChildAt(correctAnswerIndex);

                if (userSelectedIndex == correctAnswerIndex) {
                    rb.setTextColor(getColor(R.color.correct_green));
                    showTrophy();
                } else {
                    rb.setTextColor(getColor(R.color.wrong_red));
                    if (correctRb != null)
                        correctRb.setTextColor(getColor(R.color.correct_green));
                }
            }

            disableAllOptions();
            btnSubmitAnswer.setVisibility(View.GONE);
        }
    }

    /** 🔥 정답 체크 */
    private void checkAnswer() {
        int selectedIndex = radioGroup.indexOfChild(findViewById(radioGroup.getCheckedRadioButtonId()));
        if (selectedIndex == -1) {
            toast("정답을 선택하세요.");
            return;
        }

        boolean isCorrect = (selectedIndex == correctAnswerIndex);

        RadioButton selected = (RadioButton) radioGroup.getChildAt(selectedIndex);
        if (isCorrect) {
            selected.setTextColor(getColor(R.color.correct_green));
            showTrophy();
            toast("정답입니다!");
        } else {
            selected.setTextColor(getColor(R.color.wrong_red));
            RadioButton correct = (RadioButton) radioGroup.getChildAt(correctAnswerIndex);
            if (correct != null)
                correct.setTextColor(getColor(R.color.correct_green));
            toast("오답입니다.");
        }

        giveReward(isCorrect, selectedIndex);

        disableAllOptions();
        btnSubmitAnswer.setVisibility(View.GONE);
    }

    /** 🔥 보상 지급 + Firestore 저장 */
    private void giveReward(boolean isCorrect, int selectedIndex) {

        Map<String, Object> record = new HashMap<>();
        record.put("correct", isCorrect);
        record.put("answerIndex", selectedIndex);
        record.put("solvedAt", FieldValue.serverTimestamp());

        db.collection("studyRooms").document(roomId)
                .collection("problems").document(problemId)
                .collection("solvedUsers").document(currentUser.getUid())
                .set(record, SetOptions.merge())
                .addOnSuccessListener(a -> {

                    if (isCorrect && !alreadySolved) {
                        updateUserScore(problemScore);
                        titleManager.addScore(problemScore);
                        toast("+" + problemScore + "점 획득!");
                    }
                });
    }

    /** 🔥 Firestore → users.totalScore 증가 */
    private void updateUserScore(int add) {
        DocumentReference ref = db.collection("users").document(currentUser.getUid());

        db.runTransaction(tr -> {
            DocumentSnapshot snap = tr.get(ref);
            long cur = snap.contains("totalScore") ? snap.getLong("totalScore") : 0;
            tr.update(ref, "totalScore", cur + add);
            return null;
        });
    }

    /** 🔥 문제 삭제 기능 */
    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("문제 삭제")
                .setMessage("이 문제를 정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteProblem())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteProblem() {
        db.collection("studyRooms").document(roomId)
                .collection("problems").document(problemId)
                .delete()
                .addOnSuccessListener(a -> {

                    toast("문제가 삭제되었습니다.");

                    problemList.remove(currentIndex);

                    if (problemList.isEmpty()) {
                        finish();
                        return;
                    }

                    if (currentIndex >= problemList.size())
                        currentIndex = problemList.size() - 1;

                    problemId = problemList.get(currentIndex).getId();
                    showProblemAt(currentIndex);
                });
    }

    /** 🔥 문제 이동 — 이전 */
    private void moveToPreviousProblem() {
        if (currentIndex > 0) {
            currentIndex--;
            problemId = problemList.get(currentIndex).getId();
            showProblemAt(currentIndex);
        } else {
            toast("첫 번째 문제입니다.");
        }
    }

    /** 🔥 문제 이동 — 다음 */
    private void moveToNextProblem() {
        if (currentIndex < problemList.size() - 1) {
            currentIndex++;
            problemId = problemList.get(currentIndex).getId();
            showProblemAt(currentIndex);
        } else {
            toast("마지막 문제입니다.");
        }
    }

    private void disableAllOptions() {
        for (int i = 0; i < radioGroup.getChildCount(); i++)
            radioGroup.getChildAt(i).setEnabled(false);
    }

    private void showTrophy() {
        ivTrophy.setVisibility(View.VISIBLE);
    }

    private static String safeString(Object v, String fallback) {
        if (v == null) return fallback;
        String s = v.toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    private static int safeInt(Object value, int fallback) {
        try { return Integer.parseInt(value.toString()); }
        catch (Exception e) { return fallback; }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public static class ViewIdGenerator {
        private static int nextId = 1;
        public static synchronized int generateViewId() { return nextId++; }
    }
}
