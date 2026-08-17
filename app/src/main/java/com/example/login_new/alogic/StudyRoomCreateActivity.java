package com.example.login_new.alogic;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 🏠 StudyRoomCreateActivity (드롭다운 카테고리 버전)
 * - Firestore에 StudyRoom 업로드
 * - 환영 메시지(welcomeMessage) 저장
 * - 카테고리는 Exposed Dropdown(MaterialAutoCompleteTextView)로 선택
 */
public class StudyRoomCreateActivity extends AppCompatActivity {

    private TextInputEditText editRoomName,
            editStudyTopic,
            editWelcomeMsg,
            editStartDate,
            editEndDate,
            editMaxPeople;

    // 🔹 드롭다운 카테고리
    private MaterialAutoCompleteTextView spinnerCategory;

    private Button btnCreateRoom;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Date selectedStartDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studyroom_create);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupCategoryDropdown();   // ✅ 카테고리 드롭다운 세팅
        setupDatePickers();
        setupMaxPeopleLimit();

        btnCreateRoom.setOnClickListener(v -> saveRoomToFirestore());
    }

    // ------------------------------------------------------------
    private void initViews() {
        editRoomName = findViewById(R.id.editRoomName);
        spinnerCategory = findViewById(R.id.spinnerCategory);   // ✅ XML의 MaterialAutoCompleteTextView

        editStudyTopic = findViewById(R.id.editStudyTopic);
        editWelcomeMsg = findViewById(R.id.editWelcomeMsg);
        editStartDate = findViewById(R.id.editStartDate);
        editEndDate = findViewById(R.id.editEndDate);
        editMaxPeople = findViewById(R.id.editMaxPeople);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
    }

    // ------------------------------------------------------------
    /** 🔽 카테고리 드롭다운 설정 */
    private void setupCategoryDropdown() {
        // strings.xml에 있는 배열 사용 (예: study_category_filter)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.study_category_filter,              // 🔥 네가 만든 카테고리 배열
                android.R.layout.simple_list_item_1
        );
        spinnerCategory.setAdapter(adapter);
    }

    // ------------------------------------------------------------
    private void setupDatePickers() {
        editStartDate.setOnClickListener(v -> showDatePicker(editStartDate, true));
        editEndDate.setOnClickListener(v -> showDatePicker(editEndDate, false));
    }

    private void setupMaxPeopleLimit() {
        editMaxPeople.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    try {
                        int num = Integer.parseInt(s.toString());
                        if (num > 20) {
                            editMaxPeople.setText("20");
                            editMaxPeople.setSelection(editMaxPeople.getText().length());
                            toast("최대 인원은 20명입니다.");
                        }
                    } catch (NumberFormatException e) {
                        editMaxPeople.setText("");
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------
    private void showDatePicker(TextInputEditText target, boolean isStart) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    String date = y + "-" + String.format("%02d", m + 1) + "-" + String.format("%02d", d);
                    target.setText(date);
                    if (isStart) {
                        try {
                            selectedStartDate = dateFormat.parse(date);
                            editEndDate.setText("");
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        if (!isStart && selectedStartDate != null) {
            picker.getDatePicker().setMinDate(selectedStartDate.getTime());
        }

        picker.show();
    }

    // ------------------------------------------------------------
    /** ✅ Firestore 업로드 */
    private void saveRoomToFirestore() {
        String roomName = text(editRoomName);

        // ✅ 드롭다운에서 선택된 카테고리 텍스트
        String category = "";
        if (spinnerCategory.getText() != null) {
            category = spinnerCategory.getText().toString().trim();
        }

        String topic = text(editStudyTopic);
        String welcomeMsg = text(editWelcomeMsg);
        String startDate = text(editStartDate);
        String endDate = text(editEndDate);
        String maxStr = text(editMaxPeople);

        if (roomName.isEmpty() || topic.isEmpty() || startDate.isEmpty() || endDate.isEmpty() || maxStr.isEmpty()) {
            toast("모든 항목을 입력해주세요.");
            return;
        }

        int maxPeople;
        try {
            maxPeople = Integer.parseInt(maxStr);
        } catch (NumberFormatException e) {
            toast("최대 인원 수를 올바르게 입력해주세요.");
            return;
        }

        if (maxPeople < 1) {
            toast("최대 인원은 최소 1명 이상이어야 합니다.");
            return;
        }

        if (currentUser == null) {
            toast("로그인이 필요합니다.");
            return;
        }

        // 카테고리 기본값 처리
        if (category.isEmpty()) {
            category = "기타";
        }

        btnCreateRoom.setEnabled(false);

        // ✅ Firestore에 업로드할 데이터 맵 구성
        Map<String, Object> room = new HashMap<>();
        room.put("roomName", roomName);
        room.put("category", category);   // 🔥 여기 저장되는 값으로 필터링할 수 있음
        room.put("topic", topic);
        room.put("welcomeMessage", welcomeMsg.isEmpty() ? "함께 공부해요! 😊" : welcomeMsg);
        room.put("startDate", startDate);
        room.put("endDate", endDate);
        room.put("currentPeople", 1);
        room.put("maxPeople", maxPeople);
        room.put("problemCount", 0);
        room.put("open", true);
        room.put("ownerUid", currentUser.getUid());
        room.put("creatorName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "익명");
        room.put("createdAt", new Date());
        room.put("updatedAt", new Date());

        db.collection("studyRooms")
                .add(room)
                .addOnSuccessListener(doc -> {
                    // ✅ 방장 자동 등록 (participants 컬렉션)
                    Map<String, Object> ownerData = new HashMap<>();
                    ownerData.put("uid", currentUser.getUid());
                    ownerData.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "익명");
                    ownerData.put("title", "방장");
                    ownerData.put("isActive", true);
                    ownerData.put("score", 0);
                    ownerData.put("joinedAt", new Date());

                    db.collection("studyRooms").document(doc.getId())
                            .collection("participants")
                            .document(currentUser.getUid())
                            .set(ownerData)
                            .addOnSuccessListener(aVoid -> {
                                toast("✅ 스터디룸이 성공적으로 생성되었습니다!");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                toast("⚠️ 참가자 등록 실패: " + e.getMessage());
                                btnCreateRoom.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    toast("❌ 생성 실패: " + e.getMessage());
                    btnCreateRoom.setEnabled(true);
                });
    }

    // ------------------------------------------------------------
    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
