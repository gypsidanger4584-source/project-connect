package com.example.login_new.alogic;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class StudyProblemAdapter extends RecyclerView.Adapter<StudyProblemAdapter.ViewHolder> {

    private final List<StudyProblemItem> problemList;
    private String roomId;
    private long lastClickTime = 0;
    private final OnProblemClickListener clickListener;

    public interface OnProblemClickListener {
        void onProblemClick(StudyProblemItem item);
    }

    public StudyProblemAdapter(List<StudyProblemItem> problemList, String roomId) {
        this(problemList, roomId, null);
    }

    public StudyProblemAdapter(List<StudyProblemItem> problemList, String roomId, OnProblemClickListener listener) {
        this.problemList = problemList;
        this.roomId = roomId;
        this.clickListener = listener;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_problem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (problemList == null || problemList.isEmpty()) return;

        StudyProblemItem item = problemList.get(position);
        Context context = holder.itemView.getContext();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = (user != null) ? user.getUid() : "";

        // --------------------------------------------------
        // 1) 기본 정보
        // --------------------------------------------------
        holder.tvProblemTitle.setText(safeString(item.getTitle(), "제목 없음"));
        holder.tvAuthorInfo.setText(
                safeString(item.getAuthor(), "익명") + " • " + safeString(item.getTimeAgo(), "")
        );
        holder.tvDifficulty.setText(safeString(item.getDifficulty(), "보통"));
        holder.tvCategory.setText(safeString(item.getCategory(), "기타"));
        holder.imgProfile.setImageResource(R.drawable.default_profile);

        // 난이도 색상
        String level = item.getDifficulty();
        int bgRes;
        if ("쉬움".equals(level)) bgRes = R.drawable.bg_tag_easy;
        else if ("보통".equals(level)) bgRes = R.drawable.bg_tag_normal;
        else if ("어려움".equals(level)) bgRes = R.drawable.bg_tag_hard;
        else bgRes = R.drawable.bg_tag_default;
        holder.tvDifficulty.setBackgroundResource(bgRes);

        // --------------------------------------------------
        // 2) “내 문제” 태그 표시
        // --------------------------------------------------
        if (item.getCreatorUid() != null && item.getCreatorUid().equals(myUid)) {
            holder.tvMyProblemTag.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundResource(R.drawable.bg_problem_myitem);
        } else {
            holder.tvMyProblemTag.setVisibility(View.GONE);
        }

        // --------------------------------------------------
        // 3) 정답/오답 표시
        // --------------------------------------------------
        if (item.isSolved()) {
            holder.ivSolvedCheck.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.85f);

            if (item.isCorrectSolved()) {
                holder.tvProblemTitle.setText("✅ " + item.getTitle());
                holder.tvProblemTitle.setTextColor(Color.parseColor("#4CAF50"));
                holder.itemView.setBackgroundResource(R.drawable.bg_problem_solved_correct);
            } else {
                holder.tvProblemTitle.setText("❌ " + item.getTitle());
                holder.tvProblemTitle.setTextColor(Color.parseColor("#888888"));
                holder.itemView.setBackgroundResource(R.drawable.bg_problem_solved_wrong);
            }
        } else {
            holder.tvProblemTitle.setText(item.getTitle());
            holder.tvProblemTitle.setTextColor(Color.parseColor("#222222"));

            // 내 문제가 아닌 경우만 원래 카드로 복구
            if (!(item.getCreatorUid() != null && item.getCreatorUid().equals(myUid))) {
                holder.itemView.setBackgroundResource(R.drawable.bg_problem_card);
            }

            holder.itemView.setAlpha(1f);
            holder.ivSolvedCheck.setVisibility(View.GONE);
        }

        // --------------------------------------------------
        // 4) 클릭 이벤트 (내 문제여도 들어가기 허용)
        // --------------------------------------------------
        holder.itemView.setOnClickListener(v -> {

            if (SystemClock.elapsedRealtime() - lastClickTime < 600) return;
            lastClickTime = SystemClock.elapsedRealtime();

            // → 이제 더 이상 클릭을 막지 않는다!

            if (clickListener != null) {
                clickListener.onProblemClick(item);
                return;
            }

            if (roomId == null || roomId.isEmpty()) {
                Toast.makeText(context, "방 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(context, StudyProblemSolveActivity.class);
            intent.putExtra("roomId", roomId);
            intent.putExtra("problemId", item.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return problemList != null ? problemList.size() : 0;
    }

    // ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProfile;
        final ImageView ivSolvedCheck;
        final TextView tvProblemTitle;
        final TextView tvAuthorInfo;
        final TextView tvDifficulty;
        final TextView tvCategory;

        // 추가된 "내 문제" 태그
        final TextView tvMyProblemTag;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfile = itemView.findViewById(R.id.imgProfile);
            ivSolvedCheck = itemView.findViewById(R.id.ivSolvedCheck);
            tvProblemTitle = itemView.findViewById(R.id.tvProblemTitle);
            tvAuthorInfo = itemView.findViewById(R.id.tvAuthorInfo);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvMyProblemTag = itemView.findViewById(R.id.tvMyProblemTag);
        }
    }

    private static String safeString(Object value, String fallback) {
        if (value == null) return fallback;
        if (value instanceof String s) return s.isEmpty() ? fallback : s;
        return String.valueOf(value);
    }
}
