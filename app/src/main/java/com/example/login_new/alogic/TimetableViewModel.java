package com.example.login_new.alogic;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

/**
 * 🧠 TimetableViewModel
 * - Repository와 UI(Activity) 사이의 연결다리 역할
 * - Activity는 ViewModel만 보고 LiveData를 관찰하면 됩니다.
 */
public class TimetableViewModel extends AndroidViewModel {

    private final LectureRepository repository;
    private final LiveData<List<LectureEntity>> allLecturesLive;
    private final LiveData<List<MemoEntity>> allMemosLive;

    public TimetableViewModel(@NonNull Application application) {
        super(application);
        repository = new LectureRepository(application);
        allLecturesLive = repository.getAllLecturesLive();
        allMemosLive = repository.getAllMemosLive();
    }

    // 🧩 LiveData getter
    public LiveData<List<LectureEntity>> getAllLectures() {
        return allLecturesLive;
    }

    public LiveData<List<MemoEntity>> getAllMemos() {
        return allMemosLive;
    }

    // 🧩 강의 조작
    public void insertLecture(LectureEntity lecture) {
        repository.insertLecture(lecture);
    }

    public void updateLecture(LectureEntity lecture) {
        repository.updateLecture(lecture);
    }

    public void deleteLecture(LectureEntity lecture) {
        repository.deleteLecture(lecture);
    }

    // 🧩 메모 조작
    public void insertMemo(MemoEntity memo) {
        repository.insertMemo(memo);
    }

    public void updateMemo(MemoEntity memo) {
        repository.updateMemo(memo);
    }

    public void deleteMemo(MemoEntity memo) {
        repository.deleteMemo(memo);
    }
}
