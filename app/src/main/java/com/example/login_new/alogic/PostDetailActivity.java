package com.example.login_new.alogic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color; // 💡 [추가] Color import
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class PostDetailActivity extends AppCompatActivity {

    // UI (activity_post_detail.xml)
    private TextView tvTitle, tvContent, tvTimestamp, tvAuthor, tvCategory, tvLikeCount, tvCommentCount, tvViewCount;
    private EditText commentInput;
    private ImageButton btnSubmitComment, btnBackToMain, btnPostMenu;
    private LinearLayout commentContainer;
    private ImageView likeIcon, commentIcon;

    // Data
    private String postId;
    private String currentUserId;
    private String replyToCommentId = null;
    private String editingCommentId = null; // ⬅️ [복원] 댓글 수정 중인 ID
    private String postBorder;

    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> editPostLauncher;

    // 이름 캐시 (UID -> Name)
    private final Map<String, String> userCache = new HashMap<>();
    private final String ADMIN_UID = "admin_placeholder_uid"; // 관리자 UID (필요에 따라 변경)

    // 로컬 표시 상태 (낙관적 UI 및 리스너 동기화용)
    private int likeLocal = 0;
    private int commentLocal = 0;
    private boolean liked = false;

    // Firebase & Listener
    private FirebaseAuth auth;
    private DocumentReference postRef;
    private DocumentReference myLikeRef;
    private ListenerRegistration postSnapReg;
    private ListenerRegistration commentsSnapReg;
    private boolean likeBusy = false;

    private static final String TAG = "PostDetailActivity";
    private SimpleDateFormat timeFormat = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // View 연결 (activity_post_detail.xml ID)
        tvTitle = findViewById(R.id.detailTitle);
        tvContent = findViewById(R.id.detailContent);
        tvTimestamp = findViewById(R.id.detailDate);
        tvAuthor = findViewById(R.id.detailAuthor);

        tvCategory = findViewById(R.id.detailCategory);
        tvLikeCount = findViewById(R.id.likeCount);
        tvViewCount = findViewById(R.id.viewCount);
        tvCommentCount = findViewById(R.id.commentCount);

        commentInput = findViewById(R.id.commentInput);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);
        btnBackToMain = findViewById(R.id.btnBackToMain);
        btnPostMenu = findViewById(R.id.btnPostMenu);
        commentContainer = findViewById(R.id.commentContainer);

        likeIcon = findViewById(R.id.likeIcon);
        commentIcon = findViewById(R.id.commentIcon);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        postId = getIntent().getStringExtra("postId");

        FirebaseUser user = auth.getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        if (postId == null) {
            Toast.makeText(this, "게시글 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Refs
        postRef = db.collection("posts").document(postId);
        myLikeRef = (currentUserId != null) ? postRef.collection("likes").document(currentUserId) : null;

        // 편집 결과 처리 (WriteActivity에서 돌아왔을 때)
        editPostLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "게시글 수정 완료. 상세 정보 갱신.");
                        loadPostDetail();
                        loadComments();
                    }
                });

        // 초기 로드
        loadPostDetail();
        loadComments();

        // UI 이벤트 리스너
        btnBackToMain.setOnClickListener(v -> finish());
        btnPostMenu.setOnClickListener(v -> showPostMenu());
        btnSubmitComment.setOnClickListener(v -> submitComment());

        // 초기 렌더 (UI를 데이터 로드 전에 미리 기본값으로 설정)
        renderLike();
        renderComment();

        // post 문서 실시간 리스너 (좋아요/댓글 카운트 업데이트)
        postSnapReg = postRef.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null || !snap.exists()) return;

            // Firebase에서 업데이트된 likeCount, commentCount를 가져옴
            Long lc = snap.getLong("likeCount");
            Long cc = snap.getLong("commentCount");

            int newLikeCount = (lc != null) ? lc.intValue() : 0;
            int newCommentCount = (cc != null) ? cc.intValue() : 0;

            // 디버깅: 좋아요/댓글 카운트가 예기치 않게 변경되는지 확인
            if (newCommentCount != commentLocal) {
                Log.d(TAG, "🔔 Post commentCount Changed by Server: " + commentLocal + " -> " + newCommentCount);
            }
            if (newLikeCount != likeLocal) {
                Log.d(TAG, "🔔 Post likeCount Changed by Server: " + likeLocal + " -> " + newLikeCount);
            }

            likeLocal = newLikeCount;
            commentLocal = newCommentCount;
            renderLike();
            renderComment();
        });

        // 댓글 서브컬렉션 실시간 리스너 (댓글 목록 업데이트)
        commentsSnapReg = postRef.collection("comments")
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;
                    // commentLocal은 postSnapReg에서 업데이트되지만, loadComments()가 댓글 목록을 다시 그림
                    loadComments();
                });

        // 내가 이미 좋아요 눌렀는지 초기화
        fetchMyLikeState();

        // 좋아요 클릭 리스너
        View.OnClickListener likeClick = v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (likeBusy) return;
            likeBusy = true;
            Log.d(TAG, "🔵 LIKE CLICKED. Current liked: " + liked);

            boolean wantLike = !liked;
            liked = wantLike;
            likeLocal = Math.max(0, likeLocal + (liked ? 1 : -1));
            renderLike(); // 낙관적 업데이트 (즉시 UI 변경)

            toggleLikeOnServer(wantLike, success -> {
                likeBusy = false;
                if (!success) {
                    // 롤백 (서버 처리 실패 시)
                    liked = !wantLike;
                    likeLocal = Math.max(0, likeLocal + (liked ? 1 : -1));
                    renderLike();
                    Toast.makeText(this, "좋아요 처리 실패", Toast.LENGTH_SHORT).show();
                }
            });
        };
        // detailLikeArea 영역 전체에 리스너를 붙이는 것이 안전합니다.
        findViewById(R.id.detailLikeArea).setOnClickListener(likeClick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postSnapReg != null) postSnapReg.remove();
        if (commentsSnapReg != null) commentsSnapReg.remove();
    }

    /** UID -> 이름 변환 (⭐️ [수정된 부분]) */
    private void fetchAndSetUserName(String uid, TextView textView) {
        // [동기 처리] uid가 없거나 '익명'을 명시한 경우
        if (uid == null || uid.isEmpty() || "익명".equals(uid)) {
            textView.setText("익명");
            // 💡 [추가] 익명(나) 즉시 처리 (currentUserId와 비교)
            if (currentUserId != null && currentUserId.equals(uid)) {
                textView.setText("익명 (나)");
                textView.setTextColor(Color.parseColor("#2563EB"));
            } else {
                textView.setTextColor(Color.parseColor("#333333"));
            }
            return;
        }

        if (uid.equals(ADMIN_UID)) {
            textView.setText("관리자");
            textView.setTextColor(Color.parseColor("#333333"));
            return;
        }

        // [동기 처리] 캐시된 이름이 있는 경우
        if (userCache.containsKey(uid)) {
            String cachedName = userCache.get(uid);
            textView.setText(cachedName);
            // 💡 [추가] 익명(나) 즉시 처리 (캐시된 이름이 익명인 경우)
            if (currentUserId != null && currentUserId.equals(uid) && "익명".equals(cachedName)) {
                textView.setText("익명 (나)");
                textView.setTextColor(Color.parseColor("#2563EB"));
            } else {
                textView.setTextColor(Color.parseColor("#333333"));
            }
            return;
        }

        // [비동기 처리] 이름 로딩
        textView.setText("로딩 중...");
        textView.setTextColor(Color.parseColor("#333333")); // 기본 색상으로 초기화

        db.collection("users").document(uid).get()
                .addOnSuccessListener(userSnapshot -> {
                    String userName = userSnapshot.getString("name");
                    boolean isAnonymous = (userName == null || userName.isEmpty());

                    if (isAnonymous) {
                        userName = "익명";
                    }

                    userCache.put(uid, userName);

                    if (!isFinishing()) {
                        textView.setText(userName);

                        // 💡 [추가] 비동기 완료 후 익명(나) 로직
                        if (currentUserId != null && currentUserId.equals(uid) && isAnonymous) {
                            textView.setText("익명 (나)");
                            textView.setTextColor(Color.parseColor("#2563EB")); // 파란색 적용
                        } else {
                            textView.setTextColor(Color.parseColor("#333333")); // 기본 색상 적용
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user name for UID: " + uid, e);
                    userCache.put(uid, "오류");
                    if (!isFinishing()) {
                        textView.setText("오류");
                        textView.setTextColor(Color.parseColor("#333333"));
                    }
                });
    }

    // 게시글 메뉴 표시 (⭐️ [복원 및 수정])
    private void showPostMenu() {
        final String postAuthorUid = tvAuthor.getTag() != null ? tvAuthor.getTag().toString() : "";
        boolean isAuthor = currentUserId != null && currentUserId.equals(postAuthorUid);
        boolean isLoggedIn = currentUserId != null;

        // 로그인하지 않은 경우
        if (!isLoggedIn) {
            Toast.makeText(this, "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        PopupMenu popup = new PopupMenu(this, btnPostMenu);
        // R.menu.menu_post_options가 정의되어 있어야 합니다. (수정, 삭제, 신고 항목 포함)
        popup.getMenuInflater().inflate(R.menu.menu_post_options, popup.getMenu());

        // 메뉴 항목 참조
        MenuItem editItem = popup.getMenu().findItem(R.id.action_edit);
        MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete);
        MenuItem reportItem = popup.getMenu().findItem(R.id.action_report);

        // 권한에 따라 메뉴 표시 설정
        if (editItem != null) editItem.setVisible(isAuthor);
        if (deleteItem != null) deleteItem.setVisible(isAuthor);
        if (reportItem != null) reportItem.setVisible(!isAuthor && isLoggedIn);

        if (!isAuthor && !reportItem.isVisible()) {
            Toast.makeText(this, "게시글에 대한 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                // 수정 로직 (본인)
                Intent intent = new Intent(PostDetailActivity.this, WriteActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("postId", postId);
                intent.putExtra("title", tvTitle.getText().toString());
                intent.putExtra("content", tvContent.getText().toString());
                intent.putExtra("selectedBorder", postBorder);
                editPostLauncher.launch(intent);
                return true;
            } else if (id == R.id.action_delete) {
                // 삭제 로직 (본인)
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
            } else if (id == R.id.action_report) {
                // 신고하기 로직 추가 (타인)
                Toast.makeText(this, "게시글을 신고합니다. (신고 처리 로직 구현 필요: Post ID - " + postId + ")", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // 댓글 등록/수정 로직 (⭐️ [복원 및 수정])
    private void submitComment() {
        String commentText = commentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId == null) {
            Toast.makeText(this, "로그인 후 댓글을 등록할 수 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 💡 [핵심 분기] 수정 모드인지 확인 (editingCommentId 이용)
        if (editingCommentId != null) {
            // A. 댓글 수정 로직 (UPDATE)
            DocumentReference commentToEditRef = postRef.collection("comments").document(editingCommentId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("content", commentText); // 내용 업데이트
            updates.put("timestamp", Timestamp.now()); // 수정 시간 업데이트

            commentToEditRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "댓글이 수정되었습니다.", Toast.LENGTH_SHORT).show();

                        // 수정 상태 초기화
                        commentInput.setText("");
                        commentInput.setHint("댓글을 입력하세요...");
                        editingCommentId = null; // ⬅️ 수정 완료 후 ID 초기화
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "댓글 수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return; // 수정 완료 후 함수 종료
        }


        // B. 댓글 등록 로직 (ADD: 새 댓글 또는 대댓글)
        Log.d(TAG, "✅ COMMENT SUBMITTED! Comment count will be incremented.");

        Map<String, Object> comment = new HashMap<>();
        comment.put("author", currentUserId);
        comment.put("uid", currentUserId);
        comment.put("content", commentText);
        comment.put("timestamp", Timestamp.now());
        comment.put("postId", postId);
        comment.put("parentId", replyToCommentId);
        comment.put("likeCount", 0L); // 댓글 좋아요 수 초기화 (Long으로 저장)

        postRef.collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    // 상태 초기화
                    commentInput.setText("");
                    commentInput.setHint("댓글을 입력하세요...");
                    replyToCommentId = null; // 대댓글 등록 완료 후 ID 초기화

                    // 서버 집계 필드 +1
                    postRef.update("commentCount", FieldValue.increment(1))
                            .addOnFailureListener(e -> Log.e(TAG, "commentCount +1 실패", e));

                    Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "댓글 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 게시글 상세 내용 로드
    private void loadPostDetail() {
        postRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvTitle.setText(documentSnapshot.getString("title"));
                tvContent.setText(documentSnapshot.getString("content"));

                Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                    tvTimestamp.setText(sdf.format(timestamp.toDate()));
                }

                String authorUid = documentSnapshot.getString("author");
                tvAuthor.setTag(authorUid);
                fetchAndSetUserName(authorUid, tvAuthor);

                postBorder = documentSnapshot.getString("border");
                tvCategory.setText(postBorder != null ? postBorder : "미분류");

                Long likeCount = documentSnapshot.getLong("likeCount");
                Long viewCount = documentSnapshot.getLong("viewCount");
                Long commentCount = documentSnapshot.getLong("commentCount");

                tvLikeCount.setText(String.valueOf(likeCount != null ? likeCount : 0));
                tvViewCount.setText(String.valueOf(viewCount != null ? viewCount : 0));
                tvCommentCount.setText(String.format(Locale.getDefault(), "댓글 %d",
                        commentCount != null ? commentCount : 0));

                likeLocal = (likeCount != null) ? likeCount.intValue() : 0;
                commentLocal = (commentCount != null) ? commentCount.intValue() : 0;
                renderLike();
                renderComment();
            } else {
                Toast.makeText(this, "게시글이 존재하지 않거나 삭제되었습니다. 메인으로 돌아갑니다.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Post document does not exist for ID: " + postId);
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "게시글 로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error loading post detail.", e);
            finish();
        });
    }

    // 댓글 목록 로드 및 구조화
    private void loadComments() {
        postRef.collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
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

                        Long lc = doc.getLong("likeCount");
                        comment.setLikeCount((lc != null) ? lc.intValue() : 0);

                        Object parentIdRaw = doc.get("parentId");
                        if (parentIdRaw instanceof String) {
                            comment.setParentId((String) parentIdRaw);
                        }
                        commentList.add(comment);
                    }
                    displayComments(buildCommentTree(commentList));
                });
    }

    // 댓글 트리 구조 생성
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

    // 댓글 깊이 설정
    private void traverseCommentTree(String parentId, Map<String, List<Comment>> map, List<Comment> result, int depth) {
        List<Comment> children = map.get(parentId);
        if (children == null) return;
        for (Comment comment : children) {
            comment.setDepth(depth);
            result.add(comment);
            traverseCommentTree(comment.getId(), map, result, depth + 1);
        }
    }

    // 댓글 표시 (⭐️ [수정된 부분: Indentation 로직 복원 및 익명(나) 색상 처리])
    @SuppressLint("MissingInflatedId")
    private void displayComments(List<Comment> comments) {
        commentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // 동기화
        tvCommentCount.setText(String.format(Locale.getDefault(), "댓글 %d", comments.size()));
        commentLocal = comments.size();
        renderComment();

        for (Comment comment : comments) {
            View view = inflater.inflate(R.layout.item_comment, commentContainer, false);

            TextView author = view.findViewById(R.id.commentAuthor);
            TextView time = view.findViewById(R.id.commentTime);
            TextView content = view.findViewById(R.id.commentContent);
            ImageButton menuBtn = view.findViewById(R.id.btnCommentMenu);

            View arrow = view.findViewById(R.id.replyArrow);
            LinearLayout layout = view.findViewById(R.id.commentItemLayout);

            // 데이터 바인딩
            String commentAuthorUid = comment.getUid();

            // 1. 작성자 이름 설정 (fetchAndSetUserName 내에서 색상 처리 포함)
            fetchAndSetUserName(commentAuthorUid, author);

            content.setText(comment.getContent());

            Date commentDate = comment.getTimestamp();
            if (commentDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.KOREA);
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                time.setText(sdf.format(commentDate));
            }

            // 깊이(Depth)에 따른 UI 조정 (일반 댓글 마진 0 처리 포함)
            if (comment.getDepth() > 0) {
                if (arrow != null) arrow.setVisibility(View.VISIBLE);
                int marginPx = (int) (comment.getDepth() * 12 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
                params.leftMargin = marginPx;
                layout.setLayoutParams(params);
            } else {
                if (arrow != null) arrow.setVisibility(View.GONE);
                // 💡 [복원] 일반 댓글은 왼쪽 마진이 0이 되도록 확실히 설정
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
                params.leftMargin = 0;
                layout.setLayoutParams(params);
            }

            // 댓글 항목 클릭 시 대댓글 입력 UI 활성화 (⭐️ [복원])
            view.setOnClickListener(v -> {
                editingCommentId = null; // 수정 모드 해제
                replyToCommentId = comment.getId();
                String hint = String.format(" @%s 님에게 답글...", (author.getText() != null) ? author.getText() : "익명");
                commentInput.setHint(hint);
                commentInput.requestFocus();
            });

            // 댓글 메뉴 (⭐️ [복원])
            if (menuBtn != null) {
                menuBtn.setOnClickListener(v -> {
                    PopupMenu menu = new PopupMenu(PostDetailActivity.this, v);
                    // R.menu.menu_comment_options.xml을 로드합니다. (답글, 수정, 삭제, 차단 항목 포함)
                    menu.getMenuInflater().inflate(R.menu.menu_comment_options, menu.getMenu());

                    // 메뉴 항목 참조
                    MenuItem replyItem = menu.getMenu().findItem(R.id.action_reply_comment);
                    MenuItem editItem = menu.getMenu().findItem(R.id.action_edit_comment);
                    MenuItem deleteItem = menu.getMenu().findItem(R.id.action_delete_comment);
                    MenuItem blockItem = menu.getMenu().findItem(R.id.action_block_user);

                    boolean isAuthorComment = currentUserId != null && currentUserId.equals(comment.getUid());
                    boolean isLoggedIn = currentUserId != null;

                    // 메뉴 항목별 권한 설정
                    if (replyItem != null) replyItem.setVisible(isLoggedIn);
                    if (editItem != null) editItem.setVisible(isAuthorComment);
                    if (deleteItem != null) deleteItem.setVisible(isAuthorComment);
                    if (blockItem != null) blockItem.setVisible(isLoggedIn && !isAuthorComment);

                    menu.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();

                        // 답글 기능 로직
                        if (id == R.id.action_reply_comment) {
                            editingCommentId = null;
                            replyToCommentId = comment.getId();
                            String hint = String.format(" @%s 님에게 답글...", (author.getText() != null) ? author.getText() : "익명");
                            commentInput.setHint(hint);
                            commentInput.requestFocus();
                            return true;

                        } else if (id == R.id.action_edit_comment) {
                            // 수정 모드 설정 및 대댓글 모드 해제
                            commentInput.setText(comment.getContent());
                            editingCommentId = comment.getId();
                            replyToCommentId = null;

                            commentInput.requestFocus();
                            Toast.makeText(this, "댓글 수정 중...", Toast.LENGTH_SHORT).show();
                            return true;

                        } else if (id == R.id.action_delete_comment) {
                            new AlertDialog.Builder(this)
                                    .setTitle("댓글 삭제")
                                    .setMessage("댓글을 삭제하시겠습니까?")
                                    .setPositiveButton("삭제", (dialog, which) -> {
                                        postRef.collection("comments").document(comment.getId())
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    postRef.update("commentCount", FieldValue.increment(-1))
                                                            .addOnFailureListener(e ->
                                                                    Log.e(TAG, "commentCount -1 실패", e));
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .setNegativeButton("취소", null)
                                    .show();
                            return true;

                        } else if (id == R.id.action_block_user) {
                            // 사용자 차단/신고 로직
                            Toast.makeText(PostDetailActivity.this, comment.getUid() + " 사용자를 차단합니다.", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        return false;
                    });
                    menu.show();
                });
            }
            commentContainer.addView(view);
        }
    }

    // 좋아요 UI 렌더링
    private void renderLike() {
        if (tvLikeCount != null) tvLikeCount.setText(String.valueOf(likeLocal));
        int tint = liked ? 0xFF2563EB : 0xFF555555;
        if (likeIcon != null) likeIcon.setColorFilter(tint);
        if (tvLikeCount != null) tvLikeCount.setTextColor(tint);
    }

    // 댓글 수 UI 렌더링
    private void renderComment() {
        if (tvCommentCount != null) {
            tvCommentCount.setText(String.format(Locale.getDefault(), "댓글 %d", commentLocal));
        }
    }

    // 내가 이미 좋아요 눌렀는지 확인
    private void fetchMyLikeState() {
        if (myLikeRef == null) {
            liked = false;
            renderLike();
            return;
        }
        myLikeRef.get().addOnSuccessListener(doc -> {
            liked = doc.exists();
            renderLike();
        }).addOnFailureListener(e -> {
            liked = false;
            renderLike();
        });
    }

    // 좋아요 토글 트랜잭션 (서버 동기화)
    private interface ToggleCallback { void done(boolean success); }

    private void toggleLikeOnServer(boolean wantLike, ToggleCallback cb) {
        if (postRef == null || currentUserId == null) { cb.done(false); return; }

        db.runTransaction(tr -> {
                    DocumentSnapshot ps = tr.get(postRef);
                    long current = 0L;
                    if (ps.exists() && ps.contains("likeCount")) {
                        Long lc = ps.getLong("likeCount");
                        current = (lc != null) ? lc : 0L;
                    }
                    DocumentReference ref = postRef.collection("likes").document(currentUserId);
                    DocumentSnapshot my = tr.get(ref);

                    if (wantLike) {
                        if (!my.exists()) {
                            tr.set(ref, Collections.singletonMap("timestamp", Timestamp.now()));
                            tr.update(postRef, "likeCount", current + 1);
                            Log.d(TAG, "👍 Like incremented in transaction.");
                        }
                    } else {
                        if (my.exists()) {
                            tr.delete(ref);
                            tr.update(postRef, "likeCount", Math.max(0, current - 1));
                            Log.d(TAG, "👎 Like decremented in transaction.");
                        }
                    }
                    return null;
                }).addOnSuccessListener(v -> cb.done(true))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Transaction failed: " + e.getMessage());
                    cb.done(false);
                });
    }
}