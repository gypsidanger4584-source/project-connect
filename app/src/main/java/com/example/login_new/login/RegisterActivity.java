package com.example.login_new.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView; // TextView가 필요하다면 유지
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.android.material.textfield.TextInputEditText; // TextInputEditText 유지
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // ★ 수정 없음: 모든 입력 필드는 TextInputEditText입니다.
    private TextInputEditText etUserId, etPassword, etStudentNo, etEmail, etDepartment;
    private Button btnIdCheck, btnSubmit, btnSendCode, btnVerify;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;

    private boolean isIdValidated = false;
    private boolean isEmailVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // View 바인딩
        // ★ etDepartment의 ID를 XML에 맞게 searchDepartmentLayout으로 수정합니다.
        etUserId     = findViewById(R.id.personid);
        etPassword   = findViewById(R.id.passwordText);
        etStudentNo  = findViewById(R.id.studentNumberText);
        etEmail      = findViewById(R.id.emailText);
        etDepartment = findViewById(R.id.editSearchDepartment); // ★ 최종 수정된 ID

        btnIdCheck   = findViewById(R.id.validateButton);
        btnSubmit    = findViewById(R.id.registerButton);
        btnSendCode  = findViewById(R.id.btnSendVerification);
        btnVerify    = findViewById(R.id.btnVerifyEmail);

        // 아이디 중복 확인
        btnIdCheck.setOnClickListener(v -> checkIdDuplicate());

        // 이메일 인증 메일 전송 (계정 생성 → 인증메일 발송)
        btnSendCode.setOnClickListener(v -> sendVerificationEmail());

        // 이메일 인증 여부 갱신
        btnVerify.setOnClickListener(v -> refreshEmailVerifiedState());

        // 회원가입 완료 처리
        btnSubmit.setOnClickListener(v -> tryRegister());
    }

    /* ---------- 유틸 ---------- */

    // TextInputEditText만 사용하므로 getText(TextView tv) 오버로드는 제거했습니다.
    private String getText(TextInputEditText et) {
        // TextInputEditText는 EditText를 상속하므로 getText()가 안전하게 호출됩니다.
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private boolean validateInputsForIdCheck() {
        String userId = getText(etUserId);

        if (TextUtils.isEmpty(userId)) {
            toast("아이디를 입력하세요.");
            return false;
        }
        if (userId.length() < 4) {
            toast("아이디는 4자 이상 입력해주세요.");
            return false;
        }
        if (userId.length() > 10) {
            toast("아이디는 10자 이내로 입력해주세요.");
            return false;
        }
        return true;
    }

    private boolean validateInputsForAuthCreate() {
        String email = getText(etEmail);
        String password = getText(etPassword);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("유효한 이메일을 입력하세요.");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            toast("비밀번호는 6자 이상 입력하세요.");
            return false;
        }
        return true;
    }

    private boolean validateInputsForRegister() {
        String studentNo = getText(etStudentNo);
        String major = getText(etDepartment);

        if (!isIdValidated) {
            toast("아이디 중복 확인을 먼저 해주세요.");
            return false;
        }
        if (!isEmailVerified) {
            toast("이메일 인증을 완료해주세요.");
            return false;
        }
        if (TextUtils.isEmpty(studentNo)) {
            toast("학번을 입력하세요.");
            return false;
        }
        if (!studentNo.matches("\\d+")) {
            toast("학번은 숫자만 입력하세요.");
            return false;
        }
        if (TextUtils.isEmpty(major)) {
            // ★ etDepartment가 TextInputEditText이므로, 여기에 텍스트를 직접 입력받거나
            // 클릭 리스너를 추가하여 학과 검색 액션을 유도해야 합니다.
            toast("학과를 입력(검색)하세요.");
            return false;
        }
        return true;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /* ---------- 기능 ---------- */

    private void checkIdDuplicate() {
        if (!validateInputsForIdCheck()) return;

        String userId = getText(etUserId);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        toast("❌ 이미 사용 중인 아이디입니다.");
                        isIdValidated = false;
                    } else {
                        toast("✅ 사용 가능한 아이디입니다.");
                        isIdValidated = true;
                    }
                })
                .addOnFailureListener(e -> toast("아이디 확인 실패: " + e.getMessage()));
    }

    private void sendVerificationEmail() {
        if (!validateInputsForAuthCreate()) return;

        String email = getText(etEmail);
        String password = getText(etPassword);

        // 이미 로그인된 유저가 있으면 그 계정으로 처리
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current != null && email.equalsIgnoreCase(current.getEmail())) {
            current.sendEmailVerification()
                    .addOnSuccessListener(aVoid -> toast("📨 인증 메일을 다시 보냈습니다."))
                    .addOnFailureListener(e -> toast("인증 메일 전송 실패: " + e.getMessage()));
            return;
        }

        // 새 계정 생성 후 인증메일 발송
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        firebaseUser.sendEmailVerification()
                                .addOnSuccessListener(aVoid -> toast("📨 이메일 인증 링크가 전송되었습니다."))
                                .addOnFailureListener(e -> toast("인증 메일 전송 실패: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> {
                    // 이미 가입된 이메일 등
                    toast("계정 생성/전송 실패: " + e.getMessage());
                });
    }

    private void refreshEmailVerifiedState() {
        firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            toast("먼저 이메일/비밀번호로 인증 메일을 보내주세요.");
            return;
        }
        firebaseUser.reload()
                .addOnSuccessListener(aVoid -> {
                    if (firebaseUser.isEmailVerified()) {
                        isEmailVerified = true;
                        toast("✅ 이메일 인증이 완료되었습니다.");
                    } else {
                        isEmailVerified = false;
                        toast("❗ 이메일 인증이 아직 완료되지 않았습니다.");
                    }
                })
                .addOnFailureListener(e -> toast("인증 상태 갱신 실패: " + e.getMessage()));
    }

    private void tryRegister() {
        if (!validateInputsForRegister()) return;

        String userId   = getText(etUserId);
        String email    = getText(etEmail);
        String studentNo= getText(etStudentNo);
        String major    = getText(etDepartment);

        // Firestore에 저장 (비밀번호는 저장하지 않음)
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userId);
        userMap.put("email", email);
        userMap.put("studentNo", studentNo);
        userMap.put("major", major);
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId).set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "🎉 회원가입이 완료되었습니다!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> toast("Firestore 저장 실패: " + e.getMessage()));
    }
}
