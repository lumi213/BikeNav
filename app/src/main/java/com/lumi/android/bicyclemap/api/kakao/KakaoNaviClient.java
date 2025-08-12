package com.lumi.android.bicyclemap.api.kakao;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class KakaoNaviClient {
    private static Retrofit retrofit;

    public static Retrofit get(String restApiKey) {
        if (retrofit != null) return retrofit;

        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient ok = new OkHttpClient.Builder()
                .addInterceptor(new KakaoAuthInterceptor(restApiKey))
                .addInterceptor(log)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("https://apis-navi.kakaomobility.com/") // :contentReference[oaicite:5]{index=5}
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
