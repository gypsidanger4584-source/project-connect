package com.example.login_new.login;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;

public class DetailActivity extends AppCompatActivity {
    private TextView detailTextView;
    private SQLiteHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        detailTextView = findViewById(R.id.detailTextView);
        dbHelper = new SQLiteHelper(this);

        String buildingName = getIntent().getStringExtra("building_name");

        if (buildingName != null && !buildingName.isEmpty()) {
            String detailInfo = getBuildingDetail(buildingName);
            detailTextView.setText(detailInfo);
        } else {
            detailTextView.setText("건물 정보가 전달되지 않았습니다.");
        }
    }

    private String getBuildingDetail(String inputName) {
        StringBuilder sb = new StringBuilder();

        Cursor cursor = null;
        Cursor deptCursor = null;
        Cursor spaceCursor = null;

        try {
            if (inputName.startsWith("학과: ")) {

                // 학과명만 추출 ("학과: " 이후부터 첫 번째 괄호 이전까지)
                String deptName = inputName.substring(4).trim();

                // 건물명 추출: (건물: xxx) 괄호 안 부분
                String buildingName = "";
                int buildingStart = deptName.indexOf("(건물: ");
                int buildingEnd = deptName.indexOf(")", buildingStart);
                if (buildingStart != -1 && buildingEnd != -1) {
                    buildingName = deptName.substring(buildingStart + 6, buildingEnd).trim();
                    deptName = deptName.substring(0, buildingStart).trim();
                }

                sb.append("학과명: ").append(deptName).append("\n");

                // 학과에 대한 정보 조회
                deptCursor = dbHelper.getReadableDatabase().rawQuery(
                        "SELECT building_name, room_number FROM departments WHERE department_name = ?",
                        new String[]{deptName});

                if (deptCursor != null && deptCursor.moveToFirst()) {
                    sb.append("학과 위치 및 학과 사무실:\n");
                    do {
                        @SuppressLint("Range") String bName = deptCursor.getString(deptCursor.getColumnIndex("building_name"));
                        @SuppressLint("Range") String roomNum = deptCursor.getString(deptCursor.getColumnIndex("room_number"));
                        roomNum = (roomNum != null && !roomNum.isEmpty()) ? roomNum : "정보 없음";

                        sb.append("- 건물: ").append(bName);
                        sb.append(" (학과 사무실: ").append(roomNum).append(")\n");
                    } while (deptCursor.moveToNext());
                } else {
                    sb.append("해당 학과 정보가 없습니다.\n");
                }

            } else if (inputName.startsWith("공간: ")) {
                // "공간: 회의실 101 (건물: 문화 1관)" 등 처리

                // 공간명만 추출 (공간: 이후 ~ 괄호 앞까지)
                String spaceName = inputName.substring(3).trim();

                // 건물명 추출
                String buildingName = "";
                int buildingStart = spaceName.indexOf("(건물: ");
                int buildingEnd = spaceName.indexOf(")", buildingStart);
                if (buildingStart != -1 && buildingEnd != -1) {
                    buildingName = spaceName.substring(buildingStart + 6, buildingEnd).trim();
                    spaceName = spaceName.substring(0, buildingStart).trim();
                }

                sb.append("공간명: ").append(spaceName).append("\n");

                // 공간에 대한 정보 조회
                spaceCursor = dbHelper.getReadableDatabase().rawQuery(
                        "SELECT building_name, space_type FROM building_spaces WHERE space_type LIKE ?",
                        new String[]{"%" + spaceName + "%"});

                if (spaceCursor != null && spaceCursor.moveToFirst()) {
                    do {
                        @SuppressLint("Range") String bName = spaceCursor.getString(spaceCursor.getColumnIndex("building_name"));
                        sb.append("건물: ").append(bName).append("\n");
                    } while (spaceCursor.moveToNext());
                } else {
                    sb.append("해당 공간 정보가 없습니다.\n");
                }

            } else {
                String buildingName = inputName;
                if (buildingName.startsWith("건물: ")) {
                    buildingName = buildingName.substring(4).trim();
                } else {
                    int startIdx = buildingName.indexOf("(건물: ");
                    int endIdx = buildingName.indexOf(")", startIdx);
                    if (startIdx != -1 && endIdx != -1) {
                        buildingName = buildingName.substring(startIdx + 6, endIdx).trim();
                    }
                }

                cursor = dbHelper.getReadableDatabase().rawQuery(
                        "SELECT name FROM building_info WHERE name = ?",
                        new String[]{buildingName});

                if (cursor != null && cursor.moveToFirst()) {
                    sb.append("건물: ").append(buildingName).append("\n\n");

                    deptCursor = dbHelper.getReadableDatabase().rawQuery(
                            "SELECT department_name, room_number FROM departments WHERE building_name = ?",
                            new String[]{buildingName});

                    if (deptCursor != null && deptCursor.moveToFirst()) {
                        sb.append("소속 학과 및 학과 사무실:\n");
                        do {
                            @SuppressLint("Range") String deptName = deptCursor.getString(deptCursor.getColumnIndex("department_name"));
                            @SuppressLint("Range") String roomNum = deptCursor.getString(deptCursor.getColumnIndex("room_number"));
                            roomNum = (roomNum != null && !roomNum.isEmpty()) ? roomNum : "정보 없음";

                            sb.append("- ").append(deptName);
                            sb.append(" (학과 사무실: ").append(roomNum).append(")\n");
                        } while (deptCursor.moveToNext());
                    } else {
                        sb.append("이 건물에 소속된 학과 정보가 없습니다.\n");
                    }
                } else {
                    sb.append("해당 건물에 대한 정보가 없습니다.");
                }
            }
        } catch (Exception e) {
            sb.append("정보를 불러오는 중 오류가 발생했습니다.");
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (deptCursor != null) deptCursor.close();
            if (spaceCursor != null) spaceCursor.close();
        }

        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}
