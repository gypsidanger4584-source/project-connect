package com.example.login_new.alogic;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Objects;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final ArrayList<Comment> commentList = new ArrayList<>();
    private ListenerRegistration listenerRegistration;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String postId;
    private final OnCommentClickListener listener;
    private final String currentUserId;

    private static final String TAG = "CommentAdapter";

    public interface OnCommentClickListener {
        void onCommentClick(Comment comment);
    }

    public CommentAdapter(String postId, OnCommentClickListener listener) {
        this.postId = postId;
        this.listener = listener;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;

        attachListener();
    }

    private void attachListener() {
        listenerRegistration = db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Comment comment = dc.getDocument().toObject(Comment.class);
                        comment.setId(dc.getDocument().getId());

                        switch (dc.getType()) {
                            case ADDED:
                                commentList.add(dc.getNewIndex(), comment);
                                notifyItemInserted(dc.getNewIndex());
                                break;

                            case MODIFIED:
                                int indexMod = findCommentIndexById(comment.getId());
                                if (indexMod != -1) {
                                    commentList.set(indexMod, comment);
                                    notifyItemChanged(indexMod);
                                }
                                break;

                            case REMOVED:
                                int indexRem = findCommentIndexById(comment.getId());
                                if (indexRem != -1) {
                                    commentList.remove(indexRem);
                                    notifyItemRemoved(indexRem);
                                }
                                break;
                        }
                    }
                });
    }

    public void cleanup() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private int findCommentIndexById(String id) {
        for (int i = 0; i < commentList.size(); i++) {
            if (Objects.equals(commentList.get(i).getId(), id)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        String commentId = comment.getId();

        // Firestore 참조 연결
        holder.detach();
        holder.commentRef = db.collection("posts")
                .document(postId)
                .collection("comments")
                .document(commentId);

        // ------------------- 댓글 기본 내용 표시 -------------------
        String author = (comment.getAuthor() != null) ? comment.getAuthor() : "익명";
        String content = (comment.getContent() != null) ? comment.getContent() : "(내용 없음)";

        holder.tvAuthor.setText(author);
        holder.tvContent.setText(content);

        // ------------------- 🔥 대한민국 시간 표시 -------------------
        if (comment.getTimestamp() != null) {
            java.util.Date date = comment.getTimestamp();

            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("MM.dd HH:mm", java.util.Locale.KOREA);

            sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
            holder.tvTime.setText(sdf.format(date));

        } else {
            holder.tvTime.setText("시간 정보 없음");
        }

        // ------------------- 대댓글 들여쓰기 -------------------
        if (comment.getParentId() != null && !comment.getParentId().isEmpty()) {
            holder.itemView.setTranslationX(80f);
            holder.replyArrow.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setTranslationX(0f);
            holder.replyArrow.setVisibility(View.GONE);
        }

        // ------------------- 삭제 버튼 (본인만 보임) -------------------
        if (currentUserId != null && currentUserId.equals(comment.getUid())) {
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setOnClickListener(v -> {
                v.setEnabled(false);

                db.collection("posts")
                        .document(postId)
                        .collection("comments")
                        .document(comment.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            db.collection("posts")
                                    .document(postId)
                                    .update("commentCount",
                                            com.google.firebase.firestore.FieldValue.increment(-1));

                            Toast.makeText(holder.itemView.getContext(),
                                    "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            v.setEnabled(true);
                            Toast.makeText(holder.itemView.getContext(),
                                    "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        } else {
            holder.deleteBtn.setVisibility(View.GONE);
        }

        // ------------------- 댓글 클릭 = 대댓글 입력 -------------------
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(comment);
        });
    }

    @Override
    public void onViewRecycled(@NonNull CommentViewHolder holder) {
        super.onViewRecycled(holder);
        holder.detach();
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    // ------------------- ViewHolder -------------------

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvAuthor, tvContent, tvTime;
        View replyArrow;
        Button deleteBtn;

        DocumentReference commentRef;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAuthor = itemView.findViewById(R.id.commentAuthor);
            tvContent = itemView.findViewById(R.id.commentContent);
            tvTime = itemView.findViewById(R.id.commentTime);   // ⭐ 추가됨
            deleteBtn = itemView.findViewById(R.id.btnDeleteComment);
            replyArrow = itemView.findViewById(R.id.replyArrow);
        }

        void detach() {
            commentRef = null;
        }
    }
}
