package com.example.login_new.login;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CategorySettingsActivity extends AppCompatActivity {

    private LinearLayout buttonContainer;
    private Button btnCampusMap, btnMyPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_settings);

        buttonContainer = findViewById(R.id.buttonContainer);
        btnCampusMap = findViewById(R.id.btnCampusMap);
        btnMyPage = findViewById(R.id.btnMyPage);

        btnMyPage.setOnClickListener(v -> {
            Intent intent = new Intent(CategorySettingsActivity.this, MyPage.class);
            startActivity(intent);
        });

        btnCampusMap.setOnClickListener(v -> {
            Intent intent = new Intent(CategorySettingsActivity.this, CampusMap.class);
            startActivity(intent);
        });

        loadBorderButtons();
    }

    private void loadBorderButtons() {
        buttonContainer.removeAllViews();

        // 1. 메인게시판 버튼
        Button mainBoardBtn = new Button(this);
        mainBoardBtn.setText("메인게시판");
        mainBoardBtn.setAllCaps(false);
        mainBoardBtn.setBackgroundColor(Color.TRANSPARENT);
        mainBoardBtn.setTextColor(Color.BLACK);
        mainBoardBtn.setPadding(0, 20, 0, 20);
        mainBoardBtn.setTextSize(16);
        mainBoardBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        mainBoardBtn.setOnClickListener(v -> {
            Intent intent = new Intent(CategorySettingsActivity.this, MainActivity.class);
            intent.putExtra("selectedBorder", "메인 게시판");
            startActivity(intent);
        });
        buttonContainer.addView(mainBoardBtn);

        // 구분선 추가
        View topDivider = new View(this);
        topDivider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        ));
        topDivider.setBackgroundColor(Color.parseColor("#DDDDDD"));
        buttonContainer.addView(topDivider);

        // Firestore에서 borders 컬렉션 불러오기
        FirebaseFirestore.getInstance().collection("borders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = 0;
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        // 문서 필드명이 'border'인지 반드시 확인하세요
                        String border = document.getString("name");
                        if (border == null) continue;

                        Button button = new Button(CategorySettingsActivity.this);
                        button.setText(border);
                        button.setAllCaps(false);
                        button.setBackgroundColor(Color.TRANSPARENT);
                        button.setTextColor(Color.BLACK);
                        button.setPadding(0, 20, 0, 20);
                        button.setTextSize(16);
                        button.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        button.setOnClickListener(v -> {
                            Intent intent = new Intent(CategorySettingsActivity.this, MainActivity.class);
                            intent.putExtra("selectedBorder", border);
                            startActivity(intent);
                        });

                        buttonContainer.addView(button);

                        // 중간 구분선 추가
                        if (count < querySnapshot.size() - 1) {
                            View divider = new View(CategorySettingsActivity.this);
                            divider.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    1
                            ));
                            divider.setBackgroundColor(Color.parseColor("#DDDDDD"));
                            buttonContainer.addView(divider);
                        }
                        count++;
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }
}
