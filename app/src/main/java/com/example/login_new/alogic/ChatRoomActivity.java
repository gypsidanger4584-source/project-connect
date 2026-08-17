package com.example.login_new.alogic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 학식 랜덤채팅 - 실제 채팅방 화면
 * XML:
 *  - activity_chat_room.xml
 *  - chat_input.xml
 *  - item_message_me.xml / item_message_other.xml
 *  - item_message_system_chatroom.xml   ← ⭐ 추가
 */
public class ChatRoomActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "roomId";
    public static final String EXTRA_PARTNER_UID = "partnerUid";
    public static final String EXTRA_PARTNER_NAME = "partnerName";

    private static final String TAG = "ChatRoomActivity";

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageView btnSend;

    // 🔹 상단바 뷰
    private TextView tvRoomTitle;
    private ImageView btnMore;

    private final List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration msgListener;

    private String roomId;
    private String partnerUid;
    private String partnerName;

    // ⭐ 이메일 대신 표시할 내 userId 캐시
    private String myUserId = null;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("a hh:mm", Locale.KOREA);

    // ⏱ 같은 '분' 단위 비교용 포맷 (예: 202511141230)
    private final SimpleDateFormat minuteKeyFormat =
            new SimpleDateFormat("yyyyMMddHHmm", Locale.KOREA);

    // 🔔 상대 퇴장 후 10분 뒤 방 종료용 핸들러
    private final Handler closeHandler = new Handler(Looper.getMainLooper());
    private final Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            closeRoomAndExit();
        }
    };

    // 상대방 퇴장 안내를 이미 띄웠는지 여부 (중복 방지)
    private boolean partnerLeftNotified = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔥 키보드 올라오면 화면을 리사이즈해서 입력창이 가려지지 않게 함
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        setContentView(R.layout.activity_chat_room);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        partnerUid = getIntent().getStringExtra(EXTRA_PARTNER_UID);
        partnerName = getIntent().getStringExtra(EXTRA_PARTNER_NAME);

        Log.d(TAG, "onCreate: roomId=" + roomId + ", partnerUid=" + partnerUid);

        // ✅ roomId 없으면 바로 종료 (매칭 안 된 상태로 들어온 경우 방지)
        if (roomId == null || roomId.trim().isEmpty()) {
            Toast.makeText(this, "유효하지 않은 채팅방입니다. 매칭 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (partnerName == null || partnerName.isEmpty()) {
            partnerName = "학식 메이트 채팅";
        }

        // ⭐ 방 입장 시 현재 방 정보 저장 (뒤로가기 후 복귀용)
        saveActiveRoomInfo();

        // ⭐ 내 userId를 Firestore에서 한 번만 읽어오기
        if (auth.getCurrentUser() != null) {
            String myUid = auth.getCurrentUser().getUid();
            db.collection("users").document(myUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String id = doc.getString("userId"); // users 컬렉션의 userId 필드
                        if (id != null && !id.isEmpty()) {
                            myUserId = id;
                        }
                    });
        }

        initViews();
        setupRecycler();     // ← 여기서 시스템 안내 메세지 1개 넣음
        setupSendButton();
        setupToolbarMenu();
        startMessageListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (msgListener != null) {
            msgListener.remove();
            msgListener = null;
        }
        // 🔔 방 종료 타이머 제거
        closeHandler.removeCallbacks(closeRunnable);
    }

    // =====================================================
    //  🔹 방 상태 SharedPreferences 저장/삭제
    // =====================================================

    // 방에 들어왔을 때 현재 방 정보 저장
    private void saveActiveRoomInfo() {
        if (roomId == null) return;

        getSharedPreferences("random_chat", MODE_PRIVATE)
                .edit()
                .putString("activeRoomId", roomId)
                .putString("activePartnerUid", partnerUid)
                .putString("activePartnerName", partnerName)
                .putBoolean("inRoom", true)
                .apply();
    }

    // 방을 완전히 떠날 때 로컬 상태 삭제
    private void clearActiveRoomInfo() {
        getSharedPreferences("random_chat", MODE_PRIVATE)
                .edit()
                .remove("activeRoomId")
                .remove("activePartnerUid")
                .remove("activePartnerName")
                .remove("inRoom")
                .apply();
    }

    // =========================
    //  기존 UI / 채팅 로직
    // =========================

    private void initViews() {
        rvChat = findViewById(R.id.recycler_chat);

        // 🔹 상단바
        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        btnMore = findViewById(R.id.btnMore);

        tvRoomTitle.setText(partnerName);

        // chat_input.xml 안쪽의 View들
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
    }

    private void setupRecycler() {
        adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);

        // ✅ 위에서부터 내려오는 구조
        lm.setStackFromEnd(false);
        lm.setReverseLayout(false);

        rvChat.setLayoutManager(lm);
        rvChat.setAdapter(adapter);

        // ⭐ 방 입장 시 맨 위에 안내 시스템 메시지 1개 추가
        ChatMessage guide = new ChatMessage(
                "local_system_guide",
                "⚠️ 욕설·비방은 신고 및 제재 대상입니다.\n" +
                        "🚨 문제가 되는 대화는 언제든 신고할 수 있어요.\n" +
                        "😊 서로 즐거운 시간 보내요!\n"+
                        "퇴장하기를 누르시면 채팅이 중지됩니다.",
                ChatMessage.Sender.SYSTEM,
                "",
                null
        );
        messageList.add(guide);
        adapter.notifyItemInserted(messageList.size() - 1);
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;

            // 👉 욕설 필터 제거: 바로 전송
            sendMessage(text);
            etMessage.setText("");
        });
    }

    // 🔹 상단 점 세 개 메뉴: 퇴장 / 신고
    private void setupToolbarMenu() {
        btnMore.setOnClickListener(v -> {
            String[] items = new String[]{"퇴장하기", "신고하기"};

            new AlertDialog.Builder(this)
                    .setItems(items, (dialog, which) -> {
                        if (which == 0) {
                            // 퇴장하기
                            new AlertDialog.Builder(this)
                                    .setTitle("채팅방 퇴장")
                                    .setMessage("채팅방에서 퇴장하시겠습니까?")
                                    .setPositiveButton("퇴장", (d, w) -> {
                                        // ✅ 상대방에게 보일 시스템 메시지 남기기
                                        sendSystemLeaveMessage();

                                        // ✅ 이 방과 관련된 로컬 상태 제거
                                        clearActiveRoomInfo();

                                        // 👉 내 화면은 바로 종료
                                        finish();
                                    })
                                    .setNegativeButton("취소", null)
                                    .show();
                        } else if (which == 1) {
                            // 신고하기 – 현재는 토스트만, 나중에 Firestore에 reports 컬렉션 만들 수 있음
                            Toast.makeText(
                                    this,
                                    "신고가 접수되었습니다. (테스트용, 서버 연동 예정)",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })
                    .show();
        });
    }

    private void sendMessage(String text) {
        if (auth.getCurrentUser() == null || roomId == null) return;

        String myUid = auth.getCurrentUser().getUid();

        // ⭐ 이메일 대신 userId 사용, 없으면 이메일/“나”로 폴백
        String myName;
        if (myUserId != null && !myUserId.isEmpty()) {
            myName = myUserId;
        } else {
            myName = auth.getCurrentUser().getEmail();
            if (myName == null || myName.isEmpty()) {
                myName = "나";
            }
        }

        // Firestore messages 컬렉션 구조 예:
        // rooms/{roomId}/messages/{autoId}
        db.collection("rooms")
                .document(roomId)
                .collection("messages")
                .add(new MessageDoc(
                        text,
                        myUid,
                        myName,  // 👈 여기로 userId가 들어감
                        "user",
                        FieldValue.serverTimestamp()
                ).toMap());
    }

    // ✅ 퇴장 시스템 메시지 전용 (Firestore에만 남김 → 상대 기기에서 감지해서 중앙 팝업)
    private void sendSystemLeaveMessage() {
        if (auth.getCurrentUser() == null || roomId == null) return;

        String myUid = auth.getCurrentUser().getUid();
        String text = "상대방이 채팅방을 나갔습니다.";

        db.collection("rooms")
                .document(roomId)
                .collection("messages")
                .add(new MessageDoc(
                        text,
                        myUid,          // 보낸 사람 uid(시스템이라도 그냥 남겨둠)
                        "system",       // senderName (실제로는 안 씀)
                        "system",       // 👈 type=system
                        FieldValue.serverTimestamp()
                ).toMap());
    }

    // ✅ 상대방이 나갔다는 시스템 메시지를 기준으로 10분 뒤 방 종료/삭제 예약
    private void scheduleRoomClose(@Nullable Timestamp ts) {
        long baseTimeMs;
        if (ts != null) {
            baseTimeMs = ts.toDate().getTime();
        } else {
            baseTimeMs = System.currentTimeMillis();
        }

        long closeAt = baseTimeMs + 10 * 60 * 1000L; // 10분
        long delay = closeAt - System.currentTimeMillis();

        if (delay <= 0) {
            // 이미 10분이 지났으면 바로 종료
            closeRoomAndExit();
        } else {
            // 기존 예약 제거 후 다시 예약
            closeHandler.removeCallbacks(closeRunnable);
            closeHandler.postDelayed(closeRunnable, delay);
        }
    }

    // ✅ 실제로 방을 없애고 화면을 닫는 메서드
    private void closeRoomAndExit() {
        if (isFinishing() || isDestroyed()) return;

        if (roomId != null) {
            db.collection("rooms")
                    .document(roomId)
                    .delete();
        }

        // ✅ 방이 진짜 종료될 때 로컬 상태도 삭제
        clearActiveRoomInfo();

        Toast.makeText(
                this,
                "상대방이 퇴장하여 채팅방이 종료되었습니다.",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    // ✅ 중앙에 “상대방이 퇴장했습니다” 다이얼로그 띄우는 함수
    private void showPartnerLeftDialog(@Nullable Timestamp ts) {
        if (partnerLeftNotified) return; // 중복 방지
        partnerLeftNotified = true;

        new AlertDialog.Builder(this)
                .setTitle("알림")
                .setMessage("상대방이 채팅방을 나갔습니다.\n10분 후 채팅방이 자동으로 종료됩니다.")
                .setPositiveButton("확인", null)
                .show();

        // 10분 후 종료 예약
        scheduleRoomClose(ts);
    }

    private void startMessageListener() {
        if (roomId == null) return;

        msgListener = db.collection("rooms")
                .document(roomId)
                .collection("messages")
                // ✅ 오래된 순(위) → 최신(아래)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;
                        if (value == null) return;

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();
                            Map<String, Object> data = doc.getData();
                            if (data == null) continue;

                            String text = (String) data.get("text");
                            String senderUid = (String) data.get("senderUid");
                            String senderName = (String) data.get("senderName");
                            String type = (String) data.get("type");
                            Timestamp ts = doc.getTimestamp("createdAt");

                            if (senderName == null) senderName = "";
                            if (type == null) type = "user";

                            // 🔹 시스템 메시지(type=system)는 리스트에 넣지 않고 중앙 팝업만 띄움
                            if ("system".equals(type)) {
                                if (text != null && text.contains("채팅방을 나갔습니다")) {
                                    // 상대방 기기에서만 뜨게 되는 효과
                                    showPartnerLeftDialog(ts);
                                }
                                // 시스템 메시지는 리스트에 추가하지 않음
                                continue;
                            }

                            ChatMessage.Sender sender;
                            if (senderUid != null
                                    && auth.getCurrentUser() != null
                                    && senderUid.equals(auth.getCurrentUser().getUid())) {
                                sender = ChatMessage.Sender.ME;
                            } else {
                                sender = ChatMessage.Sender.OTHER;
                            }

                            ChatMessage msg = new ChatMessage(
                                    doc.getId(),
                                    text != null ? text : "",
                                    sender,
                                    senderName,
                                    ts
                            );

                            switch (dc.getType()) {
                                case ADDED:
                                    messageList.add(msg);
                                    adapter.notifyItemInserted(messageList.size() - 1);
                                    // 최신 메시지로 스크롤
                                    rvChat.scrollToPosition(messageList.size() - 1);
                                    break;
                                case MODIFIED:
                                case REMOVED:
                                    // 단순 채팅이라 수정/삭제는 생략
                                    break;
                            }
                        }
                    }
                });
    }

    // =========================
    // 내부 모델 클래스
    // =========================
    private static class ChatMessage {
        // ⭐ SYSTEM 타입 추가
        enum Sender { ME, OTHER, SYSTEM }

        final String id;
        final String text;
        final Sender sender;
        final String senderName;
        final Timestamp timestamp;

        ChatMessage(String id, String text, Sender sender, String senderName, Timestamp timestamp) {
            this.id = id;
            this.text = text;
            this.sender = sender;
            this.senderName = senderName;
            this.timestamp = timestamp;
        }
    }

    private static class MessageDoc {
        final String text;
        final String senderUid;
        final String senderName;
        final String type;
        final Object createdAt; // FieldValue.serverTimestamp()

        MessageDoc(String text, String senderUid, String senderName, String type, Object createdAt) {
            this.text = text;
            this.senderUid = senderUid;
            this.senderName = senderName;
            this.type = type;
            this.createdAt = createdAt;
        }

        Map<String, Object> toMap() {
            return new java.util.HashMap<String, Object>() {{
                put("text", text);
                put("senderUid", senderUid);
                put("senderName", senderName);
                put("type", type);
                put("createdAt", createdAt);
            }};
        }
    }

    // =========================
    // RecyclerView Adapter
    // =========================
    private class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_SYSTEM = 0;
        private static final int VIEW_ME = 1;
        private static final int VIEW_OTHER = 2;

        @Override
        public int getItemViewType(int position) {
            ChatMessage m = messageList.get(position);
            if (m.sender == ChatMessage.Sender.SYSTEM) {
                return VIEW_SYSTEM;
            }
            switch (m.sender) {
                case ME: return VIEW_ME;
                case OTHER:
                default: return VIEW_OTHER;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v;
            if (viewType == VIEW_SYSTEM) {
                v = getLayoutInflater().inflate(R.layout.item_message_system_chatroom, parent, false);
                return new SystemHolder(v);
            } else if (viewType == VIEW_ME) {
                v = getLayoutInflater().inflate(R.layout.item_message_me, parent, false);
                return new MeHolder(v);
            } else {
                v = getLayoutInflater().inflate(R.layout.item_message_other, parent, false);
                return new OtherHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage m = messageList.get(position);

            // ⏱ 표시용 시간 문자열
            String timeStr = "";
            if (m.timestamp != null) {
                timeStr = timeFormat.format(m.timestamp.toDate());
            }

            if (holder instanceof SystemHolder) {
                ((SystemHolder) holder).bind(m);
                return;
            }

            // 🔍 직전 메시지와 비교해서 "같은 사람 + 같은 분"인지 체크
            ChatMessage prev = (position > 0) ? messageList.get(position - 1) : null;
            boolean sameSender = false;
            boolean sameMinute = false;

            if (prev != null) {
                sameSender = (m.sender == prev.sender);

                if (m.timestamp != null && prev.timestamp != null) {
                    String curKey = minuteKeyFormat.format(m.timestamp.toDate());
                    String prevKey = minuteKeyFormat.format(prev.timestamp.toDate());
                    sameMinute = curKey.equals(prevKey);
                }
            }

            // 👉 같은 사람 + 같은 분 → 이름/시간 숨김 (상대 only)
            boolean hideHeader = sameSender && sameMinute;

            if (holder instanceof MeHolder) {
                ((MeHolder) holder).bind(m, timeStr, hideHeader);
            } else if (holder instanceof OtherHolder) {
                ((OtherHolder) holder).bind(m, timeStr, hideHeader);
            }
        }

        @Override
        public int getItemCount() {
            return messageList.size();
        }

        // === ViewHolders ===

        // ⭐ 시스템 안내 말풍선
        class SystemHolder extends RecyclerView.ViewHolder {
            final TextView tvSystem;

            SystemHolder(@NonNull View itemView) {
                super(itemView);
                tvSystem = itemView.findViewById(R.id.tvSystem);
            }

            void bind(ChatMessage m) {
                tvSystem.setText(m.text);
            }
        }

        // 나(me) 말풍선: item_message_me.xml
        class MeHolder extends RecyclerView.ViewHolder {
            final TextView tvMessage;
            final TextView tvTime;

            MeHolder(@NonNull View itemView) {
                super(itemView);
                // xml: @id/tvText, @id/tvTimeLeft
                tvMessage = itemView.findViewById(R.id.tvText);
                tvTime = itemView.findViewById(R.id.tvTimeLeft);
            }

            // ✅ 내 메시지는 항상 시간 보이게
            void bind(ChatMessage m, String time, boolean hideHeader) {
                tvMessage.setText(m.text);

                if (TextUtils.isEmpty(time)) {
                    tvTime.setVisibility(View.GONE);
                } else {
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText(time);
                }
            }
        }

        // 상대(other) 말풍선: item_message_other.xml
        class OtherHolder extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvMessage;
            final TextView tvTime;

            OtherHolder(@NonNull View itemView) {
                super(itemView);
                // xml: @id/tvSenderName, @id/tvText, @id/tvTimeRight
                tvName = itemView.findViewById(R.id.tvSenderName);
                tvMessage = itemView.findViewById(R.id.tvText);
                tvTime = itemView.findViewById(R.id.tvTimeRight);
            }

            void bind(ChatMessage m, String time, boolean hideHeader) {
                tvMessage.setText(m.text);

                if (hideHeader) {
                    // 같은 사람 + 같은 분이면 이름/시간 숨김
                    tvName.setVisibility(View.GONE);
                    tvTime.setVisibility(View.GONE);
                } else {
                    tvName.setVisibility(View.VISIBLE);
                    tvName.setText(m.senderName);

                    if (TextUtils.isEmpty(time)) {
                        tvTime.setVisibility(View.GONE);
                    } else {
                        tvTime.setVisibility(View.VISIBLE);
                        tvTime.setText(time);
                    }
                }
            }
        }
    }
}
