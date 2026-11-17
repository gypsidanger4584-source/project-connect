package com.example.login_new.alogic;

import com.example.login_new.BuildConfig;

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

    // 키는 소스에 넣지 않는다. local.properties의 OPENAI_API_KEY가 빌드 시 BuildConfig로 주입된다.
    // 주의: 앱에 포함된 키는 APK에서 추출될 수 있으므로 운영 환경에서는 서버 중계로 바꿔야 한다.
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;

    private static final HttpLoggingInterceptor LOGGING = createLogging();

    private static HttpLoggingInterceptor createLogging() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // 디버그 빌드에서만 본문까지 기록하고, 인증 헤더는 항상 가린다.
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);
        logging.redactHeader("Authorization");
        return logging;
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            // 로그 인터셉터
            .addInterceptor(LOGGING)

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
