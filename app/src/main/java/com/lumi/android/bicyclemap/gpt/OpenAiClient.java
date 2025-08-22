package com.lumi.android.bicyclemap.gpt;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lumi.android.bicyclemap.R;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenAiClient {
    private static OpenAiClient instance;
    private final OpenAiService service;
    private final String model;

    private OpenAiClient(Context ctx) {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor(msg -> Log.d("OpenAI_HTTP", msg));
        log.setLevel(HttpLoggingInterceptor.Level.BODY);
        log.redactHeader("Authorization");

        String apiKey = readKeyFromMeta(ctx);
        this.model = ctx.getString(R.string.openai_model);

        Interceptor auth = chain -> {
            Request req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            return chain.proceed(req);
        };

        OkHttpClient ok = new OkHttpClient.Builder()
                .addInterceptor(auth)
                .addInterceptor(log)
                .build();

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(ok)
                .build();

        service = retrofit.create(OpenAiService.class);
    }

    private String readKeyFromMeta(Context ctx) {
        try {
            ApplicationInfo ai = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.lumi.openai.API_KEY", "");
        } catch (Exception e) {
            return "";
        }
    }

    public static synchronized OpenAiClient getInstance(Context ctx){
        if (instance == null) instance = new OpenAiClient(ctx.getApplicationContext());
        return instance;
    }

    public OpenAiService api(){ return service; }

    // ⬅️ 모델을 밖에서 쓰기 위해 getter 제공
    public String getModel() { return model; }
}
