package com.example.login_new.alogic;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 📘 LectureRepository
 * - DAO를 통해 데이터에 접근하는 Repository 클래스
 * - ViewModel이 데이터 변경을 요청할 때 이 Repository를 통해 수행합니다.
 */
public class LectureRepository {
    private final TimetableDao dao;
    private final LiveData<List<LectureEntity>> allLecturesLive;
    private final LiveData<List<MemoEntity>> allMemosLive;

    // 비동기 작업용 스레드 풀
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LectureRepository(Application application) {
        TimetableDatabase db = TimetableDatabase.getDatabase(application);
        dao = db.timetableDao();
        allLecturesLive = dao.getAllLecturesLive();
        allMemosLive = dao.getAllMemosLive();
    }

    // 📘 LiveData getter (UI에서 자동 갱신)
    public LiveData<List<LectureEntity>> getAllLecturesLive() {
        return allLecturesLive;
    }

    public LiveData<List<MemoEntity>> getAllMemosLive() {
        return allMemosLive;
    }

    // 📘 강의 관련
    public void insertLecture(LectureEntity lecture) {
        executor.execute(() -> dao.insertLecture(lecture));
    }

    public void updateLecture(LectureEntity lecture) {
        executor.execute(() -> dao.updateLecture(lecture));
    }

    public void deleteLecture(LectureEntity lecture) {
        executor.execute(() -> dao.deleteLecture(lecture));
    }

    // 📘 메모 관련
    public void insertMemo(MemoEntity memo) {
        executor.execute(() -> dao.insertMemo(memo));
    }

    public void updateMemo(MemoEntity memo) {
        executor.execute(() -> dao.updateMemo(memo));
    }

    public void deleteMemo(MemoEntity memo) {
        executor.execute(() -> dao.deleteMemo(memo));
    }
}
