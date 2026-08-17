package com.example.login_new.alogic;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** ✅ RealtimeDatabase + Firestore 통합 헬퍼 클래스 */
public class FirebaseDataHelper {

    // ──────────────────────────────
    // 🔹 Realtime Database 영역
    // ──────────────────────────────
    private final DatabaseReference realtimeDb;
    private final FirebaseFirestore firestoreDb;

    public FirebaseDataHelper() {
        realtimeDb = FirebaseDatabase.getInstance().getReference();
        firestoreDb = FirebaseFirestore.getInstance();
    }

    // ──────────────────────────────
    // 🔸 Realtime DB 인터페이스 정의
    // ──────────────────────────────
    public interface DepartmentCallback {
        void onDataLoaded(Map<String, Department> departments);
        void onError(String error);
    }

    public interface BuildingCallback {
        void onDataLoaded(List<String> buildingNames);
        void onError(String error);
    }

    public interface SpaceCallback {
        void onDataLoaded(Map<String, List<String>> spacesByBuilding);
        void onError(String error);
    }

    public static class Department {
        public String department_name;
        public String building_name;
        public String room_number;

        public Department() {} // Firebase용 빈 생성자
        public Department(String name, String building, String room) {
            this.department_name = name;
            this.building_name = building;
            this.room_number = room;
        }
    }

    /** 🔹 학과 데이터 가져오기 */
    public void getDepartments(final DepartmentCallback callback) {
        realtimeDb.child("departments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Department> result = new HashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Department dept = child.getValue(Department.class);
                    result.put(child.getKey(), dept);
                }
                callback.onDataLoaded(result);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /** 🔹 건물 목록 */
    public void getBuildings(final BuildingCallback callback) {
        realtimeDb.child("buildings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> buildingNames = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    buildingNames.add(child.getKey());
                }
                callback.onDataLoaded(buildingNames);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /** 🔹 건물별 공간 */
    public void getSpaces(final SpaceCallback callback) {
        realtimeDb.child("spaces").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, List<String>> map = new HashMap<>();
                for (DataSnapshot buildingSnap : snapshot.getChildren()) {
                    List<String> spaceList = new ArrayList<>();
                    for (DataSnapshot spaceSnap : buildingSnap.getChildren()) {
                        spaceList.add(spaceSnap.getKey());
                    }
                    map.put(buildingSnap.getKey(), spaceList);
                }
                callback.onDataLoaded(map);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // ──────────────────────────────
    // 🔹 Firestore 영역 추가
    // ──────────────────────────────

    /** ✅ Firestore: 스터디룸 생성 */
    public Task<DocumentReference> addRoom(Map<String, Object> room) {
        room.put("createdAt", new Date());
        room.put("currentPeople", 0);
        room.put("problemCount", 0);
        return firestoreDb.collection("studyRooms").add(room);
    }

    /** ✅ Firestore: 시스템 메시지 */
    public Task<Void> addSystemMessage(String roomId, String text) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", "system");
        msg.put("title", "시스템");
        msg.put("time", new SimpleDateFormat("a hh:mm", Locale.KOREA).format(new Date()));
        msg.put("timestamp", new Date());
        msg.put("isSystem", true);

        return firestoreDb.collection("studyRooms")
                .document(roomId)
                .collection("messages")
                .add(msg)
                .continueWith(task -> null);
    }

    /** ✅ Firestore: 문제 추가 */
    public Task<DocumentReference> addProblem(String roomId, Map<String, Object> problemData) {
        problemData.put("createdAt", new Date());
        DocumentReference roomRef = firestoreDb.collection("studyRooms").document(roomId);
        DocumentReference newProb = roomRef.collection("problems").document();

        WriteBatch batch = firestoreDb.batch();
        batch.set(newProb, problemData);
        batch.update(roomRef, "problemCount", FieldValue.increment(1));
        return batch.commit().continueWith(task -> newProb);
    }

    /** ✅ Firestore: 문제 채점 및 점수 반영 */
    public void submitAnswer(String roomId, String problemId, String userUid, String userName,
                             String answer, int awardScore, SimpleCallback cb) {

        DocumentReference solveRef = firestoreDb.collection("studyRooms").document(roomId)
                .collection("problems").document(problemId)
                .collection("solves").document(userUid);

        DocumentReference participantRef = firestoreDb.collection("studyRooms").document(roomId)
                .collection("participants").document(userUid);

        DocumentReference probRef = firestoreDb.collection("studyRooms").document(roomId)
                .collection("problems").document(problemId);

        firestoreDb.runTransaction(transaction -> {
            DocumentSnapshot prob = transaction.get(probRef);
            if (!prob.exists())
                throw new FirebaseFirestoreException("Problem not found",
                        FirebaseFirestoreException.Code.NOT_FOUND);

            Object correctObj = prob.get("answer");
            String correct = (correctObj != null) ? correctObj.toString() : "";
            boolean isCorrect = !TextUtils.isEmpty(correct)
                    && correct.trim().equalsIgnoreCase(answer.trim());

            int problemScore = toInt(prob.get("score"), 0);
            int give = isCorrect ? Math.max(awardScore, problemScore) : 0;

            // 풀이 기록
            Map<String, Object> solve = new HashMap<>();
            solve.put("userId", userName);
            solve.put("answer", answer);
            solve.put("correct", isCorrect);
            solve.put("awarded", give);
            solve.put("solvedAt", new Date());
            transaction.set(solveRef, solve);

            // 점수 반영
            if (give > 0) {
                DocumentSnapshot pdoc = transaction.get(participantRef);
                int prev = toInt(pdoc != null ? pdoc.get("score") : 0, 0);
                int now = prev + give;

                Map<String, Object> update = new HashMap<>();
                update.put("score", now);

                String newTitle = "입문자";
                try {
                    newTitle = TitleManager.titleForScore(now);
                } catch (Exception ignored) {}

                update.put("title", newTitle);
                transaction.set(participantRef, update, SetOptions.merge());
            }

            return isCorrect;
        }).addOnSuccessListener(ok -> {
            if (cb != null)
                cb.done(true, Boolean.TRUE.equals(ok) ? "정답!" : "오답 😢");
        }).addOnFailureListener(e -> {
            if (cb != null)
                cb.done(false, "제출 실패: " + e.getMessage());
        });
    }

    /** 🔹 안전한 숫자 변환 */
    private static int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    // ──────────────────────────────
    // ✅ 내부 콜백 인터페이스 추가 (오류 해결)
    // ──────────────────────────────
    public interface SimpleCallback {
        void done(boolean success, String message);
    }
}
