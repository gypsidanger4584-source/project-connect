package com.example.login_new.alogic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.*;

public class TimetableView extends View {

    public interface OnItemClickListener {
        void onItemClick(int index, LectureItem lect);
    }

    public interface OnItemLongPressListener {
        void onItemLongPress(LectureItem item);
    }

    public interface OnEmptyCellClickListener {
        void onEmptyCellClick(int dayIndex, int timeMin);
    }

    public interface OnDragFinishListener {
        void onDragFinished(int dayIndex, int startMin, int endMin);
    }

    private OnEmptyCellClickListener emptyCellClickListener;
    private OnDragFinishListener dragFinishListener;
    private OnItemLongPressListener longListener;


    public void setOnEmptyCellClickListener(OnEmptyCellClickListener listener) {
        this.emptyCellClickListener = listener;
    }

    public void setOnDragFinishListener(OnDragFinishListener listener) {
        this.dragFinishListener = listener;
    }


    public static class LectureItem {
        public String id;
        public int dayIndex;
        public int startMin;
        public int endMin;
        public String title = "";
        public String professor = "";
        public String place = "";
        public int color = 0xFF1E88E5;

        public LectureItem() {}

        public LectureItem(int dayIndex, int startMin, int endMin, String title, int color) {
            this.id = UUID.randomUUID().toString();
            this.dayIndex = dayIndex;
            this.startMin = startMin;
            this.endMin = endMin;
            this.title = title;
            this.color = color;
        }
    }

    private final List<LectureItem> items = new ArrayList<>();
    private OnItemClickListener clickListener;


    private static final int START_HOUR = 9;
    private int END_HOUR = 18;
    private static final int DAYS = 5;

    private float hourHeightPx, timeColWidthPx, headerTopPadPx;
    private final Paint hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halfPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint memoDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean dragging = false;
    private boolean downOnBlank = false;
    private int dragDay = -1;
    private int dragStartMin = 0;
    private int dragEndMin = 0;

    private float dragStartX = -1, dragStartY = -1;
    private final float minDragDistancePx;


    private final GestureDetector detector;
    private final Set<String> memoMarkedIds = new HashSet<>();

    public TimetableView(Context c) { this(c, null); }
    public TimetableView(Context c, @Nullable AttributeSet a) { this(c, a, 0); }
    public TimetableView(Context c, @Nullable AttributeSet a, int s) {
        super(c, a, s);
        setLongClickable(true);

        float d = getResources().getDisplayMetrics().density;
        hourHeightPx = 48 * d;
        timeColWidthPx = 48 * d;
        headerTopPadPx = 8 * d;
        minDragDistancePx = 8 * d; // 최소 드래그 거리 8dp 설정

        hourPaint.setColor(0xFFCCCCCC);
        halfPaint.setColor(0xFFE0E0E0);
        vlinePaint.setColor(0xFFDDDDDD);
        textPaint.setColor(0xFF000000);
        textPaint.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
        memoDotPaint.setColor(0xFF007AFF);

        detector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                LectureItem hit = hitTest(e.getX(), e.getY());
                if (hit != null && longListener != null) longListener.onItemLongPress(hit);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // 강의 블록 클릭 처리만 남김
                LectureItem hit = hitTest(e.getX(), e.getY());
                if (hit != null && clickListener != null) {
                    clickListener.onItemClick(0, hit);
                    return true;
                }

                // 빈 칸 탭은 onTouchEvent의 ACTION_UP으로 위임
                return false;
            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener l) { clickListener = l; }
    public void setOnItemLongPressListener(OnItemLongPressListener l) { longListener = l; }


    public List<LectureItem> getItems() { return items; }
    public void addItem(LectureItem it) { items.add(it); requestLayout(); invalidate(); }
    public void updateItem(LectureItem it) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(it.id)) {
                items.set(i, it);
                requestLayout();
                invalidate();
                return;
            }
        }
    }
    public void removeItem(LectureItem it) { items.remove(it); requestLayout(); invalidate(); }

    public void setMemoMarkedIds(Set<String> ids) {
        memoMarkedIds.clear();
        if (ids != null) memoMarkedIds.addAll(ids);
        invalidate();
    }

    private int dp(float v) { return Math.round(getResources().getDisplayMetrics().density * v); }

    private int yToMin(float y) {
        float rel = Math.max(0, y - (headerTopPadPx + getPaddingTop()));
        float totalHours = END_HOUR - START_HOUR;
        return Math.round((rel / (hourHeightPx * totalHours)) * (totalHours * 60));
    }

    private int roundToTenMinutes(int min) { return Math.round(min / 10f) * 10; }

    private int snapToHalfHour(int min) {
        return Math.round(min / 30f) * 30;
    }

    private LectureItem hitTest(float x, float y) {
        float left = timeColWidthPx + dp(6);
        float width = getWidth() - left - getPaddingRight();
        float colW = width / DAYS;

        for (LectureItem it : items) {
            float x0 = left + it.dayIndex * colW;
            float x1 = left + (it.dayIndex + 1) * colW;
            float y0 = headerTopPadPx + getPaddingTop() + (it.startMin / 60f * hourHeightPx);
            float y1 = headerTopPadPx + getPaddingTop() + (it.endMin / 60f * hourHeightPx);
            if (x >= x0 && x <= x1 && y >= y0 && y <= y1) return it;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        float left = timeColWidthPx + dp(6);
        float width = getWidth() - left - getPaddingRight();
        float colW = width / DAYS;
        boolean inGridArea = (x >= left && x <= left + width);
        LectureItem hit = hitTest(x, y);

        // 1. GestureDetector에 전달
        boolean handledByDetector = detector.onTouchEvent(ev);
        if (handledByDetector && !dragging) return true;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (hit == null && inGridArea) {
                    // ⭐️ 부모 뷰가 스크롤 이벤트를 가로채지 못하도록 막음 (독점 시작)
                    getParent().requestDisallowInterceptTouchEvent(true);

                    downOnBlank = true;
                    dragDay = (int) ((x - left) / colW);
                    dragStartX = x;
                    dragStartY = y;
                    dragStartMin = snapToHalfHour(yToMin(y));
                    dragEndMin = dragStartMin;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (downOnBlank) {
                    float absDx = Math.abs(x - dragStartX); // 절대 수평 이동 거리
                    float absDy = Math.abs(y - dragStartY); // 절대 수직 이동 거리
                    float distance = (float) Math.sqrt(absDx * absDx + absDy * absDy); // 절대 거리

                    if (!dragging && distance >= minDragDistancePx) {

                        // ⭐️⭐️ [핵심 수정: 임계값 50.0f로 대폭 상향] ⭐️⭐️
                        // 수직 이동이 수평 이동의 50배를 초과해야만 ScrollView 스크롤로 간주하여,
                        // Time Drag (수직) 의도의 우선순위를 최대한 확보합니다.
                        if (absDy > absDx * 50.0f) {
                            // ScrollView Scroll 의도: 부모에게 스크롤 권한 반환
                            getParent().requestDisallowInterceptTouchEvent(false);
                            downOnBlank = false; // 현재 터치 세션 종료
                            return false; // 이벤트 처리를 포기하고 상위 뷰로 넘김 (스크롤 시작)
                        }

                        // 드래그 임계값 도달: 드래그 시작 (Time Drag로 간주)
                        dragging = true;
                        // 최소 30분 영역을 미리보기로 표시
                        dragEndMin = dragStartMin + 30;
                        invalidate();
                    }

                    if (dragging) {
                        // 드래그 중인 경우에만 미리보기 업데이트
                        int currentMin = yToMin(y);
                        int snappedMin = snapToHalfHour(currentMin);
                        dragEndMin = snappedMin;
                        invalidate();
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (downOnBlank) {
                    // ⭐️ 터치 처리가 끝났으므로 부모 뷰가 다시 가로챌 수 있도록 허용
                    getParent().requestDisallowInterceptTouchEvent(false);
                    downOnBlank = false;

                    if (dragging) {
                        // 1. 드래그가 활성화되었던 경우 (미리보기가 떴던 경우)
                        dragging = false;

                        int finalStart = Math.min(dragStartMin, dragEndMin);
                        int finalEnd = Math.max(dragStartMin, dragEndMin);
                        int duration = finalEnd - finalStart;

                        if (duration > 0 && dragFinishListener != null) {
                            dragFinishListener.onDragFinished(dragDay, finalStart, finalEnd);
                        }

                        dragDay = -1;
                        invalidate();
                        return true;
                    } else {
                        // 2. 드래그가 활성화되지 않았던 경우 (순수한 짧은 탭)
                        if (emptyCellClickListener != null) {
                            int dayIndex = (int) ((dragStartX - left) / colW);
                            int timeMin = roundToTenMinutes(yToMin(dragStartY));

                            if (dayIndex >= 0 && dayIndex < DAYS) {
                                emptyCellClickListener.onEmptyCellClick(dayIndex, timeMin);
                            }
                        }
                        return true;
                    }
                }
                break;
        }

        return super.onTouchEvent(ev);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int latestEndMin = 0;
        for (LectureItem it : items) {
            if (it.endMin > latestEndMin) latestEndMin = it.endMin;
        }
        int latestHour = START_HOUR + (latestEndMin / 60);
        END_HOUR = Math.max(18, latestHour + 1);

        float totalHours = END_HOUR - START_HOUR;
        int desiredHeight = Math.round(hourHeightPx * totalHours + headerTopPadPx + getPaddingTop() + getPaddingBottom());
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, desiredHeight);
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);

        float left = timeColWidthPx + dp(6);
        float top = headerTopPadPx + getPaddingTop();
        float width = getWidth() - left - getPaddingRight();
        float totalHours = END_HOUR - START_HOUR;
        float contentHeight = hourHeightPx * totalHours;
        float colW = width / DAYS;

        // 시간 라벨 및 그리드 라인 그리기
        for (int h = START_HOUR; h <= END_HOUR; h++) {
            float y = top + (h - START_HOUR) * hourHeightPx;
            cv.drawLine(left, y, left + width, y, hourPaint);
            String label = (h < 12 ? h + " AM" : (h == 12 ? "12 PM" : (h - 12) + " PM"));
            cv.drawText(label, dp(4) + getPaddingLeft(), y + dp(4), textPaint);
        }

        for (int i = 0; i <= DAYS; i++) {
            float x = left + i * colW;
            cv.drawLine(x, top, x, top + contentHeight, vlinePaint);
        }

        // 드래그 하이라이트
        if (dragging && dragDay >= 0 && dragDay < DAYS) {
            float x0 = left + dragDay * colW;
            float x1 = left + (dragDay + 1) * colW;
            float dragMin = Math.min(dragStartMin, dragEndMin);
            float dragMax = Math.max(dragStartMin, dragEndMin);

            float y0 = top + (dragMin / 60f * hourHeightPx);
            float y1 = top + (dragMax / 60f * hourHeightPx);

            Paint sel = new Paint(Paint.ANTI_ALIAS_FLAG);
            sel.setColor(0x443F51B5);
            cv.drawRect(x0, y0, x1, y1, sel);
        }

        // 강의 블록 그리기
        for (LectureItem it : items) {
            if (it.dayIndex < 0 || it.dayIndex >= DAYS) continue;
            float x0 = left + it.dayIndex * colW;
            float x1 = left + (it.dayIndex + 1) * colW;
            float y0 = top + (it.startMin / 60f * hourHeightPx);
            float y1 = top + (it.endMin / 60f * hourHeightPx);

            RectF r = new RectF(x0, y0, x1, y1);
            blockPaint.setColor(it.color);
            cv.drawRoundRect(r, dp(8), dp(8), blockPaint);

            if (memoMarkedIds.contains(it.id)) {
                cv.drawCircle(r.right - dp(8), r.top + dp(8), dp(5), memoDotPaint);
            }

            drawLectureText(cv, r, it);
        }
    }

    private void drawLectureText(Canvas cv, RectF r, LectureItem it) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xFFFFFFFF);
        tp.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
        tp.setFakeBoldText(true);

        float blockHeight = r.height();
        float oneLineThreshold = dp(60);

        String info;
        if (blockHeight < oneLineThreshold) {
            info = it.title;
        } else {
            info = it.title + "\n" + it.professor + "\n" + it.place;
        }

        int pad = dp(6);
        int availW = Math.max(0, Math.round(r.width()) - pad * 2);

        StaticLayout layout = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ? StaticLayout.Builder.obtain(info, 0, info.length(), tp, availW)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
                : new StaticLayout(info, tp, availW, Layout.Alignment.ALIGN_CENTER, 1f, 0f, false);

        cv.save();
        cv.translate(r.left + pad, r.top + (r.height() - layout.getHeight()) / 2f);
        layout.draw(cv);
        cv.restore();
    }


    public int getGridLeftPx() {
        return Math.round(timeColWidthPx + dp(6)) + getPaddingLeft();
    }

    public void clearAll() {
        if (items != null) {
            items.clear();
        }
        invalidate();
    }
}