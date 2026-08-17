package com.example.login_new.alogic;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ChatGptApi {

    @Headers({
            "Content-Type: application/json"
    })
    @POST("v1/chat/completions")
    Call<ChatResponse> sendMessage(@Body ChatRequest request);
}
