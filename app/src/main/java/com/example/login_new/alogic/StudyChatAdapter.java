package com.example.login_new.alogic;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;

/**
 * 💬 StudyChatAdapter — 스터디 채팅방 전용 어댑터
 *  - StudyChatMessage 를 사용 (랜덤채팅용 ChatMessage 와 별개)
 */
public class StudyChatAdapter extends ListAdapter<StudyChatMessage, StudyChatAdapter.ChatViewHolder> {

    private String currentUserUid = "";

    public StudyChatAdapter() {
        super(DIFF);
    }

    /** 🔹 현재 로그인한 사용자 UID 전달 */
    public void setCurrentUserUid(String uid) {
        this.currentUserUid = (uid == null) ? "" : uid;
    }

    // DiffUtil
    private static final DiffUtil.ItemCallback<StudyChatMessage> DIFF =
            new DiffUtil.ItemCallback<StudyChatMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull StudyChatMessage oldItem,
                                               @NonNull StudyChatMessage newItem) {
                    // timestamp 기반으로 같은 메시지인지 판단
                    if (oldItem.getTimestamp() == null || newItem.getTimestamp() == null) return false;
                    return oldItem.getTimestamp().equals(newItem.getTimestamp());
                }

                @Override
                public boolean areContentsTheSame(@NonNull StudyChatMessage oldItem,
                                                  @NonNull StudyChatMessage newItem) {
                    // 내용이 완전히 같은지 비교 (필요한 필드만 비교)
                    if (oldItem.isSystem() != newItem.isSystem()) return false;
                    if (!safeEquals(oldItem.getText(), newItem.getText())) return false;
                    if (!safeEquals(oldItem.getSenderUid(), newItem.getSenderUid())) return false;
                    if (!safeEquals(oldItem.getSenderId(), newItem.getSenderId())) return false;
                    if (!safeEquals(oldItem.getTitle(), newItem.getTitle())) return false;
                    if (!safeEquals(oldItem.getTime(), newItem.getTime())) return false;
                    return true;
                }

                private boolean safeEquals(String a, String b) {
                    if (a == null) return b == null;
                    return a.equals(b);
                }
            };

    // ----------------------------------------------------

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChatViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat_message, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder h, int position) {
        StudyChatMessage m = getItem(position);
        if (m == null) return;

        // 🟣 시스템 메시지
        if (m.isSystem()) {
            h.tvSystemMessage.setVisibility(View.VISIBLE);
            h.layoutLeft.setVisibility(View.GONE);
            h.layoutRight.setVisibility(View.GONE);

            h.tvSystemMessage.setText(m.getText());
            h.tvSystemMessage.setGravity(Gravity.CENTER);
            h.tvSystemMessage.setTextColor(Color.parseColor("#777777"));
            return;
        }

        h.tvSystemMessage.setVisibility(View.GONE);

        // ==========================
        // 🔥 좌 / 우 구분 핵심
        // ==========================
        boolean isMine = m.getSenderUid() != null &&
                m.getSenderUid().equals(currentUserUid);

        if (isMine) {
            // 👉 오른쪽 (내 메시지)
            h.layoutRight.setVisibility(View.VISIBLE);
            h.layoutLeft.setVisibility(View.GONE);

            h.tvMessageRight.setText(m.getText());
            h.tvTimeRight.setText(m.getTime());
            h.tvTitleRight.setText(makeStyledTitle(m.getSenderId(), m.getTitle()));

        } else {
            // 👈 왼쪽 (상대 메시지)
            h.layoutLeft.setVisibility(View.VISIBLE);
            h.layoutRight.setVisibility(View.GONE);

            h.tvMessageLeft.setText(m.getText());
            h.tvTimeLeft.setText(m.getTime());
            h.tvTitleLeft.setText(makeStyledTitle(m.getSenderId(), m.getTitle()));
        }
    }

    // ----------------------------------------------------
    /** 닉네임 + 칭호 색상 스타일 */
    private Spannable makeStyledTitle(String name, String title) {
        if (name == null || name.isEmpty()) name = "익명";
        if (title == null) title = "";

        String full = title.isEmpty() ? name : name + " (" + title + ")";

        SpannableStringBuilder span = new SpannableStringBuilder(full);

        int s = full.indexOf("(");
        int e = full.indexOf(")") + 1;

        if (s >= 0 && e > s) {
            span.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#7A4FF7")),
                    s,
                    e,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        span.setSpan(
                new ForegroundColorSpan(Color.BLACK),
                0,
                (s >= 0 ? s : full.length()),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return span;
    }

    // ----------------------------------------------------
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutLeft, layoutRight;
        TextView tvMessageLeft, tvMessageRight;
        TextView tvTitleLeft, tvTitleRight;
        TextView tvTimeLeft, tvTimeRight;
        TextView tvSystemMessage;

        public ChatViewHolder(@NonNull View v) {
            super(v);

            layoutLeft = v.findViewById(R.id.layoutLeft);
            layoutRight = v.findViewById(R.id.layoutRight);

            tvMessageLeft = v.findViewById(R.id.tvMessageLeft);
            tvMessageRight = v.findViewById(R.id.tvMessageRight);

            tvTitleLeft = v.findViewById(R.id.tvTitleLeft);
            tvTitleRight = v.findViewById(R.id.tvTitleRight);

            tvTimeLeft = v.findViewById(R.id.tvTimeLeft);
            tvTimeRight = v.findViewById(R.id.tvTimeRight);

            tvSystemMessage = v.findViewById(R.id.tvSystemMessage);
        }
    }
}
