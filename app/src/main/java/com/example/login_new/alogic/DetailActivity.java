package com.example.login_new.alogic;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.login_new.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class DetailActivity extends AppCompatActivity {

    private TextView detailTextView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        detailTextView = findViewById(R.id.detailTextView);
        db = FirebaseFirestore.getInstance();

        String buildingName = getIntent().getStringExtra("building_name");

        if (buildingName != null && !buildingName.isEmpty()) {
            fetchBuildingDetail(buildingName);
        } else {
            detailTextView.setText("건물 정보가 전달되지 않았습니다.");
        }
    }

    private void fetchBuildingDetail(String inputName) {
        if (inputName.startsWith("학과: ")) {
            fetchDepartmentDetail(inputName.substring(4).trim());
        } else if (inputName.startsWith("공간: ")) {
            fetchSpaceDetail(inputName.substring(4).trim());
        } else {
            fetchBuildingInfo(inputName);
        }
    }

    private void fetchDepartmentDetail(String deptNameWithBuilding) {
        String deptName = deptNameWithBuilding;
        String buildingName = "";

        int buildingStart = deptNameWithBuilding.indexOf("(건물: ");
        int buildingEnd = deptNameWithBuilding.indexOf(")", buildingStart);
        if (buildingStart != -1 && buildingEnd != -1) {
            buildingName = deptNameWithBuilding.substring(buildingStart + 6, buildingEnd).trim();
            deptName = deptNameWithBuilding.substring(0, buildingStart).trim();
        }

        final String finalDeptName = deptName;
        final String finalBuildingName = buildingName;

        StringBuilder sb = new StringBuilder();
        sb.append("학과명: ").append(finalDeptName).append("\n");

        db.collection("departments_detail")
                .whereEqualTo("department_name", finalDeptName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        sb.append("학과 위치 및 학과 사무실:\n");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bName = document.getString("building_name");
                            String roomNum = document.getString("room_number");
                            if (roomNum == null || roomNum.isEmpty()) {
                                roomNum = "정보 없음";
                            }
                            sb.append("- 건물: ").append(bName).append(" (학과 사무실: ").append(roomNum).append(")\n");
                        }
                    } else {
                        sb.append("해당 학과 정보가 없습니다.\n");
                    }
                    detailTextView.setText(sb.toString());
                });
    }

    private void fetchSpaceDetail(String spaceNameWithBuilding) {
        String spaceName = spaceNameWithBuilding;
        String buildingName = "";

        int buildingStart = spaceNameWithBuilding.indexOf("(건물: ");
        int buildingEnd = spaceNameWithBuilding.indexOf(")", buildingStart);
        if (buildingStart != -1 && buildingEnd != -1) {
            buildingName = spaceNameWithBuilding.substring(buildingStart + 6, buildingEnd).trim();
            spaceName = spaceNameWithBuilding.substring(0, buildingStart).trim();
        }

        final String finalSpaceName = spaceName;
        final String finalBuildingName = buildingName;

        StringBuilder sb = new StringBuilder();
        sb.append("공간명: ").append(finalSpaceName).append("\n");

        db.collection("buildings")
                .document(finalBuildingName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Object spacesObj = doc.get("spaces");
                        if (spacesObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<String> spaces = (java.util.List<String>) spacesObj;
                            boolean found = false;
                            for (String space : spaces) {
                                if (space.contains(finalSpaceName)) {
                                    sb.append("- ").append(space).append("\n");
                                    found = true;
                                }
                            }
                            if (!found) {
                                sb.append("해당 공간 정보가 없습니다.\n");
                            }
                        } else {
                            sb.append("공간 정보가 없습니다.\n");
                        }
                    } else {
                        sb.append("해당 건물의 공간 정보를 불러올 수 없습니다.\n");
                    }
                    detailTextView.setText(sb.toString());
                });
    }

    private void fetchBuildingInfo(String inputName) {
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

        final String finalBuildingName = buildingName;

        StringBuilder sb = new StringBuilder();

        db.collection("buildings")
                .document(finalBuildingName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();

                        sb.append("건물: ").append(finalBuildingName).append("\n\n");

                        Object departmentsObj = doc.get("departments");
                        if (departmentsObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<String> departments = (java.util.List<String>) departmentsObj;
                            if (!departments.isEmpty()) {
                                sb.append("소속 학과 목록:\n");
                                for (String dept : departments) {
                                    sb.append("- ").append(dept).append("\n");
                                }
                            } else {
                                sb.append("소속 학과 정보가 없습니다.\n");
                            }
                        } else {
                            sb.append("소속 학과 정보가 없습니다.\n");
                        }

                        Object spacesObj = doc.get("spaces");
                        if (spacesObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<String> spaces = (java.util.List<String>) spacesObj;
                            if (!spaces.isEmpty()) {
                                sb.append("\n특별한 공간 목록:\n");
                                for (String space : spaces) {
                                    sb.append("- ").append(space).append("\n");
                                }
                            } else {
                                sb.append("\n특별한 공간 정보가 없습니다.\n");
                            }
                        } else {
                            sb.append("\n특별한 공간 정보가 없습니다.\n");
                        }

                        detailTextView.setText(sb.toString());

                    } else {
                        sb.append("해당 건물에 대한 정보가 없습니다.");
                        detailTextView.setText(sb.toString());
                    }
                });
    }
}
