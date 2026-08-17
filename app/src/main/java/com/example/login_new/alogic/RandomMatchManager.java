package com.example.login_new.alogic;

import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

/**
 * 🔹 학식 랜덤 매칭 매니저
 * - 1분 타이머 안에 상대를 찾으면 onMatched() 콜백
 * - 1분 동안 매칭 실패 시 onTimeout() 콜백
 * - Firestore 컬렉션 구조 (예시)
 *   - waiting/{uid} : 매칭 대기열
 *     - status: "waiting" | "matched" | "cancelled"
 *     - createdAt, updatedAt
 *     - roomId, partnerUid (매칭완료 시)
 *   - rooms/{roomId}
 *     - userA, userB, members[], createdAt, updatedAt
 */
public class RandomMatchManager {

    // ============================================================
    //  콜백 인터페이스
    // ============================================================
    public interface MatchCallback {
        /** 남은 시간 (ms 단위) */
        void onSearchingTick(long millisUntilFinished);

        /** 매칭 성공: roomId + 상대 UID */
        void onMatched(String roomId, String partnerUid);

        /** 자동 재검색 시작 (지금 버전에서는 사용 안 할 수도 있음) */
        void onSearchRestart();

        /** 매칭 중단(취소/에러/타임아웃 등) */
        void onSearchStopped(@Nullable String reason);

        /** 에러 발생 시 */
        void onError(String message, @Nullable Exception e);

        /** 상태 텍스트 변경 (UI 상단 제목 바꾸는 용도) */
        void onStatus(String statusMsg);

        /** ⏰ 타임아웃 시 호출 → Activity에서 실패 화면으로 이동하면 됨 */
        void onTimeout();
    }

    private static final String TAG = "RandomMatchManager";

    // 1분 (ms)
    private static final long SEARCH_TIMEOUT_MS = 1 * 60 * 1000L;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MatchCallback callback;

    private boolean searching = false;

    private CountDownTimer timer;
    private ListenerRegistration waitingListener;
    private DocumentReference myWaitingRef;

    public RandomMatchManager(MatchCallback callback) {
        this.callback = callback;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // ============================================================
    //  외부에서 호출하는 API
    // ============================================================

    /** 매칭 시작 */
    public void startSearching() {
        if (searching) {
            Log.d(TAG, "startSearching: already searching");
            return;
        }

        if (auth.getCurrentUser() == null) {
            callback.onError("로그인이 필요합니다.", null);
            return;
        }

        searching = true;
        callback.onStatus("매칭 중입니다...");
        Log.d(TAG, "startSearching: 시작");

        startTimer();
        enterQueueAndTryMatch();
    }

    /** 매칭 정지 (취소, 타임아웃 등) */
    public void stopSearching(@Nullable String reason) {
        if (!searching) return;
        searching = false;

        Log.d(TAG, "stopSearching: reason=" + reason);

        // 타이머 해제
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // 리스너 해제
        if (waitingListener != null) {
            waitingListener.remove();
            waitingListener = null;
        }

        // 내 waiting 문서 상태 변경
        if (myWaitingRef != null) {
            myWaitingRef.update("status", "cancelled",
                            "updatedAt", FieldValue.serverTimestamp())
                    .addOnCompleteListener(t -> {
                        // no-op
                    });
            myWaitingRef = null;
        }

        callback.onSearchStopped(reason);
    }

    public boolean isSearching() {
        return searching;
    }

    // ============================================================
    //  타이머
    // ============================================================
    private void startTimer() {
        timer = new CountDownTimer(SEARCH_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long ms) {
                callback.onSearchingTick(ms);
            }

            @Override
            public void onFinish() {
                if (!searching) return;

                Log.d(TAG, "startTimer.onFinish: timeout");
                // 🔻 타임아웃: 매칭 중단 + 상위(Activity)에 타임아웃 알림
                stopSearching("timeout");
                callback.onTimeout();
            }
        }.start();
    }

    // ============================================================
    //  대기열 진입 + 매칭 시도
    // ============================================================
    private void enterQueueAndTryMatch() {
        final String uid = auth.getCurrentUser().getUid();
        callback.onStatus("상대를 찾고 있습니다...");

        // status == "waiting" 인 아무나 한 명
        Query query = db.collection("waiting")
                .whereEqualTo("status", "waiting")
                .limit(1);

        query.get().addOnSuccessListener((QuerySnapshot qs) -> {
            if (!searching) return;

            if (!qs.isEmpty()) {
                // 누군가가 먼저 기다리고 있음 → 방 생성해서 매칭
                DocumentSnapshot otherDoc = qs.getDocuments().get(0);
                String partnerUid = otherDoc.getString("uid");
                if (partnerUid == null) {
                    callback.onError("상대 UID를 찾을 수 없습니다.", null);
                    return;
                }
                Log.d(TAG, "enterQueueAndTryMatch: 상대 발견 partnerUid=" + partnerUid);
                createRoomAndMatch(otherDoc.getReference(), uid, partnerUid);
            } else {
                // 아무도 없으면 내가 대기열에 들어감
                Log.d(TAG, "enterQueueAndTryMatch: 기존 대기 없음, 내가 waiting에 등록");
                createMyWaitingDoc(uid);
            }
        }).addOnFailureListener(ex -> {
            Log.e(TAG, "enterQueueAndTryMatch: waiting 쿼리 실패", ex);
            callback.onError("대기열 조회 실패", ex);
        });
    }

    /** 내가 먼저 waiting 컬렉션에 들어가서, 누군가가 잡아주길 기다리는 경우 */
    private void createMyWaitingDoc(String uid) {
        myWaitingRef = db.collection("waiting").document(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("status", "waiting");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        myWaitingRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "createMyWaitingDoc: waiting 등록 완료 uid=" + uid);
                    listenMyWaitingMatched();
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "createMyWaitingDoc: 실패", ex);
                    callback.onError("대기열 등록 실패", ex);
                });
    }

    /**
     * 내가 waiting에 들어간 상태에서,
     * 누군가가 나를 매칭해서 roomId/partnerUid 필드를 채워주면
     * 그걸 감지하는 리스너
     */
    private void listenMyWaitingMatched() {
        if (myWaitingRef == null) return;

        waitingListener = myWaitingRef.addSnapshotListener((snapshot, e) -> {
            if (!searching) return;
            if (e != null) {
                Log.e(TAG, "listenMyWaitingMatched: 리스너 에러", e);
                callback.onError("대기 상태 감시 중 오류 발생", e);
                return;
            }
            if (snapshot == null || !snapshot.exists()) return;

            String status = snapshot.getString("status");
            if ("matched".equals(status)) {
                String roomId = snapshot.getString("roomId");
                String partnerUid = snapshot.getString("partnerUid");
                Log.d(TAG, "listenMyWaitingMatched: matched! roomId=" + roomId +
                        ", partnerUid=" + partnerUid);

                if (roomId != null && partnerUid != null && searching) {
                    // 이 시점에서 타이머/리스너 정리는 이쪽에서 처리
                    searching = false;
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    if (waitingListener != null) {
                        waitingListener.remove();
                        waitingListener = null;
                    }
                    myWaitingRef = null;

                    callback.onMatched(roomId, partnerUid);
                }
            }
        });
    }

    /**
     * 내가 상대 waitingDoc을 잡아서 방을 만드는 경우
     */
    private void createRoomAndMatch(DocumentReference otherWaitingRef,
                                    String myUid,
                                    String partnerUid) {
        String roomId = UUID.randomUUID().toString();
        DocumentReference roomRef = db.collection("rooms").document(roomId);

        db.runTransaction((Transaction transaction) -> {
            // 혹시 동시에 다른 애가 잡았는지 상태 체크
            DocumentSnapshot otherSnap = transaction.get(otherWaitingRef);
            String status = otherSnap.getString("status");
            if (status == null || !"waiting".equals(status)) {
                // 이미 매칭되었거나 취소된 상태 → 트랜잭션 실패
                throw new IllegalStateException("상대가 더 이상 waiting 상태가 아님");
            }

            // 방 생성
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("userA", myUid);
            roomData.put("userB", partnerUid);

            // 🔥 규칙에서 요구하는 members 배열 추가
            roomData.put("members", Arrays.asList(myUid, partnerUid));

            roomData.put("createdAt", FieldValue.serverTimestamp());
            roomData.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(roomRef, roomData);

            // 상대 waiting 문서 업데이트
            Map<String, Object> otherUpdate = new HashMap<>();
            otherUpdate.put("status", "matched");
            otherUpdate.put("roomId", roomId);
            otherUpdate.put("partnerUid", myUid);
            otherUpdate.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(otherWaitingRef, otherUpdate);

            return null;
        }).addOnSuccessListener(aVoid -> {
            if (!searching) return;
            Log.d(TAG, "createRoomAndMatch: 방 생성 및 매칭 성공 roomId=" + roomId);
            searching = false;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            callback.onMatched(roomId, partnerUid);
        }).addOnFailureListener(ex -> {
            Log.e(TAG, "createRoomAndMatch: 트랜잭션 실패", ex);
            callback.onError("방 생성 실패", ex);
        });
    }
}
