package com.example.login_new.alogic; // ✅ 패키지 경로: alogic

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox; // ✅ 자동 로그인: CheckBox 추가
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// Firebase
import com.example.login_new.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// Biometric imports
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;

import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;

public class LoginActivity extends AppCompatActivity {

    private EditText emailText, passwordText;
    private Button loginButton;
    private TextView registerButton;
    private TextView findPassword;
    private CheckBox chkAutoLogin; // ✅ 자동 로그인: CheckBox 변수 추가

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    // Biometric prefs/keystore
    private static final String BIOMETRIC_PREFS = "auth_prefs";
    private static final String PREF_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String PREF_FIRST_PROMPT_SHOWN = "first_prompt_shown";
    private static final String KEYSTORE_ALIAS = "login_biometric_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    // ✅ 자동 로그인: SharedPreferences 상수 추가
    private static final String KEY_AUTO_LOGIN = "AutoLogin";
    private static final String KEY_USER_STUDENT_ID = "UserStudentId";
    private static final String KEY_USER_PASSWORD = "UserPassword";

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private Executor biometricExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);

        emailText = findViewById(R.id.studentNumberText);
        passwordText = findViewById(R.id.passwordText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        findPassword = findViewById(R.id.findPasswordButton);
        chkAutoLogin = findViewById(R.id.chkAutoLogin); // ✅ 자동 로그인: 체크박스 초기화

        // 항상 보이도록
        emailText.setVisibility(View.VISIBLE);
        emailText.setText("");



        loginButton.setOnClickListener(v -> doLogin());

        registerButton.setOnClickListener(v -> startActivity(new Intent(this, com.example.login_new.alogic.RegisterActivity.class)));

        findPassword.setOnClickListener(v -> sendPasswordReset());

        // 지문 로그인 초기화(자동 인증 먼저) + 생체정보 변경 감지
        initBiometricIfAvailable();
        checkBiometricChangeAndReofferIfNeeded();
    }

    /**
     * 핵심 로직: 학번(studentNumber)을 받아 Firestore users 컬렉션에서 이메일을 찾아서
     * 그 이메일로 FirebaseAuth 로그인 수행.
     */
    private void doLogin() {
        String studentNumber = emailText.getText().toString().trim();
        String pw = passwordText.getText().toString().trim();

        if (studentNumber.isEmpty() || pw.isEmpty()) {
            toast("학번과 비밀번호를 입력하세요.");
            return;
        }

        // ⭐️ 학번 형식 클라이언트 측 검증 추가 (10자리 숫자) ⭐️
        if (studentNumber.length() != 10 || !studentNumber.matches("\\d{10}")) {
            toast("학번은 정확히 10자리 숫자로 입력해 주세요.");
            emailText.setError("10자리 학번만 가능");
            return;
        }
        emailText.setError(null);

        setUiEnabled(false);

        // 1. Firestore에서 학번 -> 이메일 조회
        db.collection("users")
                .whereEqualTo("studentNumber", studentNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        setUiEnabled(true);
                        toast("해당 학번으로 가입된 계정이 없습니다.");
                        // ✅ 자동 로그인: 로그인 실패 시 상태 해제
                        prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                        return;
                    }

                    String email = querySnapshot.getDocuments().get(0).getString("email");
                    if (email == null || email.isEmpty()) {
                        setUiEnabled(true);
                        toast("계정 이메일을 찾을 수 없습니다. 관리자에게 문의하세요.");
                        // ✅ 자동 로그인: 로그인 실패 시 상태 해제
                        prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                        return;
                    }

                    // 2. 찾은 이메일로 FirebaseAuth 로그인 시도
                    mAuth.signInWithEmailAndPassword(email, pw)
                            .addOnCompleteListener(task -> {
                                setUiEnabled(true);
                                if (!task.isSuccessful()) {
                                    String msg = task.getException() != null ? task.getException().getMessage() : "로그인 실패";
                                    toast("로그인 실패: " + msg);
                                    // ✅ 자동 로그인: 로그인 실패 시 상태 해제
                                    prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                                    return;
                                }

                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user == null) {
                                    toast("로그인 상태를 확인할 수 없습니다. 다시 시도해주세요.");
                                    return;
                                }

                                if (user.isEmailVerified()) {
                                    // ✅ 자동 로그인: 성공 시 학번/비밀번호 저장 로직 호출
                                    saveAutoLoginState(studentNumber, pw);

                                    onPasswordLoginSuccess_ShowBiometricOfferIfNeeded(() -> {
                                        startActivity(new Intent(this, MainActivity.class)
                                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                                        finish();
                                    });
                                } else {
                                    showNotVerifiedDialog(user);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    setUiEnabled(true);
                    toast("로그인 오류: " + e.getMessage());
                    // ✅ 자동 로그인: 로그인 실패 시 상태 해제
                    prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                });
    }

    /**
     * ✅ 자동 로그인: 앱 실행 시 저장된 상태 확인 및 로그인 시도
     */
    private void checkAndAttemptAutoLogin() {
        boolean isAutoLoginEnabled = prefs.getBoolean(KEY_AUTO_LOGIN, false);

        // UI에 체크박스 상태 반영
        chkAutoLogin.setChecked(isAutoLoginEnabled);

        if (isAutoLoginEnabled) {
            String savedStudentId = prefs.getString(KEY_USER_STUDENT_ID, null);
            String savedPassword = prefs.getString(KEY_USER_PASSWORD, null);

            if (!TextUtils.isEmpty(savedStudentId) && !TextUtils.isEmpty(savedPassword)) {
                // UI를 비활성화하고 자동 로그인 시도
                setUiEnabled(false);
                toast("자동 로그인 시도 중...");

                // 1. Firestore에서 학번 -> 이메일 조회 (doLogin 로직 재활용)
                db.collection("users")
                        .whereEqualTo("studentNumber", savedStudentId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                setUiEnabled(true);
                                toast("자동 로그인 실패: 계정 정보 불일치");
                                prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                                return;
                            }

                            String email = querySnapshot.getDocuments().get(0).getString("email");
                            if (email == null || email.isEmpty()) {
                                setUiEnabled(true);
                                toast("자동 로그인 실패: 이메일 정보 오류");
                                prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                                return;
                            }

                            // 2. 찾은 이메일로 FirebaseAuth 로그인 시도
                            mAuth.signInWithEmailAndPassword(email, savedPassword)
                                    .addOnCompleteListener(task -> {
                                        setUiEnabled(true);
                                        if (!task.isSuccessful()) {
                                            toast("자동 로그인 실패: 인증 오류");
                                            prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                                            return;
                                        }

                                        FirebaseUser user = mAuth.getCurrentUser();
                                        if (user != null && user.isEmailVerified()) {
                                            // 성공 시 메인 화면으로 이동 (지문 제안 건너뛰기)
                                            toast("자동 로그인 성공");
                                            startActivity(new Intent(this, MainActivity.class)
                                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                                            finish();
                                        } else {
                                            // 이메일 미인증 상태로 로그인됨
                                            toast("자동 로그인 실패: 이메일 인증 필요");
                                            prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            setUiEnabled(true);
                            toast("자동 로그인 오류: " + e.getMessage());
                            prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                        });
            } else {
                // 저장된 정보가 없으면 자동 로그인 상태 해제
                prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
            }
        }
    }

    /**
     * ✅ 자동 로그인: 로그인 성공 후 상태를 SharedPreferences에 저장/삭제
     */
    private void saveAutoLoginState(String studentId, String password) {
        SharedPreferences.Editor editor = prefs.edit();

        boolean isChecked = chkAutoLogin.isChecked();
        editor.putBoolean(KEY_AUTO_LOGIN, isChecked);

        if (isChecked) {
            // 자동 로그인 활성화 시, 학번과 비밀번호 저장
            editor.putString(KEY_USER_STUDENT_ID, studentId);
            editor.putString(KEY_USER_PASSWORD, password);
        } else {
            // 자동 로그인 해제 시, 저장된 정보 삭제
            editor.remove(KEY_USER_STUDENT_ID);
            editor.remove(KEY_USER_PASSWORD);
        }

        editor.apply();
    }


    /**
     * 학번으로 비밀번호 재설정 메일 보내기: 학번 -> 이메일 조회 후 sendPasswordResetEmail 호출
     */
    private void sendPasswordReset() {
        String studentNumber = emailText.getText().toString().trim();
        if (studentNumber.isEmpty()) { toast("먼저 학번을 입력하세요."); return; }

        // ⭐️ 학번 형식 클라이언트 측 검증 추가 ⭐️
        if (studentNumber.length() != 10 || !studentNumber.matches("\\d{10}")) {
            toast("학번은 정확히 10자리 숫자로 입력해 주세요.");
            emailText.setError("10자리 학번만 가능");
            return;
        }
        emailText.setError(null);

        setUiEnabled(false);
        db.collection("users")
                .whereEqualTo("studentNumber", studentNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    setUiEnabled(true);
                    if (querySnapshot.isEmpty()) {
                        toast("해당 학번으로 가입된 계정이 없습니다.");
                        return;
                    }
                    String email = querySnapshot.getDocuments().get(0).getString("email");
                    if (email == null || email.isEmpty()) {
                        toast("이메일 정보를 찾을 수 없습니다.");
                        return;
                    }
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(v -> toast("비밀번호 재설정 메일을 보냈습니다."))
                            .addOnFailureListener(e -> toast("전송 실패: " + e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    setUiEnabled(true);
                    toast("조회 실패: " + e.getMessage());
                });
    }

    private void showNotVerifiedDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("이메일 인증 필요")
                .setMessage("이메일 인증을 완료한 후 다시 로그인해 주세요. 인증 메일을 재전송할까요?")
                .setPositiveButton("재전송", (d, w) -> {
                    user.sendEmailVerification();
                    toast("인증 메일을 재전송했습니다.");
                })
                .setNegativeButton("닫기", (d, w) -> {
                    // ✅ 자동 로그인: 인증 실패 시 자동 로그인 상태 해제
                    prefs.edit().putBoolean(KEY_AUTO_LOGIN, false).apply();
                })
                .show();
    }

    private void setUiEnabled(boolean enabled) {
        if (emailText != null) emailText.setEnabled(enabled);
        if (passwordText != null) passwordText.setEnabled(enabled);
        if (loginButton != null) loginButton.setEnabled(enabled);
        if (chkAutoLogin != null) chkAutoLogin.setEnabled(enabled); // ✅ 자동 로그인: 체크박스도 활성화/비활성화
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ===================== Biometric additions START =====================

    private void initBiometricIfAvailable() {
        biometricExecutor = ContextCompat.getMainExecutor(this);

        BiometricManager manager = BiometricManager.from(this);
        int canAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        // 🔹 지문 기능이 없는 경우 → 바로 자동 로그인 시도
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            checkAndAttemptAutoLogin();
            return;
        }

        biometricPrompt = new BiometricPrompt(this, biometricExecutor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                toast("지문 인증 성공");

                // ✅ 지문 인증 성공 시: 자동 로그인 정보 사용하여 Firebase 로그인
                String savedStudentId = prefs.getString(KEY_USER_STUDENT_ID, null);
                String savedPassword = prefs.getString(KEY_USER_PASSWORD, null);
                if (savedStudentId != null && savedPassword != null) {
                    signInWithSavedCredentials(savedStudentId, savedPassword);
                } else {
                    toast("저장된 로그인 정보가 없습니다.");
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                toast("지문 인증 취소됨");
                // ❌ 사용자가 취소하면 → 자동 로그인 시도
                checkAndAttemptAutoLogin();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                toast("지문 인증 실패");
                // ❌ 실패해도 자동 로그인 시도
                checkAndAttemptAutoLogin();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("지문으로 로그인")
                .setSubtitle("등록된 생체인식으로 로그인합니다")
                .setNegativeButtonText("취소")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }


    // 기존 무인자 메서드는 유지 (혹시 기존 코드에서 호출 중일 수 있으니)
    private void onPasswordLoginSuccess_ShowBiometricOfferIfNeeded() {
        onPasswordLoginSuccess_ShowBiometricOfferIfNeeded(() -> {});
    }

    // ✅ 콜백 버전: 팝업 응답 이후 onDone.run()으로 다음 화면 이동
    private void onPasswordLoginSuccess_ShowBiometricOfferIfNeeded(Runnable onDone) {
        boolean firstShown = getPrefs().getBoolean(PREF_FIRST_PROMPT_SHOWN, false);
        boolean enabled = isBiometricEnabled();

        if (enabled) { onDone.run(); return; }

        if (!firstShown) {
            new AlertDialog.Builder(this)
                    .setTitle("지문 로그인 설정")
                    .setMessage("지문(또는 얼굴) 인증으로 앞으로 빠르게 로그인하시겠습니까?")
                    .setPositiveButton("예", (d, w) -> {
                        BiometricManager bm = BiometricManager.from(this);
                        int can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
                        if (can == BiometricManager.BIOMETRIC_SUCCESS) {
                            ensureKey();
                            getPrefs().edit().putBoolean(PREF_BIOMETRIC_ENABLED, true).apply();
                            toast("지문 로그인이 활성화되었습니다.");
                        } else {
                            toast("이 기기에서 생체 인증을 사용할 수 없습니다.");
                        }
                        getPrefs().edit().putBoolean(PREF_FIRST_PROMPT_SHOWN, true).apply();
                        onDone.run();
                    })
                    .setNegativeButton("아니오", (d, w) -> {
                        getPrefs().edit().putBoolean(PREF_FIRST_PROMPT_SHOWN, true).apply();
                        toast("설정에서 언제든지 지문 로그인을 켤 수 있어요.");
                        onDone.run();
                    })
                    .show();
        } else {
            onDone.run();
        }
    }

    private void checkBiometricChangeAndReofferIfNeeded() {
        if (!isBiometricEnabled()) return;
        if (!isKeyUsable()) {
            getPrefs().edit().putBoolean(PREF_BIOMETRIC_ENABLED, false).apply();
            new AlertDialog.Builder(this)
                    .setTitle("생체정보가 변경됨")
                    .setMessage("기기에서 생체정보(지문/얼굴)가 변경되었습니다. 다시 등록하시겠습니까?")
                    .setPositiveButton("다시 등록", (d, w) -> {
                        ensureKey();
                        getPrefs().edit().putBoolean(PREF_BIOMETRIC_ENABLED, true).apply();
                        toast("지문 로그인이 활성화되었습니다.");
                    })
                    .setNegativeButton("나중에", null)
                    .show();
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(BIOMETRIC_PREFS, MODE_PRIVATE);
    }

    private boolean isBiometricEnabled() {
        return getPrefs().getBoolean(PREF_BIOMETRIC_ENABLED, false);
    }

    private void ensureKey() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEYSTORE_ALIAS)) ks.deleteEntry(KEYSTORE_ALIAS);

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build();
            keyGenerator.init(spec);
            keyGenerator.generateKey();
        } catch (Exception e) {
            toast("키 생성 실패: " + e.getMessage());
        }
    }

    private boolean isKeyUsable() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            SecretKey key = (SecretKey) ks.getKey(KEYSTORE_ALIAS, null);
            if (key == null) return false;

            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (android.security.keystore.KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void setBiometricEnabled(boolean enable) {
        if (enable) {
            ensureKey();
        } else {
            try {
                KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
                ks.load(null);
                if (ks.containsAlias(KEYSTORE_ALIAS)) ks.deleteEntry(KEYSTORE_ALIAS);
            } catch (Exception ignored) {}
        }
        getPrefs().edit().putBoolean(PREF_BIOMETRIC_ENABLED, enable).apply();
    }

    private void signInWithSavedCredentials(String studentId, String password) {
        db.collection("users")
                .whereEqualTo("studentNumber", studentId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        toast("자동 로그인 실패: 계정 정보 불일치");
                        return;
                    }

                    String email = querySnapshot.getDocuments().get(0).getString("email");
                    if (email == null || email.isEmpty()) {
                        toast("자동 로그인 실패: 이메일 정보 오류");
                        return;
                    }

                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(result -> {
                                toast("로그인 성공");
                                startActivity(new Intent(this, MainActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                                finish();
                            })
                            .addOnFailureListener(e -> toast("로그인 실패: " + e.getMessage()));
                })
                .addOnFailureListener(e -> toast("자동 로그인 오류: " + e.getMessage()));
    }


    // ===================== Biometric additions END =====================
}