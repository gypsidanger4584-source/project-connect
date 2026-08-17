package com.example.login_new.alogic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {

    private final List<NoticeItem> noticeList;
    private final Context context;
    private boolean isAdmin;

    // 🔥 공지 갱신 콜백 인터페이스
    public interface OnNoticeChangedListener {
        void onNoticeChanged();
    }

    private OnNoticeChangedListener noticeChangedListener;

    // Firestore posts 컬렉션
    private static final String COLLECTION_NAME = "posts";

    public NoticeAdapter(Context context, List<NoticeItem> noticeList, boolean isAdmin,
                         OnNoticeChangedListener listener) {

        this.context = context;
        this.noticeList = noticeList != null ? noticeList : new ArrayList<>();
        this.isAdmin = isAdmin;
        this.noticeChangedListener = listener;
    }

    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
        notifyDataSetChanged();
    }

    public void setNoticeChangedListener(OnNoticeChangedListener listener) {
        this.noticeChangedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(noticeList.get(position));
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    public void updateList(List<NoticeItem> newList) {
        noticeList.clear();
        noticeList.addAll(newList);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle, tvTime, tvContent, tvMore;
        private final LinearLayout layoutActions;
        private final TextView btnEdit, btnDelete;
        private boolean isExpanded = false;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tvNoticeTitle);
            tvTime        = itemView.findViewById(R.id.tvNoticeTime);
            tvContent     = itemView.findViewById(R.id.tvNoticeContent);
            tvMore        = itemView.findViewById(R.id.tvMore);
            layoutActions = itemView.findViewById(R.id.layoutNoticeActions);
            btnEdit       = itemView.findViewById(R.id.btnEditNotice);
            btnDelete     = itemView.findViewById(R.id.btnDeleteNotice);
        }

        void bind(NoticeItem item) {

            // 🔹 제목
            tvTitle.setText(item.getTitle());

            // 🔹 시간
            if (item.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREA);
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                tvTime.setText(sdf.format(item.getTimestamp().toDate()));
            } else {
                tvTime.setText("");
            }

            // 🔹 본문 (기본: 한 줄 프리뷰)
            tvContent.setText(item.getContent() != null ? item.getContent() : "");
            isExpanded = false;
            tvContent.setMaxLines(1);   // ⭐ 한 줄만 보여주기 (XML에서 ellipsize="end" 설정됨)
            tvMore.setText("[더보기]");

            // 🔹 [더보기] / [접기] 토글
            tvMore.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                if (isExpanded) {
                    // 전체 내용 보기
                    tvContent.setMaxLines(Integer.MAX_VALUE);
                    tvMore.setText("[접기]");
                } else {
                    // 다시 한 줄 프리뷰
                    tvContent.setMaxLines(1);
                    tvMore.setText("[더보기]");
                }
            });

            // 🔹 관리자만 수정/삭제 버튼 보이기
            layoutActions.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

            // ============================================================
            //       📌 공지 수정
            // ============================================================
            btnEdit.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle("공지 수정")
                    .setMessage("수정하시겠습니까?")
                    .setPositiveButton("예", (dialog, w) -> {

                        Intent intent = new Intent(context, WriteActivity.class);
                        intent.putExtra("mode", "edit_notice");
                        intent.putExtra("noticeId", item.getId());
                        intent.putExtra("title", item.getTitle());
                        intent.putExtra("content", item.getContent());

                        if (!(context instanceof Activity)) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        context.startActivity(intent);

                        // ✨ 수정 후 즉시 갱신 요청
                        if (noticeChangedListener != null) {
                            noticeChangedListener.onNoticeChanged();
                        }

                    })
                    .setNegativeButton("아니오", null)
                    .show()
            );

            // ============================================================
            //       📌 공지 삭제
            // ============================================================
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle("공지 삭제")
                    .setMessage("삭제하시겠습니까?")
                    .setPositiveButton("예", (dialog, w) -> {

                        FirebaseFirestore.getInstance()
                                .collection(COLLECTION_NAME)
                                .document(item.getId())
                                .delete()
                                .addOnSuccessListener(a -> {

                                    Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show();

                                    int pos = getAdapterPosition();
                                    if (pos != RecyclerView.NO_POSITION) {
                                        noticeList.remove(pos);
                                        notifyItemRemoved(pos);
                                    }

                                    // ✨ 삭제 후 즉시 갱신 요청
                                    if (noticeChangedListener != null) {
                                        noticeChangedListener.onNoticeChanged();
                                    }

                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );

                    })
                    .setNegativeButton("아니오", null)
                    .show()
            );
        }
    }
}
