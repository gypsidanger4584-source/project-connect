package com.example.login_new.alogic;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.login_new.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {

    private final Context context;
    private List<Participant> participants;
    private final String currentUserUid;
    private String ownerUid = "";     // ⭐ 방장 UID 저장
    private final boolean isOwner;
    private final OnKickListener kickListener;

    public interface OnKickListener {
        void onKick(String uid);
    }

    // 기본 생성자
    public ParticipantAdapter(Context ctx, List<Participant> list, String uid) {
        this(ctx, list, uid, false, null);
    }

    // 확장된 생성자
    public ParticipantAdapter(Context ctx, List<Participant> list, String uid,
                              boolean isOwner, OnKickListener listener) {

        this.context = ctx;
        this.participants = list != null ? list : new ArrayList<>();
        this.currentUserUid = uid != null ? uid : "";
        this.isOwner = isOwner;
        this.kickListener = listener;
    }

    // ⭐ StudyChatActivity에서 ownerUid를 전달받아 설정
    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid != null ? ownerUid : "";
        notifyDataSetChanged();
    }

    // ===========================================================================
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ViewHolder(v);
    }

    // ===========================================================================
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Participant p = participants.get(pos);
        if (p == null) return;

        String name = nonNull(p.getName(), "익명");
        String title = nonNull(p.getTitle(), "입문자");

        boolean isRoomOwner = p.getUid() != null && p.getUid().equals(ownerUid);

        // ⭐ 방장 표시
        if (isRoomOwner) {
            h.tvName.setText("👑 " + name);
            h.tvName.setTextColor(Color.parseColor("#FFD700"));   // 금색
            h.tvTitle.setText(title + " · 방장");

        } else {
            h.tvName.setText(name);
            h.tvTitle.setText(title);
        }

        // ================================
        // 🔥 점수
        // ================================
        int score = Math.max(p.getScore(), 0);
        h.tvScore.setText(score + "점");
        h.tvScore.setTextColor(getScoreColor(score));

        // ================================
        // 🔥 프로필 이미지
        // ================================
        String url = nonNull(p.getProfileUrl(), "");

        if (!url.isEmpty()) {

            if (url.startsWith("http")) {
                Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(h.ivProfile);

            } else {
                int id = context.getResources()
                        .getIdentifier(url, "drawable", context.getPackageName());
                h.ivProfile.setImageResource(id != 0 ? id : R.drawable.default_profile);
            }

        } else {
            h.ivProfile.setImageResource(
                    p.getProfileResId() != 0 ? p.getProfileResId() : R.drawable.default_profile
            );
        }

        // 활/비활
        applyActiveState(h, p.isActive());

        // ================================
        // 🔥 방장만 강퇴 버튼 보임
        // ================================
        boolean isSelf = p.getUid() != null && p.getUid().equals(currentUserUid);
        boolean canKick = isOwner && !isSelf && p.isActive() && kickListener != null
                && !isRoomOwner;   // ⭐ 방장은 강퇴 금지

        h.btnKick.setVisibility(canKick ? View.VISIBLE : View.GONE);
        if (canKick) h.btnKick.setOnClickListener(v -> kickListener.onKick(p.getUid()));
    }

    @Override
    public int getItemCount() {
        return participants != null ? participants.size() : 0;
    }

    // ===========================================================================
    public void updateParticipants(List<Participant> newList) {
        this.participants = newList != null ? newList : Collections.emptyList();
        notifyDataSetChanged();
    }

    // ===========================================================================
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvTitle, tvScore;
        ImageButton btnKick;

        public ViewHolder(@NonNull View v) {
            super(v);
            ivProfile = v.findViewById(R.id.ivProfile);
            tvName = v.findViewById(R.id.tvName);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvScore = v.findViewById(R.id.tvScore);
            btnKick = v.findViewById(R.id.btnKick);
        }
    }

    // ===========================================================================
    private int getScoreColor(int score) {
        if (score >= 30) return ContextCompat.getColor(context, R.color.gold);
        if (score >= 10) return ContextCompat.getColor(context, R.color.blue_500);
        return ContextCompat.getColor(context, R.color.gray_600);
    }

    private void applyActiveState(ViewHolder h, boolean active) {
        int nameColor = ContextCompat.getColor(context,
                active ? R.color.white : R.color.gray_400);

        int titleColor = ContextCompat.getColor(context,
                active ? R.color.gray_200 : R.color.gray_600);

        h.tvName.setAlpha(active ? 1f : 0.4f);
        h.tvTitle.setAlpha(active ? 1f : 0.4f);

        h.tvName.setTextColor(nameColor);
        h.tvTitle.setTextColor(titleColor);
        h.tvScore.setTextColor(titleColor);
        h.ivProfile.setAlpha(active ? 1f : 0.4f);
    }

    private static String nonNull(String v, String fb) {
        return (v == null || v.trim().isEmpty()) ? fb : v.trim();
    }
}
