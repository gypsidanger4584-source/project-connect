package com.example.login_new.alogic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;

public class MatchFailActivity extends AppCompatActivity {

    private LinearLayout btnRetry;
    private LinearLayout btnGoBack;

    // 🔹 상단 텍스트들
    private TextView tvTitle;
    private TextView tvSub1;
    private TextView tvSub2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_fail); // ✅ 네가 준 XML

        // ✅ 뷰 바인딩
        btnRetry = findViewById(R.id.btnRetry);
        btnGoBack = findViewById(R.id.btnGoBack);

        tvTitle = findViewById(R.id.tvTitle);
        tvSub1  = findViewById(R.id.tvSub1);
        tvSub2  = findViewById(R.id.tvSub2);

        // ✅ RandomMatchActivity 쪽에서 넘겨줄 예정인 reason 값 읽기
        //  - "timeout"     : 매칭 실패 (기본)
        //  - "user_cancel" : 사용자가 직접 취소
        String reason = getIntent().getStringExtra("reason");
        if (reason == null) {
            reason = "timeout"; // 기본값
        }

        // 🔥 reason 에 따라 문구 바꾸기
        if ("user_cancel".equals(reason)) {
            // 사용자가 직접 취소한 경우
            tvTitle.setText("매칭 취소");
            tvSub1.setText("학식 메이트 찾기를 취소했어요");
            tvSub2.setText("원한다면 언제든 다시 시도할 수 있어요");
        } else {
            // 기본: 시간초과 등으로 매칭 실패
            tvTitle.setText("매칭 실패");
            tvSub1.setText("학식 메이트를 찾지 못했어요");
            tvSub2.setText("다시 시도하거나 나중에 다시 시도해주세요");
        }

        // 🔁 다시 매칭하기
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 다시 매칭 시작 화면으로 이동
                Intent intent = new Intent(MatchFailActivity.this, RandomMatchActivity.class);
                // 필요하면 기존 매칭 관련 상태 초기화 로직을 RandomMatchActivity에서 처리
                startActivity(intent);
                finish();
            }
        });

        // 🚪 나가기 (그냥 이전 화면으로)
        btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
