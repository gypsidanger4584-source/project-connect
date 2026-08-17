package com.example.login_new.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.login.MainActivity;
import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MyPage extends AppCompatActivity {

    private TextView userIdTextView, emailTextView, postTextView, numberTextView, departmentTextView;
    private Button homeButton;

    private FirebaseFirestore db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_page);

        // Firebase
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);

        // View 연결
        userIdTextView = findViewById(R.id.user_id);
        emailTextView = findViewById(R.id.user_email);
        postTextView = findViewById(R.id.user_post);
        numberTextView = findViewById(R.id.user_number);
        departmentTextView = findViewById(R.id.user_department);
        homeButton = findViewById(R.id.btn_home);

        // 사용자 정보 불러오기
        loadUserInfo();

        // 홈으로 이동
        homeButton.setOnClickListener(v -> {
            startActivity(new Intent(MyPage.this, MainActivity.class));
            finish();
        });
    }

    private void loadUserInfo() {
        String userId = prefs.getString("userId", null);
        if (userId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String email = documentSnapshot.getString("email");
                        String studentNo = documentSnapshot.getString("studentNo");
                        String major = documentSnapshot.getString("major");

                        userIdTextView.setText(userId);
                        emailTextView.setText(email);
                        numberTextView.setText(studentNo);
                        departmentTextView.setText(major);

                        loadPostCount(userId);
                    } else {
                        Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "데이터 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPostCount(String userId) {
        db.collection("posts")
                .whereEqualTo("authorId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    postTextView.setText("작성한 게시글 수: " + count + "개");
                })
                .addOnFailureListener(e -> {
                    postTextView.setText("게시글 수 불러오기 실패");
                });
    }
}
