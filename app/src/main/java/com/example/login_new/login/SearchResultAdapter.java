package com.example.login_new.login;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;

import java.util.ArrayList;
import java.util.HashMap;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private final ArrayList<String> data;
    private final Context context;

    // 건물명 → 마커 ID 매핑
    private final HashMap<String, String> buildingToMarkerMap = new HashMap<String, String>() {{
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

    public SearchResultAdapter(Context context, ArrayList<String> data) {
        this.context = context;
        this.data = data;
    }

    // 1) 클릭 리스너 인터페이스 선언
    public interface OnItemClickListener {
        void onItemClick(String buildingName, String markerIdName);
    }

    // 2) 리스너 필드
    private OnItemClickListener listener;

    // 3) 리스너 등록 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvResultText;

        public ViewHolder(View itemView) {
            super(itemView);
            tvResultText = itemView.findViewById(R.id.tvResultText);
            tvResultText.setTextColor(Color.BLACK);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String item = data.get(position);
        holder.tvResultText.setText(item);

        holder.itemView.setOnClickListener(v -> {
            String buildingName = extractBuildingName(item);  // 전처리된 건물명
            String markerIdName = buildingToMarkerMap.getOrDefault(buildingName, "");

            if (listener != null) {
                listener.onItemClick(buildingName, markerIdName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(ArrayList<String> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    // 건물명 또는 학과명에서 건물명만 추출
    private String extractBuildingName(String item) {
        if (item.startsWith("건물: ")) {
            return item.replace("건물: ", "").trim();
        }

        int start = item.indexOf("(건물:");
        int end = item.indexOf(")");
        if (start != -1 && end != -1 && start < end) {
            return item.substring(start + 4, end).trim();
        }

        return "";
    }
}
