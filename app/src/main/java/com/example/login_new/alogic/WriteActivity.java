package com.example.login_new.alogic;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class WriteActivity extends AppCompatActivity {

    private static final String TAG = "WriteActivity";

    private EditText editTitle, editContent;
    private TextView textCounter;
    private Button btnSave, btnCancel;
    private CheckBox checkNotice;

    private final int MAX_LENGTH = 500;

    private FirebaseFirestore db;

    // 기존 게시판 수정 여부
    private boolean isEdit = false;

    // 공지 수정 여부
    private boolean isNoticeEdit = false;

    // 문서 ID
    private String postId;     // posts 컬렉션 ID
    private String noticeId;   // notices 컬렉션 ID

    private boolean isAdmin = false; // 관리자 여부

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        textCounter = findViewById(R.id.textCounter);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        checkNotice = findViewById(R.id.checkNotice);

        db = FirebaseFirestore.getInstance();

        String mode = getIntent().getStringExtra("mode");

        // 🔥 1) 일반 글 수정
        if ("edit".equals(mode)) {
            isEdit = true;
            postId = getIntent().getStringExtra("postId");
            editTitle.setText(getIntent().getStringExtra("title"));
            editContent.setText(getIntent().getStringExtra("content"));
        }

        // 🔥 2) 공지 수정
        else if ("edit_notice".equals(mode)) {
            isNoticeEdit = true;
            noticeId = getIntent().getStringExtra("noticeId");
            editTitle.setText(getIntent().getStringExtra("title"));
            editContent.setText(getIntent().getStringExtra("content"));

            // 공지 수정할 때는 체크박스 의미없으므로 숨김 처리
            checkNotice.setVisibility(View.GONE);
        }

        // 🔥 3) 새 글 작성
        else {
            isEdit = false;
            isNoticeEdit = false;
        }

        // 🔹 관리자 체크 (새 글 작성 시 공지 작성 가능)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String role = snapshot.getString("role");
                        if ("admin".equals(role)) {
                            isAdmin = true;

                            // 새 글 작성 모드에서만 공지 체크박스 보이기
                            if (!isNoticeEdit) {
                                checkNotice.setVisibility(View.VISIBLE);
                            }
                        } else {
                            isAdmin = false;
                            checkNotice.setVisibility(View.GONE);
                        }
                    });
        }

        // 🔹 글자 수 카운트
        editContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textCounter.setText((MAX_LENGTH - s.length()) + "자 남음");
            }
            @Override public void afterTextChanged(Editable s){}
        });

        btnSave.setOnClickListener(v -> savePost());
        btnCancel.setOnClickListener(v -> finish());
    }


    /** 🔥 저장 버튼 클릭 시 처리 */
    private void savePost() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        if (isNoticeEdit) {
            updateNotice();
        }
        else if (isEdit) {
            updatePost();
        }
        else {
            createNewPost();
        }
    }

    // ============================================================
    // 🔥 1) 공지사항 수정 기능
    // ============================================================
    private void updateNotice() {

        Map<String, Object> notice = new HashMap<>();
        notice.put("title", editTitle.getText().toString());
        notice.put("content", editContent.getText().toString());
        notice.put("timestamp", Timestamp.now());
        notice.put("border", "공지사항"); // ⭐ 공지 속성 유지

        db.collection("posts").document(noticeId)   // ← 반드시 posts로 수정!
                .update(notice)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "공지사항이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "공지 수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }

    // ============================================================
    // 🔥 2) 기존 게시판 글 수정 기능
    // ============================================================
    private void updatePost() {

        Map<String, Object> post = new HashMap<>();
        post.put("title", editTitle.getText().toString());
        post.put("content", editContent.getText().toString());
        post.put("timestamp", Timestamp.now());

        db.collection("posts").document(postId)
                .update(post)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }


    // ============================================================
    // 🔥 3) 새 게시글 작성 기능 (공지 포함)
    // ============================================================
    private void createNewPost() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {

                    String major = snapshot.getString("major");
                    if (major == null) major = "무소속";

                    // 🔥 체크박스 기준으로 공지/일반글 구분
                    String border = (isAdmin && checkNotice.isChecked())
                            ? "공지사항"
                            : "일반게시글";

                    Map<String, Object> post = new HashMap<>();
                    post.put("title", editTitle.getText().toString());
                    post.put("content", editContent.getText().toString());
                    post.put("author", user.getUid());
                    post.put("major", major);
                    post.put("border", border);
                    post.put("timestamp", Timestamp.now());
                    post.put("likeCount", 0L);
                    post.put("commentCount", 0L);
                    post.put("viewCount", 0L);

                    db.collection("posts")
                            .add(post)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnSave.setEnabled(true);
                            });
                });
    }
}
