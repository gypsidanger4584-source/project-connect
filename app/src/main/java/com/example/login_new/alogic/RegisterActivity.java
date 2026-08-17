package com.example.login_new.alogic;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 회원가입 화면
 * - 비밀번호 재확인 로직 (실시간 Helper Text로 피드백)
 * - 이메일 인증 발송 전 비밀번호 일치 확인 로직 유지
 */
public class RegisterActivity extends AppCompatActivity {

    // UI
    private EditText emailText, passwordText, passwordcheckText, numberText, idText;
    private Button validateButton, btnSendVerification, btnVerifyEmail, registerButton;
    private TextInputEditText searchDepartmentText;

    // TextInputLayout 변수: Helper Text/색상 설정을 위해 필요
    private TextInputLayout tilPasswordCheck;

    private List<String> departmentList = new ArrayList<>();
    private String selectedDepartment = "";

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // State
    private boolean emailVerified = false;
    private boolean idOk = false;
    private boolean passwordMatch = false; // 비밀번호 일치 상태

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // XML 매핑
        emailText = findViewById(R.id.emailText);
        passwordText = findViewById(R.id.passwordText);
        passwordcheckText = findViewById(R.id.passwordTextcheck); // 비밀번호 재확인 매핑
        tilPasswordCheck = findViewById(R.id.tilPasswordcheck); // TextInputLayout 매핑
        numberText = findViewById(R.id.studentNumberText);
        idText = findViewById(R.id.personid);
        validateButton = findViewById(R.id.validateButton);
        btnSendVerification = findViewById(R.id.btnSendVerification);
        btnVerifyEmail = findViewById(R.id.btnVerifyEmail);
        registerButton = findViewById(R.id.registerButton);
        searchDepartmentText = findViewById(R.id.searchDepartmentText);

        // 학번 필터 및 리스너
        numberText.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(10) });
        numberText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                idOk = false;
                String v = s.toString().trim();
                boolean lenOk = v.length() == 10;
                boolean digitsOnly = v.matches("\\d+");

                if (v.length() == 0) {
                    numberText.setError("학번을 입력해 주세요.");
                } else if (!digitsOnly) {
                    numberText.setError("학번은 숫자만 입력하세요.");
                } else if (!lenOk) {
                    numberText.setError("학번은 정확히 10자리여야 합니다.");
                } else {
                    numberText.setError(null);
                }

                validateButton.setEnabled(lenOk && digitsOnly);
                updateRegisterButtonState();
            }
        });

        // 비밀번호 실시간 대조 로직 (TextWatcher)
        TextWatcher passwordMatchWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkPasswordMatchRealtime();
                updateRegisterButtonState();
            }
        };

        passwordText.addTextChangedListener(passwordMatchWatcher);
        passwordcheckText.addTextChangedListener(passwordMatchWatcher);

        // 리스너
        validateButton.setOnClickListener(v -> checkIdDuplicate());
        btnSendVerification.setOnClickListener(v -> sendVerificationEmail());
        btnVerifyEmail.setOnClickListener(v -> checkVerified());
        registerButton.setOnClickListener(v -> finalizeSignUp());

        searchDepartmentText.setOnClickListener(v -> showDepartmentDialog());
        loadDepartmentsFromFirestore();


        // 초기 상태
        setUiEnabled(true);
        validateButton.setEnabled(false);
        registerButton.setEnabled(false);
        registerButton.setAlpha(0.5f);

        // 초기 비밀번호 확인 상태 설정
        checkPasswordMatchRealtime();
    }

    /**
     * 비밀번호 일치 여부를 실시간으로 확인하고 Helper Text를 설정합니다.
     * (아이콘 대신 헬퍼 텍스트와 색상을 사용)
     */
    private void checkPasswordMatchRealtime() {
        String pw = passwordText.getText().toString();
        String pwcheck = passwordcheckText.getText().toString();

        // StartIcon을 제거합니다 (기존 잠금 아이콘 유지)
        tilPasswordCheck.setStartIconDrawable(R.drawable.ic_lock);
        tilPasswordCheck.setHelperTextEnabled(true);
        tilPasswordCheck.setError(null); // 에러 텍스트는 최종 가입 시에만 사용

        if (TextUtils.isEmpty(pwcheck)) {
            // 비밀번호 확인란이 비어있으면
            tilPasswordCheck.setHelperText("비밀번호 확인");
            tilPasswordCheck.setHelperTextColor(ContextCompat.getColorStateList(this, R.color.gray)); // 기본 회색
            passwordMatch = false;
        } else if (pw.equals(pwcheck)) {
            // 비밀번호 일치 시
            tilPasswordCheck.setHelperText("비밀번호가 일치합니다.");
            tilPasswordCheck.setHelperTextColor(ContextCompat.getColorStateList(this, R.color.green)); // 초록색
            passwordMatch = true;
        } else {
            // 비밀번호 불일치 시
            tilPasswordCheck.setHelperText("비밀번호가 일치하지 않습니다.");
            tilPasswordCheck.setHelperTextColor(ContextCompat.getColorStateList(this, R.color.red)); // 빨간색
            passwordMatch = false;
        }
    }

    /**
     * 최종 가입 버튼 상태 업데이트
     */
    private void updateRegisterButtonState() {
        String sn = numberText.getText().toString().trim();
        boolean lenOk = sn.length() == 10;
        boolean digitsOnly = sn.matches("\\d{10}");

        // 비밀번호 일치 상태(passwordMatch)를 활성화 조건에 추가
        registerButton.setEnabled(emailVerified && idOk && lenOk && digitsOnly && passwordMatch);
        registerButton.setAlpha(registerButton.isEnabled() ? 1f : 0.5f);
    }

    /**
     * 이메일 인증 메일 전송: 계정이 없으면 생성 → 전송, 있으면 로그인 후 전송
     * 비밀번호 일치 확인 후 발송
     */
    private void sendVerificationEmail() {
        String email = emailText.getText().toString().trim();
        String pw = passwordText.getText().toString();
        String pwcheck = passwordcheckText.getText().toString(); // 확인 비밀번호

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pw) || TextUtils.isEmpty(pwcheck)) {
            toast("이메일, 비밀번호, 비밀번호 확인란을 모두 입력하세요.");
            return;
        }

        // 비밀번호 일치 확인
        if (!pw.equals(pwcheck)) {
            tilPasswordCheck.setError("비밀번호가 일치하지 않습니다."); // 에러 텍스트를 이용해 최종 경고
            toast("비밀번호가 일치하지 않습니다. 인증 메일을 보낼 수 없습니다.");
            return;
        }
        tilPasswordCheck.setError(null); // 일치하면 에러 제거

        setUiEnabled(false);
        auth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) { setUiEnabled(true); toast("사용자 정보를 가져올 수 없습니다."); return; }
                        user.sendEmailVerification().addOnCompleteListener(t2 -> {
                            setUiEnabled(true);
                            if (t2.isSuccessful()) toast("인증 메일을 보냈습니다. 메일함을 확인하세요.");
                            else toast("인증 메일 전송 실패: " + (t2.getException()!=null ? t2.getException().getMessage() : ""));
                        });
                    } else {
                        Exception e = t.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            // 이미 계정 존재 → 로그인 후 전송
                            auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(t2 -> {
                                if (!t2.isSuccessful()) { setUiEnabled(true); toast("로그인 실패: " + (t2.getException()!=null?t2.getException().getMessage():"")); return; }
                                FirebaseUser user = auth.getCurrentUser();
                                if (user == null) { setUiEnabled(true); toast("사용자 정보를 가져올 수 없습니다."); return; }
                                user.sendEmailVerification().addOnCompleteListener(t3 -> {
                                    setUiEnabled(true);
                                    if (t3.isSuccessful()) toast("인증 메일을 보냈습니다. 메일함을 확인하세요.");
                                    else toast("인증 메일 전송 실패: " + (t3.getException()!=null ? t3.getException().getMessage() : ""));
                                });
                            });
                        } else {
                            setUiEnabled(true);
                            toast("계정 생성 실패: " + (e!=null?e.getMessage():""));
                        }
                    }
                });
    }

    /** 이메일 인증 여부 확인 */
    private void checkVerified() {
        String email = emailText.getText().toString().trim();
        String pw = passwordText.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pw)) { toast("이메일/비밀번호를 입력하세요."); return; }

        setUiEnabled(false);
        FirebaseUser cur = auth.getCurrentUser();
        if (cur == null || cur.getEmail() == null || !cur.getEmail().equals(email)) {
            auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(task -> {
                if (task.isSuccessful()) reloadAndCheck();
                else {
                    setUiEnabled(true);
                    toast("로그인 실패: " + (task.getException()!=null?task.getException().getMessage():""));
                }
            });
        } else {
            reloadAndCheck();
        }
    }

    /** 현재 로그인된 사용자 새로고침 후 이메일 인증 여부 반영 */
    private void reloadAndCheck() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { setUiEnabled(true); toast("로그인이 필요합니다."); return; }

        user.reload().addOnCompleteListener(t -> {
            setUiEnabled(true);

            if (t.isSuccessful() && user.isEmailVerified()) {
                emailVerified = true;
                toast("이메일 인증이 확인되었습니다. 학번 중복확인을 진행하세요.");
            } else {
                emailVerified = false;
                toast("아직 이메일 인증이 완료되지 않았습니다.");
            }

            updateRegisterButtonState();
        });
    }

    /** 학번 중복 확인 (studentNumber) */
    private void checkIdDuplicate() {
        String number = numberText.getText().toString().trim();

        if (TextUtils.isEmpty(number)) {
            numberText.setError("학번을 입력해 주세요.");
            toast("학번을 입력해 주세요.");
            return;
        }

        if (number.length() != 10) { toast("학번은 10자리여야 합니다."); return; }
        if (!number.matches("\\d{10}")) { toast("학번은 숫자만 입력하세요."); return; }

        idOk = false;
        setUiEnabled(false);
        db.collection("users")
                .whereEqualTo("studentNumber", number)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    setUiEnabled(true);
                    if (qs.isEmpty()) {
                        idOk = true;
                        toast("사용 가능한 학번입니다.");
                    } else {
                        idOk = false;
                        toast("이미 사용 중인 학번입니다.");
                    }

                    updateRegisterButtonState();
                })
                .addOnFailureListener(e -> {
                    setUiEnabled(true);
                    idOk = false;
                    toast("중복 확인 실패: " + (e!=null?e.getMessage():""));
                });
    }

    /** 학과 선택 다이얼로그 표시 */
    private void showDepartmentDialog() {
        // department_dialog.xml을 사용합니다.
        View dialogView = getLayoutInflater().inflate(R.layout.department_dialog, null);
        EditText editSearch = dialogView.findViewById(R.id.editSearchDepartment);
        RecyclerView recyclerView = dialogView.findViewById(R.id.departmentRecyclerView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        List<String> filteredList = new ArrayList<>(departmentList);
        DepartmentAdapter adapter = new DepartmentAdapter(filteredList, selected -> {
            // 선택 결과를 TextInputEditText에 표시하고 String 변수에 저장
            searchDepartmentText.setText(selected);
            searchDepartmentText.setHint("");
            selectedDepartment = selected;
            dialog.dismiss();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 실시간 검색
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filteredList.clear();
                String query = s.toString().toLowerCase();
                for (String item : departmentList) {
                    if (item.toLowerCase().contains(query)) {
                        filteredList.add(item);
                    }
                }
                adapter.updateList(filteredList);
            }
        });

        dialog.show();
    }

    private void loadDepartmentsFromFirestore() {
        db.collection("departments_detail").get().addOnSuccessListener(query -> {
            departmentList.clear();
            for (QueryDocumentSnapshot doc : query) {
                String deptName = doc.getString("department_name");
                if (deptName != null && !departmentList.contains(deptName)) {
                    departmentList.add(deptName);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "학과 목록 로딩 실패", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 가입 확정: Firebase 저장
     * 비밀번호 불일치 및 공백 확인 후 진행
     */
    private void finalizeSignUp() {
        String email = emailText.getText().toString().trim();
        String pw = passwordText.getText().toString();
        String pwcheck = passwordcheckText.getText().toString(); // 재확인 비밀번호
        String studentNo = numberText.getText().toString().trim();
        String personId = idText.getText().toString().trim();
        String major = selectedDepartment;

        // 필수 상태 확인
        if (!emailVerified) { toast("먼저 이메일 인증을 완료하세요."); return; }
        if (!idOk) { toast("학번 중복 확인을 완료하세요."); return; }
        if (studentNo.length() != 10 || !studentNo.matches("\\d{10}")) {
            toast("학번 형식을 다시 확인하세요.");
            return;
        }

        if (TextUtils.isEmpty(personId)) {
            idText.setError("아이디를 입력하세요.");
            toast("아이디를 입력하세요.");
            return;
        }

        // 비밀번호와 확인란 공백 체크 및 최종 일치 확인
        if (TextUtils.isEmpty(pw)) {
            passwordText.setError("비밀번호를 입력해야 합니다.");
            toast("비밀번호를 입력해야 합니다.");
            return;
        }
        if (TextUtils.isEmpty(pwcheck)) {
            passwordcheckText.setError("비밀번호 확인란을 입력해야 합니다.");
            toast("비밀번호 확인란을 입력해야 합니다.");
            return;
        }

        if (!pw.equals(pwcheck)){
            tilPasswordCheck.setError("비밀번호가 일치하지 않습니다."); // 최종 에러 텍스트 표시
            toast("비밀번호가 일치하지 않습니다.");
            return;
        }
        tilPasswordCheck.setError(null);

        // 학과를 선택했는지 확인
        if (TextUtils.isEmpty(major)) {
            searchDepartmentText.setError("학과를 선택해 주세요.");
            toast("학과를 선택해 주세요.");
            return;
        }
        searchDepartmentText.setError(null);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { toast("로그인이 필요합니다."); return; }

        setUiEnabled(false);
        String uid = user.getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", email);
        data.put("studentNumber", studentNo);
        data.put("userId", personId);
        data.put("role", "user");
        if (!TextUtils.isEmpty(major)) data.put("major", major);

        db.collection("users").document(uid).set(data)
                .addOnSuccessListener(v -> {
                    setUiEnabled(true);
                    toast("회원가입이 완료되었습니다.");
                    finish();
                })
                .addOnFailureListener(e -> {
                    setUiEnabled(true);
                    toast("회원가입 저장 실패: " + (e!=null?e.getMessage():""));
                });
    }

    /** 공통 UI 활성/비활성 */
    private void setUiEnabled(boolean enabled) {
        emailText.setEnabled(enabled);
        passwordText.setEnabled(enabled);
        passwordcheckText.setEnabled(enabled); // 재확인 필드 추가
        numberText.setEnabled(enabled);
        idText.setEnabled(enabled);
        btnSendVerification.setEnabled(enabled);
        btnVerifyEmail.setEnabled(enabled);
        searchDepartmentText.setEnabled(enabled);

        validateButton.setEnabled(enabled && numberText.getText().toString().trim().length() == 10
                && numberText.getText().toString().trim().matches("\\d{10}"));

        updateRegisterButtonState(); // 전체 버튼 상태를 업데이트 메서드로 통일
    }

    private void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
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
            View view = LayoutInflater.from(parent.getContext())
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