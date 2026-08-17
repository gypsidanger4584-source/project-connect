package com.example.login_new.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.example.login_new.login.PostAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<DocumentSnapshot> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;
    private ImageButton menuButton;

    // 하단 네비게이션 버튼
    private LinearLayout navBoard, btnStudyBoard, btnTimetable;

    private List<DocumentSnapshot> fullPostList = new ArrayList<>();
    private String selectedBorder = "자유게시판"; // 기본 게시판 이름

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        setupViews();
        setupBottomButtons(); // 하단 버튼 클릭 처리
        updateBorderFromIntent();
        loadPostsFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBorderFromIntent();
        loadPostsFromFirestore();
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(adapter);

        fabAdd = findViewById(R.id.fab_add);
        menuButton = findViewById(R.id.menu_btn);

        // 메뉴 버튼 클릭 → 카테고리 설정 페이지로 이동
        menuButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CategorySettingsActivity.class));
        });

        // 글쓰기 버튼 클릭
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WriteActivity.class);
            intent.putExtra("selectedBorder", selectedBorder);
            startActivity(intent);
        });

        // 게시글 클릭 → 상세보기 이동
        adapter.setOnItemClickListener(snapshot -> {
            Intent intent = new Intent(MainActivity.this, PostDetailActivity.class);
            intent.putExtra("postId", snapshot.getId());
            startActivity(intent);
        });

        // 툴바 제목 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(selectedBorder);
        }
    }

    /** ✅ 하단 네비게이션 버튼 설정 (게시판 / 스터디 / 시간표) */
    private void setupBottomButtons() {
        navBoard = findViewById(R.id.nav_board);
        btnStudyBoard = findViewById(R.id.btnStudyBoard);
        btnTimetable = findViewById(R.id.btntimetable);

        // 게시판
        navBoard.setOnClickListener(v -> {
            selectedBorder = "자유게시판";
            loadPostsFromFirestore();
        });

        // 스터디 게시판
        btnStudyBoard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.login_new.alogic.StudyBoardActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 시간표
        btnTimetable.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.login_new.alogic.TimetableActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    /** ✅ 인텐트로 넘어온 게시판 카테고리 업데이트 */
    private void updateBorderFromIntent() {
        String borderFromIntent = getIntent().getStringExtra("selectedBorder");
        if (borderFromIntent != null) {
            selectedBorder = borderFromIntent;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(selectedBorder);
            }
        }
    }

    /** ✅ Firestore 게시글 불러오기 */
    private void loadPostsFromFirestore() {
        Query query = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereEqualTo("border", selectedBorder.trim());

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            fullPostList.clear();
            fullPostList.addAll(queryDocumentSnapshots.getDocuments());
            adapter.updateList(fullPostList);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(selectedBorder);
            }
        });
    }

    /** ✅ 메뉴 로그아웃 처리 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.action_logout);
        View actionView = item.getActionView();
        if (actionView != null) {
            TextView logoutText = actionView.findViewById(R.id.logoutText);
            if (logoutText != null) {
                logoutText.setOnClickListener(v -> performLogout());
            }
        }

        return true;
    }

    private void performLogout() {
        getSharedPreferences("loginPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
