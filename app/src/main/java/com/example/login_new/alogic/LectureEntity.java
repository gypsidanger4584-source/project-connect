package com.example.login_new.alogic;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lectures")
public class LectureEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public int dayIndex;
    public int startMin;
    public int endMin;
    public String title;
    public String professor;
    public String place;
    public int color;
}
