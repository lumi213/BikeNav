package com.lumi.android.bicyclemap.api.kakao;

import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class KakaoAuthInterceptor implements Interceptor {
    private final String restApiKey; // "KakaoAK {REST_API_KEY}" 에 들어갈 키만

    public KakaoAuthInterceptor(@NonNull String restApiKey) {
        this.restApiKey = restApiKey;
    }
    @Override public Response intercept(Chain chain) throws IOException {
        Request req = chain.request().newBuilder()
                .addHeader("Authorization", "KakaoAK " + restApiKey) // :contentReference[oaicite:4]{index=4}
                .addHeader("Content-Type", "application/json")
                .build();
        return chain.proceed(req);
    }
}
