package com.example.login_new.alogic;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatResponse {

    @SerializedName("choices")
    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public static class Choice {

        @SerializedName("index")
        private int index;

        @SerializedName("message")
        private Message message;

        @SerializedName("finish_reason")
        private String finishReason;

        public Message getMessage() {
            return message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public int getIndex() {
            return index;
        }
    }
}
