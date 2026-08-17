package com.example.login_new.login;

import android.content.Context;
import android.util.Log; // 로그 추가
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapterDebug"; // 로그 태그

    private final Context context;
    private List<DocumentSnapshot> postList;
    private OnItemClickListener listener;

    public PostAdapter(Context context, List<DocumentSnapshot> postList) {
        this.context = context;
        this.postList = postList;
    }

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot snapshot);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        DocumentSnapshot snapshot = postList.get(position);

        String title = snapshot.getString("title");
        String content = snapshot.getString("content");
        Timestamp timestamp = snapshot.getTimestamp("timestamp");

        Log.d(TAG, "Binding item at position " + position);
        Log.d(TAG, "Title: " + title + ", Content: " + content + ", Timestamp: " + timestamp);

        holder.postTitle.setText(title != null ? title : "제목 없음");

        // 15자 미리보기
        if (content != null && content.length() > 15) {
            holder.postContent.setText(content.substring(0, 15) + "...");
        } else {
            holder.postContent.setText(content != null ? content : "");
        }

        if (timestamp != null) {
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(timestamp.toDate());
            holder.postDate.setText(formattedDate);
        } else {
            holder.postDate.setText("날짜 없음");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(snapshot);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: " + postList.size()); // 리스트 크기 로그
        return postList.size();
    }

    public void updateList(List<DocumentSnapshot> newList) {
        this.postList = newList;
        Log.d("PostAdapterDebug", "updateList called: size = " + newList.size());
        notifyDataSetChanged();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView postTitle, postContent, postDate;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.postTitle);
            postContent = itemView.findViewById(R.id.postContent);
            postDate = itemView.findViewById(R.id.postDate);
        }
    }
}
