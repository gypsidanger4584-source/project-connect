package com.example.login_new.alogic;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;

public abstract class BaseBottomNavActivity extends AppCompatActivity {

    protected static final int TAB_BOARD = 0;
    protected static final int TAB_STUDY = 1;
    protected static final int TAB_TIMETABLE = 2;

    protected void setupBottomNav(int currentTab) {
        LinearLayout navBoard      = findViewById(R.id.nav_board);
        LinearLayout btnStudyBoard = findViewById(R.id.btnStudyBoard);
        LinearLayout btnTimetable  = findViewById(R.id.btntimetable);
        LinearLayout navRandomChat = findViewById(R.id.nav_random_chat);

        // 📌 게시판 탭
        navBoard.setOnClickListener(v -> {
            if (currentTab == TAB_BOARD) {
                Toast.makeText(this, "이미 게시판 탭입니다.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();    // 뒤에 쌓이지 않게 현재 화면 정리
            }
        });

        // 📌 스터디 탭
        btnStudyBoard.setOnClickListener(v -> {
            if (currentTab == TAB_STUDY) {
                Toast.makeText(this, "이미 스터디 탭입니다.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, StudyBoardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });

        // 📌 시간표 탭
        btnTimetable.setOnClickListener(v -> {
            if (currentTab == TAB_TIMETABLE) {
                Toast.makeText(this, "이미 시간표 탭입니다.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, TimetableActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        // 📌 랜덤 매칭 (현재 탭과 상관없이 항상 이동)
        if (navRandomChat != null) {
            navRandomChat.setOnClickListener(v -> {
                Intent intent = new Intent(this, RandomMatchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }
}

