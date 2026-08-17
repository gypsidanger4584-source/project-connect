package com.example.login_new.alogic;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.login_new.R;

import java.util.Arrays;
import java.util.List;

public class CampusMap extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MapPager adapter;
    private LinearLayout searchLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.campus_main);

        initViews();
        initViewPager();
        initSearch();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        searchLayout = findViewById(R.id.searchLayout);
    }

    private void initViewPager() {
        List<Integer> imageList = Arrays.asList(
                R.drawable.map_part1,
                R.drawable.map_part2
        );
        adapter = new MapPager(imageList, this);
        viewPager.setAdapter(adapter);
    }

    private void initSearch() {
        searchLayout.setOnClickListener(v -> {
            SearchDialog dialog = new SearchDialog();
            dialog.show(getSupportFragmentManager(), "SearchDialog");
        });
    }
}
