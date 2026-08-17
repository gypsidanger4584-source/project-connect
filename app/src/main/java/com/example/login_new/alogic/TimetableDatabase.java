package com.example.login_new.alogic;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LectureEntity.class, MemoEntity.class}, version = 21) // ✅ 반드시 버전 올리기
public abstract class TimetableDatabase extends RoomDatabase {
    public abstract TimetableDao timetableDao();

    private static volatile TimetableDatabase INSTANCE;

    public static TimetableDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (TimetableDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    TimetableDatabase.class,
                                    "timetable_db"
                            )
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

