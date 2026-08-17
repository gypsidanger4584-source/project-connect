package com.example.login_new.alogic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.view.View;

public class StudyChatActivity extends AppCompatActivity {

    // -------------------- UI --------------------
    private TextView tvRoomTitle, tvRoomInfo, tabChat, tabProblem;
    private TextView tvMyId, tvMyTitle, tvActiveTitle, tvInactiveTitle;
    private RecyclerView recyclerChat, recyclerActive, recyclerInactive;
    private EditText editMessage;
    private ImageButton btnSend, btnBack, btnMenu;
    private DrawerLayout drawerLayout;
    private ImageView ivMyProfile;

    private Button btnLeaveRoom;
    private Button btnDeleteRoom;

    // -------------------- Adapter --------------------
    private StudyChatAdapter chatAdapter;
    private ParticipantAdapter activeAdapter, inactiveAdapter;

    // -------------------- Lists --------------------
    private final List<StudyChatMessage> messageList = new ArrayList<>();
    private final List<Participant> activeList = new ArrayList<>();
    private final List<Participant> inactiveList = new ArrayList<>();

    // -------------------- Firebase --------------------
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // -------------------- States --------------------
    private String roomId, roomName, ownerUid;
    private String userUid = "";
    private String userId = "익명";
    private String userTitle = "입문자";
    private String profileUrl = "";
    private int userProfileResId = R.drawable.default_profile;
    private boolean isOwner = false;

    // 🔹 웰컴 시스템 메시지를 한 번만 쏘기 위한 플래그
    private boolean systemWelcomeSent = false;

    private ListenerRegistration messageListener;
    private ListenerRegistration roomListener;
    private ListenerRegistration participantListener;
    private ListenerRegistration userProfileListener;

    // =====================================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_chat);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) userUid = currentUser.getUid();

        initViews();
        setupRecycler();
        setupButtons();

        roomId = getIntent().getStringExtra("roomId");
        roomName = getIntent().getStringExtra("roomName");

        if (TextUtils.isEmpty(roomId)) {
            Toast.makeText(this, "⚠️ 방 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 기본 방 제목: roomName 없으면 strings.xml에 있는 Room A 사용
        tvRoomTitle.setText(
                TextUtils.isEmpty(roomName)
                        ? getString(R.string.room_a)
                        : roomName
        );

        initRoomRealtime();
    }


    // =====================================================
    private void initViews() {

        drawerLayout = findViewById(R.id.studyDrawerLayout);

        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        tvRoomInfo = findViewById(R.id.tvRoomInfo);

        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);

        tabChat = findViewById(R.id.tabChat);
        tabProblem = findViewById(R.id.tabProblem);

        recyclerChat = findViewById(R.id.recyclerChat);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);

        ivMyProfile = findViewById(R.id.userProfile);
        tvMyId = findViewById(R.id.userName);
        tvMyTitle = findViewById(R.id.userTitle);

        tvActiveTitle = findViewById(R.id.tvActiveTitle);
        tvInactiveTitle = findViewById(R.id.tvInactiveTitle);

        recyclerActive = findViewById(R.id.recyclerActive);
        recyclerInactive = findViewById(R.id.recyclerInactive);

        btnLeaveRoom = findViewById(R.id.btnLeaveRoom);
        btnDeleteRoom = findViewById(R.id.btnDeleteRoom);

        // 🔹 strings.xml 적용 (탭 / 힌트)
        tabChat.setText(getString(R.string.tab_chat));          // "💬 채팅"
        tabProblem.setText(getString(R.string.tab_quiz));       // "🧩 문제"
        editMessage.setHint(R.string.hint_enter_message);       // "메시지를 입력하세요"
    }

    // =====================================================
    private void setupRecycler() {
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));

        chatAdapter = new StudyChatAdapter();
        chatAdapter.setCurrentUserUid(userUid);
        recyclerChat.setAdapter(chatAdapter);

        recyclerActive.setLayoutManager(new LinearLayoutManager(this));
        recyclerInactive.setLayoutManager(new LinearLayoutManager(this));

        activeAdapter = new ParticipantAdapter(this, activeList, userUid);
        inactiveAdapter = new ParticipantAdapter(this, inactiveList, userUid);

        recyclerActive.setAdapter(activeAdapter);
        recyclerInactive.setAdapter(inactiveAdapter);
    }

    // =====================================================
    private void initRoomRealtime() {

        loadUserInfoAndRegister();

        DocumentReference roomRef = db.collection("studyRooms").document(roomId);

        roomListener = roomRef.addSnapshotListener((snapshot, e) -> {
            if (snapshot == null || !snapshot.exists()) return;

            ownerUid = safeString(snapshot.get("ownerUid"), "");
            isOwner = userUid.equals(ownerUid);

            activeAdapter.setOwnerUid(ownerUid);
            inactiveAdapter.setOwnerUid(ownerUid);

            Long cur = snapshot.getLong("currentPeople");
            Long max = snapshot.getLong("maxPeople");
            Long prob = snapshot.getLong("problemCount");

            tvRoomInfo.setText("👥 " + cur + "/" + max + " · 문제 " + prob);

            if (isOwner) {
                btnDeleteRoom.setVisibility(View.VISIBLE);
                btnLeaveRoom.setVisibility(View.GONE);
                btnDeleteRoom.setOnClickListener(v -> confirmDeleteRoom());
            } else {
                btnDeleteRoom.setVisibility(View.GONE);
                btnLeaveRoom.setVisibility(View.VISIBLE);
                btnLeaveRoom.setText("🚪 방 나가기");
                btnLeaveRoom.setOnClickListener(v -> leaveRoom());
            }
        });

        // ---------------- 참여자 ----------------
        participantListener = db.collection("studyRooms").document(roomId)
                .collection("participants")
                .addSnapshotListener((snapshots, e) -> {

                    if (snapshots == null) return;

                    activeList.clear();
                    inactiveList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Participant p = doc.toObject(Participant.class);
                        if (p == null) continue;

                        if (p.isActive()) activeList.add(p);
                        else inactiveList.add(p);
                    }

                    activeAdapter.updateParticipants(activeList);
                    inactiveAdapter.updateParticipants(inactiveList);

                    tvActiveTitle.setText("활동자 (" + activeList.size() + ")");
                    tvInactiveTitle.setText("비활동자 (" + inactiveList.size() + ")");
                });

        listenMessagesRealtime();
        loadMyDrawerProfile();

        // 문제 개수
        db.collection("studyRooms").document(roomId)
                .collection("problems")
                .addSnapshotListener((problems, ex) -> {
                    if (problems == null) return;

                    int count = problems.size();

                    String base = tvRoomInfo.getText().toString();
                    String peoplePart = base.contains("·") ? base.split("·")[0] : base;

                    tvRoomInfo.setText(peoplePart + " · 문제 " + count);
                });
    }

    // =====================================================
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

                    try {
                        int id = Integer.parseInt(resStr);
                        ivMyProfile.setImageResource(id);
                        return;
                    } catch (Exception ignored) {}

                    ivMyProfile.setImageResource(R.drawable.default_profile);
                });
    }

    // =====================================================
    private void loadUserInfoAndRegister() {

        if (currentUser == null) return;
        String uid = currentUser.getUid();

        db.collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {

                    if (doc == null || !doc.exists()) return;

                    userId = safeString(doc.get("userId"), "익명");
                    userTitle = safeString(doc.get("title"), "입문자");
                    profileUrl = safeString(doc.get("profileUrl"), "");

                    long score = doc.getLong("score") != null ? doc.getLong("score") : 0;

                    userProfileResId = R.drawable.default_profile;

                    if (!profileUrl.isEmpty() && !profileUrl.startsWith("http")) {
                        int id = getResources().getIdentifier(profileUrl, "drawable", getPackageName());
                        if (id != 0) userProfileResId = id;
                    }

                    registerParticipant(uid, score);
                });
    }

    private void registerParticipant(String uid, long score) {

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("name", userId);
        data.put("title", userTitle);
        data.put("profileUrl", profileUrl);
        data.put("profileResId", userProfileResId);
        data.put("active", true);
        data.put("joinedAt", new Date());
        data.put("score", score);

        db.collection("studyRooms").document(roomId)
                .collection("participants").document(uid)
                .set(data, SetOptions.merge());
    }

    // =====================================================
    private void listenMessagesRealtime() {

        messageListener = db.collection("studyRooms").document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {

                    if (snap == null) return;

                    // 🔹 아직 메시지가 없고, 웰컴 안 보냈으면 → 시스템 웰컴 메시지 한 번만 생성
                    if (snap.isEmpty() && !systemWelcomeSent) {
                        systemWelcomeSent = true;
                        sendSystemWelcomeMessage();
                        return; // 웰컴 메시지 추가 후, 다음 스냅샷에서 다시 리스트 그림
                    }

                    messageList.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        String text      = safeString(doc.get("text"), "");
                        String senderUid = safeString(doc.get("senderUid"), "");
                        boolean isMine   = senderUid.equals(userUid);
                        String time      = safeString(doc.get("time"), "");
                        String senderId  = safeString(doc.get("senderId"), "");
                        String title     = safeString(doc.get("title"), "입문자");
                        Boolean sysFlag  = doc.getBoolean("isSystem");
                        boolean isSystem = sysFlag != null && sysFlag;
                        Date ts          = doc.getDate("timestamp");

                        StudyChatMessage msg = new StudyChatMessage(
                                text,
                                isMine,
                                time,
                                senderId,
                                senderUid,
                                title,
                                isSystem,
                                ts
                        );

                        messageList.add(msg);
                    }

                    chatAdapter.submitList(new ArrayList<>(messageList));
                    if (!messageList.isEmpty()) {
                        recyclerChat.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    // 🔹 스터디룸 입장 시 채팅 리스트 맨 위에 한 번만 쌓이는 시스템 웰컴 메시지
    private void sendSystemWelcomeMessage() {
        if (TextUtils.isEmpty(roomId)) return;

        // welcome_message + 이용 안내 본문 붙이기
        String text = getString(R.string.welcome_message)
                + "\n\n"
                + getString(R.string.studyroom_notice_content);

        Map<String, Object> map = new HashMap<>();
        map.put("text", text);
        map.put("senderId", "SYSTEM");
        map.put("senderUid", "SYSTEM");
        map.put("title", "SYSTEM");
        map.put("time", new SimpleDateFormat("a hh:mm", Locale.KOREA).format(new Date()));
        map.put("timestamp", new Date());
        map.put("isSystem", true);

        db.collection("studyRooms").document(roomId)
                .collection("messages")
                .add(map);
    }

    // =====================================================
    private void setupButtons() {

        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> setInactiveAndExit());
        btnMenu.setOnClickListener(v -> toggleDrawer());

        tabProblem.setOnClickListener(v -> {
            Intent i = new Intent(this, StudyProblemActivity.class);
            i.putExtra("roomId", roomId);
            i.putExtra("roomName", roomName);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        tabChat.setOnClickListener(v ->
                Toast.makeText(this, "이미 채팅 탭입니다.", Toast.LENGTH_SHORT).show());
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END))
            drawerLayout.closeDrawer(GravityCompat.END);
        else drawerLayout.openDrawer(GravityCompat.END);
    }

    // =====================================================
    private void sendMessage() {

        String msg = editMessage.getText().toString().trim();
        if (msg.isEmpty() || currentUser == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("text", msg);
        map.put("senderId", userId);
        map.put("senderUid", userUid);
        map.put("title", userTitle);
        map.put("time", new SimpleDateFormat("a hh:mm", Locale.KOREA).format(new Date()));
        map.put("timestamp", new Date());
        map.put("isSystem", false);

        db.collection("studyRooms").document(roomId)
                .collection("messages")
                .add(map);

        editMessage.setText("");
    }

    // =====================================================
    private void leaveRoom() {

        if (currentUser == null) return;

        new AlertDialog.Builder(this)
                .setTitle("방 나가기")
                .setMessage("정말 나가시겠습니까?")
                .setPositiveButton("나가기", (d, w) -> {

                    db.collection("studyRooms").document(roomId)
                            .collection("participants").document(userUid)
                            .update("active", false)
                            .addOnSuccessListener(aVoid -> goBackToBoard())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "나가기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );

                })
                .setNegativeButton("취소", null)
                .show();
    }


    // =====================================================
    private void confirmDeleteRoom() {

        new AlertDialog.Builder(this)
                .setTitle("방 삭제")
                .setMessage("정말 삭제하시겠습니까?\n삭제하면 되돌릴 수 없습니다.")
                .setPositiveButton("삭제", (d, w) -> deleteRoomCompletely())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteRoomCompletely() {

        if (messageListener != null) messageListener.remove();
        if (roomListener != null) roomListener.remove();
        if (participantListener != null) participantListener.remove();

        DocumentReference roomRef = db.collection("studyRooms").document(roomId);

        roomRef.collection("participants").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap) d.getReference().delete();
                });

        roomRef.collection("messages").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap) d.getReference().delete();
                });

        roomRef.collection("problems").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap) d.getReference().delete();
                });

        roomRef.delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "방이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    goBackToBoard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // =====================================================
    private void goBackToBoard() {

        Intent intent = new Intent(this, StudyBoardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // =====================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ❌ isActive=false 자동 적용 금지 (중요)

        if (messageListener != null) messageListener.remove();
        if (roomListener != null) roomListener.remove();
        if (participantListener != null) participantListener.remove();
        if (userProfileListener != null) userProfileListener.remove();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        setInactiveAndExit();
    }

    private void setInactiveAndExit() {
        if (currentUser == null) {
            goBackToBoard();
            return;
        }

        db.collection("studyRooms").document(roomId)
                .collection("participants").document(userUid)
                .update("active", false)
                .addOnSuccessListener(aVoid -> goBackToBoard());
    }

    // =====================================================
    private static String safeString(Object v, String fallback) {
        if (v == null) return fallback;
        String s = v.toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}
