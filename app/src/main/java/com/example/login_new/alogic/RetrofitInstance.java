package com.example.login_new.alogic;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class RetrofitInstance {

    // ✅ OpenAI 기본 URL
    private static final String BASE_URL = "https://api.openai.com/";

    // ✅ 여기에 직접 API 키 넣기 (테스트용)
    private static final String OPENAI_API_KEY =
            "";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            // 로그 인터셉터
            .addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY))

            // 헤더 자동 추가
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();

                    Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + OPENAI_API_KEY)
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())  // ← 이 줄 중요함
                            .build();

                    return chain.proceed(request);
                }
            })
            .build();

    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    public static ChatGptApi getApiService() {
        return retrofit.create(ChatGptApi.class);
    }
}
