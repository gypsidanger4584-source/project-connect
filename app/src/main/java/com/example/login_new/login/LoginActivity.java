package com.example.login_new.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // emailInput 변수는 XML의 R.id.studentNumberText를 받습니다.
    private EditText emailInput, passwordText;

    private MaterialButton loginButton;
    private TextView registerButton;
    private TextView findPassword;
    private FirebaseAuth mAuth;

    private SharedPreferences prefs;

    // ★★★ 중요: 회원가입 시 사용한 이메일 도메인과 일치해야 합니다.
    private static final String YONSEI_DOMAIN = "@ysu.ac.kr";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // XML ID 바인딩 (activity_login.xml의 ID를 사용)
        emailInput = findViewById(R.id.studentNumberText);
        passwordText = findViewById(R.id.passwordText);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        findPassword = findViewById(R.id.findPasswordButton);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);

        // ✅ 자동 입력 처리 (저장된 이메일에서 학번만 추출하여 표시)
        String savedEmail = prefs.getString("userEmail", "");

        // 저장된 이메일이 있다면 @ 앞의 학번만 추출하여 필드에 표시
        if (savedEmail.contains("@")) {
            String savedStudentId = savedEmail.substring(0, savedEmail.indexOf("@"));
            emailInput.setText(savedStudentId);
        }

        loginButton.setOnClickListener(v -> {
            // 1. 사용자가 입력한 값은 학번(ID)입니다.
            String studentId = emailInput.getText().toString().trim();
            String password = passwordText.getText().toString().trim();

            if (studentId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "학번과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. 학번을 Firebase 로그인에 필요한 이메일 형식으로 변환 (핵심 수정)
            String emailToLogin;

            // 사용자가 혹시라도 @까지 입력했다면 그대로 사용하고, 아니면 도메인을 붙여줍니다.
            if (studentId.contains("@")) {
                emailToLogin = studentId;
            } else {
                emailToLogin = studentId + YONSEI_DOMAIN;
            }

            // 3. 변환된 이메일로 Firebase 로그인 시도
            mAuth.signInWithEmailAndPassword(emailToLogin, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // ✅ 로그인 성공 및 인증됨

                                // SharedPreferences에 학번이 아닌 '이메일'을 저장합니다.
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("userEmail", emailToLogin);
                                editor.putBoolean("emailVerified", true);
                                editor.apply();

                                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "이메일 인증을 완료해주세요.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // 로그인 실패 메시지 출력
                            Toast.makeText(this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        findPassword.setOnClickListener(v -> {
            Toast.makeText(this, "아이디/비밀번호 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show();
        });
    }
}
