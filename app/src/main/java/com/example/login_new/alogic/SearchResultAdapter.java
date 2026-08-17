package com.example.login_new.alogic;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private final Context context;
    private final List<SearchResultItem> resultList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(SearchResultItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public SearchResultAdapter(Context context, List<SearchResultItem> resultList) {
        this.context = context;
        this.resultList = resultList;
    }

    public void updateData(List<SearchResultItem> newList) {
        resultList.clear();
        resultList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultAdapter.ViewHolder holder, int position) {
        SearchResultItem item = resultList.get(position);
        holder.titleTextView.setText(item.getDisplayName());

        // 타입에 따라 색상 구분
        switch (item.getType()) {
            case BUILDING:
                holder.titleTextView.setTextColor(Color.parseColor("#1E88E5")); // 파란색
                break;
            case DEPARTMENT:
                holder.titleTextView.setTextColor(Color.parseColor("#388E3C")); // 초록색
                break;
            case SPACE:
                holder.titleTextView.setTextColor(Color.parseColor("#F57C00")); // 주황색
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.searchItemTitle);
        }
    }
}
