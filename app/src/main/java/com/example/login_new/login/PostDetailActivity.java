package com.example.login_new.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.NestedScrollView; // XML의 detailScroll에 맞춤

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class PostDetailActivity extends AppCompatActivity {

    // PopupMenu에서 사용할 ID 상수 정의
    private static final int MENU_EDIT = 1001;
    private static final int MENU_DELETE = 1002;

    // UI 변수 (XML에 존재하는 ID 및 타입에 맞게 수정)
    private TextView tvTitle, tvContent, tvTimestamp, tvAuthor;
    private EditText commentInput;
    private ImageButton btnSubmitComment;

    // ❌ btnDeletePost, btnEditPost 변수는 제거됨.
    private Button btnTogglePin, btnCancelReply;
    private ImageButton btnBackToMain; // XML에 맞게 ImageButton으로 선언
    private ImageButton btnPostMenu; // 수정/삭제 기능을 통합한 메뉴 버튼

    private NestedScrollView detailScrollView; // ✅ XML의 R.id.detailScroll에 맞게 수정
    private LinearLayout commentContainer;

    // Data
    private String postId;
    private String currentUserId;
    private String replyToCommentId = null;
    private String postBorder;

    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> editPostLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // View 연결 (오류 라인 수정)
        tvTitle = findViewById(R.id.detailTitle);
        tvContent = findViewById(R.id.detailContent);
        tvTimestamp = findViewById(R.id.detailDate);
        tvAuthor = findViewById(R.id.detailAuthor);
        commentInput = findViewById(R.id.commentInput);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);

        // ✅ R.id.btnBackToMain은 XML에서 ImageButton 타입
        btnBackToMain = findViewById(R.id.btnBackToMain);

        // ✅ R.id.btnPostMenu는 XML에서 ImageButton 타입 (수정/삭제 통합)
        btnPostMenu = findViewById(R.id.btnPostMenu);

        // ❌ btnEditPost, btnDeletePost는 XML에 없으므로 연결 코드 제거됨

//        btnTogglePin = findViewById(R.id.btnTogglePin);
//        btnCancelReply = findViewById(R.id.btnCancelReply);

        // ✅ R.id.commentScrollView 대신 XML에 있는 R.id.detailScroll에 연결
        detailScrollView = findViewById(R.id.detailScroll);

        commentContainer = findViewById(R.id.commentContainer);

        btnTogglePin.setVisibility(View.GONE);
        btnCancelReply.setVisibility(View.GONE);
        btnPostMenu.setVisibility(View.GONE); // 기본 숨김 (loadPostDetail에서 작성자에게만 표시)

        db = FirebaseFirestore.getInstance();
        postId = getIntent().getStringExtra("postId");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        if (postId == null) {
            Toast.makeText(this, "게시글 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editPostLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadPostDetail();
                        loadComments();
                    }
                });

        loadPostDetail();
        loadComments();

        btnSubmitComment.setOnClickListener(v -> submitComment());

        btnCancelReply.setOnClickListener(v -> {
            replyToCommentId = null;
            commentInput.setHint("댓글을 입력하세요");
            btnCancelReply.setVisibility(View.GONE);
        });

        btnBackToMain.setOnClickListener(v -> finish());

        // ✅ 수정/삭제 기능을 통합한 메뉴 버튼 리스너
        btnPostMenu.setOnClickListener(this::showPostMenu);

        // ❌ 기존 btnDeletePost, btnEditPost 리스너 로직은 showPostMenu로 통합됨

        setupPinButtonLogic();
    }

    // 게시글 수정/삭제를 처리하는 팝업 메뉴 메서드
    @SuppressLint("NonConstantResourceId")
    private void showPostMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);

        // 팝업 메뉴 항목 추가
        popup.getMenu().add(Menu.NONE, MENU_EDIT, 0, "수정");
        popup.getMenu().add(Menu.NONE, MENU_DELETE, 1, "삭제");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == MENU_EDIT) {
                // 수정 로직
                Intent intent = new Intent(PostDetailActivity.this, WriteActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("postId", postId);
                intent.putExtra("title", tvTitle.getText().toString());
                intent.putExtra("content", tvContent.getText().toString());
                intent.putExtra("selectedBorder", postBorder);
                editPostLauncher.launch(intent);
                return true;

            } else if (id == MENU_DELETE) {
                // 삭제 로직
                new AlertDialog.Builder(this)
                        .setTitle("게시글 삭제")
                        .setMessage("정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (dialog, which) -> {
                            db.collection("posts").document(postId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("취소", null)
                        .show();
                return true;
            }
            return false;
        });

        popup.show();
    }


    private void submitComment() {
        String commentText = commentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> comment = new HashMap<>();
        comment.put("author", currentUserId != null ? currentUserId : "익명");
        comment.put("uid", currentUserId);
        comment.put("content", commentText);
        comment.put("timestamp", Timestamp.now());
        comment.put("postId", postId);
        comment.put("parentId", replyToCommentId);

        db.collection("posts").document(postId).collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    commentInput.setText("");
                    commentInput.setHint("댓글을 입력하세요");
                    replyToCommentId = null;
                    btnCancelReply.setVisibility(View.GONE);
                    loadComments();
                    // ✅ detailScrollView로 스크롤
                    if(detailScrollView != null) detailScrollView.post(() -> detailScrollView.fullScroll(View.FOCUS_DOWN));
                    Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "댓글 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private void loadPostDetail() {
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvTitle.setText(documentSnapshot.getString("title"));
                        tvContent.setText(documentSnapshot.getString("content"));
                        Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
                        if (timestamp != null) {
                            tvTimestamp.setText("작성일: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(timestamp.toDate()));
                        }
                        tvAuthor.setText("작성자: 익명");

                        postBorder = documentSnapshot.getString("border");

                        String author = documentSnapshot.getString("author");
                        if (author != null && author.equals(currentUserId)) {
                            // ✅ 작성자일 경우 메뉴 버튼 표시
                            btnPostMenu.setVisibility(View.VISIBLE);
                        } else {
                            // ✅ 작성자가 아닐 경우 메뉴 버튼 숨김
                            btnPostMenu.setVisibility(View.GONE);
                        }
                        // ❌ 기존 btnDeletePost, btnEditPost 관련 로직 제거됨
                    }
                });
    }

    private void loadComments() {
        db.collection("posts").document(postId).collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> commentList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment comment = new Comment();
                        comment.setId(doc.getId());
                        comment.setPostId(doc.getString("postId"));
                        comment.setAuthor(doc.getString("author"));
                        comment.setUid(doc.getString("uid"));
                        comment.setContent(doc.getString("content"));

                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts != null) comment.setTimestamp(ts.toDate());

                        Object parentIdRaw = doc.get("parentId");
                        if (parentIdRaw instanceof String) {
                            comment.setParentId((String) parentIdRaw);
                        } else {
                            comment.setParentId(null);
                        }

                        commentList.add(comment);
                    }
                    displayComments(buildCommentTree(commentList));
                });
    }



    private List<Comment> buildCommentTree(List<Comment> comments) {
        Map<String, List<Comment>> childrenMap = new HashMap<>();
        for (Comment comment : comments) {
            String parent = comment.getParentId() == null ? "root" : comment.getParentId();
            childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(comment);
        }
        List<Comment> ordered = new ArrayList<>();
        traverseCommentTree("root", childrenMap, ordered, 0);
        return ordered;
    }

    private void traverseCommentTree(String parentId, Map<String, List<Comment>> map, List<Comment> result, int depth) {
        List<Comment> children = map.get(parentId);
        if (children == null) return;
        for (Comment comment : children) {
            comment.setDepth(depth);
            result.add(comment);
            traverseCommentTree(comment.getId(), map, result, depth + 1);
        }
    }

    @SuppressLint("MissingInflatedId")
    private void displayComments(List<Comment> comments) {
        commentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Comment comment : comments) {
            View view = inflater.inflate(R.layout.item_comment, commentContainer, false);

            TextView author = view.findViewById(R.id.commentAuthor);
            TextView content = view.findViewById(R.id.commentContent);
            Button deleteBtn = view.findViewById(R.id.btnDeleteComment);

            // item_comment.xml에 존재하는 ID (만약 없다면 여기서 NPE 발생 가능)
            View arrow = view.findViewById(R.id.replyArrow);
            LinearLayout layout = view.findViewById(R.id.commentItemLayout);

            // 댓글 내용 표시
            author.setText(comment.getAuthor() != null ? comment.getAuthor() : "익명");
            content.setText(comment.getContent());

            // 댓글 깊이에 따라 들여쓰기
            if (comment.getDepth() > 0) {
                if(arrow != null) arrow.setVisibility(View.VISIBLE);
                if(layout != null) layout.setPadding(comment.getDepth() * 50, 0, 0, 0);
            } else {
                if(arrow != null) arrow.setVisibility(View.GONE);
                if(layout != null) layout.setPadding(0, 0, 0, 0);
            }

            // 답글 달기
            view.setOnClickListener(v -> {
                replyToCommentId = comment.getId();
                commentInput.setHint((comment.getAuthor() != null ? comment.getAuthor() : "익명") + " 님에게 답글 달기...");
                btnCancelReply.setVisibility(View.VISIBLE);
            });

            // 현재 사용자 본인 댓글일 때 삭제 버튼 표시
            if (currentUserId != null && currentUserId.equals(comment.getUid())) {
                deleteBtn.setVisibility(View.VISIBLE);

                deleteBtn.setOnClickListener(v -> {
                    new AlertDialog.Builder(PostDetailActivity.this)
                            .setTitle("댓글 삭제")
                            .setMessage("댓글을 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (dialog, which) -> {
                                db.collection("posts").document(postId)
                                        .collection("comments").document(comment.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> loadComments())
                                        .addOnFailureListener(e -> Toast.makeText(this, "댓글 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("취소", null)
                            .show();
                });

            } else {
                deleteBtn.setVisibility(View.GONE);
            }

            commentContainer.addView(view);
        }
    }


    private void setupPinButtonLogic() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(snapshot -> {
                    boolean isAdmin = snapshot.getBoolean("admin") != null && snapshot.getBoolean("admin");
                    String currentUserMajor = snapshot.getString("major");

                    db.collection("posts").document(postId).get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) return;

                                Boolean isPinnedObj = doc.getBoolean("isPinned");
                                boolean isPinned = isPinnedObj != null && isPinnedObj;

                                // 예: 게시판 구분
                                String postBoard = doc.getString("border");

                                // 핀 토글 가능 여부 (예: 관리자 or 게시판 관리자)
                                boolean canPin = isAdmin || (currentUserMajor != null && currentUserMajor.equals(postBoard));

                                if (canPin) {
                                    btnTogglePin.setVisibility(View.VISIBLE);
                                    btnTogglePin.setText(isPinned ? "고정 해제" : "고정");

                                    btnTogglePin.setOnClickListener(v -> {
                                        boolean currentlyPinned = btnTogglePin.getText().toString().equals("고정 해제");
                                        boolean newState = !currentlyPinned;

                                        Map<String, Object> update = new HashMap<>();
                                        update.put("isPinned", newState);

                                        if (newState && currentUserMajor != null) {
                                            update.put("major", currentUserMajor);
                                        }

                                        db.collection("posts").document(postId)
                                                .update(update)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(PostDetailActivity.this,
                                                            newState ? "고정됨" : "고정 해제됨", Toast.LENGTH_SHORT).show();
                                                    btnTogglePin.setText(newState ? "고정 해제" : "고정");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(PostDetailActivity.this,
                                                            "고정 상태 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    });
                                } else {
                                    btnTogglePin.setVisibility(View.GONE);
                                }
                            });
                });
    }

    // Comment 클래스 내부 정의 (유지)
    private static class Comment {
        private String id;
        private String postId;
        private String author;
        private String uid;
        private String content;
        private java.util.Date timestamp;
        private String parentId;
        private List<Comment> children;
        private int depth;

        public Comment() {
            children = new ArrayList<>();
        }

        // getter / setter (나머지 생략)
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getPostId() { return postId; }
        public void setPostId(String postId) { this.postId = postId; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public java.util.Date getTimestamp() { return timestamp; }
        public void setTimestamp(java.util.Date timestamp) { this.timestamp = timestamp; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public List<Comment> getChildren() { return children; }
        public void setChildren(List<Comment> children) { this.children = children; }
        public int getDepth() { return depth; }
        public void setDepth(int depth) { this.depth = depth; }
    }
}