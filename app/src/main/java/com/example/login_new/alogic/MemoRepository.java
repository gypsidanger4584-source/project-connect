package com.example.login_new.alogic;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration registration;

    // 🔁 로그인 없이 공용 컬렉션 사용 (개발용)
    private CollectionReference memosRef() {
        return db.collection("memos"); // 필요하면 "memos_dev" 등으로 바꿔도 됨
    }

    public interface MemoListener {
        void onChanged(List<Memo> memos);
        void onError(Exception e);
    }

    public void startListen(MemoListener listener) {
        stopListen();
        registration = memosRef()
                .orderBy("createdAtMs", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { listener.onError(e); return; }
                    List<Memo> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            list.add(toMemo(doc));
                        }
                    }
                    listener.onChanged(list);
                });
    }

    public void stopListen() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public Task<DocumentReference> addMemo(String text, @Nullable Long expireAtMs) {
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("createdAtMs", System.currentTimeMillis());
        if (expireAtMs != null) data.put("expireAtMs", expireAtMs);
        return memosRef().add(data);
    }

    public Task<Void> updateExpire(String memoId, @Nullable Long expireAtMs) {
        Map<String, Object> up = new HashMap<>();
        if (expireAtMs == null) {
            up.put("expireAtMs", FieldValue.delete());
        } else {
            up.put("expireAtMs", expireAtMs);
        }
        return memosRef().document(memoId).update(up);
    }

    public Task<Void> deleteMemo(String memoId) {
        return memosRef().document(memoId).delete();
    }

    private Memo toMemo(DocumentSnapshot doc) {
        Memo m = new Memo();
        m.setId(doc.getId());
        m.setText(doc.getString("text") != null ? doc.getString("text") : "");
        Long created = doc.getLong("createdAtMs");
        m.setCreatedAtMs(created != null ? created : 0L);
        Long expire = doc.getLong("expireAtMs");
        m.setExpireAtMs(expire);
        return m;
    }
}
