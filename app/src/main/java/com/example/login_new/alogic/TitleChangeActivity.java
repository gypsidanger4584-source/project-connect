package com.example.login_new.alogic;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TitleChangeActivity extends AppCompatActivity {

    private ListView listViewTitles;
    private List<String> titleList = new ArrayList<>();

    private TextView tvScoreInfo;
    private ProgressBar progressScore;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private int userScore = 0;
    private String currentTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title_change);

        listViewTitles = findViewById(R.id.listViewTitles);
        tvScoreInfo = findViewById(R.id.tvScoreInfo);
        progressScore = findViewById(R.id.progressScore);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 칭호 목록
        String[] titles = {
                "연습생 코더", "도전하는 학습자", "기초 다지는 초보", "알고리즘 탐험가", "문제 풀이 입문자",
                "꾸준한 해결사", "성장하는 개발자", "코드 연구생", "집중력 마스터", "실전 코더",
                "문제 해결 전문가", "논리의 장인", "버그 헌터", "알고리즘 설계자", "코드 공학자",
                "최적화 전문가", "시스템 분석가", "기술 마법사", "패턴 인식가", "프로그래밍 마스터",
                "연성대학교의 별 ⭐", "계산의 신", "지식 탐험가", "코드 조련사", "천재적인 분석가",
                "디버깅 마왕", "로직 설계 장인", "알고리즘 박사", "창의력 괴물", "코드 혁신가",
                "연성의 영웅", "알고리즘의 전설", "데이터 마스터", "논리의 제왕", "프로그래밍의 현자",
                "문제 해결 귀재", "디지털 장군", "코드의 왕좌", "AI의 동반자", "궁극의 개발자",
                "연성의 수호자", "천상계 코더", "문제의 신", "코딩 현자", "전설적인 두뇌",
                "코드의 신화", "완벽한 논리체", "알고리즘 절대자", "디지털 신성", "연성의 창조주 👑"
        };
        for (String t : titles) titleList.add(t);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 🔹 사용자 정보 불러오기
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        if (doc.contains("score")) {
                            Long scoreValue = doc.getLong("score");
                            userScore = (scoreValue != null) ? scoreValue.intValue() : 0;
                        }
                        if (doc.contains("title")) {
                            currentTitle = doc.getString("title");
                        }
                    }
                    updateScoreCard();
                    setupListAdapter();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "점수 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setupListAdapter();
                });
    }

    /** ✅ 점수 카드 업데이트 */
    private void updateScoreCard() {
        int nextUnlock = ((userScore / 5) + 1) * 5;
        int remaining = nextUnlock - userScore;
        if (remaining < 0) remaining = 0;

        tvScoreInfo.setText("현재 점수: " + userScore + "점  ｜  다음 칭호까지 " + remaining + "점");
        progressScore.setMax(5);
        progressScore.setProgress(userScore % 5);
    }

    /** ✅ 칭호 리스트 */
    private void setupListAdapter() {
        final int unlockedCount = userScore / 5;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, titleList) {

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);

                // 🔹 잠금 여부 표시
                if (position <= unlockedCount) {
                    text.setTextColor(Color.parseColor("#7A4FF7"));
                    text.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_unlocked, 0);
                } else {
                    text.setTextColor(Color.parseColor("#AAAAAA"));
                    text.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_locked, 0);
                }

                // 🔹 현재 착용 중인 칭호 강조
                if (titleList.get(position).equals(currentTitle)) {
                    text.setTextColor(Color.parseColor("#FFD700")); // 골드 색상
                    text.setText("★ " + titleList.get(position) + " (현재 착용)");
                }

                text.setPadding(32, 40, 32, 40);
                text.setTextSize(16);
                return view;
            }
        };

        listViewTitles.setAdapter(adapter);

        listViewTitles.setOnItemClickListener((parent, view, position, id) -> {
            if (position > unlockedCount) {
                Toast.makeText(this, "🔒 이 칭호는 아직 잠겨 있습니다! (점수를 더 쌓으세요)", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedTitle = titleList.get(position);
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            db.collection("users").document(user.getUid())
                    .update("title", selectedTitle)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this,
                                "칭호가 \"" + selectedTitle + "\"(으)로 변경되었습니다 ✅",
                                Toast.LENGTH_SHORT).show();
                        currentTitle = selectedTitle;
                        adapter.notifyDataSetChanged();
                    });
        });
    }
}
