package com.example.login_new.login;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.login_new.R;

import java.util.ArrayList;
import java.util.List;

public class CampusMap extends AppCompatActivity {

    ViewPager2 viewPager;
    MapPager adapter;
    SQLiteHelper sqLiteHelper;
    LinearLayout searchLayout;  // 사용자 클릭 UI (ex. 버튼 대신 LinearLayout 사용)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.campus_main); // 메인 레이아웃

        sqLiteHelper = new SQLiteHelper(this);

        // 이미지 슬라이드 설정
        viewPager = findViewById(R.id.viewPager);
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.map_part1);
        imageList.add(R.drawable.map_part2);
        adapter = new MapPager(imageList, this);
        viewPager.setAdapter(adapter);

        // Search 레이아웃 클릭 시 팝업 다이얼로그 실행
        searchLayout = findViewById(R.id.searchLayout);
        searchLayout.setOnClickListener(v -> {
            SearchDialog dialog = new SearchDialog();
            dialog.show(getSupportFragmentManager(), "SearchDialog");
        });
    }
}
