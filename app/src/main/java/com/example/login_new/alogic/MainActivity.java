package com.example.login_new.alogic;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ Firestore 기반 게시판 메인 화면 (통합 버전)
 * - 일반 게시글 / 공지사항 게시판
 * - 공지 팝업 + 관리자 권한
 * - 스터디·시간표 네비게이션
 * - 랜덤 매칭 버튼(RandomMatchActivity 이동)
 * - AI 버튼(ChatActivity 이동)
 * - getUserName 캐시 + 자동 UI 새로고침
 * - 좋아요/댓글 수 로그 출력
 */
public class MainActivity extends BaseBottomNavActivity {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private final List<DocumentSnapshot> postList = new ArrayList<>();

    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;
    private ImageButton menuBtn, btnNotice;

    // 🔹 새로 추가: AI 버튼
    private ImageButton btnAi;

    // 🔹 공지 팝업 관련
    private PopupWindow noticeWindow;
    private NoticeAdapter noticeAdapter;

    // ✅ 기본 게시판을 "일반게시글"로 사용
    private String selectedBorder = "일반게시글";

    // 🔹 사용자 이름 캐시
    private final Map<String, String> userNameCache = new HashMap<>();

    // 🔥 관리자 여부
    private boolean isAdmin = false;

    // 🔥 현재 로그인한 사용자의 학과 (공지 필터용)
    private String currentUserMajor = null;

    // 🔙 뒤로가기 두 번 눌러 종료용
    private long backPressedTime = 0L;

    // ⭐ 새 공지 감지용 리스너 + 읽음 기준 저장용 키
    private ListenerRegistration noticeListener;
    private static final String PREF_NOTICE = "notice_prefs";
    private static final String KEY_LAST_SEEN_NOTICE = "last_seen_notice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        // 관리자 여부 체크 (공지 팝업용)
        detectAdminUser();

        // 뷰 및 리스너 설정
        setupViews();

        // ⭐ 새 공지 실시간 감지 시작
        listenLatestNotice();

        updateBorderFromIntent();
        loadPostsFromFirestore();
        setupBottomNav(TAB_BOARD);
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

    @Override
    protected void onResume() {
        super.onResume();
        updateBorderFromIntent();
        loadPostsFromFirestore();
        detectAdminUser();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ⭐ 리스너 해제
        if (noticeListener != null) {
            noticeListener.remove();
            noticeListener = null;
        }
    }

    // -------------------------------------------------------
    // 🔥 관리자 체크 (공지사항 수정/삭제 권한)
    // -------------------------------------------------------
    private void detectAdminUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String role = snapshot.getString("role");
                        isAdmin = "admin".equals(role);
                        // 🔹 여기서 현재 사용자 학과도 같이 저장
                        currentUserMajor = snapshot.getString("major");
                        Log.d("MainActivity", "isAdmin = " + isAdmin + ", major = " + currentUserMajor);
                    })
                    .addOnFailureListener(e -> Log.e("MainActivity", "Admin load error: " + e));
        }
    }

    // -------------------------------------------------------
    // UI 초기화
    // -------------------------------------------------------
    private void setupViews() {

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(adapter);

        fabAdd = findViewById(R.id.fab_add);
        menuBtn = findViewById(R.id.menu_btn);
        btnNotice = findViewById(R.id.btnNotice);
        // 🔹 새로 추가: AI 버튼 findViewById
        btnAi = findViewById(R.id.btn_ai);

        // 🔥 공지 팝업 버튼
        btnNotice.setOnClickListener(v -> toggleNoticePopup());

        // 🔹 AI 버튼 → ChatActivity 이동
        if (btnAi != null) {
            btnAi.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // 게시글 클릭 → 상세보기 이동
        adapter.setOnItemClickListener(snapshot -> {
            Intent intent = new Intent(MainActivity.this, PostDetailActivity.class);
            intent.putExtra("postId", snapshot.getId());
            startActivity(intent);
        });

        // 메뉴 버튼 → 카테고리 설정
        menuBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CategorySettingsActivity.class))
        );

        // 글쓰기 버튼
        fabAdd.setOnClickListener(v -> openWriteActivity());
    }

    // -------------------------------------------------------
    // 글쓰기 이동 (공지사항은 관리자만 작성 가능)
    // -------------------------------------------------------
    private void openWriteActivity() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {

                    String role = snapshot.getString("role");
                    if ("공지사항".equals(selectedBorder) && !"admin".equals(role)) {
                        Toast.makeText(this, "공지사항은 관리자만 작성할 수 있습니다.", Toast.LENGTH_SHORT).show();
                        selectedBorder = "일반게시글";
                    }

                    Intent intent = new Intent(MainActivity.this, WriteActivity.class);
                    intent.putExtra("selectedBorder", selectedBorder);
                    startActivity(intent);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "사용자 정보 로드 실패", Toast.LENGTH_SHORT).show()
                );
    }

    // -------------------------------------------------------
    // 공지 팝업 표시
    // -------------------------------------------------------
    private void showNoticePopup() {

        View content = getLayoutInflater().inflate(R.layout.layout_notice_panel, null);
        RecyclerView rv = content.findViewById(R.id.recyclerNotice);
        TextView tvNoNotice = content.findViewById(R.id.tvNoNotice);
        TextView btnClose = content.findViewById(R.id.btnCloseNotice);

        // 🔹 상단 제목: "컴퓨터공학과 공지사항" 이런 식으로 표시
        TextView tvNoticeMajor = content.findViewById(R.id.tvNoticeMajor);
        TextView tvNoticeTitle = content.findViewById(R.id.tvNoticeTitle); // "공지사항" 고정

        if (tvNoticeMajor != null) {
            String majorLabel = (currentUserMajor != null && !currentUserMajor.isEmpty())
                    ? currentUserMajor
                    : "전체";
            tvNoticeMajor.setText(majorLabel);
        }

        rv.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 NoticeAdapter 에 isAdmin 전달 (관리자는 수정/삭제 가능)
        noticeAdapter = new NoticeAdapter(
                this,
                new ArrayList<>(),
                isAdmin,
                () -> loadNoticesInto(tvNoNotice)  // 🔥 수정/삭제 후 즉시 갱신
        );

        rv.setAdapter(noticeAdapter);

        // 팝업 설정
        int popupW = getResources().getDisplayMetrics().widthPixels - dpToPx(32);
        noticeWindow = new PopupWindow(content, popupW, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        noticeWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        noticeWindow.setOutsideTouchable(true);
        noticeWindow.setFocusable(true);

        // 위치 계산
        int[] btnPos = new int[2];
        btnNotice.getLocationInWindow(btnPos);
        int popupX = btnPos[0] + btnNotice.getWidth() - popupW;
        int popupY = btnPos[1];

        popupX = Math.max(dpToPx(16), popupX);

        noticeWindow.showAtLocation(btnNotice, Gravity.NO_GRAVITY, popupX, popupY);

        // 닫기
        btnClose.setOnClickListener(v -> noticeWindow.dismiss());

        // 공지 로드
        loadNoticesInto(tvNoNotice);
    }

    private void hideNoticePopup() {
        if (noticeWindow != null && noticeWindow.isShowing())
            noticeWindow.dismiss();
    }

    private void toggleNoticePopup() {
        if (noticeWindow != null && noticeWindow.isShowing())
            hideNoticePopup();
        else
            showNoticePopup();
    }

    // -------------------------------------------------------
    // 공지사항 불러오기
    // -------------------------------------------------------
    private void loadNoticesInto(TextView tvNoNotice) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1️⃣ 먼저 users 에서 현재 내 학과를 다시 읽어온다 (매번 최신값)
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snap -> {
                    currentUserMajor = snap.getString("major");

                    // 🔹 팝업 타이틀도 여기서 최신 학과로 갱신
                    if (noticeWindow != null && noticeWindow.isShowing()) {
                        View content = noticeWindow.getContentView();
                        TextView tvNoticeMajor = content.findViewById(R.id.tvNoticeMajor);
                        if (tvNoticeMajor != null) {
                            String majorLabel = (currentUserMajor != null && !currentUserMajor.isEmpty())
                                    ? currentUserMajor
                                    : "전체";
                            tvNoticeMajor.setText(majorLabel);
                        }
                    }

                    // 2️⃣ 이제 공지사항 글을 불러와서, 내 학과와 같은 것만 필터링
                    db.collection("posts")
                            .whereEqualTo("border", "공지사항")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(query -> {

                                List<NoticeItem> notices = new ArrayList<>();

                                for (QueryDocumentSnapshot doc : query) {
                                    String postMajor = doc.getString("major");
                                    if (postMajor != null && postMajor.equals(currentUserMajor)) {
                                        notices.add(new NoticeItem(
                                                doc.getId(),
                                                doc.getString("title"),
                                                doc.getString("content"),
                                                doc.getTimestamp("timestamp")
                                        ));
                                    }
                                }

                                // UI 반영
                                tvNoNotice.setVisibility(notices.isEmpty() ? View.VISIBLE : View.GONE);
                                noticeAdapter.updateList(notices);

                                // ⭐ 팝업 열었으니 "읽음 처리" + 강조 해제
                                if (!notices.isEmpty()) {
                                    NoticeItem latest = notices.get(0);
                                    if (latest.getTimestamp() != null) {
                                        long latestMs = latest.getTimestamp().toDate().getTime();
                                        getSharedPreferences(PREF_NOTICE, MODE_PRIVATE)
                                                .edit()
                                                .putLong(KEY_LAST_SEEN_NOTICE, latestMs)
                                                .apply();
                                        setNoticeHighlight(false);
                                    }
                                }

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "공지 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "학과 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                );
    }

    // -------------------------------------------------------
    // 일반 글 불러오기 (좋아요/댓글 로그 포함 버전)
    // -------------------------------------------------------
    private void loadPostsFromFirestore() {

        Query query = db.collection("posts")
                .whereEqualTo("border", selectedBorder)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            postList.clear();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                long likeCount = document.contains("likeCount") ? document.getLong("likeCount") : 0;
                long commentCount = document.contains("commentCount") ? document.getLong("commentCount") : 0;

                Log.d("MainActivity", "게시글: " + document.getString("title") +
                        " / 좋아요: " + likeCount + " / 댓글: " + commentCount);

                postList.add(document);
            }

            adapter.notifyDataSetChanged();

            if (postList.isEmpty()) {
                Toast.makeText(this, selectedBorder + "에 게시글이 없습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("MainActivity", selectedBorder + " 게시글 " + postList.size() + "개 로드됨");
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "게시글 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // -------------------------------------------------------
    // 사용자 이름 캐시 로드 (개선된 버전)
    // -------------------------------------------------------
    public String getUserName(String uid) {
        // 🔹 null 또는 빈 문자열이면 익명 처리
        if (uid == null || uid.isEmpty()) return "익명";

        // 캐시되어 있으면 바로 리턴
        if (userNameCache.containsKey(uid)) {
            return userNameCache.get(uid);
        }

        // 캐시에 없으면 Firestore 요청
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("userId");
                        if (name != null && !name.isEmpty()) {
                            userNameCache.put(uid, name);
                            Log.d("MainActivity", "✅ 사용자 이름 캐시에 저장: " + name);

                            // 🔹 이름 갱신 즉시 UI 업데이트
                            runOnUiThread(() -> {
                                if (adapter != null) adapter.notifyDataSetChanged();
                            });
                        }
                    } else {
                        Log.w("MainActivity", "⚠️ 사용자 문서 없음: " + uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("MainActivity", "❌ 사용자 이름 불러오기 실패: " + e.getMessage());
                    // 🔁 실패 시 2초 후 재시도 (선택)
                    recyclerView.postDelayed(() -> getUserName(uid), 2000);
                });

        return "로딩중...";
    }

    private void updateBorderFromIntent() {
        String borderFromIntent = getIntent().getStringExtra("selectedBorder");
        if (borderFromIntent != null) selectedBorder = borderFromIntent;
    }

    /** ✅ 게시판 전환 */
    public void switchBoard(String boardName) {
        selectedBorder = boardName;
        Toast.makeText(this, boardName + "으로 이동", Toast.LENGTH_SHORT).show();
        loadPostsFromFirestore();
    }

    // -------------------------------------------------------
    // ⭐ 새 공지 실시간 감지 → 버튼 테두리 색 변경
    // -------------------------------------------------------
    private void listenLatestNotice() {
        noticeListener = db.collection("posts")
                .whereEqualTo("border", "공지사항")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("MainActivity", "notice listener error: " + e.getMessage());
                        return;
                    }
                    if (snap == null || snap.isEmpty()) {
                        setNoticeHighlight(false);
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    Timestamp ts = doc.getTimestamp("timestamp");
                    if (ts == null) {
                        setNoticeHighlight(false);
                        return;
                    }

                    long latestMs = ts.toDate().getTime();
                    long lastSeen = getSharedPreferences(PREF_NOTICE, MODE_PRIVATE)
                            .getLong(KEY_LAST_SEEN_NOTICE, 0L);

                    boolean hasNew = latestMs > lastSeen;
                    setNoticeHighlight(hasNew);
                });
    }

    private void setNoticeHighlight(boolean highlight) {
        if (btnNotice == null) return;

        runOnUiThread(() -> {
            if (highlight) {
                btnNotice.setBackgroundResource(R.drawable.bg_notice_circle_alert);
            } else {
                btnNotice.setBackgroundResource(R.drawable.bg_notice_circle);
            }
        });
    }

}
