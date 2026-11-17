package com.example.login_new.alogic;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 마이페이지 화면(Activity)
 * - 현재 로그인된 사용자 정보를 Firestore에서 가져와서 표시
 * - 프로필, 학번, 학과, 이메일, 칭호 등 표시
 * - 로그아웃, 테마 설정, 지문 로그인, 관리자 전환 등 포함
 */
public class CategorySettingsActivity extends AppCompatActivity {

    // 상단 UI 요소
    private ImageButton btnBack, btnMap;
    private ImageView userProfile;
    private TextView textId, textDepartment, textStudentId;

    // 카테고리별 항목 (계정, 유틸, 커뮤니티 등)
    private TextView textAccountId, textAccountPassword, textAccountEmail, security_key;
    private TextView textUtilDepartment;
    private TextView textCommunityUsage, textCommunityRestriction, textSecurity;
    private TextView text_logout;
    private TextView textAccountTitle;   // 칭호 표시용

    // Firebase 관련
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // 프로필 이미지
    private int selectedProfileResId = R.drawable.default_profile;
    private int cachedProfileResId   = R.drawable.default_profile;

    // 학과 리스트
    private List<String> departmentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 레이아웃
        setContentView(R.layout.my_page_main);

        // UI 바인딩
        initViews();

        // 🔥 캐싱된 프로필 이미지 먼저 반영 (user_profile)
        SharedPreferences prefProfile = getSharedPreferences("user_profile", MODE_PRIVATE);
        cachedProfileResId = prefProfile.getInt("profile_res_id", R.drawable.default_profile);
        userProfile.setImageResource(cachedProfileResId);
        selectedProfileResId = cachedProfileResId;

        // Firebase 초기화
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 리스너 등록
        setListeners();

        // 사용자 정보 불러오기
        loadUserInfo();
    }

    /**
     * UI 요소 초기화 (findViewById)
     */
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnMap  = findViewById(R.id.btn_map);

        userProfile    = findViewById(R.id.user_profile);
        textId         = findViewById(R.id.text_id);
        textDepartment = findViewById(R.id.text_department);
        textStudentId  = findViewById(R.id.text_student_id);
        security_key   = findViewById(R.id.security_key);

        textAccountId       = findViewById(R.id.text_account_id);
        textAccountPassword = findViewById(R.id.text_account_password);
        textAccountEmail    = findViewById(R.id.text_account_email);

        textUtilDepartment = findViewById(R.id.text_util_department);

        textCommunityUsage       = findViewById(R.id.text_community_usage);
        textCommunityRestriction = findViewById(R.id.text_community_restriction);
        textSecurity             = findViewById(R.id.text_security);

        text_logout      = findViewById(R.id.text_logout);
        textAccountTitle = findViewById(R.id.text_account_title); // 칭호용
    }

    /**
     * 버튼 및 텍스트 클릭 리스너 등록
     */
    private void setListeners() {
        // 뒤로가기 버튼 → 현재 액티비티 종료
        btnBack.setOnClickListener(v -> finish());

        // 지도 버튼 → CampusMap Activity 실행 (있을 경우)
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(CategorySettingsActivity.this, CampusMap.class);
            startActivity(intent);
        });

        // 프로필 이미지 클릭 → 프로필 선택 다이얼로그 띄우기
        userProfile.setOnClickListener(v -> showProfilePickerDialog());

        // 계정 관련
        textAccountId.setOnClickListener(v -> changeUserId());
        textAccountPassword.setOnClickListener(v -> changePassword());
        textAccountEmail.setOnClickListener(v -> changeEmail());
        // 관리자 전환 기능 제거: 관리자 지정은 Firebase 콘솔에서 users 문서의 role 값으로만 변경
        security_key.setVisibility(android.view.View.GONE);

        // 유틸 관련
        textUtilDepartment.setOnClickListener(v -> changeMajor());

        // 커뮤니티 이용 제한 내역
        textCommunityUsage.setOnClickListener(v -> {
            FirebaseUser cur = mAuth.getCurrentUser();
            if (cur == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("restrictions")
                    .whereEqualTo("userId", cur.getUid())
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("이용 제한 내역")
                                    .setMessage("현재 제한 내역이 없습니다.")
                                    .setPositiveButton("확인", null)
                                    .show();
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (QueryDocumentSnapshot doc : query) {
                                String reason = doc.getString("reason");
                                String date   = doc.getString("date");
                                sb.append("• 사유: ").append(reason)
                                        .append("\n날짜: ").append(date)
                                        .append("\n\n");
                            }
                            new AlertDialog.Builder(this)
                                    .setTitle("이용 제한 내역")
                                    .setMessage(sb.toString())
                                    .setPositiveButton("확인", null)
                                    .show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "내역 조회 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        // 커뮤니티 이용 규칙
        textCommunityRestriction.setOnClickListener(v -> {
            ScrollView scrollView = new ScrollView(this);
            scrollView.setPadding(40, 40, 40, 40);

            TextView rulesText = new TextView(this);
            rulesText.setTextColor(getColor(R.color.text_primary));
            rulesText.setTextSize(15f);
            rulesText.setLineSpacing(0f, 1.3f);
            rulesText.setText(
                    "💡 커뮤니티 핵심 규칙: 존중, 안전, 참여\n" +
                            "이 규칙들은 커뮤니티가 긍정적이고 건설적인 공간이 되도록 보장하는 데 목적이 있습니다.\n\n" +
                            "\n" +
                            "1️⃣ 상호 존중 및 배려 (Respect & Consideration)\n\n" +
                            "1-1️⃣ 비방, 욕설, 혐오 표현을 금지합니다.\n" +
                            "   • 특정 개인, 집단, 인종, 성별 등에 대한 모욕적이거나 차별적인 언행은 허용되지 않습니다.\n" +
                            "   • 비판은 건설적이어야 합니다.\n\n" +
                            "1-2️⃣ 사생활 침해 및 개인 정보 공유를 금지합니다.\n" +
                            "   • 타인의 동의 없이 개인적인 정보(실명, 연락처, 사진 등)를 공개하거나 공유하는 행위는 엄격히 금지됩니다.\n\n" +
                            "1-3️⃣ 저작권을 존중해 주세요.\n" +
                            "   • 타인이 만든 콘텐츠(글, 이미지, 영상 등)를 무단으로 복제, 배포하거나 출처 없이 사용하는 행위를 금지합니다.\n\n" +
                            "\n" +
                            "2️⃣ 안전한 환경 유지 (Safety & Integrity)\n\n" +
                            "2-1️⃣ 주제와 무관한 도배 및 광고를 금지합니다.\n" +
                            "   • 커뮤니티의 주요 논의를 방해하는 반복 메시지, 스팸성 홍보, 영리 목적의 게시물은 삭제 대상입니다.\n\n" +
                            "2-2️⃣ 불법적이거나 위험한 콘텐츠를 공유하지 않습니다.\n" +
                            "   • 법률 위반, 폭력, 자해, 착취 등 위험 행위를 조장하는 모든 콘텐츠 공유를 금지합니다.\n\n" +
                            "2-3️⃣ 분란 조장 및 어그로 행위를 자제해 주세요.\n" +
                            "   • 확인되지 않은 허위 정보를 퍼뜨리거나, 의도적으로 갈등을 유발하는 행위를 금지합니다.\n\n" +
                            "\n" +
                            "3️⃣ 활발하고 건강한 참여 (Active & Constructive Participation)\n\n" +
                            "3-1️⃣ 긍정적이고 생산적인 대화를 지향합니다.\n" +
                            "   • 질문에는 친절하게 답변하고, 의견 차이가 있어도 성숙하게 토론합니다.\n\n" +
                            "3-2️⃣ 자신이 만든 콘텐츠를 환영합니다.\n" +
                            "   • 주제에 맞는 자유롭고 창의적인 의견과 경험을 나누세요.\n\n" +
                            "3-3️⃣ 문제가 생기면 관리자에게 알려주세요.\n" +
                            "   • 규칙 위반이나 기술적 문제가 발생하면 관리자에게 신고해 주세요.\n"
            );

            scrollView.addView(rulesText);

            new AlertDialog.Builder(this)
                    .setTitle("📜 커뮤니티 이용규칙")
                    .setView(scrollView)
                    .setPositiveButton("확인", null)
                    .show();
        });

        // 보안 설정 (지문 로그인)
        textSecurity.setOnClickListener(v -> {
            SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
            boolean biometricEnabled = authPrefs.getBoolean("biometric_enabled", false);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(50, 40, 50, 40);
            layout.setGravity(Gravity.CENTER_VERTICAL);

            TextView label = new TextView(this);
            label.setText("지문 로그인 사용");
            label.setTextSize(16f);
            label.setPadding(0, 0, 20, 0);
            label.setTextColor(getColor(R.color.text_primary));

            Switch switchView = new Switch(this);
            switchView.setChecked(biometricEnabled);

            layout.addView(label);
            layout.addView(switchView);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("보안 설정")
                    .setView(layout)
                    .setPositiveButton("저장", (d, w) -> {
                        boolean newState = switchView.isChecked();

                        authPrefs.edit()
                                .putBoolean("biometric_enabled", newState)
                                .apply();

                        if (newState) {
                            Toast.makeText(this, "지문 로그인이 활성화되었습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "지문 로그인이 비활성화되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("취소", null)
                    .create();

            dialog.show();
        });

        // 🔹 칭호 변경 화면으로 이동
        textAccountTitle.setOnClickListener(v -> {
            Intent intent = new Intent(CategorySettingsActivity.this, TitleChangeActivity.class);
            startActivity(intent);
        });

        // 로그아웃 버튼
        text_logout.setOnClickListener(v -> performLogout());

        // 학과 목록 로딩
        loadDepartmentsFromFirestore();
    }

    /**
     * 테마 설정 다이얼로그
     */

    private void setAppTheme(String theme) {
        SharedPreferences prefs = getSharedPreferences("user_settings", MODE_PRIVATE);
        prefs.edit().putString("app_theme", theme).apply();

        switch (theme) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Toast.makeText(this, "다크 모드가 적용되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(this, "라이트 모드가 적용되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                Toast.makeText(this, "시스템 기본 설정을 따릅니다.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Firestore에서 현재 로그인된 사용자 정보 불러오기
     * - role == admin → 아이디 옆에 " · 관리자" + 프로필 테두리
     * - title → 칭호 텍스트
     * - profileResId → 이미지 + user_profile 캐싱
     */
    private void loadUserInfo() {
        FirebaseUser cur = mAuth.getCurrentUser();

        if (cur == null) {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("skipBiometricOnce", true);
            startActivity(intent);
            finish();
            return;
        }

        String uid = cur.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userId        = documentSnapshot.getString("userId");
                    String studentNumber = documentSnapshot.getString("studentNumber");
                    String major         = documentSnapshot.getString("major");
                    String role          = documentSnapshot.getString("role");
                    String title         = documentSnapshot.getString("title");
                    String resIdString   = documentSnapshot.getString("profileResId");

                    // 아이디 + 관리자 텍스트
                    String displayId = (userId != null) ? userId : "ID 미설정";
                    if ("admin".equals(role)) {
                        displayId = displayId + " · 관리자";
                    }
                    textId.setText(displayId);

                    textStudentId.setText(studentNumber != null ? studentNumber : "학번 미설정");
                    textDepartment.setText(major != null ? major : "학과 미설정");

                    // 칭호 텍스트
                    if (title != null && !title.isEmpty()) {
                        textAccountTitle.setText("칭호 - " + title);
                    } else {
                        textAccountTitle.setText("칭호 - 없음");
                    }

                    // 관리자라면 프로필 테두리 적용
                    if ("admin".equals(role)) {
                        userProfile.setBackgroundResource(R.drawable.bg_admin_profile);
                        int padding = (int) (4 * getResources().getDisplayMetrics().density);
                        userProfile.setPadding(padding, padding, padding, padding);
                    } else {
                        userProfile.setBackground(null);
                        userProfile.setPadding(0, 0, 0, 0);
                    }

                    // profileResId 처리 + 캐싱
                    if (resIdString != null) {
                        try {
                            int savedResId = Integer.parseInt(resIdString);
                            selectedProfileResId = savedResId;
                            userProfile.setImageResource(savedResId);

                            SharedPreferences pref = getSharedPreferences("user_profile", MODE_PRIVATE);
                            pref.edit().putInt("profile_res_id", savedResId).apply();

                            cachedProfileResId = savedResId;
                        } catch (NumberFormatException e) {
                            userProfile.setImageResource(R.drawable.default_profile);
                            selectedProfileResId = R.drawable.default_profile;
                        }
                    } else {
                        userProfile.setImageResource(R.drawable.default_profile);
                        selectedProfileResId = R.drawable.default_profile;
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "데이터 불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 프로필 선택 다이얼로그 표시
     * - 기종 상관 없이 동일한 모양으로 나오도록 dp 기준 고정 사이즈 사용
     */
    private void showProfilePickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_profile_picker);
        dialog.show();

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(width, height);
        }

        GridView gridView = dialog.findViewById(R.id.grid_profile_images);
        Button btnSave    = dialog.findViewById(R.id.btn_save);
        Button btnCancel  = dialog.findViewById(R.id.btn_cancel);

        Integer[] profileImages = {
                R.drawable.default_profile,
                R.drawable.coopy_1,
                R.drawable.cri_1,
                R.drawable.culty_1
        };

        // 🔹 기종에 상관 없는 고정 dp 기준 사이즈
        float density = getResources().getDisplayMetrics().density;
        int itemSize  = (int) (84 * density);   // 하나의 셀(아바타 박스) 크기
        int checkSize = (int) (24 * density);   // 체크 아이콘 크기
        int padding   = (int) (6 * density);    // 내부 여백

        // GridView 구성 통일
        gridView.setNumColumns(3);
        gridView.setHorizontalSpacing((int) (8 * density));
        gridView.setVerticalSpacing((int) (8 * density));

        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,
                android.R.layout.simple_list_item_1, profileImages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                FrameLayout frame;
                ImageView imageView, checkView;

                if (convertView == null) {
                    frame = new FrameLayout(CategorySettingsActivity.this);

                    // 셀 자체 크기 고정
                    GridView.LayoutParams lp =
                            new GridView.LayoutParams(itemSize, itemSize);
                    frame.setLayoutParams(lp);

                    // 프로필 이미지 뷰
                    imageView = new ImageView(CategorySettingsActivity.this);
                    FrameLayout.LayoutParams imageParams =
                            new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            );
                    imageView.setLayoutParams(imageParams);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setPadding(padding, padding, padding, padding);

                    // 체크 표시 뷰 (오버레이)
                    checkView = new ImageView(CategorySettingsActivity.this);
                    FrameLayout.LayoutParams checkParams =
                            new FrameLayout.LayoutParams(checkSize, checkSize);
                    checkParams.gravity = Gravity.END | Gravity.TOP;
                    checkParams.topMargin = padding;
                    checkParams.rightMargin = padding;
                    checkView.setLayoutParams(checkParams);
                    checkView.setImageResource(R.drawable.ic_check);
                    checkView.setVisibility(View.GONE);

                    frame.addView(imageView);
                    frame.addView(checkView);
                } else {
                    frame = (FrameLayout) convertView;
                    imageView = (ImageView) frame.getChildAt(0);
                    checkView = (ImageView) frame.getChildAt(1);
                }

                imageView.setImageResource(profileImages[position]);
                checkView.setVisibility(
                        profileImages[position].equals(selectedProfileResId)
                                ? View.VISIBLE
                                : View.GONE
                );
                return frame;
            }
        };

        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            selectedProfileResId = profileImages[position];
            adapter.notifyDataSetChanged();
        });

        btnSave.setOnClickListener(v -> {
            userProfile.setImageResource(selectedProfileResId);
            saveProfileResId(selectedProfileResId);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * Firestore + SharedPreferences에 선택된 프로필 이미지 리소스 ID 저장
     */
    private void saveProfileResId(int resId) {
        FirebaseUser cur = mAuth.getCurrentUser();
        if (cur == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = cur.getUid();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("profileResId", String.valueOf(resId));

        db.collection("users").document(uid)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "프로필 사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    SharedPreferences pref = getSharedPreferences("user_profile", MODE_PRIVATE);
                    pref.edit().putInt("profile_res_id", resId).apply();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "프로필 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 로그아웃 처리
     */
    private void performLogout() {
        FirebaseAuth.getInstance().signOut();

        getSharedPreferences("loginPrefs", MODE_PRIVATE).edit().clear().apply();
        // 자동 로그인용 비밀번호(암호화 저장소)도 함께 삭제
        SecureCredentialStore.removePassword(this);

        getSharedPreferences("auth_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("biometric_enabled", false)
                .apply();

        Intent intent = new Intent(CategorySettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("skipBiometricOnce", true);
        startActivity(intent);
        finish();
    }

    /**
     * ID 변경
     */
    private void changeUserId() {
        FirebaseUser cur = mAuth.getCurrentUser();
        if (cur == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("새로운 아이디 입력");
        input.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginInDp = (int) (24 * getResources().getDisplayMetrics().density);
        params.setMargins(marginInDp, 0, marginInDp, 0);
        input.setLayoutParams(params);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("아이디 변경")
                .setView(container)
                .setPositiveButton("변경", (dialog, which) -> {
                    String newId = input.getText().toString().trim();
                    if (newId.isEmpty()) {
                        Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("아이디 변경 확인")
                            .setMessage("아이디를 \"" + newId + "\"(으)로 변경하시겠습니까?")
                            .setPositiveButton("예", (confirmDialog, w) -> {
                                db.collection("users").document(cur.getUid())
                                        .update("userId", newId)
                                        .addOnSuccessListener(a -> {
                                            loadUserInfo(); // role/관리자 텍스트 포함 다시 적용
                                            Toast.makeText(this, "아이디가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                            })
                            .setNegativeButton("아니요", null)
                            .show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 비밀번호 변경
     */
    private void changePassword() {
        FirebaseUser cur = mAuth.getCurrentUser();
        if (cur == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(margin, margin / 2, margin, margin / 2);

        TextView labelCurrent = new TextView(this);
        labelCurrent.setText("기존 비밀번호");
        labelCurrent.setTextSize(13);
        labelCurrent.setTextColor(Color.DKGRAY);
        container.addView(labelCurrent);

        EditText currentPwInput = new EditText(this);
        currentPwInput.setHint("현재 비밀번호 입력");
        currentPwInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(createPasswordFieldWithToggle(currentPwInput));

        TextView labelNew = new TextView(this);
        labelNew.setText("새 비밀번호");
        labelNew.setTextSize(13);
        labelNew.setTextColor(Color.DKGRAY);
        container.addView(labelNew);

        EditText newPwInput = new EditText(this);
        newPwInput.setHint("새 비밀번호 입력 (6자 이상)");
        newPwInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(createPasswordFieldWithToggle(newPwInput));

        EditText confirmPwInput = new EditText(this);
        confirmPwInput.setHint("새 비밀번호를 다시 입력");
        confirmPwInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(createPasswordFieldWithToggle(confirmPwInput));

        new AlertDialog.Builder(this)
                .setTitle("비밀번호 변경")
                .setView(container)
                .setPositiveButton("변경", (dialog, which) -> {
                    String currentPw = currentPwInput.getText().toString().trim();
                    String newPw     = newPwInput.getText().toString().trim();
                    String confirmPw = confirmPwInput.getText().toString().trim();

                    if (currentPw.isEmpty()) {
                        Toast.makeText(this, "현재 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPw.length() < 6) {
                        Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPw.equals(confirmPw)) {
                        Toast.makeText(this, "새 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider.getCredential(cur.getEmail(), currentPw);
                    cur.reauthenticate(credential)
                            .addOnSuccessListener(a -> {
                                cur.updatePassword(newPw)
                                        .addOnSuccessListener(b -> {
                                            Toast.makeText(this, "비밀번호가 변경되었습니다. 다시 로그인하세요.", Toast.LENGTH_LONG).show();
                                            performLogout();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "현재 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private LinearLayout createPasswordFieldWithToggle(EditText editText) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        editText.setLayoutParams(editParams);

        ImageButton toggleButton = new ImageButton(this);
        toggleButton.setImageResource(android.R.drawable.ic_menu_view);
        toggleButton.setBackgroundColor(Color.TRANSPARENT);
        toggleButton.setPadding(20, 20, 20, 20);

        toggleButton.setOnClickListener(v -> {
            if (editText.getInputType() ==
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleButton.setImageResource(android.R.drawable.ic_menu_view);
            }
            editText.setSelection(editText.getText().length());
        });

        layout.addView(editText);
        layout.addView(toggleButton);
        return layout;
    }

    /**
     * 전공 변경
     */
    private void changeMajor() {
        FirebaseUser cur = mAuth.getCurrentUser();
        if (cur == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.department_dialog, null);
        EditText editSearch        = dialogView.findViewById(R.id.editSearchDepartment);
        RecyclerView recyclerView  = dialogView.findViewById(R.id.departmentRecyclerView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("전공 선택")
                .setView(dialogView)
                .create();

        List<String> filteredList = new ArrayList<>(departmentList);

        DepartmentAdapter adapter = new DepartmentAdapter(filteredList, selected -> {
            new AlertDialog.Builder(this)
                    .setTitle("전공 변경 확인")
                    .setMessage("학과를 \"" + selected + "\"(으)로 변경하시겠습니까?")
                    .setPositiveButton("예", (d, w) -> {
                        db.collection("users").document(cur.getUid())
                                .update("major", selected)
                                .addOnSuccessListener(a -> {
                                    textDepartment.setText(selected);
                                    Toast.makeText(this, "전공이 \"" + selected + "\"(으)로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        dialog.dismiss();
                    })
                    .setNegativeButton("아니요", null)
                    .show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filteredList.clear();
                for (String item : departmentList) {
                    if (item.toLowerCase().contains(s.toString().toLowerCase())) {
                        filteredList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        dialog.show();
    }

    private void loadDepartmentsFromFirestore() {
        db.collection("departments_detail").get()
                .addOnSuccessListener(query -> {
                    departmentList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String deptName = doc.getString("department_name");
                        if (deptName != null && !departmentList.contains(deptName)) {
                            departmentList.add(deptName);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "학과 목록 로딩 실패", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * 이메일 변경
     */
    private void changeEmail() {
        FirebaseUser cur = mAuth.getCurrentUser();
        if (cur == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(margin, margin / 2, margin, margin / 2);

        EditText newEmailInput = new EditText(this);
        newEmailInput.setHint("새 이메일 입력");
        newEmailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        container.addView(newEmailInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("이메일 변경")
                .setView(container)
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.END);
            buttonLayout.setPadding(0, 30, 0, 0);

            Button sendButton = new Button(this);
            sendButton.setText("인증 메일 보내기");
            sendButton.setTextColor(Color.WHITE);
            sendButton.setBackgroundColor(Color.parseColor("#007BFF"));
            sendButton.setPadding(40, 20, 40, 20);

            buttonLayout.addView(sendButton);
            container.addView(buttonLayout);

            sendButton.setOnClickListener(v -> {
                String newEmail = newEmailInput.getText().toString().trim();
                if (newEmail.isEmpty() ||
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    Toast.makeText(this, "올바른 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                sendButton.setEnabled(false);

                cur.verifyBeforeUpdateEmail(newEmail)
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this,
                                    "새 이메일로 인증 메일을 전송했습니다.\n메일에서 인증을 완료한 후 아래 버튼을 누르세요.",
                                    Toast.LENGTH_LONG).show();

                            showEmailVerificationDialog(cur, newEmail);
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            sendButton.setEnabled(true);
                            Toast.makeText(this, "이메일 변경 요청 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
        });

        dialog.show();
    }

    private void showEmailVerificationDialog(FirebaseUser cur, String newEmail) {
        new AlertDialog.Builder(this)
                .setTitle("이메일 인증 확인")
                .setMessage("새 이메일로 전송된 인증 링크를 클릭한 후 아래 버튼을 눌러주세요.")
                .setPositiveButton("인증 완료", (dialog, which) -> {
                    cur.reload()
                            .addOnSuccessListener(a -> {
                                String updatedEmail = cur.getEmail();
                                Log.d("EmailChange", "Reload 성공. 현재 이메일: " + updatedEmail);

                                if (updatedEmail != null && updatedEmail.equals(newEmail)) {
                                    db.collection("users").document(cur.getUid())
                                            .update("email", newEmail)
                                            .addOnSuccessListener(b ->
                                                    Toast.makeText(this,
                                                            "이메일이 성공적으로 변경되었습니다.",
                                                            Toast.LENGTH_SHORT).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this,
                                                            "Firestore 업데이트 실패: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                } else {
                                    Toast.makeText(this,
                                            "이메일 인증이 아직 완료되지 않았습니다.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EmailChange", "reload() 실패", e);

                                if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                    showReLoginDialog(newEmail);
                                } else {
                                    Toast.makeText(this,
                                            "인증 확인 실패: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    /**
     * 세션 만료 시 재로그인 다이얼로그
     */
    private void showReLoginDialog(String newEmail) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(margin, margin / 2, margin, margin / 2);

        EditText emailInput = new EditText(this);
        emailInput.setHint("이메일 입력");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(newEmail);
        container.addView(emailInput);

        EditText pwInput = new EditText(this);
        pwInput.setHint("비밀번호 입력");
        pwInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(pwInput);

        new AlertDialog.Builder(this)
                .setTitle("재로그인 필요")
                .setMessage("보안을 위해 다시 로그인해야 합니다.")
                .setView(container)
                .setPositiveButton("로그인", (d, w) -> {
                    String email = emailInput.getText().toString().trim();
                    String pw    = pwInput.getText().toString().trim();

                    if (email.isEmpty() || pw.isEmpty()) {
                        Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mAuth.signInWithEmailAndPassword(email, pw)
                            .addOnSuccessListener(result -> {
                                FirebaseUser newUser = result.getUser();
                                if (newUser != null) {
                                    db.collection("users").document(newUser.getUid())
                                            .update("email", newEmail)
                                            .addOnSuccessListener(a ->
                                                    Toast.makeText(this,
                                                            "이메일이 성공적으로 변경 및 동기화되었습니다.",
                                                            Toast.LENGTH_LONG).show()
                                            )
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this,
                                                            "Firestore 업데이트 실패: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("취소", null)
                .show();
    }


    // --- DepartmentAdapter (학과 목록 RecyclerView 어댑터) ---
    private static class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {
        private List<String> data;
        private OnDepartmentSelectedListener listener;

        public interface OnDepartmentSelectedListener {
            void onSelected(String departmentName);
        }

        public DepartmentAdapter(List<String> data, OnDepartmentSelectedListener listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String departmentName = data.get(position);
            holder.text.setText(departmentName);
            holder.itemView.setOnClickListener(v -> listener.onSelected(departmentName));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        // 실시간 검색을 위해 추가된 메서드
        public void updateList(List<String> newList) {
            data = newList;
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;

            ViewHolder(View itemView) {
                super(itemView);
                text = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
