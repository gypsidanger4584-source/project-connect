package com.example.login_new.alogic;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;

public class RandomMatchActivity extends AppCompatActivity
        implements RandomMatchManager.MatchCallback {

    private RandomMatchManager manager;

    private TextView tvTitle;
    private TextView tvTimer;
    private ProgressBar progress;
    private LinearLayout btnCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching);

        // 로그인 안 되어 있으면 바로 종료
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tvTitle);
        tvTimer = findViewById(R.id.tvTimer);
        progress = findViewById(R.id.progress);
        btnCancel = findViewById(R.id.btnCancel);

        // 🔹 1분 기준 프로그레스바 설정 (0~60초)
        progress.setIndeterminate(false);
        progress.setMax(60);
        progress.setProgress(0);

        // ✅ 1순위: 기존에 이어야 할 방이 있는지 확인
        if (!tryResumeActiveRoom()) {
            // 👉 없으면 새 매칭 시작
            manager = new RandomMatchManager(this);
            manager.startSearching();
        }

        // ❌ 매칭 취소 → user_cancel 이유로 stop 호출
        btnCancel.setOnClickListener(v -> {
            if (manager != null && manager.isSearching()) {
                manager.stopSearching("user_cancel");
            } else {
                // 이미 방으로 바로 들어간 경우 등: 그냥 뒤로가기 느낌으로 종료
                finish();
            }
        });
    }

    /**
     * SharedPreferences에 "아직 안 끝난 방" 정보가 있으면
     * 새 매칭 대신 그 방으로 바로 입장한다.
     *
     * @return true면 기존 방으로 이동(새 매칭 X), false면 새 매칭 시작 필요
     */
    private boolean tryResumeActiveRoom() {
        var prefs = getSharedPreferences("random_chat", MODE_PRIVATE);
        boolean inRoom = prefs.getBoolean("inRoom", false);
        String activeRoomId = prefs.getString("activeRoomId", null);
        String partnerUid = prefs.getString("activePartnerUid", null);
        String partnerName = prefs.getString("activePartnerName", "학식 메이트 채팅");

        if (inRoom && activeRoomId != null && !activeRoomId.trim().isEmpty()) {
            // 🔁 이전에 입장했던 방으로 복귀
            Intent intent = new Intent(this, ChatRoomActivity.class);
            intent.putExtra(ChatRoomActivity.EXTRA_ROOM_ID, activeRoomId);
            intent.putExtra(ChatRoomActivity.EXTRA_PARTNER_UID, partnerUid);
            intent.putExtra(ChatRoomActivity.EXTRA_PARTNER_NAME, partnerName);
            startActivity(intent);
            finish();    // 매칭 화면은 닫는다
            return true;
        }

        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (manager != null && manager.isSearching()) {
            manager.stopSearching("lifecycle_stop");
        }
    }

    @Override
    public void onSearchingTick(long ms) {
        runOnUiThread(() -> {
            long sec = ms / 1000;   // 남은 초

            // ⏱ 타이머 텍스트 (MM:SS)
            tvTimer.setText(String.format("%02d:%02d", sec / 60, sec % 60));

            // 🔹 1분(60초) 기준으로 경과 시간 계산
            int elapsed = (int) (60 - sec);  // 지난 시간(초)
            if (elapsed < 0) elapsed = 0;
            if (elapsed > 60) elapsed = 60;
            progress.setProgress(elapsed);
        });
    }

    @Override
    public void onMatched(String roomId, String partnerUid) {
        runOnUiThread(() -> {
            // 🛡 안전장치: 이상한 값이면 채팅방 진입 막기
            if (roomId == null || roomId.trim().isEmpty()) {
                Toast.makeText(this, "매칭 정보가 올바르지 않습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (partnerUid == null || partnerUid.trim().isEmpty()) {
                Toast.makeText(this, "상대 정보를 불러오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "매칭 완료!", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, ChatRoomActivity.class);
            i.putExtra(ChatRoomActivity.EXTRA_ROOM_ID, roomId);
            i.putExtra(ChatRoomActivity.EXTRA_PARTNER_UID, partnerUid);
            // partnerName은 ChatRoomActivity에서 기본값 처리
            startActivity(i);
            finish();
        });
    }

    @Override
    public void onSearchRestart() {
        runOnUiThread(() -> tvTitle.setText("재검색 중..."));
    }

    // ⏰ 1분 타임아웃 시 호출 → 실패 화면으로 이동
    @Override
    public void onTimeout() {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, MatchFailActivity.class);
            intent.putExtra("reason", "timeout"); // 🔹 타임아웃 표시용
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onSearchStopped(@Nullable String reason) {
        runOnUiThread(() -> {

            String r = (reason == null) ? "" : reason.trim();

            // ✅ 1) 사용자가 직접 취소한 경우 → 매칭 취소 화면으로 이동
            if ("user_cancel".equals(r)) {
                Intent intent = new Intent(this, MatchFailActivity.class);
                intent.putExtra("reason", "user_cancel");   // MatchFailActivity에서 "매칭 취소"로 표시
                startActivity(intent);
                finish();
                return;
            }

            // ✅ 2) 생명주기 종료 같은 내부 이유면 그냥 조용히 종료
            if ("lifecycle_stop".equals(r)) {
                finish();
                return;
            }

            // ✅ 3) 그 밖의 이유 (예외적인 중단) → 내부 코드 안 보이게 처리
            tvTitle.setText("매칭이 종료되었습니다.");
            progress.setIndeterminate(false);
        });
    }

    @Override
    public void onError(String message, @Nullable Exception e) {
        String extra = (e != null && e.getMessage() != null) ? ("\n" + e.getMessage()) : "";
        Toast.makeText(this, "오류: " + message + extra, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatus(String statusMsg) {
        runOnUiThread(() -> tvTitle.setText(statusMsg));
    }
}
