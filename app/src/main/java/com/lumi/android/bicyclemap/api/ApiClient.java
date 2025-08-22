package com.lumi.android.bicyclemap.api;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://lumi.pe.kr:12345/";
    private static final String TEST_URL = "http://10.0.2.2:8080/"; // 로컬 서버 주소
    private static final String USB_URL = "http://127.0.0.1:8080/";
    private static final String WIFI_URL = "http://0.0.0.0:8080/";
    private static final String HOT_URL = "http://192.168.188.62:8080/";
    private static ApiClient instance;
    private Retrofit retrofit;
    private ApiService apiService;
    private Context context;

    private ApiClient(Context context) {
        this.context = context;

        // 로깅 인터셉터 설정
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // JWT 인터셉터 설정
        JwtInterceptor jwtInterceptor = new JwtInterceptor(context);

        // OkHttp 클라이언트 설정
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(jwtInterceptor)  // JWT 인터셉터 추가
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // Retrofit 설정
        retrofit = new Retrofit.Builder()
                .baseUrl(TEST_URL) // URL교체부분
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    // 간단한 API 호출 메서드들
    public void testConnection(ApiCallback<String> callback) {
        // 서버 연결 테스트
        callback.onSuccess("API 클라이언트 준비 완료");
    }

    // 콜백 인터페이스
    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }
}