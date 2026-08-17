package com.example.login_new.alogic;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

/**
 * ✅ TimetableDao
 * 강의(Lecture)와 메모(Memo) 데이터를 Room DB에서 관리하는 DAO.
 * 즉시 조회용(List) + 실시간 관찰용(LiveData) 메서드를 모두 포함합니다.
 */
@Dao
public interface TimetableDao {

    // -----------------------------
    // 🧩 Lecture 관련 메서드
    // -----------------------------

    /** 모든 강의를 즉시 반환 (UI 초기 로딩용) */
    @Query("SELECT * FROM lectures")
    List<LectureEntity> getAllLectures();

    /** LiveData로 모든 강의 반환 (ViewModel 관찰용) */
    @Query("SELECT * FROM lectures")
    LiveData<List<LectureEntity>> getAllLecturesLive();

    @Insert
    void insertLecture(LectureEntity lecture);

    @Update
    void updateLecture(LectureEntity lecture);

    @Delete
    void deleteLecture(LectureEntity lecture);

    /** ✅ 특정 강의를 삭제할 때 그 강의 ID로 삭제 (보조용) */
    @Query("DELETE FROM lectures WHERE id = :lectureId")
    void deleteLectureById(String lectureId);



    // -----------------------------
    // 🧩 Memo 관련 메서드
    // -----------------------------

    /** 모든 메모를 즉시 반환 (앱 실행 시 초기 로딩용) */
    @Query("SELECT * FROM memos")
    List<MemoEntity> getAllMemos();

    /** LiveData로 모든 메모 반환 (ViewModel 관찰용) */
    @Query("SELECT * FROM memos")
    LiveData<List<MemoEntity>> getAllMemosLive();

    /** 특정 메모 ID로 1개 조회 (필요시 사용) */
    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    MemoEntity findMemoById(String id);

    @Insert
    void insertMemo(MemoEntity memo);

    @Update
    void updateMemo(MemoEntity memo);

    @Delete
    void deleteMemo(MemoEntity memo);

    /** ✅ ID 기반 삭제 — 객체 비교 문제 없이 완전 삭제 */
    @Query("DELETE FROM memos WHERE id = :id")
    void deleteMemoById(String id);

    /** ✅ 특정 강의의 모든 메모 삭제 (강의 삭제 시 함께 제거) */
    @Query("DELETE FROM memos WHERE lectureId = :lectureId")
    void deleteMemosByLectureId(String lectureId);
}
