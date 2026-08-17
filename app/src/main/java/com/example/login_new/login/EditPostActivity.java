package com.example.login_new.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditPostActivity extends AppCompatActivity {

    private EditText editTitle, editContent;
    private Button btnSave, btnCancel;
    private TextView textCounter;
    private FirebaseFirestore db;
    private String postId;

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        db = FirebaseFirestore.getInstance();

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        textCounter = findViewById(R.id.textCounter);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        String category = intent.getStringExtra("category");

        if (postId == null || title == null || content == null) {
            Toast.makeText(this, "게시글 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editTitle.setText(title);
        editContent.setText(content);

        // 글자 수 초기 업데이트
        updateCounter(content.length());

        editContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > MAX_CONTENT_LENGTH) {
                    s.delete(MAX_CONTENT_LENGTH, s.length());
                }
                updateCounter(s.length());
            }
        });


        btnSave.setOnClickListener(v -> {
            String newTitle = editTitle.getText().toString().trim();
            String newContent = editContent.getText().toString().trim();
            String originalBorder = getIntent().getStringExtra("border");

            if (newTitle.isEmpty() || newContent.isEmpty()) {
                Toast.makeText(this, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newTitle.length() > MAX_TITLE_LENGTH) {
                Toast.makeText(this, "제목은 100자 이하로 작성해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newContent.length() > MAX_CONTENT_LENGTH) {
                Toast.makeText(this, "내용은 500자 이하로 작성해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSave.setEnabled(false); // 중복 클릭 방지

            Map<String, Object> post = new HashMap<>();
            post.put("title", newTitle);
            post.put("content", newContent);
            post.put("border", originalBorder);

            db.collection("posts").document(postId)
                    .update(post)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "수정에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true); // 실패 시 다시 활성화
                    });
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void updateCounter(int currentLength) {
        int remaining = MAX_CONTENT_LENGTH - currentLength;
        textCounter.setText(remaining + "자 남음");
    }

}
