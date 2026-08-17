package com.example.login_new.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class WriteActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private TextView textCounter;
    private Button btnSave, btnCancel;
    private final int MAX_LENGTH = 500;

    private FirebaseFirestore db;
    private boolean isEdit = false;
    private String postId;
    private String border; // 게시판 분류

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        textCounter = findViewById(R.id.textCounter);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        db = FirebaseFirestore.getInstance();

        isEdit = "edit".equals(getIntent().getStringExtra("mode"));
        border = getIntent().getStringExtra("selectedBorder");

        if (border == null && !isEdit) {
            Toast.makeText(this, "게시판 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isEdit) {
            postId = getIntent().getStringExtra("postId");
            String passedTitle = getIntent().getStringExtra("title");
            String passedContent = getIntent().getStringExtra("content");

            editTitle.setText(passedTitle);
            editContent.setText(passedContent);
            btnSave.setText("수정"); // 버튼 텍스트 변경
        }

        editContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                if (length > MAX_LENGTH) {
                    s.delete(MAX_LENGTH, length); // 500자 넘으면 삭제
                }
                int remaining = MAX_LENGTH - s.length();
                textCounter.setText(remaining + "자 남음");
            }
        });

        btnSave.setOnClickListener(v -> savePost());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void savePost() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false); // 중복 클릭 방지
        String userId = user.getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    String major = snapshot.getString("major");
                    if (major == null) major = "무소속";

                    String collectionName = "posts";

                    Map<String, Object> post = new HashMap<>();
                    post.put("title", title);
                    post.put("content", content);
                    post.put("timestamp", Timestamp.now());
                    post.put("author", userId);
                    post.put("major", major);
                    post.put("border", border);

                    if (isEdit) {
                        db.collection(collectionName).document(postId)
                                .update(post)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK); // 🔁 수정 성공 결과 전달
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                    btnSave.setEnabled(true);
                                });
                    } else {
                        db.collection(collectionName)
                                .add(post)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                    btnSave.setEnabled(true);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "사용자 정보 불러오기 실패", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }
}
