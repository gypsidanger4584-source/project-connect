package com.example.login_new.alogic;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.login_new.R;
import com.example.login_new.alogic.RetrofitInstance;
import com.example.login_new.alogic.ChatAdapter;
import com.example.login_new.alogic.ChatRequest;
import com.example.login_new.alogic.ChatResponse;
import com.example.login_new.alogic.Message;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private ImageView btnThemeToggle;
    private SharedPreferences prefs;
    private boolean isDarkMode;

    private RecyclerView chatRecyclerView;
    private EditText editMessage;

    private final List<ChatMessageAi> uiMessages = new ArrayList<>();
    private ChatAdapter adapter;

    private final List<Message> convoMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        editMessage = findViewById(R.id.editMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);
//        btnThemeToggle = findViewById(R.id.btnThemeToggle);

        // adapter
        adapter = new ChatAdapter(uiMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        // system 시간 초기 메시지
        convoMessages.add(new Message(
                "system",
                "지금 시간은 " + getCurrentTime() + "입니다."
        ));

        btnSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();

            if (text.isEmpty()) {
                return; // 빈 메시지면 전송 X
            }

            // UI에 메시지 추가
            addUiMessage("user", text);
            convoMessages.add(new Message("user", text));

            // GPT에게 메시지 전송
            sendToChatGpt(convoMessages);

            // 입력창 비우기
            editMessage.setText("");
        });


        /*// 모드 불러오기
        prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);

        applyTheme(isDarkMode);

        btnThemeToggle.setOnClickListener(v -> {
            isDarkMode = !isDarkMode;
            prefs.edit().putBoolean("dark_mode", isDarkMode).apply();
            applyTheme(isDarkMode);
        });*/
    }

    // ======================== UI 시간 ========================
    private String getUiTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm", Locale.KOREA);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
        return sdf.format(new Date());
    }

    private void addUiMessage(String role, String content) {
        uiMessages.add(new ChatMessageAi(role, content, getUiTime()));
        adapter.notifyItemInserted(uiMessages.size() - 1);
        chatRecyclerView.scrollToPosition(uiMessages.size() - 1);
    }

    // ======================== GPT 시간 ========================
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 a hh:mm", Locale.KOREA);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Seoul"));
        return sdf.format(new Date());
    }

    // ======================== GPT API ========================
    private void sendToChatGpt(List<Message> messagesForApi) {
        ChatRequest request = new ChatRequest("gpt-4o-mini", messagesForApi);

        RetrofitInstance.getApiService().sendMessage(request)
                .enqueue(new Callback<ChatResponse>() {
                    @Override
                    public void onResponse(Call<ChatResponse> call, Response<ChatResponse> res) {
                        if (res.isSuccessful() && res.body() != null
                                && res.body().getChoices() != null
                                && !res.body().getChoices().isEmpty()) {

                            String reply = res.body().getChoices().get(0).getMessage().getContent();
                            addUiMessage("assistant", reply);
                            convoMessages.add(new Message("assistant", reply));

                        } else {
                            addUiMessage("assistant", "응답 오류: " + res.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatResponse> call, Throwable t) {
                        addUiMessage("assistant", "실패: " + t.getMessage());
                    }
                });
    }



    // ======================== 직접 커스텀 테마 적용 ========================
    /*private void applyTheme(boolean dark) {

        // 배경
        if (dark) {
            findViewById(android.R.id.content).setBackgroundColor(Color.parseColor("#000000"));
            btnThemeToggle.setImageResource(R.drawable.ic_sun);

        } else {
            findViewById(android.R.id.content).setBackgroundColor(Color.parseColor("#F8F9FB"));
            btnThemeToggle.setImageResource(R.drawable.ic_moon);
        }

        // 입력창 배경
        if (dark) {
            editMessage.setBackgroundResource(R.drawable.bg_edit_dark);
            editMessage.setTextColor(Color.parseColor("#DDDDDD"));
            editMessage.setHintTextColor(Color.parseColor("#777777"));
        } else {
            editMessage.setBackgroundResource(R.drawable.bg_edit_light);
            editMessage.setTextColor(Color.BLACK);
            editMessage.setHintTextColor(Color.parseColor("#999999"));
        }

        // 채팅버블 색 변경
        adapter.setDarkMode(dark);
    }*/
}
