package com.example.login_new.alogic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import android.view.View;

import java.util.*;

public class StudyProblemActivity extends AppCompatActivity {

    // -------------------- UI --------------------
    private TextView tvRoomTitle, tvRoomInfo, tabChat, tabProblem;
    private ImageButton btnBack, btnMenu;
    private RecyclerView recyclerProblems, recyclerActive, recyclerInactive;
    private Button btnAddProblem, btnLeaveRoom;   // ⭐ 방 나가기만 유지

    private Button btnDeleteRoom;


    // Drawer
    private DrawerLayout drawerLayout;
    private ImageView ivMyProfile;
    private TextView tvMyId, tvMyTitle;
    private TextView tvActiveTitle, tvInactiveTitle;

    // -------------------- Adapter --------------------
    private StudyProblemAdapter problemAdapter;
    private ParticipantAdapter activeAdapter, inactiveAdapter;

    // -------------------- Lists --------------------
    private final List<StudyProblemItem> problemList = new ArrayList<>();
    private final List<Participant> activeList = new ArrayList<>();
    private final List<Participant> inactiveList = new ArrayList<>();

    // -------------------- Firebase --------------------
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // -------------------- States --------------------
    private String roomId, roomName, ownerUid;
    private String userUid = "";
    private boolean isOwner = false;

    private ListenerRegistration roomListener;
    private ListenerRegistration problemListener;
    private ListenerRegistration participantListener;
    private ListenerRegistration userProfileListener;

    // =============================================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_problem_list);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) userUid = currentUser.getUid();

        initViews();
        setupRecyclerViews();
        setupButtons();

        roomName = getIntent().getStringExtra("roomName");
        roomId = getIntent().getStringExtra("roomId");

        if (TextUtils.isEmpty(roomName)) roomName = "스터디룸";
        tvRoomTitle.setText(roomName);

        if (!TextUtils.isEmpty(roomId)) initRoomData();
        else findRoomByName();
    }

    // =============================================================
    private void initViews() {
        drawerLayout = findViewById(R.id.studyDrawerLayout);

        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        tvRoomInfo = findViewById(R.id.tvRoomInfo);

        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);

        tabChat = findViewById(R.id.tabChat);
        tabProblem = findViewById(R.id.tabProblem);

        recyclerProblems = findViewById(R.id.recyclerProblems);
        btnAddProblem = findViewById(R.id.btnAddProblem);

        recyclerActive = findViewById(R.id.recyclerActive);
        recyclerInactive = findViewById(R.id.recyclerInactive);
        tvActiveTitle = findViewById(R.id.tvActiveTitle);
        tvInactiveTitle = findViewById(R.id.tvInactiveTitle);

        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);   // ⭐ 방 나가기 버튼 유지
        btnDeleteRoom = findViewById(R.id.btnDeleteRoom);


        // Drawer 프로필
        ivMyProfile = findViewById(R.id.userProfile);
        tvMyId = findViewById(R.id.userName);
        tvMyTitle = findViewById(R.id.userTitle);
    }

    // =============================================================
    private void setupRecyclerViews() {
        recyclerProblems.setLayoutManager(new LinearLayoutManager(this));
        problemAdapter = new StudyProblemAdapter(problemList, roomId, this::onProblemClick);
        recyclerProblems.setAdapter(problemAdapter);

        recyclerActive.setLayoutManager(new LinearLayoutManager(this));
        recyclerInactive.setLayoutManager(new LinearLayoutManager(this));

        activeAdapter = new ParticipantAdapter(this, activeList, userUid);
        inactiveAdapter = new ParticipantAdapter(this, inactiveList, userUid);

        recyclerActive.setAdapter(activeAdapter);
        recyclerInactive.setAdapter(inactiveAdapter);
    }

    // =============================================================
    private void setupButtons() {
        btnBack.setOnClickListener(v -> goBackToBoard());
        btnMenu.setOnClickListener(v -> toggleDrawer());

        tabProblem.setOnClickListener(v ->
                Toast.makeText(this, "이미 문제 탭입니다.", Toast.LENGTH_SHORT).show());

        tabChat.setOnClickListener(v -> {
            Intent i = new Intent(this, StudyChatActivity.class);
            i.putExtra("roomId", roomId);
            i.putExtra("roomName", roomName);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnAddProblem.setOnClickListener(v -> {
            Intent i = new Intent(this, StudyProblemCreateActivity.class);
            i.putExtra("roomId", roomId);
            i.putExtra("roomName", roomName);
            startActivity(i);
        });
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END))
            drawerLayout.closeDrawer(GravityCompat.END);
        else
            drawerLayout.openDrawer(GravityCompat.END);
    }

    // =============================================================
    private void initRoomData() {

        loadMyDrawerProfile();

        roomListener = db.collection("studyRooms").document(roomId)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !snap.exists()) return;

                    ownerUid = safeString(snap.get("ownerUid"), "");
                    isOwner = userUid.equals(ownerUid);

                    activeAdapter.setOwnerUid(ownerUid);
                    inactiveAdapter.setOwnerUid(ownerUid);

                    Long cur = snap.getLong("currentPeople");
                    Long max = snap.getLong("maxPeople");
                    Long prob = snap.getLong("problemCount");

                    tvRoomInfo.setText("👥 " + cur + "/" + max + " · 문제 " + prob);

                    // ⭐ 방장/참가자 UI 구분
                    if (isOwner) {
                        // 🔥 방장: 방 삭제 버튼 표시
                        btnLeaveRoom.setVisibility(View.GONE);          // 방 나가기 숨김
                        btnDeleteRoom.setVisibility(View.VISIBLE);      // 방 삭제 표시
                        btnDeleteRoom.setOnClickListener(v -> confirmDeleteRoom());

                    } else {
                        // 🔥 일반 참가자: 방 나가기 버튼 표시
                        btnDeleteRoom.setVisibility(View.GONE);         // 방 삭제 숨김
                        btnLeaveRoom.setVisibility(View.VISIBLE);       // 방 나가기 표시
                        btnLeaveRoom.setText("🚪 방 나가기");
                        btnLeaveRoom.setOnClickListener(v -> leaveRoom());
                    }
                });

        listenParticipants();
        listenProblems();
    }




    private void findRoomByName() {
        db.collection("studyRooms")
                .whereEqualTo("roomName", roomName)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) return;
                    roomId = q.getDocuments().get(0).getId();
                    initRoomData();
                });
    }

    // =============================================================
    private void listenParticipants() {
        if (participantListener != null) participantListener.remove();

        participantListener = db.collection("studyRooms").document(roomId)
                .collection("participants")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;

                    activeList.clear();
                    inactiveList.clear();

                    for (DocumentSnapshot d : snap) {
                        Participant p = d.toObject(Participant.class);
                        if (p == null) continue;

                        if (p.isActive()) activeList.add(p);
                        else inactiveList.add(p);
                    }

                    activeAdapter.updateParticipants(activeList);
                    inactiveAdapter.updateParticipants(inactiveList);

                    tvActiveTitle.setText("활동자 (" + activeList.size() + ")");
                    tvInactiveTitle.setText("비활동자 (" + inactiveList.size() + ")");
                });
    }

    // =============================================================
    private void loadMyDrawerProfile() {

        if (currentUser == null) return;
        String uid = currentUser.getUid();

        if (userProfileListener != null) userProfileListener.remove();

        userProfileListener = db.collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (doc == null || !doc.exists()) return;

                    String name = safeString(doc.get("userId"), "익명");
                    String title = safeString(doc.get("title"), "입문자");
                    String url = safeString(doc.get("profileUrl"), "");
                    String resStr = safeString(doc.get("profileResId"), "");

                    tvMyId.setText(name);
                    tvMyTitle.setText(title);

                    if (!url.isEmpty()) {

                        if (url.startsWith("http")) {
                            Glide.with(this)
                                    .load(url)
                                    .placeholder(R.drawable.default_profile)
                                    .circleCrop()
                                    .into(ivMyProfile);
                            return;
                        }

                        int drawableId = getResources().getIdentifier(url, "drawable", getPackageName());
                        if (drawableId != 0) {
                            ivMyProfile.setImageResource(drawableId);
                            return;
                        }
                    }

                    try {
                        int id = Integer.parseInt(resStr);
                        ivMyProfile.setImageResource(id);
                        return;
                    } catch (Exception ignored) {}

                    ivMyProfile.setImageResource(R.drawable.default_profile);
                });
    }

    // =============================================================
    private void listenProblems() {
        if (problemListener != null) problemListener.remove();

        problemListener = db.collection("studyRooms").document(roomId)
                .collection("problems")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;

                    problemList.clear();
                    int count = snap.size();
                    updateProblemCountUI(count);

                    for (DocumentSnapshot d : snap) {

                        StudyProblemItem item = new StudyProblemItem(
                                d.getId(),
                                safeString(d.get("title"), "제목 없음"),
                                safeString(d.get("creatorName"), "익명"),
                                "",
                                safeString(d.get("level"), "보통"),
                                safeString(d.get("category"), "기타")
                        );

                        // creatorUid
                        item.setCreatorUid(safeString(d.get("creatorUid"), ""));

                        // solvedUsers
                        db.collection("studyRooms").document(roomId)
                                .collection("problems").document(d.getId())
                                .collection("solvedUsers")
                                .document(userUid)
                                .get()
                                .addOnSuccessListener(solvedDoc -> {

                                    if (solvedDoc.exists()) {
                                        item.setSolved(true);

                                        Boolean correct = solvedDoc.getBoolean("correct");
                                        item.setCorrectSolved(correct != null && correct);
                                    }

                                    problemAdapter.notifyDataSetChanged();
                                });

                        problemList.add(item);
                    }

                    problemAdapter.notifyDataSetChanged();
                });
    }


    private void updateProblemCountUI(int count) {
        String base = tvRoomInfo.getText().toString();
        String people = base.contains("·") ? base.split("·")[0] : base;
        tvRoomInfo.setText(people + " · 문제 " + count);
    }
    // =============================================================
// 🔥 방 삭제 확인 다이얼로그
    private void confirmDeleteRoom() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("방 삭제")
                .setMessage("정말 삭제하시겠습니까?\n삭제하면 되돌릴 수 없습니다.")
                .setPositiveButton("삭제", (d, w) -> deleteRoomCompletely())
                .setNegativeButton("취소", null)
                .show();
    }
    /** 🔥 방 전체 삭제 (문제, 메시지, 참가자 포함) */
    private void deleteRoomCompletely() {

        // 🔥 리스너 제거 (아주 중요)
        if (roomListener != null) roomListener.remove();
        if (problemListener != null) problemListener.remove();
        if (participantListener != null) participantListener.remove();
        if (userProfileListener != null) userProfileListener.remove();


        DocumentReference roomRef = db.collection("studyRooms").document(roomId);

        // 1) 참가자 삭제
        roomRef.collection("participants").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap) {
                        d.getReference().delete();
                    }
                });

        // 2) 메시지 삭제
        roomRef.collection("messages").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap) {
                        d.getReference().delete();
                    }
                });

        // 3) 문제 삭제
        roomRef.collection("problems").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap) {
                        d.getReference().delete();
                    }
                });

        // 4) 최종 방 삭제
        roomRef.delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "방이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    goBackToBoard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }



    // =============================================================
    // 🔥 방 나가기 기능
    private void leaveRoom() {
        if (currentUser == null) return;

        db.collection("studyRooms").document(roomId)
                .collection("participants").document(userUid)
                .update("isActive", false);

        goBackToBoard();
    }

    // =============================================================
    private void onProblemClick(StudyProblemItem item) {
        Intent i = new Intent(this, StudyProblemSolveActivity.class);
        i.putExtra("roomId", roomId);
        i.putExtra("problemId", item.getId());
        startActivity(i);
    }

    private void goBackToBoard() {
        Intent i = new Intent(this, StudyBoardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (roomListener != null) roomListener.remove();
        if (problemListener != null) problemListener.remove();
        if (participantListener != null) participantListener.remove();
        if (userProfileListener != null) userProfileListener.remove();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        goBackToBoard();
    }

    private static String safeString(Object v, String fallback) {
        if (v == null) return fallback;
        String s = v.toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}
