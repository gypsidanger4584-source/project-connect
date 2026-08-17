package com.example.login_new.alogic;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudyRoomAdapter extends RecyclerView.Adapter<StudyRoomAdapter.RoomViewHolder> {

    private final List<StudyRoom> rooms;
    private final OnRoomClickListener listener;
    private final FirebaseUser currentUser;
    private final String currentUid;

    public interface OnRoomClickListener {
        void onClick(StudyRoom room);
    }

    public StudyRoomAdapter(List<StudyRoom> list, OnRoomClickListener listener) {
        this.rooms = list != null ? list : new ArrayList<>();
        this.listener = listener;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUid = currentUser != null ? currentUser.getUid() : "";
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_room, parent, false);
        return new RoomViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder h, int pos) {

        StudyRoom room = rooms.get(pos);
        if (room == null) return;

        // 🔹 상단 요약 영역
        h.tvRoomName.setText(room.getRoomName());

        // ✅ 카테고리 말고 "학습 주제" 중심으로 한 줄 요약
        //    (필요하면 category + topic 조합으로 살짝 바꿀 수도 있음)
        h.tvRoomDesc.setText(room.getTopic() != null ? room.getTopic() : "");

        h.tvOwnerInfo.setText("👑 " + room.getCreatorName() + " · " + room.getStartDate() + " 생성");

        // 🔹 펼쳐진 영역
        h.tvTopic.setText("📚 학습 주제: " + room.getTopic());
        h.tvCategory.setText("🏷 카테고리: " + room.getCategory());
        h.tvPeople.setText("👥 인원: " + room.getCurrentPeople() + " / " + room.getMaxPeople());
        h.tvProblems.setText("🎯 문제: " + room.getProblemCount() + "개");
        h.tvPeriod.setText("📅 기간: " + room.getStartDate() + " ~ " + room.getEndDate());

        // ==========================================================
        // ⭐ 내가 만든 방 여부 체크
        // ==========================================================
        boolean isMyRoom = room.getOwnerUid() != null
                && room.getOwnerUid().equals(currentUid);

        if (isMyRoom) {
            // "내 방" 배지 + 카드 색상 강조
            h.tvMyBadge.setVisibility(View.VISIBLE);
            h.cardRoot.setCardBackgroundColor(Color.parseColor("#F3E8FF")); // 연보라
            h.cardRoot.setStrokeColor(Color.parseColor("#A47CF3"));        // 진보라 라인
        } else {
            h.tvMyBadge.setVisibility(View.GONE);
            h.cardRoot.setCardBackgroundColor(Color.WHITE);
            h.cardRoot.setStrokeColor(Color.parseColor("#C8B8FF"));
        }

        // ==========================================================
        // ⭐ 참여 여부에 따라 방 제목 색상 강조
        // ==========================================================
        boolean isJoined = room.getParticipants() != null &&
                room.getParticipants().contains(currentUid);

        if (isJoined) {
            h.tvRoomName.setTextColor(Color.parseColor("#0B325A")); // 이미 참여한 방 → 진파랑
        } else {
            h.tvRoomName.setTextColor(Color.parseColor("#1E1E1E")); // 기본
        }

        // ==========================================================
        // 🔽 펼치기 / 접기 버튼
        // ==========================================================
        h.btnExpand.setRotation(room.isExpanded() ? 180f : 0f);
        h.layoutExpanded.setVisibility(room.isExpanded() ? View.VISIBLE : View.GONE);

        h.btnExpand.setOnClickListener(v -> {
            room.setExpanded(!room.isExpanded());
            notifyItemChanged(pos);
        });

        // ==========================================================
        // 🟣 카드 전체 클릭 동작
        //   1) 아직 참여하지 않은 방 → 카드 클릭 시 "더보기(펼치기)"만
        //   2) 이미 참여한 방        → 카드 클릭 한 번으로 바로 입장
        // ==========================================================
        h.cardRoot.setOnClickListener(v -> {
            int adapterPos = h.getAdapterPosition();   // ← getBindingAdapterPosition 대신 사용
            if (adapterPos == RecyclerView.NO_POSITION) return;

            StudyRoom clickedRoom = rooms.get(adapterPos);
            if (clickedRoom == null) return;

            boolean clickedJoined = clickedRoom.getParticipants() != null &&
                    clickedRoom.getParticipants().contains(currentUid);

            if (clickedJoined) {
                // ✅ 이미 들어간 방이면 → 카드 한 번 클릭으로 바로 입장
                if (listener != null) listener.onClick(clickedRoom);
            } else {
                // ✅ 아직 참여 안 한 방이면 → 카드 클릭하면 더보기(펼치기)만
                clickedRoom.setExpanded(!clickedRoom.isExpanded());
                notifyItemChanged(adapterPos);
            }
        });

        // ==========================================================
        // 🟢 "방 입장하기" 버튼
        //    - 참여했든 안 했든, 여기 누르면 항상 listener 통해 입장/참여 로직 실행
        // ==========================================================
        h.btnEnterRoom.setOnClickListener(v -> {
            if (listener != null) listener.onClick(room);
        });
    }

    @Override
    public int getItemCount() {
        return rooms != null ? rooms.size() : 0;
    }

    // ==========================================================
    // 🔥 외부에서 리스트를 갈아끼울 때 내 방을 항상 위로 정렬
    // ==========================================================
    public void updateRooms(List<StudyRoom> newList) {
        if (newList == null) return;

        rooms.clear();
        rooms.addAll(newList);

        // ⭐ 내 방을 맨 위로 정렬 (나머지는 기존 순서 유지)
        Collections.sort(rooms, (a, b) -> {
            boolean aMy = a.getOwnerUid() != null && a.getOwnerUid().equals(currentUid);
            boolean bMy = b.getOwnerUid() != null && b.getOwnerUid().equals(currentUid);

            if (aMy && !bMy) return -1;
            if (!aMy && bMy) return 1;
            return 0;
        });

        notifyDataSetChanged();
    }

    // ==========================================================
    // ViewHolder
    // ==========================================================
    public static class RoomViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardRoot;
        TextView tvRoomName, tvRoomDesc, tvOwnerInfo, tvMyBadge;
        TextView tvTopic, tvCategory, tvPeople, tvProblems, tvPeriod;
        LinearLayout layoutExpanded;
        ImageView btnExpand;
        Button btnEnterRoom;

        public RoomViewHolder(@NonNull View v) {
            super(v);

            cardRoot = v.findViewById(R.id.cardRoot);
            tvRoomName = v.findViewById(R.id.tvRoomName);
            tvRoomDesc = v.findViewById(R.id.tvRoomDesc);
            tvOwnerInfo = v.findViewById(R.id.tvOwnerInfo);

            tvMyBadge = v.findViewById(R.id.tvMyBadge);

            tvTopic = v.findViewById(R.id.tvTopic);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvPeople = v.findViewById(R.id.tvPeople);
            tvProblems = v.findViewById(R.id.tvProblems);
            tvPeriod = v.findViewById(R.id.tvPeriod);

            layoutExpanded = v.findViewById(R.id.layoutExpanded);
            btnExpand = v.findViewById(R.id.btnExpand);
            btnEnterRoom = v.findViewById(R.id.btnEnterRoom);
        }
    }
}
