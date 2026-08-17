package com.example.login_new.alogic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class BuildingDetail extends AppCompatActivity {

    private LinearLayout infoContainer;
    private String buildingName;
    private String markerIdName;
    private ImageView buildingImage;
    private ImageButton home;
    private TextView toolbar_title;
    private FirebaseFirestore firestore;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.building_detail);

        // ✅ Toolbar 설정
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // ✅ Intent 데이터
        buildingName = getIntent().getStringExtra("building_name");
        markerIdName = getIntent().getStringExtra("markerIdName");

        // ✅ Firestore 초기화
        firestore = FirebaseFirestore.getInstance();

        // ✅ View 연결
        infoContainer = findViewById(R.id.info_container);
        buildingImage = findViewById(R.id.buildingImage);
        home = findViewById(R.id.homeButton);
        toolbar_title = findViewById(R.id.toolbar_title);

        // ✅ 홈 버튼
        home.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // ✅ 건물 이미지 설정
        if (markerIdName != null) {
            int resId = getResources().getIdentifier(markerIdName, "drawable", getPackageName());
            buildingImage.setImageResource(resId != 0 ? resId : R.drawable.default_image);
        } else {
            buildingImage.setImageResource(R.drawable.default_image);
        }

        // ✅ 제목 처리
        if (buildingName == null || buildingName.isEmpty()) {
            toolbar_title.setText("건물 정보 없음");
            addTextView("선택된 건물 정보가 없습니다.");
        } else {
            toolbar_title.setText(buildingName + " 정보");
            loadBuildingDetails();
        }

        // ✅ 테마에 맞춰 배경 및 글씨색 자동 조정
        applyThemeToLayout();
    }

    /**
     * 🌙 라이트/다크 모드에 따른 전체 레이아웃 스타일 적용
     */
    private void applyThemeToLayout() {
        int nightModeFlags = getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            infoContainer.setBackgroundColor(getColor(R.color.bg_main)); // 어두운 배경
        } else {
            infoContainer.setBackgroundColor(getColor(R.color.white)); // 밝은 배경
        }
    }

    /**
     * Firestore에서 학과 목록, 학과 사무실 위치, 공간 정보를 한꺼번에 불러와 UI에 추가한다.
     */
    private void loadBuildingDetails() {
        infoContainer.removeAllViews();

        firestore.collection("buildings").document(buildingName).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 1️⃣ 소속 학과 목록
                        List<String> departments = (List<String>) documentSnapshot.get("departments");
                        if (departments == null || departments.isEmpty()) {
                            addTextView("해당 건물에 등록된 학과 정보가 없습니다.");
                        } else {
                            addSectionTitle("◆ 소속 학과 목록:");
                            for (String deptName : departments) {
                                addTextView("- " + deptName);
                            }
                        }

                        // 2️⃣ 특별한 공간 목록
                        List<String> spaces = (List<String>) documentSnapshot.get("spaces");
                        if (spaces != null && !spaces.isEmpty()) {
                            addSectionTitle("\n◆ 특별한 공간:");
                            for (String space : spaces) {
                                addTextView("- " + space);
                            }
                        }

                        // 3️⃣ 학과 사무실 위치
                        firestore.collection("departments_detail")
                                .whereEqualTo("building_name", buildingName)
                                .get()
                                .addOnSuccessListener(detailSnapshots -> {
                                    if (!detailSnapshots.isEmpty()) {
                                        addSectionTitle("\n◆ 학과 사무실 위치:");
                                        for (QueryDocumentSnapshot doc : detailSnapshots) {
                                            String dept = doc.getString("department_name");
                                            String room = doc.getString("room_number");
                                            if (dept != null && room != null) {
                                                addTextView("- " + dept + " : " + room);
                                            }
                                        }
                                    }
                                });
                    } else {
                        addTextView("해당 건물 정보를 찾을 수 없습니다.");
                    }
                })
                .addOnFailureListener(e -> addTextView("건물 정보 로드 중 오류가 발생했습니다."));
    }

    /**
     * ✅ 기본 내용 텍스트 (자동 색상 적용)
     */
    private void addTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setPadding(16, 12, 16, 12);

        // 🌙 다크/라이트 모드에 따른 글자색 자동 반전
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            tv.setTextColor(getColor(R.color.text_primary)); // themes-night.xml 정의된 밝은 글자색
        } else {
            tv.setTextColor(getColor(R.color.text_primary)); // 라이트모드용 색상
        }

        infoContainer.addView(tv);
    }

    /**
     * ✅ 섹션 제목 텍스트 (자동 색상 적용)
     */
    private void addSectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(16, 24, 16, 12);

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            tv.setTextColor(getColor(R.color.primary_pink)); // 다크모드용 강조색
        } else {
            tv.setTextColor(getColor(R.color.blue)); // 라이트모드용 강조색
        }

        infoContainer.addView(tv);
    }
}
