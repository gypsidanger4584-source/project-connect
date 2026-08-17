package com.example.login_new.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class BuildingDetail extends AppCompatActivity {

    private SQLiteHelper dbHelper;
    private LinearLayout infoContainer;
    private String buildingName;
    private String markerIdName;
    private ImageView buildingImage;
    private ImageButton home;
    private TextView toolbar_title;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.building_detail);

        // 툴바 설정
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish()); // 뒤로가기

        // 인텐트로부터 정보 받기
        buildingName = getIntent().getStringExtra("building_name");
        markerIdName = getIntent().getStringExtra("markerIdName");

        dbHelper = new SQLiteHelper(this);
        infoContainer = findViewById(R.id.info_container);
        buildingImage = findViewById(R.id.buildingImage);
        home = findViewById(R.id.homeButton);
        toolbar_title = findViewById(R.id.toolbar_title);

        // 건물 이미지 설정
        if (markerIdName != null) {
            int resId = getResources().getIdentifier(markerIdName, "drawable", getPackageName());
            if (resId != 0) {
                buildingImage.setImageResource(resId);
            } else {
                buildingImage.setImageResource(R.drawable.default_image);
            }
        } else {
            buildingImage.setImageResource(R.drawable.default_image);
        }

        home.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 학과 정보 로딩
        loadDepartmentInfo();
        toolbar_title.setText(buildingName+" 정보");
    }

    private void loadDepartmentInfo() {
        ArrayList<String> departmentNames = dbHelper.getDepartmentsByBuilding(buildingName);
        ArrayList<String> departmentOffices = dbHelper.getDepartmentOfficesByBuilding(buildingName);
        ArrayList<String> spacesByBuilding = dbHelper.getSpacesByBuilding(buildingName);

        if (departmentNames.isEmpty()) {
            addTextView("해당 건물에 등록된 학과 정보가 없습니다.");
        } else {
            addTextView("◆ 소속 학과 목록:");
            for (String dept : departmentNames) {
                addTextView("- " + dept);
            }
        }

        if (!departmentOffices.isEmpty()) {
            addTextView("\n◆ 학과 사무실 위치:");
            for (String office : departmentOffices) {
                addTextView("- " + office);
            }
        }

        if (!spacesByBuilding.isEmpty()) {
            addTextView("\n◆ 특별한 공간:");
            for (String space : spacesByBuilding) {
                addTextView("- " + space);
            }
        }
    }

    private void addTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(16);
        tv.setPadding(10, 10, 10, 10);
        infoContainer.addView(tv);
    }
}
