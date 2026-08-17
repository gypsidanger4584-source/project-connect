package com.example.login_new.alogic;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.example.login_new.R;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** 메모 어댑터: 날짜 MM/dd, 만료 - nD, 강의 색상칩, 강의 제목, D-Day 칩 */
public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoVH> {

    // 🌟 1. 리스너 인터페이스 정의
    public interface OnMemoLongClickListener {
        void onMemoLongClick(Memo memo, int position);
    }

    // === 헬퍼 모델: 강의 정보 ===
    public static class LectureInfo {
        public String title;
        public int color;
        public int dayIndex; // 0=월, 1=화, ...

        public LectureInfo(String title, int color, int dayIndex) {
            this.title = title;
            this.color = color;
            this.dayIndex = dayIndex;
        }
    }

    // === 메모 데이터 모델 (어댑터 내부용) ===
    public static class Memo {
        public String id;
        public String lectureId;
        public String text;
        public Object createdAt;
        @Nullable public Object expiryAt;
        @Nullable public Integer customDays;

        public boolean isImportant;
        private boolean isExpanded = false;

        public String getId() { return id; }
        public String getLectureId() { return lectureId; }
        public String getText() { return text; }
        public Object getCreatedAt() { return createdAt; }
        @Nullable public Object getExpiryAt() { return expiryAt; }
        @Nullable public Integer getCustomDays() { return customDays; }
        public boolean isImportant() { return isImportant; }

        public boolean isExpanded() { return isExpanded; }
        public void setExpanded(boolean expanded) { isExpanded = expanded; }

        public Long getExpiryAtLong() {
            Object exp = getExpiryAt();
            if (exp instanceof Long) return (Long) exp;
            if (exp instanceof Date) return ((Date) exp).getTime();
            if (exp instanceof Timestamp) return ((Timestamp) exp).toDate().getTime();
            return null;
        }
    }

    // === 뷰 홀더 ===
    public static class MemoVH extends RecyclerView.ViewHolder {
        TextView tvMemoDate;
        TextView tvLectureTitle;
        TextView tvDDayChip;
        View vColorChip;
        TextView tvMemoText;
        ImageView ivImportantStarIndicator;

        TextView tvReadMore;

        public MemoVH(@NonNull View itemView) {
            super(itemView);
            tvMemoDate = itemView.findViewById(R.id.tvMemoDate);
            tvLectureTitle = itemView.findViewById(R.id.tvLectureTitle);
            tvDDayChip = itemView.findViewById(R.id.tvDDayChip);
            vColorChip = itemView.findViewById(R.id.vColorChip);
            tvMemoText = itemView.findViewById(R.id.tvMemoText);
            ivImportantStarIndicator = itemView.findViewById(R.id.ivImportantStarIndicator);

            tvReadMore = itemView.findViewById(R.id.tvReadMore);
        }
    }

    private final LayoutInflater inflater;
    private Map<String, LectureInfo> lectureInfoMap;

    private final int DEFAULT_COLOR = 0xFF9E9E9E;
    private final DisplayMetrics displayMetrics;
    private final Context context;

    private final List<Memo> items = new ArrayList<>();
    private final OnMemoLongClickListener longClickListener;

    private static final int MAX_LINES_COLLAPSED = 2;

    // 생성자
    public MemoAdapter(Context ctx, @NonNull Map<String, LectureInfo> lectureInfoMap,
                       OnMemoLongClickListener longClickListener) {
        this.context = ctx;
        this.inflater = LayoutInflater.from(ctx);
        this.lectureInfoMap = lectureInfoMap;
        this.displayMetrics = ctx.getResources().getDisplayMetrics();
        this.longClickListener = longClickListener;
        setHasStableIds(true);
    }

    public void submitList(@NonNull List<Memo> newItems) {
        items.clear();
        items.addAll(newItems);

        // ⭐️ 3단계 복합 정렬 로직 적용: 중요도 -> D-Day -> 작성일(최신순)
        items.sort((a, b) -> {

            // 1. 🥇 1순위: 중요 메모 우선 정렬
            if (a.isImportant() != b.isImportant()) {
                return a.isImportant() ? -1 : 1;
            }

            // --- 1순위가 같을 경우 2순위로 넘어감 ---

            // D-Day 값을 가져옵니다. (calculateMemoDDay가 만료 없음 메모에 대해 -1을 반환함)
            int dDayA = calculateMemoDDay(a);
            int dDayB = calculateMemoDDay(b);

            // 2. 🥈 2순위: D-Day에 가까운 순 (오름차순) 정렬
            if (dDayA != dDayB) {
                if (dDayA >= 0 && dDayB >= 0) {
                    return Integer.compare(dDayA, dDayB);
                }
                // D-Day가 -1인 메모(만료 없음)는 유효한 D-Day보다 뒤로 밀립니다.
                else if (dDayA >= 0) {
                    return -1;
                }
                else if (dDayB >= 0) {
                    return 1;
                }
            }


            // --- 2순위까지 같거나 D-Day 정보가 없을 경우 3순위로 넘어감 ---

            // 3. 🥉 3순위: 입력순 (작성일 기준 최신순, DESC) 정렬
            Date da = toDate(a.getCreatedAt());
            Date db = toDate(b.getCreatedAt());

            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;

            return Long.compare(db.getTime(), da.getTime());
        });

        notifyDataSetChanged();
    }

    @Override public long getItemId(int position) {
        String id = items.get(position).getId();
        return id != null ? id.hashCode() : position;
    }

    @NonNull @Override
    public MemoVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_memo, parent, false);
        return new MemoVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoVH h, int position) {
        Memo m = items.get(position);
        LectureInfo lectInfo = lectureInfoMap.get(m.getLectureId());

        String lectureTitle = (lectInfo != null) ? lectInfo.title : "(삭제된 강의)";
        int lectureColor = (lectInfo != null) ? lectInfo.color : DEFAULT_COLOR;

        // 0b) 별표 아이콘 표시
        if (h.ivImportantStarIndicator != null) {
            h.ivImportantStarIndicator.setVisibility(m.isImportant() ? View.VISIBLE : View.GONE);
        }

        // 1) 본문 (텍스트, 줄 수 제어)
        h.tvMemoText.setText(m.getText());

        // 2) 날짜
        String dateLabel = "작성일: " + formatMmDd(m.getCreatedAt());
        h.tvMemoDate.setText(dateLabel);

        // 3) 강의 제목 설정
        h.tvLectureTitle.setText(lectureTitle);

        // 4) 색상칩
        tintChip(h.vColorChip, lectureColor);

        // ⭐️ 5) D-Day 칩 설정 로직 (만료 기간이 있을 때만 D-Day 칩 표시)
        int dDay = -1;
        Long expiryAt = m.getExpiryAtLong();

        // 5a) 만료 기간 (Expiry) D-Day 계산
        if (expiryAt != null) {
            dDay = getDaysUntilExpiry(expiryAt);
        }

        // D-Day 칩 최종 표시
        if (dDay >= 0) { // 만료 기간이 유효하고 아직 지나지 않았다면
            h.tvDDayChip.setVisibility(View.VISIBLE);
            String dDayText = (dDay == 0) ? "D-DAY" : "D-" + dDay;
            h.tvDDayChip.setText(dDayText);

            // D-Day 배경색 로직 (만료 기간 기반으로만 작동)
            int bgColor = (dDay <= 7) ? 0xFFFF0000 : 0xFF777777;

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setShape(GradientDrawable.RECTANGLE);
            chipBg.setCornerRadius(dp(6));
            chipBg.setColor(bgColor);
            h.tvDDayChip.setBackground(chipBg);
        } else {
            // ⭐️ 만료 기간이 없거나 이미 지난 경우 숨김 (강의 D-Day 표시 로직 제거됨)
            h.tvDDayChip.setVisibility(View.GONE);
        }

        // 6) 롱클릭 리스너 설정
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMemoLongClick(m, position);
            }
            return true;
        });

        // -------------------------------------------------------------
        // ⭐️⭐️⭐️ [더보기]/[접기] 로직 구현 ⭐️⭐️⭐️ (변경 없음)
        // -------------------------------------------------------------

        // 1. 초기 상태 설정
        if (m.isExpanded()) {
            h.tvMemoText.setMaxLines(Integer.MAX_VALUE);
            h.tvMemoText.setEllipsize(null);
            h.tvReadMore.setText("[접기]");
            h.tvReadMore.setVisibility(View.VISIBLE);
        } else {
            h.tvMemoText.setMaxLines(MAX_LINES_COLLAPSED);
            h.tvMemoText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            h.tvReadMore.setVisibility(View.GONE);
        }

        // 2. 텍스트 레이아웃이 완료된 후, 실제로 잘렸는지 확인하고 [더보기] 표시
        h.tvMemoText.post(() -> {
            if (h.tvMemoText.getLayout() != null) {
                Layout layout = h.tvMemoText.getLayout();

                if (!m.isExpanded()) {
                    int lineCount = layout.getLineCount();
                    boolean isTextTruncated = lineCount > 0 && layout.getEllipsisCount(lineCount - 1) > 0;

                    if (isTextTruncated) {
                        h.tvReadMore.setVisibility(View.VISIBLE);
                        h.tvReadMore.setText("[더보기]");
                    } else {
                        h.tvReadMore.setVisibility(View.GONE);
                    }
                }
            }
        });

        // 3. [더보기]/[접기] 클릭 리스너
        h.tvReadMore.setOnClickListener(v -> {
            m.setExpanded(!m.isExpanded());
            notifyItemChanged(position);
        });

        // -------------------------------------------------------------
    }

    @Override public int getItemCount() { return items.size(); }

    // ---------- helpers ----------

    /**
     * 메모의 D-Day를 계산합니다. (만료일 기준으로만)
     * @return 유효한 D-Day (0 이상), D-Day 정보가 없거나 지났으면 -1
     */
    private int calculateMemoDDay(Memo m) {
        Long expiryAt = m.getExpiryAtLong();
        if (expiryAt != null) {
            int dDay = getDaysUntilExpiry(expiryAt);
            if (dDay >= 0) {
                return dDay; // 만료 기간이 유효하면 해당 D-Day 반환
            }
        }
        // ⭐️ 중요: 만료 기한이 없거나 지났다면 무조건 -1을 반환
        return -1;
    }

    private int dp(float v){
        return Math.round(displayMetrics.density * v);
    }

    @Nullable
    private static Date toDate(@Nullable Object tsOrDate) {
        if (tsOrDate == null) return null;
        if (tsOrDate instanceof Timestamp) return ((Timestamp) tsOrDate).toDate();
        if (tsOrDate instanceof Date) return (Date) tsOrDate;
        if (tsOrDate instanceof Long) return new Date((Long) tsOrDate);
        return null;
    }

    private static String formatMmDd(@Nullable Object tsOrDate) {
        Date d = toDate(tsOrDate);
        if (d == null) return "";
        return new SimpleDateFormat("M/d (E)", Locale.KOREA).format(d);
    }

    private void tintChip(View chip, int color) {
        if (chip == null) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(5));
        gd.setColor(color);
        chip.setBackground(gd);
    }

    private int getDaysUntilExpiry(long expiryAtMillis) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        LocalDate expiryLd = new Date(expiryAtMillis).toInstant().atZone(zoneId).toLocalDate();
        long days = ChronoUnit.DAYS.between(today, expiryLd);
        return (days < 0) ? -1 : (int) days;
    }

    private int getLectureDDay(int dayIndex) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        DayOfWeek targetDayOfWeek = DayOfWeek.of(dayIndex + 1);

        LocalDate nextLectureDate = today.with(TemporalAdjusters.nextOrSame(targetDayOfWeek));
        long days = ChronoUnit.DAYS.between(today, nextLectureDate);

        if (days < 0) {
            nextLectureDate = nextLectureDate.plusWeeks(1);
            days = ChronoUnit.DAYS.between(today, nextLectureDate);
        }
        return (int) days;
    }

    // ✅ 최신 강의 목록 갱신용 메서드
    public void setLectureInfoMap(Map<String, LectureInfo> newMap) {
        this.lectureInfoMap = newMap;
        notifyDataSetChanged(); // 즉시 UI에 반영
    }




}

