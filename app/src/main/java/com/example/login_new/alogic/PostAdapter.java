package com.example.login_new.alogic;

import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 🧩 PostAdapter (헤더 제거 버전)
 * - 게시글 리스트만 표시
 * - Firestore 좋아요/댓글 실시간 반영
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapterClean";

    private final MainActivity mainActivity;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final String currentUserId;

    private List<DocumentSnapshot> postList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot snapshot);
    }

    public PostAdapter(MainActivity activity, List<DocumentSnapshot> postList) {
        this.mainActivity = activity;
        this.postList = postList;
        FirebaseUser user = auth.getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mainActivity).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        DocumentSnapshot snapshot = postList.get(position);
        bindPost(holder, snapshot);
    }

    @Override
    public int getItemCount() {
        return (postList != null ? postList.size() : 0);
    }

    // ===============================
    // 🧠 게시글 데이터 바인딩
    // ===============================
    private void bindPost(@NonNull PostViewHolder holder, DocumentSnapshot snapshot) {
        String postId = snapshot.getId();

        String title = snapshot.getString("title");
        String content = snapshot.getString("content");
        String authorUid = snapshot.getString("author");
        String authorName = mainActivity.getUserName(authorUid);
        String border = snapshot.getString("border");
        Timestamp timestamp = snapshot.getTimestamp("timestamp");

        holder.postTitle.setText(title != null ? title : "제목 없음");
        holder.postContent.setText(content != null && content.length() > 15 ? content.substring(0, 15) + "..." : (content != null ? content : ""));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        holder.postDate.setText(sdf.format(timestamp.toDate()));
        holder.postAuthor.setText(authorName != null ? authorName : "익명");

        if (border != null) {
            holder.postBorder.setText(border);
            holder.postBorder.setTextColor("공지사항".equals(border)
                    ? mainActivity.getColor(android.R.color.holo_red_dark)
                    : Color.parseColor("#1F2937"));
        } else holder.postBorder.setText("미분류");

        // 좋아요 / 댓글 실시간 반영
        holder.detach();
        holder.postRef = db.collection("posts").document(postId);
        holder.myLikeRef = (currentUserId != null)
                ? holder.postRef.collection("likes").document(currentUserId)
                : null;

        Long likeCount = snapshot.getLong("likeCount");
        Long commentCount = snapshot.getLong("commentCount");

        holder.likeCountValue = (likeCount != null) ? likeCount.intValue() : 0;
        holder.commentCountValue = (commentCount != null) ? commentCount.intValue() : 0;
        holder.postLikeCount.setText(String.valueOf(holder.likeCountValue));
        holder.postCommentCount.setText(String.valueOf(holder.commentCountValue));

        // 실시간 리스너
        holder.postReg = holder.postRef.addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists()) return;
            Long lc = doc.getLong("likeCount");
            Long cc = doc.getLong("commentCount");
            holder.likeCountValue = (lc != null) ? lc.intValue() : 0;
            holder.commentCountValue = (cc != null) ? cc.intValue() : 0;
            holder.postLikeCount.setText(String.valueOf(holder.likeCountValue));
            holder.postCommentCount.setText(String.valueOf(holder.commentCountValue));
            applyLikeTint(holder);
        });

        // 좋아요 상태
        holder.liked = false;
        if (holder.myLikeRef != null) {
            holder.myLikeRef.get().addOnSuccessListener(d -> {
                holder.liked = (d != null && d.exists());
                applyLikeTint(holder);
            });
        } else applyLikeTint(holder);

        // 좋아요 클릭
        View.OnClickListener likeClick = v -> {
            if (currentUserId == null || holder.likeBusy) return;
            holder.likeBusy = true;
            boolean wantLike = !holder.liked;
            holder.liked = wantLike;
            holder.likeCountValue = Math.max(0, holder.likeCountValue + (wantLike ? 1 : -1));
            holder.postLikeCount.setText(String.valueOf(holder.likeCountValue));
            applyLikeTint(holder);
            toggleLike(holder, wantLike, ok -> holder.likeBusy = false);
        };
        holder.postLikeIcon.setOnClickListener(likeClick);
        holder.postLikeCount.setOnClickListener(likeClick);

        // 상세보기 클릭
        View.OnClickListener goDetail = v -> {
            if (listener != null) listener.onItemClick(snapshot);
        };
        holder.itemView.setOnClickListener(goDetail);
        holder.postCommentCount.setOnClickListener(goDetail);
        holder.postCommentIcon.setOnClickListener(goDetail);
    }

    // ===============================
    // 🧩 Firestore 좋아요 토글
    // ===============================
    private interface ToggleCb { void done(boolean ok); }

    private void toggleLike(@NonNull PostViewHolder h, boolean wantLike, ToggleCb cb) {
        if (h.postRef == null || currentUserId == null) { cb.done(false); return; }
        DocumentReference likeRef = h.postRef.collection("likes").document(currentUserId);

        db.runTransaction(tr -> {
                    DocumentSnapshot post = tr.get(h.postRef);
                    long current = post.getLong("likeCount") != null ? post.getLong("likeCount") : 0L;
                    DocumentSnapshot my = tr.get(likeRef);
                    if (wantLike && !my.exists()) {
                        tr.set(likeRef, Collections.singletonMap("timestamp", Timestamp.now()));
                        tr.update(h.postRef, "likeCount", current + 1);
                    } else if (!wantLike && my.exists()) {
                        tr.delete(likeRef);
                        tr.update(h.postRef, "likeCount", Math.max(0, current - 1));
                    }
                    return null;
                }).addOnCompleteListener(t -> cb.done(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "toggleLike failed", e);
                    cb.done(false);
                });
    }

    private void applyLikeTint(@NonNull PostViewHolder h) {
        int tint = h.liked ? 0xFF2563EB : 0xFF555555;
        h.postLikeIcon.setColorFilter(tint);
        h.postLikeCount.setTextColor(tint);
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        super.onViewRecycled(holder);
        holder.detach();
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.postList = newList;
        notifyDataSetChanged();
    }

    // ===============================
    // 🧱 ViewHolder 정의
    // ===============================
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView postTitle, postContent, postDate;
        TextView postAuthor, postBorder, postLikeCount, postCommentCount;
        ImageView postLikeIcon, postCommentIcon;

        int likeCountValue, commentCountValue;
        boolean liked, likeBusy;
        DocumentReference postRef, myLikeRef;
        ListenerRegistration postReg;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.postTitle);
            postContent = itemView.findViewById(R.id.postContent);
            postDate = itemView.findViewById(R.id.postDate);
            postAuthor = itemView.findViewById(R.id.postAuthor);
            postBorder = itemView.findViewById(R.id.postBorder);
            postLikeCount = itemView.findViewById(R.id.postLikeCount);
            postCommentCount = itemView.findViewById(R.id.postCommentCount);
            postLikeIcon = itemView.findViewById(R.id.postLikeIcon);
            postCommentIcon = itemView.findViewById(R.id.postCommentIcon);
        }

        void detach() {
            if (postReg != null) { postReg.remove(); postReg = null; }
            postRef = null; myLikeRef = null; likeBusy = false;
        }
    }
}
