package com.example.login_new.alogic;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.login_new.R;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * WorkManager를 사용해 주기적으로 다음 수업 알림을 띄워주는 Worker
 */
public class ReminderWorker extends Worker {

    private static final String CHANNEL_ID = "timetable_reminder_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "ReminderWorker";
    private final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final TimetableDao timetableDao;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        // ✅ TimetableDatabase로 변경 (AppDatabase 제거)
        TimetableDatabase db = TimetableDatabase.getDatabase(context);
        timetableDao = db.timetableDao();



        createNotificationChannel();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ReminderWorker started. Checking next class...");

        List<LectureEntity> lectures;
        try {
            lectures = getLecturesFromDatabase();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching lectures from DB: " + e.getMessage());
            return Result.retry();
        }

        String notificationText = calculateAndGetNextClass(lectures);
        if (notificationText != null) {
            sendNotification(notificationText);
        }

        Log.d(TAG, "ReminderWorker finished.");
        return Result.success();
    }

    // ✅ DB에서 LectureEntity 리스트 불러오기
    private List<LectureEntity> getLecturesFromDatabase() {
        // TimetableDao에 추가:
        // @Query("SELECT * FROM lectures") List<LectureEntity> getAllLecturesList();
        return timetableDao.getAllLectures();
    }

    // ✅ 다음 수업 계산 로직
    private String calculateAndGetNextClass(List<LectureEntity> lectureItems) {
        if (lectureItems == null || lectureItems.isEmpty()) return null;

        final int TIMETABLE_START_HOUR = 9;
        LocalDateTime nowDateTime = LocalDateTime.now(KOREA_ZONE);
        long minToNextClass = Long.MAX_VALUE;
        LocalDateTime nextClassDateTime = null;
        String nextClassTitle = null;

        for (LectureEntity item : lectureItems) {
            int dayIndex = item.dayIndex;
            int startMin = item.startMin;
            String title = item.title;

            if (title == null || title.isEmpty()) continue;

            int classDayValue = DayOfWeek.MONDAY.getValue() + dayIndex;
            if (classDayValue > DayOfWeek.FRIDAY.getValue()) continue;

            int classStartMinAbs = startMin + (TIMETABLE_START_HOUR * 60);
            LocalTime classStartTime = LocalTime.of(classStartMinAbs / 60, classStartMinAbs % 60);

            LocalDateTime nextClassCandidate = LocalDateTime.of(nowDateTime.toLocalDate(), classStartTime)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(classDayValue)));

            if (nextClassCandidate.isBefore(nowDateTime)) {
                nextClassCandidate = nextClassCandidate.plusWeeks(1);
            }

            long minutesDiff = ChronoUnit.MINUTES.between(nowDateTime, nextClassCandidate);

            if (minutesDiff > 0 && minutesDiff <= 15) {
                if (minutesDiff < minToNextClass) {
                    minToNextClass = minutesDiff;
                    nextClassDateTime = nextClassCandidate;
                    nextClassTitle = title;
                }
            }
        }

        if (nextClassTitle != null) {
            long minutes = ChronoUnit.MINUTES.between(nowDateTime, nextClassDateTime);
            String startTimeStr = nextClassDateTime.toLocalTime().toString();

            return String.format(Locale.KOREA,
                    "%s 수업이 %d분 뒤 (%s)에 시작합니다.",
                    nextClassTitle, minutes, startTimeStr);
        }
        return null;
    }

    // ✅ 알림 채널 생성
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "수업 알림";
            String description = "다가오는 수업에 대한 알림";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    getApplicationContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // ✅ 실제 알림 전송
    private void sendNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.white_hexagon)
                .setContentTitle("다음 수업 알림")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
