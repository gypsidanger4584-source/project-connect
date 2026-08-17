package com.example.login_new.alogic;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.login_new.R;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;            // customView 타입이 View라면
import android.view.ViewGroup;
import android.view.Window;

import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Locale;

// ⭐️ WorkManager 및 권한 관련 Import 추가 ⭐️
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class TimetableActivity extends AppCompatActivity
        implements MemoAdapter.OnMemoLongClickListener {

    private TimetableView timetableView;

    @ColorInt
    private final int[] palette = new int[]{
            0xFFD7E5FF, 0xFFEAD9FF, 0xFFD8F9DD, 0xFFFCEFD5, 0xFFFCE4EC, 0xFFC9F4FF
    };

    private TextView tvTodayStatus;
    private TextView tvNextClass;

    private TextView[] dateTvs;
    private RecyclerView memoList;
    private MemoAdapter memoAdapter;
    private TimetableDatabase db;
    private TimetableDao dao;

    private final List<MemoAdapter.Memo> memos = new ArrayList<>();
    private final Set<String> lectureIdsWithMemo = new HashSet<>();
    private final Map<String, Integer> colorMap = new HashMap<>();

    // 🔹 LiveData 변환용 MemoEntity → MemoAdapter.Memo
    private MemoAdapter.Memo toUiMemo(MemoEntity e) {
        MemoAdapter.Memo m = new MemoAdapter.Memo();
        m.id = UUID.randomUUID().toString();  // int → String으로 변환
        m.lectureId = e.lectureId;
        m.text = e.text;
        m.createdAt = e.createdAt;
        m.isImportant = e.isImportant;
        m.setExpanded(false);
        m.expiryAt = e.expiryAt;
        return m;
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private int dp(float v){
        DisplayMetrics m = getResources().getDisplayMetrics();
        return Math.round(m.density * v);
    }

    private int roundToTenMinutes(int min){
        return Math.round(min/10f) * 10;
    }

    private String formatExpiry(Long expiryMillis) {
        if (expiryMillis == null) return "만료 기간 없음";
        return new SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREA).format(new Date(expiryMillis));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void scheduleReminderWork() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "TimetableReminder",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        timetableView = findViewById(R.id.timetableView);

        TimetableDatabase db = TimetableDatabase.getDatabase(this);
        TimetableDao dao = db.timetableDao();
        this.db = db;
        this.dao = dao;

// 메모 리사이클러뷰
        memoList = findViewById(R.id.memoList);
        if (memoList != null) {
            memoList.setLayoutManager(new LinearLayoutManager(this));
            // memoAdapter 생성 시 createLectureInfoMap() 호출 필요 (메서드는 전체 코드에 있음)
            memoAdapter = new MemoAdapter(this, createLectureInfoMap(), this);
            memoList.setAdapter(memoAdapter);
        }

        loadSavedData();



        // 기본 UI 설정
        int bottomPadding = dp(20);
        int horizontalPadding = dp(10);
        int topPadding = dp(10);
        timetableView.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);

        timetableView.setOnItemClickListener((index, lect) -> showLectureDetailsDialog(lect));

        timetableView.setOnEmptyCellClickListener((dayIndex, timeMin) -> {

            int absoluteMin = timeMin + (9 * 60);
            // 🔹 클릭된 상대 시간(분)을 정시 단위로 내림
            int hour = absoluteMin / 60;

            // ✅ 항상 xx:30 시작으로 고정

            int startAbsoluteMin = hour * 60 + 30;
            // ✅ 50분짜리 강의
            int endAbsoluteMin = startAbsoluteMin + 50;

            // 다시 9:00 기준 상대분으로 변환
            int startFrom0900 = startAbsoluteMin - (9 * 60);
            int endFrom0900 = endAbsoluteMin - (9 * 60);

            // ✅ 다이얼로그 호출
            showInputDialog(null, dayIndex, startFrom0900, endFrom0900);
        });
        // 🚀 [추가] 드래그 완료 리스너 (30분 단위 스냅 결과 사용)
        timetableView.setOnDragFinishListener((dayIndex, startMin, endMin) -> {
            // TimetableView에서 30분 단위로 스냅된 startMin, endMin 값을 그대로 showInputDialog에 전달
            showInputDialog(null, dayIndex, startMin, endMin);
        });






        tvTodayStatus = findViewById(R.id.tvTodayStatus);
        tvNextClass = findViewById(R.id.tvNextClass);
        setupTodayStatus();
        updateNextClassStatus();

        dateTvs = new TextView[]{
                findViewById(R.id.tvD0),
                findViewById(R.id.tvD1),
                findViewById(R.id.tvD2),
                findViewById(R.id.tvD3),
                findViewById(R.id.tvD4)
        };
        setupDateRow();
        timetableView.post(this::alignDateRowToGrid);




        timetableView.setOnItemClickListener((index, lect) -> showLectureDetailsDialog(lect));
        timetableView.setOnItemLongPressListener(this::showItemActionDialog);


        requestNotificationPermission();
        scheduleReminderWork();
    }


    private LectureEntity toEntity(TimetableView.LectureItem item) {
        LectureEntity e = new LectureEntity();

        if (item.id == null || item.id.isEmpty()) {
            item.id = UUID.randomUUID().toString();
        }
        e.id = item.id; // LectureEntity와 TimetableView.LectureItem의 PK 연결

        e.dayIndex = item.dayIndex;
        e.startMin = item.startMin;
        e.endMin = item.endMin;
        e.title = item.title;
        e.professor = item.professor;
        e.place = item.place;
        e.color = item.color;
        return e;
    }




    private MemoEntity toEntity(MemoAdapter.Memo m) {
        MemoEntity e = new MemoEntity();
        e.id = m.id;
        e.lectureId = m.lectureId;
        e.text = m.text;
        e.createdAt = (long) m.createdAt;
        e.expiryAt = m.getExpiryAtLong();
        e.isImportant = m.isImportant;
        return e;
    }

    // ✅ DB → UI 로드
    private void loadSavedData() {
        // 1️⃣ 강의 목록 불러오기
        List<LectureEntity> lectures = dao.getAllLectures();
        Set<String> validLectureIds = new HashSet<>();

        timetableView.clearAll(); // 기존 강의 초기화
        for (LectureEntity e : lectures) {
            TimetableView.LectureItem item =
                    new TimetableView.LectureItem(e.dayIndex, e.startMin, e.endMin, e.title, e.color);
            item.professor = e.professor;
            item.place = e.place;
            item.id = e.id;
            timetableView.addItem(item);
            validLectureIds.add(e.id);
        }

        // 2️⃣ 메모 불러오기 (삭제된 강의 메모는 무시)
        List<MemoEntity> memoEntities = dao.getAllMemos();
        memos.clear();
        for (MemoEntity e : memoEntities) {
            if (!validLectureIds.contains(e.lectureId)) {
                // 💡 유효하지 않은 메모는 DB에서도 삭제
                new Thread(() -> dao.deleteMemoById(e.id)).start();
                continue;
            }

            MemoAdapter.Memo m = new MemoAdapter.Memo();
            m.id = e.id;
            m.lectureId = e.lectureId;
            m.text = e.text;
            m.createdAt = e.createdAt;
            m.isImportant = e.isImportant;
            m.expiryAt = e.expiryAt;
            m.setExpanded(false);
            memos.add(m);
        }

        memoAdapter.submitList(new ArrayList<>(memos));
        rebuildMemoMarkers();
    }





    @Override
    protected void onResume() {
        super.onResume();
        pruneExpiredMemos();
        updateNextClassStatus();
        // onResume 시에도 Adapter에 최신 Map을 반영하고 리스트를 제출
        if (memoAdapter != null) {
            memoAdapter = new MemoAdapter(this, createLectureInfoMap(), this);
            memoList.setAdapter(memoAdapter);
            memoAdapter.submitList(memos);
        }
        // ⭐️⭐️⭐️ WorkManager 작업 재-예약 (KEEP 정책으로 중복 방지) ⭐️⭐️⭐️
        scheduleReminderWork();
    }

    /**
     * 현재 TimetableView의 강의 목록을 MemoAdapter.LectureInfo Map으로 변환
     */
    private Map<String, MemoAdapter.LectureInfo> createLectureInfoMap() {
        Map<String, MemoAdapter.LectureInfo> map = new HashMap<>();
        if (timetableView.getItems() != null) {
            for (TimetableView.LectureItem item : timetableView.getItems()) {
                map.put(item.id, new MemoAdapter.LectureInfo(item.title, item.color, item.dayIndex));
            }
        }
        return map;
    }

    /**
     * 메모 목록 변경 후 Adapter에 제출하는 공통 로직
     * 메모가 추가/수정/삭제될 때마다 호출됩니다.
     */
    private void updateMemoAdapter() {
        if (memoAdapter == null) {
            // 어댑터 최초 생성
            memoAdapter = new MemoAdapter(this, createLectureInfoMap(), this);
            memoList.setAdapter(memoAdapter);
        } else {
            // 🟢 기존 어댑터에 최신 강의 정보 맵 갱신
            memoAdapter.setLectureInfoMap(createLectureInfoMap());
            memoAdapter.submitList(new ArrayList<>(memos));
        }

        rebuildMemoMarkers(); // 메모 마커 재생성
    }




    // 🌟 메모 롱클릭 시 호출됨 (커스텀 다이얼로그로 편집/삭제/취소 표시)
    @Override
    public void onMemoLongClick(MemoAdapter.Memo memo, int position) {

        // 1. 커스텀 뷰 인플레이션
        LayoutInflater inflater = getLayoutInflater();
        View customView = inflater.inflate(R.layout.dialog_memo_option, null);

        // 2. 뷰 바인딩
        TextView dialogTitle = customView.findViewById(R.id.dialogTitle);
        Button btnCancel = customView.findViewById(R.id.btnCancel);
        Button btnDelete = customView.findViewById(R.id.btnDelete);
        Button btnEdit = customView.findViewById(R.id.btnEdit);

        // 3. 제목 설정: 메모 내용 요약을 사용해 어떤 메모인지 표시
        String shortText = memo.getText().length() > 20
                ? memo.getText().substring(0, 20) + "..."
                : memo.getText();

        dialogTitle.setText(shortText); // 제목 자리에 메모 내용을 넣습니다.

        // 4. AlertDialog 생성 (커스텀 뷰 사용)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(customView);

        AlertDialog dialog = builder.create();

        // 5. 버튼 리스너 설정
        // [삭제] 버튼 로직
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(memo, position); // 삭제 확인 다이얼로그 호출
            dialog.dismiss(); // 옵션 다이얼로그 닫기
        });

        // [취소] 버튼 로직
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // [편집] 버튼 로직
        btnEdit.setOnClickListener(v -> {
            showMemoInputDialogForEdit(memo);
            dialog.dismiss();
        });

        // 6. 다이얼로그 표시
        dialog.show();
    }


    // 시간표 눈금과 날짜 헤더 정렬
    private void alignDateRowToGrid(){
        View row = findViewById(R.id.dateRow);
        if (row == null || timetableView == null) return;
        int left = timetableView.getGridLeftPx();
        row.setPadding(left, row.getPaddingTop(), row.getPaddingRight(), row.getPaddingBottom());
    }

    // 강의 블록 롱클릭 시 나오는 메뉴 (편집/삭제/메모 추가)
    private void showItemActionDialog(TimetableView.LectureItem item){
        String[] actions = {"편집", "삭제", "메모"};
        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) { // 편집
                        showInputDialog(item, item.dayIndex, item.startMin, item.endMin);
                    } else if (which == 1) { // 삭제
                        timetableView.removeItem(item);
                        dao.deleteLecture(toEntity(item));
                        // 해당 강의에 연결된 모든 메모 삭제
                        Iterator<MemoAdapter.Memo> it = memos.iterator();
                        boolean changed = false;
                        while (it.hasNext()){
                            MemoAdapter.Memo m = it.next();
                            if (item.id.equals(m.lectureId)) {
                                it.remove(); changed = true;
                            }
                        }
                        if (changed) updateMemoAdapter();
                        rebuildMemoMarkers();
                        updateNextClassStatus();
                    } else if (which == 2) { // 메모
                        showMemoDialog(item);
                    }
                })
                .show();
    }

    // 커스텀 시간 선택 다이얼로그
// ⭐️⭐️⭐️ [TimePicker 및 커스텀 버튼을 사용하는 시간 선택 다이얼로그 함수] ⭐️⭐️⭐️
    private void openCustomTimePicker(final int[] minutesHolder, final Button targetButton) {
        // 1. XML에서 View 인플레이트
        View customView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null, false);

        // 2. XML ID에 맞춰 뷰 요소 찾기
        TimePicker timePicker = customView.findViewById(R.id.timePicker);
        TextView tvSelectedTime = customView.findViewById(R.id.tvSelectedTime);
        Button btnConfirm = customView.findViewById(R.id.btnConfirm);
        Button btnCancel = customView.findViewById(R.id.btnCancel);

        // TimePicker 설정 (24시간 형식 강제)
        timePicker.setIs24HourView(true);

        // ⭐️ 시간 제약 조건 정의
        final int MIN_HOUR = 9; // 시작 시간은 9시로 유지
        final int MAX_HOUR = 23; // ⭐️ [로직 추가] 마지노선 시간은 23시
        final int TIMETABLE_START_HOUR = MIN_HOUR;
        final int TIMETABLE_START_MINUTES = TIMETABLE_START_HOUR * 60;

        // 3. 초기 시간 설정
        int absoluteMin = TIMETABLE_START_MINUTES + minutesHolder[0];
        // roundToTenMinutes 헬퍼 함수 사용 가정
        absoluteMin = roundToTenMinutes(absoluteMin);

        int initH = absoluteMin / 60;
        int initM = absoluteMin % 60;

        // ⭐️ [로직 추가] 초기값이 9시 ~ 23시 범위를 벗어나지 않도록 보정
        if (initH < MIN_HOUR) initH = MIN_HOUR;
        if (initH > MAX_HOUR) {
            initH = MAX_HOUR;
            initM = 50; // 최대 시간인 23시 50분으로 설정
        }

        // 초기값 TimePicker에 반영
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.setHour(initH);
            timePicker.setMinute(initM);
        } else {
            timePicker.setCurrentHour(initH);
            timePicker.setCurrentMinute(initM);
        }

        // 초기 선택된 시간 텍스트 설정
        String initialTimeLabel = String.format(Locale.getDefault(), "%02d:%02d", initH, initM);
        tvSelectedTime.setText(initialTimeLabel);


        // 4. TimePicker 값 변경 리스너 설정
        TimePicker.OnTimeChangedListener timeChangeListener = (view, hourOfDay, minute) -> {
            // ⭐️ [10분 단위 로직] 분을 10분 단위로 스냅하여 텍스트 뷰에 표시
            int snappedMinute = (minute / 10) * 10;

            // ⭐️ [23시까지 로직] 시간도 범위(9시~23시)를 벗어나면 텍스트를 보정하여 표시
            int displayedHour = hourOfDay;
            if (displayedHour < MIN_HOUR) displayedHour = MIN_HOUR;
            if (displayedHour > MAX_HOUR) displayedHour = MAX_HOUR;

            String newTimeLabel = String.format(Locale.getDefault(), "%02d:%02d", displayedHour, snappedMinute);
            tvSelectedTime.setText(newTimeLabel);
        };

        timePicker.setOnTimeChangedListener(timeChangeListener);


        // 5. 커스텀 다이얼로그 생성
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(customView);

        // 다이얼로그 스타일 설정 (투명 배경, 크기 등)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setDimAmount(0.35f);
        }


        // 6. 버튼 리스너 설정
        // [확인] 버튼 로직
        btnConfirm.setOnClickListener(v -> {
            int finalH;
            int finalM;

            // 최종 선택된 시간 가져오기
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                finalH = timePicker.getHour();
                finalM = timePicker.getMinute();
            } else {
                finalH = timePicker.getCurrentHour();
                finalM = timePicker.getCurrentMinute();
            }

            // ⭐️ [23시까지 로직] 최종 시간 제한 (9시 ~ 23시) 적용
            if (finalH < MIN_HOUR) {
                finalH = MIN_HOUR;
                finalM = 0;
            }
            if (finalH > MAX_HOUR) {
                finalH = MAX_HOUR;
                finalM = 50; // 최대 시간인 23시 50분으로 제한
            }

            // ⭐️ [10분 단위 로직] 최종 분 값을 10분 단위로 스냅하여 적용
            int snappedFinalM = (finalM / 10) * 10;

            // 09:00 AM (START_HOUR) 기준으로 상대적인 분을 계산
            minutesHolder[0] = (finalH * 60 + snappedFinalM) - TIMETABLE_START_MINUTES;

            targetButton.setText(minToLabel(minutesHolder[0]));
            dialog.dismiss();
        });

        // [취소] 버튼 로직
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // 분 단위 시간을 "HH:MM" 형식으로 변환 (minFrom0900은 09:00 기준의 분)
    private String minToLabel(int minFrom0900){
        int total = 9*60 + minFrom0900;
        int hh = total / 60, mm = total % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hh, mm);
    }

    // TODAY 상태 표시줄 설정
    private void setupTodayStatus() {
        if (tvTodayStatus == null) return;

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String datePart = String.format(Locale.getDefault(),
                "%d/%02d (%s)",
                today.getMonthValue(),
                today.getDayOfMonth(),
                today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).toUpperCase(Locale.ROOT));

        String todayText = "TODAY";
        String combined = todayText + " " + datePart;

        SpannableString ss = new SpannableString(combined);

        final float largeTextSizeSp = 20f;
        final float smallTextSizeSp = largeTextSizeSp - 4f;

        int todayLength = todayText.length();

        ss.setSpan(new StyleSpan(Typeface.BOLD), 0, todayLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new AbsoluteSizeSpan((int) largeTextSizeSp, true), 0, todayLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new StyleSpan(Typeface.NORMAL), todayLength, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new AbsoluteSizeSpan((int) smallTextSizeSp, true), todayLength, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvTodayStatus.setText(ss);
        tvTodayStatus.setTextColor(0xFF000000);

    }

    // ⭐️ 다음 수업 시간 계산 및 표시 (기존 로직 유지)
    private void updateNextClassStatus() {
        if (tvNextClass == null) return;

        final int TIMETABLE_START_HOUR = 9;
        final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

        LocalDateTime nowDateTime = LocalDateTime.now(KOREA_ZONE);
        // 오늘의 23:59:59.999 시점 (오늘 수업 종료 체크용)
        LocalDateTime endOfToday = nowDateTime.toLocalDate().atTime(23, 59, 59, 999999999);

        long minToNextClass = Long.MAX_VALUE;
        boolean foundClass = false;
        LocalDateTime nextClassDateTime = null; // 가장 가까운 다음 수업 시간을 추적하기 위한 변수

        for (TimetableView.LectureItem item : timetableView.getItems()) {

            int classDayValue = DayOfWeek.MONDAY.getValue() + item.dayIndex;
            if (classDayValue > DayOfWeek.FRIDAY.getValue()) continue;

            int classStartMinAbs = item.startMin + (TIMETABLE_START_HOUR * 60);
            LocalTime classStartTime = LocalTime.of(classStartMinAbs / 60, classStartMinAbs % 60);

            LocalDate classDate;
            classDate = nowDateTime.toLocalDate().with(DayOfWeek.of(classDayValue));

            // 이미 지난 수업이라면 다음 주로 넘김
            if (classDate.isEqual(nowDateTime.toLocalDate()) && classStartTime.isBefore(nowDateTime.toLocalTime())) {
                classDate = classDate.plusWeeks(1);
            }
            // 현재보다 이전 요일이라면 다음 주로 넘김
            else if (classDate.isBefore(nowDateTime.toLocalDate())) {
                classDate = nowDateTime.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.of(classDayValue)));
            }

            LocalDateTime classDateTime = LocalDateTime.of(classDate, classStartTime);

            if (classDateTime.isAfter(nowDateTime.minusMinutes(1))) {
                long minutesDiff = ChronoUnit.MINUTES.between(nowDateTime, classDateTime);

                if (minutesDiff < minToNextClass) {
                    minToNextClass = minutesDiff;
                    foundClass = true;
                    nextClassDateTime = classDateTime; // 가장 가까운 다음 수업 시간 기록
                }
            }
        }

        // 최종 텍스트와 강조할 숫자 값
        String finalStatusText;
        long valueForSpan = 0;

        if (foundClass) {

            // 오늘이 주중이고 (월~금) && 밤 12시가 되기 전(nowDateTime.isBefore(endOfToday)) &&
            // 가장 빠른 다음 수업이 오늘이 아닌 경우 (nextClassDateTime.toLocalDate().isAfter(nowDateTime.toLocalDate()))
            boolean isTodayWeekday = nowDateTime.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue();
            boolean isNextClassLater = nextClassDateTime.toLocalDate().isAfter(nowDateTime.toLocalDate());

            if (isTodayWeekday && nowDateTime.isBefore(endOfToday) && isNextClassLater) {
                // ⭐️ 오늘 수업이 모두 끝났고 밤 12시 전인 경우
                finalStatusText = "No Class Today";
            }
            // ⭐️ 그 외 모든 경우 (오늘 수업이 남았거나, 밤 12시가 넘었거나, 주말인 경우)
            else if (minToNextClass > 60 * 24 * 7) {
                finalStatusText = "No Class This Week";
            } else if (minToNextClass > 60 * 24) {
                valueForSpan = minToNextClass / (60 * 24) + 1;
                finalStatusText = String.format(Locale.getDefault(), "Next Class in %d days", valueForSpan);
            } else if (minToNextClass > 60 * 2) {
                valueForSpan = minToNextClass / 60;
                finalStatusText = String.format(Locale.getDefault(), "Next Class in %d hr", valueForSpan);
            } else {
                valueForSpan = minToNextClass;
                finalStatusText = String.format(Locale.getDefault(), "Next Class in %d min", valueForSpan);
            }
        } else {
            // 수업 일정이 전혀 없는 경우 (토/일은 자동으로 No Class Today에 해당)
            finalStatusText = "No Class Today";
        }

        // ⭐️ 텍스트에 SpannableString 적용 및 출력 (숫자 흰색 강조)
        if (valueForSpan > 0) {
            SpannableString ss = new SpannableString(finalStatusText);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = pattern.matcher(finalStatusText);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                // 💡 숫자를 강조 색상으로 변경 (예: 파란색)
                ss.setSpan(new ForegroundColorSpan(0xFF1E88E5), // 🔵 연성대 블루톤 추천
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            }
            tvNextClass.setText(ss);
        } else {
            tvNextClass.setText(finalStatusText);
        }
    }

    // 상단 날짜 헤더 설정
    private void setupDateRow() {
        if (dateTvs == null) return;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        final String[] wd = {"MON","TUE","WED","THU","FRI"};
        for (int i=0;i<5;i++) {
            LocalDate d = monday.plusDays(i);
            String text = wd[i];
            dateTvs[i].setText(text);
            dateTvs[i].setGravity(Gravity.CENTER);
            boolean isToday = d.equals(today);
            dateTvs[i].setTextColor(isToday ? 0xFF007AFF : 0xFF888888);
            dateTvs[i].setAlpha(isToday ? 1f : 0.95f);
        }
    }

    // 강의 블록 겹침 확인
    private boolean hasOverlap(int dayIndex, int startMin, int endMin, String ignoreId){
        for (TimetableView.LectureItem it : timetableView.getItems()) {
            if (ignoreId != null && ignoreId.equals(it.id)) continue;
            if (it.dayIndex != dayIndex) continue;
            if (startMin < it.endMin && it.startMin < endMin) return true;
        }
        return false;
    }

    // 강의 제목으로 색상 결정 및 매핑 저장
    private int colorForTitle(String title, int userPick){
        if (title == null || title.isEmpty()) return userPick;
        String key = normalizeTitle(title);

        Integer saved = colorMap.get(key);
        if (saved != null) return saved;

        colorMap.put(key, userPick);
        return userPick;
    }

    // 메모 작성 다이얼로그 표시
    private void showMemoDialog(TimetableView.LectureItem lect){
        View v = getLayoutInflater().inflate(R.layout.dialog_memo_input, null, false);

        // 뷰 바인딩
        TextView labelTitle = v.findViewById(R.id.labelTitle);
        EditText etMemo   = v.findViewById(R.id.etMemoText);
        Spinner spExpire  = v.findViewById(R.id.spExpire);
        Button btnCancel  = v.findViewById(R.id.btnCancel);
        Button btnSave    = v.findViewById(R.id.btnSave);
        ImageView ivStar  = v.findViewById(R.id.ivImportantStar);

        // ⭐️ 별표 상태 관리 변수
        final boolean[] isImportant = {false};
        // ⭐️ 사용자 지정 일수를 임시 저장할 메모 인스턴스
        final MemoAdapter.Memo tempMemo = new MemoAdapter.Memo();


        // ⭐️ 별표 클릭 리스너: 상태 토글 및 아이콘 업데이트
        ivStar.setOnClickListener(vClick -> {
            isImportant[0] = !isImportant[0];
            updateStarIcon(ivStar, isImportant[0]);
        });
        updateStarIcon(ivStar, isImportant[0]); // 초기 상태 업데이트

        // ⭐️⭐️⭐️ D-Day 스피너 옵션
        final String[] options = {"만료 없음", "1일", "3일", "7일", "사용자 지정"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options
        );
        spExpire.setAdapter(spinAdapter);
        spExpire.setSelection(0); // 만료 없음 기본 선택

        // ⭐️ 사용자 지정 선택 시 처리
        spExpire.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == options.length - 1) { // '사용자 지정'을 선택한 경우 (인덱스 4)
                    // tempMemo의 customDays를 null로 초기화 후 다이얼로그 호출
                    tempMemo.customDays = null;
                    openCustomDaysDialog(tempMemo, spExpire);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        btnSave.setOnClickListener(view -> {
            String txt = etMemo.getText().toString().trim();
            if (txt.isEmpty()) { Toast.makeText(this, "메모를 입력하세요.", Toast.LENGTH_SHORT).show(); return; }

            int sel = spExpire.getSelectedItemPosition();

            // ⭐️ 사용자 지정이 선택되었는데 일수가 없는 경우 처리
            if (sel == 4 && tempMemo.customDays == null) {
                Toast.makeText(this, "사용자 지정 일수를 입력해야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            MemoAdapter.Memo m = new MemoAdapter.Memo();
            m.id = UUID.randomUUID().toString();
            m.lectureId = lect.id;
            m.text = txt;
            m.createdAt = System.currentTimeMillis();
            m.isImportant = isImportant[0];
            m.setExpanded(false);

            // ⭐️ customDays: 사용자 지정 선택 시 tempMemo에서 가져옴 (다른 경우 null)
            m.customDays = (sel == 4) ? tempMemo.customDays : null;

            // ⭐️ 만료 기간 계산 로직 호출
            Long expiryLong = calcExpire(sel, m.customDays);
            m.expiryAt = expiryLong;


            memos.add(0, m);
            memoAdapter.submitList(new ArrayList<>(memos));
            new Thread(() -> dao.insertMemo(toEntity(m))).start();
            // ✅ DB에만 추가
            dialog.dismiss();
            rebuildMemoMarkers();       // 🔹 점 표시 데이터 즉시 갱신
            timetableView.invalidate(); // 🔹 UI 즉시 다시 그리기
// ✅ 나머지 유지

        });

        dialog.show();
    }

    // ⭐️ 사용자 지정 일수 입력 다이얼로그 (Memo 인스턴스를 직접 업데이트)
    private void openCustomDaysDialog(MemoAdapter.Memo memoToUpdate, Spinner spExpire) {
        // ⭐️⭐️⭐️ 새로운 XML 레이아웃 사용: dialog_custom_days_picker.xml ⭐️⭐️⭐️
        View v = getLayoutInflater().inflate(R.layout.dialog_custom_days_picker, null, false);
        final NumberPicker npDays = v.findViewById(R.id.npDays);

        // NumberPicker 설정 (1일 ~ 30일)
        npDays.setMinValue(1);
        npDays.setMaxValue(30);
        npDays.setWrapSelectorWheel(false); // 휠 순환 방지

        // 기존 값이 있으면 해당 값으로 초기화 (기본값은 1)
        int initialDays = (memoToUpdate.customDays != null && memoToUpdate.customDays >= 1 && memoToUpdate.customDays <= 30)
                ? memoToUpdate.customDays : 1;
        npDays.setValue(initialDays);

        // 확인 버튼을 눌렀을 때만 선택된 일수를 저장합니다.
        new AlertDialog.Builder(this)
                .setTitle("만료 기간 설정")
                .setView(v)
                .setPositiveButton("확인", (d, w) -> {
                    int days = npDays.getValue();

                    // ⭐️ 선택된 일수를 Memo 인스턴스에 직접 저장
                    memoToUpdate.customDays = days;
                })
                .setNegativeButton("취소", (d, w) -> {
                    // ⭐️ 취소 시: customDays를 null로 만들고, 스피너를 '만료 없음' (인덱스 0)으로 되돌립니다.
                    memoToUpdate.customDays = null;
                    spExpire.setSelection(0);
                })
                .show();
    }


    // ⭐️ 별표 아이콘 상태 업데이트 헬퍼
    private void updateStarIcon(ImageView ivStar, boolean isImportant) {
        if (isImportant) {
            ivStar.setImageResource(R.drawable.baseline_star_24); // 꽉 찬 별 (반드시 리소스에 있어야 함)
            ivStar.setColorFilter(0xFFFFC107); // 노란색
        } else {
            ivStar.setImageResource(R.drawable.baseline_star_border_24); // 테두리 별
            ivStar.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN); // 흰색
        }
    }


    /** 메모 수정 다이얼로그 */
    private void showMemoInputDialogForEdit(MemoAdapter.Memo memoToEdit) {
        View v = getLayoutInflater().inflate(R.layout.dialog_memo_input, null, false);

        // 뷰 초기화 및 바인딩
        TextView labelTitle = v.findViewById(R.id.labelTitle);
        EditText etMemo   = v.findViewById(R.id.etMemoText);
        Spinner spExpire  = v.findViewById(R.id.spExpire);
        Button btnCancel  = v.findViewById(R.id.btnCancel);
        Button btnSave    = v.findViewById(R.id.btnSave);
        LinearLayout footerRow = v.findViewById(R.id.footerRow);
        ImageView ivStar  = v.findViewById(R.id.ivImportantStar);

        // ⭐️ 기존 중요도 상태 로드
        final boolean[] isImportant = {memoToEdit.isImportant};
        // ⭐️ 메모 수정 시에도 임시 저장용으로 사용 (memoToEdit 인스턴스 자체를 사용)

        // 1. 타이틀 및 내용 설정 (기존 내용 유지)
        labelTitle.setText("메모 수정");
        etMemo.setText(memoToEdit.text);

        // ⭐️ 별표 클릭 리스너 및 초기 상태 업데이트
        ivStar.setOnClickListener(vClick -> {
            isImportant[0] = !isImportant[0];
            updateStarIcon(ivStar, isImportant[0]);
        });
        updateStarIcon(ivStar, isImportant[0]); // 초기 상태 적용

        // 2. 만료 기간 스피너 설정
        final String[] options = {"만료 없음", "1일", "3일", "7일", "사용자 지정"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options
        );
        spExpire.setAdapter(spinAdapter);

        // ⭐️ 수정 시에는 명확성을 위해 '만료 없음'으로 설정 (사용자가 다시 선택하도록 유도)
        spExpire.setSelection(0); // 기본값 "만료 없음"으로 설정

        // ⭐️ 사용자 지정 선택 시 처리
        spExpire.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == options.length - 1) { // '사용자 지정'을 선택한 경우 (인덱스 4)
                    // memoToEdit의 customDays를 null로 초기화 후 다이얼로그 호출
                    memoToEdit.customDays = null;
                    openCustomDaysDialog(memoToEdit, spExpire);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 3. 기존 만료 기간 정보 표시
        TextView tvExpiryInfo = new TextView(this);
        tvExpiryInfo.setText("기존 만료 기간: " + formatExpiry(memoToEdit.getExpiryAtLong()));
        tvExpiryInfo.setTextColor(memoToEdit.getExpiryAtLong() != null ? 0xFFFF0000 : 0xFFFFFFFF);
        tvExpiryInfo.setPadding(dp(0), dp(8), dp(0), dp(0));

        // footerRow 위에 삽입
        ((ViewGroup)footerRow.getParent()).addView(tvExpiryInfo, ((ViewGroup)footerRow.getParent()).indexOfChild(footerRow));


        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.show(); // **show가 먼저 와야 window 접근 가능**

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        btnSave.setOnClickListener(vSave -> {
            String newContent = etMemo.getText().toString().trim();
            if (newContent.isEmpty()) {
                Toast.makeText(this, "메모 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            int sel = spExpire.getSelectedItemPosition();

            // ⭐️ 사용자 지정이 선택되었는데 일수가 없는 경우 처리
            if (sel == 4 && memoToEdit.customDays == null) {
                Toast.makeText(this, "사용자 지정 일수를 입력해야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // customDays 정리 (사용자 지정이 아니면 null)
            Integer finalCustomDays = (sel == 4) ? memoToEdit.customDays : null;

            // ⭐️ 만료 기간 계산 로직 호출
            Long newExpiry = calcExpire(sel, finalCustomDays);

            // 최종 업데이트
            updateExistingMemo(memoToEdit.id, newContent, newExpiry, isImportant[0], finalCustomDays);
            dialog.dismiss();
            timetableView.invalidate();
        });
        dialog.show();
    }

    /** 메모 내용, 만료일, 중요도, 사용자 지정 일수 업데이트 */
    private void updateExistingMemo(String memoId, String newContent, @Nullable Long newExpiry, boolean newIsImportant, @Nullable Integer newCustomDays) {
        for (MemoAdapter.Memo memo : memos) {
            if (memo.id.equals(memoId)) {
                memo.text = newContent;
                memo.expiryAt = newExpiry;
                memo.isImportant = newIsImportant; // ⭐️ 중요도 업데이트
                memo.customDays = newCustomDays;
                dao.updateMemo(toEntity(memo));// ⭐️ customDays 업데이트
                break;
            }
        }
        updateMemoAdapter();
    }

    /** 메모 삭제 2차 확인 */
    private void showDeleteConfirmationDialog(final MemoAdapter.Memo memo, final int position) {
        // 기존의 메모 옵션 다이얼로그에서 삭제 버튼을 눌렀을 때만 호출됩니다.
        new AlertDialog.Builder(this)
                .setTitle("메모 삭제 확인")
                .setMessage("정말로 이 메모를 삭제하시겠습니까?\n\n\"" + memo.text + "\"")
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteMemoFromList(memo);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /** 메모 삭제 */
    private void deleteMemoFromList(MemoAdapter.Memo memo) {
        if (memo == null) return;

        new Thread(() -> {
            try {
                TimetableDatabase db = TimetableDatabase.getDatabase(getApplicationContext());
                TimetableDao dao = db.timetableDao();

                MemoEntity entity = dao.findMemoById(memo.id);
                if (entity != null) dao.deleteMemo(entity);

                // ✅ DB에서 최신 메모 목록 가져오기
                List<MemoEntity> updatedList = dao.getAllMemos();

                // ✅ 메모 목록을 UI 객체로 다시 변환
                List<MemoAdapter.Memo> refreshedMemos = new ArrayList<>();
                for (MemoEntity e : updatedList) {
                    MemoAdapter.Memo m = new MemoAdapter.Memo();
                    m.id = e.id;
                    m.lectureId = e.lectureId;
                    m.text = e.text;
                    m.createdAt = e.createdAt;
                    m.isImportant = e.isImportant;
                    m.expiryAt = e.expiryAt;
                    m.setExpanded(false);
                    refreshedMemos.add(m);
                }

                // ✅ 메인스레드에서 Adapter 갱신 (DiffUtil 캐시 강제 무효화)
                runOnUiThread(() -> {
                    memos.clear();
                    memos.addAll(refreshedMemos);
                    if (memoAdapter != null) {
                        memoAdapter.submitList(new ArrayList<>(refreshedMemos));
                        memoAdapter.notifyDataSetChanged(); // 🔥 완전 강제 새로고침
                    }
                    rebuildMemoMarkers();
                });

            } catch (Exception e) {
                android.util.Log.e("deleteMemoFromList", "Delete failed", e);
            }
        }).start();
    }










    // ⭐️⭐️⭐️ 만료 시간 계산 로직 ⭐️⭐️⭐️
    /**
     * 만료 시간 계산
     * @param idx 스피너 인덱스 (0: 만료 없음, 1: 1일, 2: 3일, 3: 7일, 4: 사용자 지정)
     * @param customDays 사용자 지정 일수 (idx=4 일 때 사용)
     * @return 계산된 만료 시간 밀리초, 만료 없음 시 null
     */
    private Long calcExpire(int idx, @Nullable Integer customDays){
        if (idx == 0) return null; // ⭐️ 만료 없음 선택 시 확실히 null 반환

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int daysToAdd = 0;

        switch (idx){
            case 1: // 1일 뒤
                daysToAdd = 1;
                break;
            case 2: // 3일 뒤
                daysToAdd = 3;
                break;
            case 3: // 7일 뒤
                daysToAdd = 7;
                break;
            case 4: // 사용자 지정
                if (customDays == null || customDays < 1) {
                    // 사용자 지정이 선택되었으나 유효한 일수가 없는 경우, 만료 없음 처리
                    return null;
                }
                daysToAdd = customDays;
                break;
            default:
                return null;
        }

        // ⭐️ 만료 시점을 'daysToAdd' 일 후의 23:59:59.999로 설정
        c.add(Calendar.DAY_OF_YEAR, daysToAdd);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);

        return c.getTimeInMillis();
    }


    // 만료된 메모 삭제
    private void pruneExpiredMemos(){
        long now = System.currentTimeMillis();
        Iterator<MemoAdapter.Memo> it = memos.iterator();
        boolean changed = false;
        while (it.hasNext()){
            MemoAdapter.Memo m = it.next();
            Long expiryAt = m.getExpiryAtLong();
            if (expiryAt != null && expiryAt <= now) { it.remove(); changed = true; }
        }
        if (changed) updateMemoAdapter();
        rebuildMemoMarkers();
        updateNextClassStatus();
    }

    // 메모가 있는 강의에 마커를 표시하도록 TimetableView에 ID 목록 전달
    private void rebuildMemoMarkers(){
        lectureIdsWithMemo.clear();
        for (MemoAdapter.Memo m : memos) lectureIdsWithMemo.add(m.lectureId);
        timetableView.setMemoMarkedIds(lectureIdsWithMemo);
    }

    // ⭐️⭐️⭐️ 수정된 showInputDialog 메서드: XML 버튼 (btnSubmit) 사용 ⭐️⭐️⭐️
    private void showInputDialog(TimetableView.LectureItem editing, int dayIndex, int startMin, int endMin) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_timetable_input, null, false);
        EditText inputTitle = v.findViewById(R.id.inputTitle);
        EditText inputProfessor = v.findViewById(R.id.inputProfessor);
        EditText inputPlace = v.findViewById(R.id.inputPlace);
        Button btnStart = v.findViewById(R.id.btnStart);
        Button btnEnd   = v.findViewById(R.id.btnEnd);
        LinearLayout colorRow = v.findViewById(R.id.colorRow);
        View colorPreview = v.findViewById(R.id.colorPreview);

        // ⭐️ XML의 btnSubmit 버튼을 바인딩합니다.
        com.google.android.material.button.MaterialButton btnSubmit = v.findViewById(R.id.btnSubmit);

        // 편집 모드에 따라 버튼 텍스트 변경
        if (editing != null) {
            btnSubmit.setText("강의 수정하기");
        } else {
            btnSubmit.setText("강의 추가하기");
        }

        final int[] chosenColor = { 0xFF8E9AFF };
        final int[] selStart    = { startMin };
        final int[] selEnd      = { endMin };

        if (editing != null) {
            inputTitle.setText(editing.title);
            inputProfessor.setText(editing.professor);
            inputPlace.setText(editing.place);
            selStart[0] = editing.startMin;
            selEnd[0]   = editing.endMin;
            chosenColor[0] = editing.color;
        }

        if (colorPreview != null) {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(chosenColor[0]);
            gd.setCornerRadius(dp(8));
            gd.setStroke(dp(1), 0xFFFFFFFF);
            colorPreview.setBackground(gd);
        }

        btnStart.setText(minToLabel(selStart[0]));
        btnEnd.setText(minToLabel(selEnd[0]));

        btnStart.setOnClickListener(v2 -> openCustomTimePicker(selStart, btnStart));
        btnEnd.setOnClickListener(v2 -> openCustomTimePicker(selEnd, btnEnd));

        colorRow.removeAllViews();
        for (int color : palette) {
            FrameLayout chipWrap = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            chipWrap.setLayoutParams(lp);

            GradientDrawable border = new GradientDrawable();
            border.setShape(GradientDrawable.RECTANGLE);
            border.setCornerRadius(dp(8));
            border.setColor(0x00000000);
            border.setStroke(dp(1), 0x66FFFFFF);
            chipWrap.setBackground(border);

            View colorView = new View(this);
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            int inset = dp(3);
            clp.setMargins(inset, inset, inset, inset);
            colorView.setLayoutParams(clp);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(color);
            gd.setCornerRadius(dp(6));
            colorView.setBackground(gd);
            chipWrap.addView(colorView);

            chipWrap.setOnClickListener(v2 -> {
                chosenColor[0] = color;
                String title = inputTitle.getText().toString().trim();
                if (!title.isEmpty()) {
                    colorMap.put(normalizeTitle(title), color);
                }
                for (int i=0;i<colorRow.getChildCount();i++) {
                    View w = colorRow.getChildAt(i);
                    if (w instanceof FrameLayout) w.setAlpha(0.6f);
                }
                chipWrap.setAlpha(color == chosenColor[0] ? 1f : 0.6f);
                if (colorPreview != null) {
                    GradientDrawable gd2 = new GradientDrawable();
                    gd2.setColor(color);
                    gd2.setCornerRadius(dp(8));
                    gd2.setStroke(dp(1), 0xFFFFFFFF);
                    colorPreview.setBackground(gd2);
                }
            });
            colorRow.addView(chipWrap);
            chipWrap.setAlpha(color == chosenColor[0] ? 1f : 0.6f);
        }

        // ⭐️ 1. Dialog 객체 생성 (AlertDialog.Builder 대신)
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(v);

        // 배경을 투명하게 설정하여 XML CardView의 디자인을 살립니다.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // ⭐️ 2. 바깥 영역 클릭 및 뒤로가기 버튼으로 닫기 설정
        dialog.setCanceledOnTouchOutside(true); // 여백 클릭으로 닫기
        dialog.setCancelable(true);             // 뒤로가기 버튼으로 닫기

        // ⭐️ 3. XML 버튼에 리스너 연결 (Java의 저장 로직 실행)
        btnSubmit.setOnClickListener(vSubmit -> {
            String title = inputTitle.getText().toString().trim();
            if (title.isEmpty()) title = "새 강의";
            int a = Math.min(selStart[0], selEnd[0]);
            int b = Math.max(selStart[0], selEnd[0]);

            // 10분 단위 최소 간격 보장 로직
            if (b - a < 10) b = a + 10;

            int finalColor = colorForTitle(title, chosenColor[0]);

            if (editing == null) {
                if (hasOverlap(dayIndex, a, b, null)) {
                    Toast.makeText(this, "이미 겹치는 시간이 있어요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                TimetableView.LectureItem it = new TimetableView.LectureItem(dayIndex, a, b, title, finalColor);
                it.professor = inputProfessor.getText().toString().trim();
                it.place = inputPlace.getText().toString().trim();
                timetableView.addItem(it);
                dao.insertLecture(toEntity(it));
            } else {
                if (hasOverlap(dayIndex, a, b, editing.id)) {
                    Toast.makeText(this, "이미 겹치는 시간이 있어요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                TimetableView.LectureItem updated = new TimetableView.LectureItem(dayIndex, a, b, title, finalColor);
                updated.professor = inputProfessor.getText().toString().trim();
                updated.place = inputPlace.getText().toString().trim();
                updated.id = editing.id;
                timetableView.updateItem(updated);
                dao.updateLecture(toEntity(updated));
            }
            updateMemoAdapter();
            updateNextClassStatus();

            dialog.dismiss(); // 저장 성공 후 다이얼로그 닫기
        });

        dialog.show(); // 다이얼로그 표시

        Log.d("Timetable", "Clicked day=" + dayIndex);

    }

    // ⭐️⭐️⭐️ [XML 커스텀 다이얼로그 최종 통합] 강의 상세 정보 팝업을 띄우는 메서드 ⭐️⭐️⭐️
    private void showLectureDetailsDialog(TimetableView.LectureItem lect) {
        // 1. XML 레이아웃 인플레이션 (dialog_lecture_details.xml 파일명을 가정)
        View customView = getLayoutInflater().inflate(R.layout.dialog_lecture_details, null);

        // 2. 뷰 바인딩
        TextView tvTitle = customView.findViewById(R.id.tvLectureTitle);
        TextView tvProfessor = customView.findViewById(R.id.tvProfessor);
        TextView tvPlace = customView.findViewById(R.id.tvPlace);
        TextView tvDate = customView.findViewById(R.id.tvDate);
        TextView tvTime = customView.findViewById(R.id.tvTime);
        View colorChip = customView.findViewById(R.id.colorChip);


        // 3. 데이터 포맷팅
        String startTime = minToLabel(lect.startMin);
        String endTime = minToLabel(lect.endMin);
        String dayLabel = getDayLabel(lect.dayIndex);

        // 4. 뷰에 데이터 채우기
        tvTitle.setText(lect.title);
        tvProfessor.setText("교수: " + (lect.professor.isEmpty() ? "미정" : lect.professor));
        tvPlace.setText("장소: " + (lect.place.isEmpty() ? "미정" : lect.place));
        tvDate.setText("요일: " + dayLabel);
        tvTime.setText("시간: " + startTime + " - " + endTime);

        // 5. 색상 칩 배경 설정
        if (colorChip != null) {
            GradientDrawable gd = new GradientDrawable();
            // DP 헬퍼 함수를 사용하여 둥근 모서리 크기(8dp)를 지정합니다.
            gd.setCornerRadius(dp(8));
            gd.setColor(lect.color);
            colorChip.setBackground(gd);
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(customView);

// 창 배경 투명 (하얀 바탕 제거)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            // 뒤 배경 어둡게 정도 (원하면 0~0.6): 0이면 완전 투명
            dialog.getWindow().setDimAmount(0.35f);
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        // 7. XML의 배경을 온전히 표시하기 위해 기본 AlertDialog 배경을 투명하게 설정
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }



        dialog.show();
    }

    // ⭐️⭐️⭐️ [요일 인덱스를 레이블로 변환하는 헬퍼 함수] ⭐️⭐️⭐️
    private String getDayLabel(int dayIndex) {
        // MON, TUE, WED, THE, FRI 대신 "월요일" 등으로 변경하여 사용자에게 친숙하게 표시
        String[] days = {"월요일", "화요일", "수요일", "목요일", "금요일"};
        if (dayIndex >= 0 && dayIndex < days.length) {
            return days[dayIndex];
        }
        return "알 수 없음";
    }
}