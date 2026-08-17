package com.example.login_new.login;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;

import java.util.Arrays;
import java.util.List;

public class MapPager extends RecyclerView.Adapter<MapPager.ViewHolder> {

    private List<Integer> images;
    private Context context;

    public MapPager(List<Integer> images, Context context) {
        this.images = images;
        this.context = context;
    }

    // 마커 정보 클래스
    private static class MarkerInfo {
        float leftPercent, topPercent;
        String buildingName, markerIdName;

        MarkerInfo(float leftPercent, float topPercent, String buildingName, String markerIdName) {
            this.leftPercent = leftPercent;
            this.topPercent = topPercent;
            this.buildingName = buildingName;
            this.markerIdName = markerIdName;
        }
    }

    // 페이지별 마커 리스트
    private final List<List<MarkerInfo>> markerPages = Arrays.asList(
            Arrays.asList( // Page 0
                    new MarkerInfo(0.293f, 0.74f, "공학 1관", "marker1"),
                    new MarkerInfo(0.453f, 0.605f, "공학 2관", "marker2"),
                    new MarkerInfo(0.14f, 0.713f, "학술정보관", "marker3"),
                    new MarkerInfo(0.20f, 0.617f, "자연과학관", "marker4"),
                    new MarkerInfo(0.56f, 0.487f, "식품과학관", "marker5"),
                    new MarkerInfo(0.51f, 0.824f, "문화 2관", "marker6"),
                    new MarkerInfo(0.77f, 0.911f, "문화 1관", "marker7"),
                    new MarkerInfo(0.76f, 0.595f, "도의관", "marker8")
            ),
            Arrays.asList( // Page 1
                    new MarkerInfo(0.271f, 0.484f, "연성대학교", "marker2_1"),
                    new MarkerInfo(0.465f, 0.262f, "연곡문화센터", "marker2_2"),
                    new MarkerInfo(0.60f, 0.885f, "학생복지센터", "marker2_3"),
                    new MarkerInfo(0.623f, 0.495f, "창의교육센터", "marker2_4"),
                    new MarkerInfo(0.812f, 0.511f, "창조관", "marker2_5")
            )
    );

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        FrameLayout containerLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.mapImageView);
            containerLayout = (FrameLayout) itemView;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.map_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageResource(images.get(position));

        // 마커 추가 전에 기존 마커 제거 (중복 방지)
        for (int i = holder.containerLayout.getChildCount() - 1; i >= 0; i--) {
            View child = holder.containerLayout.getChildAt(i);
            // mapImageView는 삭제하지 않고, 태그 "marker" 인 것만 삭제
            if (child != holder.imageView && child.getTag() != null && "marker".equals(child.getTag())) {
                holder.containerLayout.removeViewAt(i);
            }
        }

        // 레이아웃 크기 확보 후 마커 추가
        holder.containerLayout.post(() -> {
            int width = holder.imageView.getWidth();
            int height = holder.imageView.getHeight();

            // 현재 position에 해당하는 마커만 추가
            for (MarkerInfo marker : markerPages.get(position)) {
                addMarker(holder.containerLayout, width, height, marker);
            }
        });
    }

    private void addMarker(FrameLayout container, int width, int height, MarkerInfo markerInfo) {
        ImageView marker = new ImageView(context);
        marker.setImageResource(R.drawable.marker_icon); // 공통 마커 이미지
        marker.setTag("marker"); // 나중에 제거할 수 있도록 태그 설정
        marker.setContentDescription(markerInfo.buildingName);

        int size = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30, context.getResources().getDisplayMetrics());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.leftMargin = (int) (markerInfo.leftPercent * width) - size / 2;
        params.topMargin = (int) (markerInfo.topPercent * height) - size / 2;

        marker.setLayoutParams(params);
        marker.setClickable(true);
        marker.setOnClickListener(v -> showBuildingPopup(markerInfo.buildingName, markerInfo.markerIdName));
        container.addView(marker);
    }

    private void showBuildingPopup(String buildingName, String markerIdName) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.building_info, null);
        ImageView imageView = popupView.findViewById(R.id.buildingImage);
        TextView nameView = popupView.findViewById(R.id.buildingName);

        nameView.setText(buildingName);

        int resId = context.getResources().getIdentifier(markerIdName, "drawable", context.getPackageName());
        if (resId != 0) {
            imageView.setImageResource(resId);
        } else {
            imageView.setImageResource(R.drawable.default_image);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(popupView)
                .create();
        dialog.show();

        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BuildingDetail.class);
            intent.putExtra("building_name", buildingName);
            intent.putExtra("markerIdName", markerIdName);
            context.startActivity(intent);
            dialog.dismiss();
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
