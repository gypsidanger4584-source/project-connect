package com.example.login_new.alogic;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_GPT  = 1;

    private final List<ChatMessageAi> messages;

    private boolean isDark = false;

    public ChatAdapter(List<ChatMessageAi> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return "user".equals(messages.get(position).getRole()) ? TYPE_USER : TYPE_GPT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_message, parent, false);
            return new UserVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gpt_message, parent, false);
            return new GptVH(v);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        ChatMessageAi msg = messages.get(position);


        if (h instanceof UserVH) {
            UserVH vh = (UserVH) h;
            vh.text.setText(msg.getContent());
            vh.time.setText(msg.getTime());

            if (isDark) {
                vh.text.setBackgroundResource(R.drawable.bg_bubble_user_dark);
                vh.text.setTextColor(Color.WHITE);
                vh.time.setTextColor(Color.parseColor("#C0C0C0"));
            } else {
                vh.text.setBackgroundResource(R.drawable.bg_chat_bubble_user);
                vh.text.setTextColor(Color.WHITE);
                vh.time.setTextColor(Color.parseColor("#777777"));
            }

        } else {
            GptVH vh = (GptVH) h;
            vh.text.setText(msg.getContent());
            vh.time.setText(msg.getTime());

            if (isDark) {
                vh.text.setBackgroundResource(R.drawable.bg_bubble_gpt_dark);
                vh.text.setTextColor(Color.parseColor("#F0F0F0"));
                vh.time.setTextColor(Color.parseColor("#C0C0C0"));
            } else {
                vh.text.setBackgroundResource(R.drawable.bg_chat_bubble_ai);
                vh.text.setTextColor(Color.BLACK);
                vh.time.setTextColor(Color.parseColor("#888888"));
            }
        }
    }



    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    // ====================
    // ViewHolder 정의부
    // ====================

    static class UserVH extends RecyclerView.ViewHolder {
        TextView text, time;
        UserVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.textUser);
            time = itemView.findViewById(R.id.textTimeUser);
        }
    }

    static class GptVH extends RecyclerView.ViewHolder {
        TextView text, time;
        GptVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tvMessage);
            time = itemView.findViewById(R.id.tvTime);
        }
    }

    public void setDarkMode(boolean dark) {
        isDark = dark;
        notifyDataSetChanged();
    }

}
