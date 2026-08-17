package com.example.login_new.alogic;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SearchDialog extends DialogFragment {

    private SearchView searchView;
    private RecyclerView searchRecyclerView;
    private SearchResultAdapter adapter;
    private FirebaseFirestore db;

    private static final HashMap<String, String> buildingToMarkerMap = new HashMap<String, String>() {{
        put("공학 1관", "marker1");
        put("공학 2관", "marker2");
        put("학술정보관", "marker3");
        put("자연과학관", "marker4");
        put("식품과학관", "marker5");
        put("문화 2관", "marker6");
        put("문화 1관", "marker7");
        put("도의관", "marker8");
        put("연성대학교", "marker2_1");
        put("연곡문화센터", "marker2_2");
        put("학생복지센터", "marker2_3");
        put("창의교육센터", "marker2_4");
        put("창조관", "marker2_5");
    }};

    private static final char[] CHOSEONG = {
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };

    public SearchDialog() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        setCancelable(false);
        View view = inflater.inflate(R.layout.search_popup, container, false);

        searchView = view.findViewById(R.id.popupSearchView);
        searchRecyclerView = view.findViewById(R.id.popupSearchRecyclerView);
        TextView closeButton = view.findViewById(R.id.closeButton);

        db = FirebaseFirestore.getInstance();

        // ✅ RecyclerView 초기화
        adapter = new SearchResultAdapter(getContext(), new ArrayList<>());
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(getContext(), BuildingDetail.class);
            intent.putExtra("building_name", item.getBuildingName());
            intent.putExtra("markerIdName", item.getMarkerIdName());
            startActivity(intent);
            dismiss();
        });

        searchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchRecyclerView.setAdapter(adapter);

        // ✅ SearchView 기본 설정
        searchView.setIconified(false);
        searchView.setQueryHint("건물, 학과, 공간 검색");

        // ✅ SearchView 색상 커스터마이징 (다크/라이트 자동 대응)
        try {
            int id = getResources().getIdentifier("search_src_text", "id", requireContext().getPackageName());
            EditText searchEditText = searchView.findViewById(id);
            if (searchEditText != null) {
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    // 🌙 다크모드
                    searchEditText.setTextColor(Color.WHITE);
                    searchEditText.setHintTextColor(Color.LTGRAY);
                    searchView.setBackgroundColor(getResources().getColor(R.color.bg_main, null));
                } else {
                    // ☀️ 라이트모드
                    searchEditText.setTextColor(Color.BLACK);
                    searchEditText.setHintTextColor(Color.GRAY);
                    searchView.setBackgroundColor(getResources().getColor(R.color.white, null));
                }

                searchEditText.setBackgroundColor(Color.TRANSPARENT);
            }
        } catch (Exception e) {
            Log.w("SearchDialog", "EditText 색상 설정 실패", e);
        }

        // ✅ 검색 이벤트
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });

        // 닫기 버튼
        closeButton.setOnClickListener(v -> dismiss());

        // ✅ 다크모드 시 다이얼로그 배경 자동 변경
        applyDialogBackground(view);

        return view;
    }

    /** 🌙 다크/라이트 모드별 배경 자동 조정 */
    private void applyDialogBackground(View rootView) {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            rootView.setBackgroundColor(getResources().getColor(R.color.bg_main, null));
        } else {
            rootView.setBackgroundColor(getResources().getColor(R.color.white, null));
        }
    }

    /** 🔍 Firestore 검색 로직 */
    private void performSearch(String keyword) {
        if (keyword.trim().isEmpty()) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        String lowerKeyword = keyword.toLowerCase();
        HashSet<String> seenKeys = new HashSet<>();

        List<SearchResultItem> buildingResults = new ArrayList<>();
        List<SearchResultItem> departmentResults = new ArrayList<>();
        List<SearchResultItem> spaceResults = new ArrayList<>();

        db.collection("departments_detail").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String dept = doc.getString("department_name");
                    String building = doc.getString("building_name");
                    String room = doc.getString("room_number");

                    String markerId = buildingToMarkerMap.getOrDefault(building, "");

                    if (dept != null) {
                        String choseong = extractChoseongSequence(dept);
                        if (choseong.contains(lowerKeyword) || dept.toLowerCase().contains(lowerKeyword)) {
                            String key = "DEPARTMENT|" + dept;
                            if (seenKeys.add(key)) {
                                departmentResults.add(new SearchResultItem(
                                        SearchResultItem.ItemType.DEPARTMENT,
                                        "학과: " + dept + " (건물: " + building + ")",
                                        dept,
                                        building,
                                        markerId
                                ));
                            }
                        }
                    }

                    if (room != null) {
                        String key = "SPACE|" + room;
                        if ((room.toLowerCase().contains(lowerKeyword)) && seenKeys.add(key)) {
                            spaceResults.add(new SearchResultItem(
                                    SearchResultItem.ItemType.SPACE,
                                    "공간: " + room + " (건물: " + building + ")",
                                    room,
                                    building,
                                    markerId
                            ));
                        }
                    }
                }

                db.collection("buildings").get().addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task2.getResult()) {
                            String name = doc.getString("name");
                            String markerId = buildingToMarkerMap.getOrDefault(name, "");

                            if (name != null) {
                                String choseong = extractChoseongSequence(name);
                                if ((choseong.contains(lowerKeyword) || name.toLowerCase().contains(lowerKeyword))
                                        && seenKeys.add("BUILDING|" + name)) {
                                    buildingResults.add(new SearchResultItem(
                                            SearchResultItem.ItemType.BUILDING,
                                            "건물: " + name,
                                            name,
                                            name,
                                            markerId
                                    ));
                                }
                            }

                            List<String> departments = (List<String>) doc.get("departments");
                            if (departments != null) {
                                for (String dept : departments) {
                                    String choseong = extractChoseongSequence(dept);
                                    String key = "DEPARTMENT|" + dept;
                                    if ((choseong.contains(lowerKeyword) || dept.toLowerCase().contains(lowerKeyword))
                                            && seenKeys.add(key)) {
                                        departmentResults.add(new SearchResultItem(
                                                SearchResultItem.ItemType.DEPARTMENT,
                                                "학과: " + dept + " (건물: " + name + ")",
                                                dept,
                                                name,
                                                markerId
                                        ));
                                    }
                                }
                            }

                            List<String> spaces = (List<String>) doc.get("spaces");
                            if (spaces != null) {
                                for (String space : spaces) {
                                    String choseong = extractChoseongSequence(space);
                                    String key = "SPACE|" + space;
                                    if ((choseong.contains(lowerKeyword) || space.toLowerCase().contains(lowerKeyword))
                                            && seenKeys.add(key)) {
                                        spaceResults.add(new SearchResultItem(
                                                SearchResultItem.ItemType.SPACE,
                                                "공간: " + space + " (건물: " + name + ")",
                                                space,
                                                name,
                                                markerId
                                        ));
                                    }
                                }
                            }
                        }

                        List<SearchResultItem> combined = new ArrayList<>();
                        combined.addAll(buildingResults);
                        combined.addAll(departmentResults);
                        combined.addAll(spaceResults);

                        adapter.updateData(combined);
                    } else {
                        Log.e("SearchDialog", "buildings 실패", task2.getException());
                    }
                });

            } else {
                Log.e("SearchDialog", "departments_detail 실패", task.getException());
            }
        });
    }

    private String extractChoseongSequence(String text) {
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int base = c - 0xAC00;
                int choseongIndex = base / (21 * 28);
                builder.append(CHOSEONG[choseongIndex]);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
