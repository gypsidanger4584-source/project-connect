package com.example.login_new.alogic;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import android.graphics.Color;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Entity(tableName = "lecture_table")
public class LectureItem {

    // ⭐️ ID 오류 해결: PrimaryKey에 @NonNull을 붙이고 UUID로 초기화
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id = UUID.randomUUID().toString();

    @ColumnInfo(name = "title")
    public String title = "";
    @ColumnInfo(name = "dayIndex")
    public int dayIndex; // 0=월 ~ 6=일
    @ColumnInfo(name = "startMin")
    public int startMin; // Minutes from 09:00 (TimetableView.START_HOUR)
    @ColumnInfo(name = "endMin")
    public int endMin;
    @ColumnInfo(name = "professor")
    public String professor = "";
    @ColumnInfo(name = "place")
    public String place = "";
    @ColumnInfo(name = "color")
    public int color = 0; // Default color

    // Project 2 Color Palette
    public static final int[] COLOR_PALETTE = {
            Color.parseColor("#1E88E5"),
            Color.parseColor("#00BFA5"),
            Color.parseColor("#FFB300"),
            Color.parseColor("#7E57C2"),
            Color.parseColor("#D81B60"),
            Color.parseColor("#00ACC1"),
            Color.parseColor("#F4511E"),
            Color.parseColor("#66BB6A")
    };

    public LectureItem() {
        this.color = getRandomColor();
        this.title = "";
    }

    public static int getRandomColor() {
        return COLOR_PALETTE[ThreadLocalRandom.current().nextInt(COLOR_PALETTE.length)];
    }

    // ----------------------
    // Getter/Setter (Room 접근용)
    // ----------------------
    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    // (나머지 Getter/Setter 생략)
    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
    public int getStartMin() { return startMin; }
    public void setStartMin(int startMin) { this.startMin = startMin; }
    public int getEndMin() { return endMin; }
    public void setEndMin(int endMin) { this.endMin = endMin; }
    public String getProfessor() { return professor; }
    public void setProfessor(String professor) { this.professor = professor; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}