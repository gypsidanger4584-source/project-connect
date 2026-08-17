package com.example.login_new.alogic;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;

public class ParticipantManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listener;
    private final CollectionReference participantRef;

    public interface ParticipantListener {
        void onParticipantsUpdated(List<Participant> participants);
    }

    public ParticipantManager(String roomId) {
        participantRef = db.collection("studyRooms")
                .document(roomId)
                .collection("participants");
    }

    // ======================================================================
    public void startListening(ParticipantListener callback) {
        stopListening();

        listener = participantRef.addSnapshotListener((snapshots, e) -> {

            if (snapshots == null || e != null) return;

            List<Participant> list = new ArrayList<>();

            for (QueryDocumentSnapshot doc : snapshots) {
                try {
                    Participant p = new Participant();

                    p.setUid(safeString(doc.get("uid")));
                    p.setName(safeString(doc.get("name"), "익명"));
                    p.setTitle(safeString(doc.get("title"), "입문자"));
                    p.setActive(safeBool(doc.get("isActive")));

                    // 🔥 score 가져오기
                    p.setScore(coerceInt(doc.get("score"), 0));

                    // 🔥 프로필
                    p.setProfileUrl(safeString(doc.get("profileUrl"), ""));
                    p.setProfileResId(coerceInt(doc.get("profileResId"), 0));

                    // 🔥 Timestamp 유지
                    Object ts = doc.get("joinedAt");
                    if (ts instanceof Timestamp)
                        p.setJoinedAt((Timestamp) ts);

                    list.add(p);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // 정렬: active → inactive
            list.sort((a, b) -> Boolean.compare(!a.isActive(), !b.isActive()));

            if (callback != null)
                callback.onParticipantsUpdated(list);
        });
    }

    // ======================================================================
    public void stopListening() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    public void updateActiveState(String uid, boolean active) {
        participantRef.document(uid).update("isActive", active);
    }

    public void removeParticipant(String uid) {
        participantRef.document(uid).delete();
    }

    // ======================================================================
    private static String safeString(Object v) {
        return safeString(v, "");
    }

    private static String safeString(Object v, String fb) {
        if (v == null) return fb;
        String s = v.toString().trim();
        return s.isEmpty() ? fb : s;
    }

    private static boolean safeBool(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return false;
    }

    private static int coerceInt(Object v, int fb) {
        try {
            if (v instanceof Integer) return (Integer) v;
            if (v instanceof Long) return ((Long) v).intValue();
            if (v instanceof Double) return ((Double) v).intValue();
            if (v instanceof String) return Integer.parseInt((String) v);
        } catch (Exception ignored) {}
        return fb;
    }
}
