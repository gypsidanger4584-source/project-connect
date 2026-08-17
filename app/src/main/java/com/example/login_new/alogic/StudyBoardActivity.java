package com.example.login_new.alogic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🏆 StudyBoardActivity (2025-11 안정형)
 * ✅ 공개된 스터디룸 로드
 * ✅ 실시간 인원수 / 문제수 반영
 * ✅ 내 점수(tvCoin) Firestore에서 실시간 연동
 * 🔥 추가 기능:
 *   - 내가 만든 방 최상단 정렬
 *   - 내가 만든 방 배지 표시
 *   - 참여 여부 전달
 *   - 카테고리 스피너 필터
 *   - 하단 랜덤매칭 버튼(nav_random_chat)
 */
public class StudyBoardActivity extends BaseBottomNavActivity {

    private RecyclerView recyclerStudyRooms;
    private StudyRoomAdapter adapter;

    // 🔹 실제로 화면에 표시하는 리스트
    private final List<StudyRoom> studyRooms = new ArrayList<>();

    // 🔹 Firestore에서 받아온 전체 방 목록 (필터 적용 전)
    private final List<StudyRoom> allRooms = new ArrayList<>();

    private FirebaseFirestore db;
    private static final String TAG = "📚StudyBoardActivity";

    private TextView tvCoin;
    private FirebaseUser currentUser;
    private ListenerRegistration scoreListener;

    // 🔙 뒤로가기 두 번 눌러 종료용
    private long backPressedTime = 0L;

    // 🔹 카테고리 필터 스피너
    private Spinner spinnerCategory;

    // 🔹 현재 선택된 카테고리 (기본값: 전체)
    private String currentCategory = "전체";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_board);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerStudyRooms = findViewById(R.id.recyclerStudyRooms);
        recyclerStudyRooms.setLayoutManager(new LinearLayoutManager(this));

        tvCoin = findViewById(R.id.tvCoin);
        listenUserScoreRealtime();

        // 🔹 어댑터 설정
        adapter = new StudyRoomAdapter(studyRooms, room -> {
            if (room.getRoomId() == null || room.getRoomId().isEmpty()) {
                Toast.makeText(this, "⚠️ 방 ID가 비어있습니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "roomId is null for room: " + room.getRoomName());
                return;
            }

            Intent intent = new Intent(this, StudyChatActivity.class);
            intent.putExtra("roomId", room.getRoomId());
            intent.putExtra("roomName", room.getRoomName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        recyclerStudyRooms.setAdapter(adapter);

        // 🔹 카테고리 스피너 초기화 (XML에서 entries로 데이터 연결되어 있음)
        spinnerCategory = findViewById(R.id.spinnerCategory);
        if (spinnerCategory != null) {
            spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selected = parent.getItemAtPosition(position).toString();
                    currentCategory = selected;
                    Log.d(TAG, "🔎 선택된 카테고리: " + currentCategory);
                    applyCategoryFilter();   // ✅ 선택이 바뀔 때마다 필터 적용
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // 아무것도 선택 안 되면 전체로 유지
                }
            });
        }

        // 🔹 전체 방 로드 (실시간)
        loadOpenRooms();

        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudyRoomCreateActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        ImageButton menuBtn = findViewById(R.id.btnMenu);
        if (menuBtn != null) {
            menuBtn.setOnClickListener(v -> {
                Intent intent = new Intent(StudyBoardActivity.this, CategorySettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        setupBottomNav(TAB_STUDY);
    }

    /**
     * 🔹 Firestore에서 모든 스터디룸 로드
     *  - 만들어진 방 전체를 allRooms에 저장
     *  - currentCategory에 맞춰 studyRooms를 필터링해서 보여줌
     */
    private void loadOpenRooms() {
        db.collection("studyRooms")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "❌ Firestore 로드 실패: " + e.getMessage());
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        allRooms.clear();
                        applyCategoryFilter(); // 화면에서도 빈 상태로 반영
                        return;
                    }

                    List<StudyRoom> firestoreRooms = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        StudyRoom room = doc.toObject(StudyRoom.class);

                        if (room != null) {
                            room.setRoomId(doc.getId());
                            firestoreRooms.add(room);

                            Log.d(TAG, "📄 로드된 방: " + room.getRoomName());

                            // ---------------------------------------
                            // ⭐ 참여자 목록 로드 → 참여 여부 구분 가능
                            // ---------------------------------------
                            db.collection("studyRooms")
                                    .document(room.getRoomId())
                                    .collection("participants")
                                    .addSnapshotListener((participantSnap, ex) -> {

                                        List<String> uidList = new ArrayList<>();
                                        int activeCount = 0;

                                        if (participantSnap != null) {
                                            for (DocumentSnapshot p : participantSnap) {
                                                String uid = p.getId();
                                                uidList.add(uid);

                                                Boolean isActive = p.getBoolean("isActive");
                                                if (isActive != null && isActive) activeCount++;
                                            }
                                        }

                                        room.setParticipants(uidList);       // ⭐ 참여자 저장
                                        room.setCurrentPeople(activeCount);  // 사람수 반영

                                        // Firestore 실시간 업데이트
                                        db.collection("studyRooms").document(room.getRoomId())
                                                .update("currentPeople", activeCount);

                                        // 인원 수 변경만 반영 → 필터는 그대로
                                        adapter.notifyDataSetChanged();
                                    });

                            // ---- 문제 개수 실시간 반영 ----
                            db.collection("studyRooms").document(room.getRoomId())
                                    .collection("problems")
                                    .addSnapshotListener((problemSnap, ex) -> {
                                        if (problemSnap == null) return;

                                        int problemCount = problemSnap.size();
                                        room.setProblemCount(problemCount);

                                        db.collection("studyRooms").document(room.getRoomId())
                                                .update("problemCount", problemCount);

                                        adapter.notifyDataSetChanged();
                                    });
                        }
                    }

                    // -----------------------------------------------------
                    // ⭐⭐ 여기서 "내가 만든 방" 최상단 정렬 + 배지 표시
                    // -----------------------------------------------------
                    if (currentUser != null) {
                        String myUid = currentUser.getUid();

                        for (StudyRoom r : firestoreRooms) {
                            if (r.getOwnerUid().equals(myUid)) {
                                r.setCreatorName("내 방"); // 표시용
                            }
                        }

                        Collections.sort(firestoreRooms, (a, b) -> {
                            boolean aMine = a.getOwnerUid().equals(myUid);
                            boolean bMine = b.getOwnerUid().equals(myUid);

                            if (aMine && !bMine) return -1;
                            if (!aMine && bMine) return 1;

                            // createdAt이 null일 수 있으니 방어 코드
                            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                            if (a.getCreatedAt() == null) return 1;
                            if (b.getCreatedAt() == null) return -1;

                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        });
                    }

                    // 🔹 Firestore에서 가져온 전체 방을 allRooms에 저장
                    allRooms.clear();
                    allRooms.addAll(firestoreRooms);

                    // 🔹 현재 선택된 카테고리에 맞춰 화면에 표시
                    applyCategoryFilter();
                });
    }

    /**
     * 🔎 현재 선택된 카테고리(currentCategory)에 맞춰 allRooms에서 studyRooms를 필터링
     */
    private void applyCategoryFilter() {
        studyRooms.clear();

        // "전체"면 필터 없이 다 보여주기
        if (currentCategory == null || currentCategory.equals("전체")) {
            studyRooms.addAll(allRooms);
        } else {
            for (StudyRoom r : allRooms) {
                String roomCategory = r.getCategory();
                if (roomCategory == null) roomCategory = "기타";

                // 🔹 스피너 항목과 StudyRoom.category 문자열이 정확히 같아야 매칭됨
                if (currentCategory.equals(roomCategory)) {
                    studyRooms.add(r);
                }
            }
        }

        adapter.notifyDataSetChanged();
        Log.d(TAG, "📌 필터 적용 완료 - 카테고리: " + currentCategory + ", 방 수: " + studyRooms.size());
    }

    /** 🔹 Firestore users/{uid}/score 실시간 감시 */
    private void listenUserScoreRealtime() {
        if (currentUser == null) return;

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        if (scoreListener != null) {
            scoreListener.remove();
            scoreListener = null;
        }

        scoreListener = userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "❌ 점수 리스너 오류: " + e.getMessage());
                return;
            }

            long scoreValue = 0;
            if (snapshot != null && snapshot.exists()) {
                Long score = snapshot.getLong("score");
                if (score != null) scoreValue = score;
            }

            long finalScore = scoreValue;

            runOnUiThread(() -> {
                if (tvCoin != null) tvCoin.setText(String.valueOf(finalScore));
            });

            Log.d(TAG, "🏆 실시간 점수 수신 (score): " + finalScore);
        });
    }

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - backPressedTime < 2000) {
            // 2초 안에 한 번 더 눌렀으니까 앱 종료
            finishAffinity();  // 현재 Task에 있는 액티비티 전부 종료
        } else {
            backPressedTime = currentTime;
            Toast.makeText(this, "한번 더 클릭시 연성 커넥트를 끕니다", Toast.LENGTH_SHORT).show();
        }
    }

    /** 하단 네비 설정 */
//    private void setupBottomNav() {
//        LinearLayout navBoard = findViewById(R.id.nav_board);
//        LinearLayout btnStudyBoard = findViewById(R.id.btnStudyBoard);
//        LinearLayout btnTimetable = findViewById(R.id.btntimetable);
//        LinearLayout navRandomChat = findViewById(R.id.nav_random_chat); // ⭐ 학식 랜덤매칭
//
//        navBoard.setOnClickListener(v -> {
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//            startActivity(intent);
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//            finish();
//        });
//
//        btnStudyBoard.setOnClickListener(v ->
//                Toast.makeText(this, "이미 스터디 탭입니다.", Toast.LENGTH_SHORT).show()
//        );
//
//        btnTimetable.setOnClickListener(v -> {
//            Intent intent = new Intent(this, TimetableActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//            startActivity(intent);
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//            finish();
//        });
//
//        // ⭐ 랜덤매칭 버튼 → RandomMatchActivity로 이동
//        if (navRandomChat != null) {
//            navRandomChat.setOnClickListener(v -> {
//                Intent intent = new Intent(this, RandomMatchActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                startActivity(intent);
//                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//            });
//        }
//    }
}
