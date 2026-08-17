package com.example.login_new.login;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import java.util.ArrayList;

public class SearchDialog extends DialogFragment {

    private SearchView searchView;
    private RecyclerView searchRecyclerView;
    private SearchResultAdapter adapter;
    private SQLiteHelper sqLiteHelper;

    public SearchDialog() {
        // Default constructor
    }

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

        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(Color.BLACK);

        sqLiteHelper = new SQLiteHelper(getContext());
        adapter = new SearchResultAdapter(getContext(), new ArrayList<>());

        adapter.setOnItemClickListener((rawName, markerIdName) -> {
            String buildingName = extractPureBuildingName(rawName);  // 전처리

            if (buildingName != null && !buildingName.isEmpty()) {
                Intent intent = new Intent(getContext(), BuildingDetail.class);
                intent.putExtra("building_name", buildingName);
                intent.putExtra("markerIdName", markerIdName);
                startActivity(intent);
                dismiss();
            } else {
                Intent intent = new Intent(getContext(), DetailActivity.class);
                intent.putExtra("building_name", "");
                startActivity(intent);
                dismiss();
            }
        });

        searchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchRecyclerView.setAdapter(adapter);

        searchView.setIconified(false); // 기본 펼쳐진 상태
        searchView.setQueryHint("건물, 학과, 공간 검색");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchAndDisplay(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchAndDisplay(newText);
                return true;
            }
        });

        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    private void searchAndDisplay(String keyword) {
        if (keyword.trim().isEmpty()) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        ArrayList<String> rawResults = sqLiteHelper.searchAllTables(""); // 모든 데이터
        ArrayList<String> filteredResults = new ArrayList<>();

        String lowerKeyword = keyword.toLowerCase();

        for (String item : rawResults) {
            String pureName = getPureName(item); // "건물: " 등 접두어 제거
            String choseongSeq = extractChoseongSequence(pureName);

            if (choseongSeq.contains(lowerKeyword)) {
                filteredResults.add(item);
            }
            else if (item.toLowerCase().contains(lowerKeyword)) {
                filteredResults.add(item);
            }
        }

        adapter.updateData(filteredResults);
    }




    private String extractPureBuildingName(String input) {
        if (input.startsWith("건물: ")) {
            return input.replace("건물: ", "").trim();
        } else if (input.startsWith("학과: ")) {
            return input.replace("학과: ", "").trim();
        } else if (input.startsWith("공간: ")) {
            return input.replace("공간: ", "").trim();
        } else {
            return input.trim();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static final char[] CHOSEONG = {
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };
    private String getPureName(String input) {
        if (input.startsWith("건물: ")) {
            return input.substring(4); // "건물: " 제거
        } else if (input.startsWith("학과: ")) {
            return input.substring(4); // "학과: " 제거
        } else if (input.startsWith("공간: ")) {
            return input.substring(4); // "공간: " 제거
        }
        return input;
    }
    private String extractChoseongSequence(String text) {
        StringBuilder builder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int base = c - 0xAC00;
                int choseongIndex = base / (21 * 28);
                builder.append(CHOSEONG[choseongIndex]);
            } else {
                builder.append(c);  // 한글 외 문자도 포함 (예: 숫자나 영문)
            }
        }
        return builder.toString();
    }
}
