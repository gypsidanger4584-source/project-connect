package com.example.login_new.login;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import android.util.Log;

import java.util.ArrayList;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final ArrayList<Comment> commentList = new ArrayList<>();
    private ListenerRegistration listenerRegistration;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String postId;
    private final OnCommentClickListener listener;
    private final String currentUserId;

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
                                commentList.add(comment);
                                notifyItemInserted(commentList.size() - 1);
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
                                Log.d("CommentAdapter", "삭제 이벤트 감지: " + comment.getId() + ", index: " + indexRem);
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
            if (commentList.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        String author = (comment.getAuthor() != null) ? comment.getAuthor() : "익명";
        String content = (comment.getContent() != null && !comment.getContent().isEmpty()) ? comment.getContent() : "(내용 없음)";

        holder.tvAuthor.setText(author);
        holder.tvContent.setText(content);

        // 대댓글 여부에 따라 들여쓰기 및 화살표 표시
        if (comment.getParentId() != null && !comment.getParentId().isEmpty()) {
            holder.itemView.setTranslationX(80f);
            holder.replyArrow.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setTranslationX(0f);
            holder.replyArrow.setVisibility(View.GONE);
        }

        // 삭제 버튼 표시 여부 및 동작
        if (currentUserId != null && currentUserId.equals(comment.getUid())) {
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setOnClickListener(v -> {
                v.setEnabled(false);
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                Comment commentToDelete = commentList.get(adapterPosition);

                db.collection("posts")
                        .document(postId)
                        .collection("comments")
                        .document(commentToDelete.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(holder.itemView.getContext(), "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            // commentList 및 notifyItemRemoved()는 addSnapshotListener가 처리함
                        })
                        .addOnFailureListener(e -> {
                            v.setEnabled(true);
                            Toast.makeText(holder.itemView.getContext(), "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });

        } else {
            holder.deleteBtn.setVisibility(View.GONE);
        }

        // 댓글 클릭 시 대댓글 작성 처리
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(comment);
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvContent, replyArrow;
        Button deleteBtn;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.commentAuthor);
            tvContent = itemView.findViewById(R.id.commentContent);
            deleteBtn = itemView.findViewById(R.id.btnDeleteComment);
            replyArrow = itemView.findViewById(R.id.replyArrow);
        }
    }
}
