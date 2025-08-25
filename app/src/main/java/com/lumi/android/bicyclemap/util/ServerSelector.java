// ServerSelector.java
package com.lumi.android.bicyclemap.util;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.lumi.android.bicyclemap.api.ApiService;
import com.lumi.android.bicyclemap.api.JwtInterceptor;
import com.lumi.android.bicyclemap.api.dto.CourseListResponse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ServerSelector {
    private static final String TAG = "ServerSelector";

    // 원하는 3개 주소(순서대로 시도)
    private static final List<String> SERVER_CANDIDATES = Arrays.asList(
            "http://lumi.pe.kr:12345/",
            "http://192.168.16.62:8080/",
            "http://10.0.2.2:8080/"
    );

    private static final int TIMEOUT_SEC = 1;

    private ServerSelector() {}

    /** 메인에서 호출돼도 백그라운드로 넘겨 '동기적으로' 결과를 돌려주는 안전한 진입점 */
    public static String detectBaseUrlBlocking(Context context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 메인 스레드면 백그라운드에서 실행 후 결과를 기다림
            try {
                Future<String> fut = Executors.newSingleThreadExecutor().submit(
                        (Callable<String>) () -> detectInternal(context.getApplicationContext())
                );
                // 필요 시 전체 대기 시간 제한 (여기서는 6초)
                return fut.get(6, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e(TAG, "백그라운드 감지 실패: " + e);
                return null;
            }
        } else {
            // 이미 백그라운드라면 바로 내부 로직 실행
            return detectInternal(context.getApplicationContext());
        }
    }

    /** 실제 주소 탐색 로직 (백그라운드에서 실행 전제) */
    private static String detectInternal(Context context) {
        OkHttpClient client = buildSameHttpClient(context);

        for (String baseUrl : SERVER_CANDIDATES) {
            try {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client)
                        .build();

                ApiService service = retrofit.create(ApiService.class);

                // 주소 유효성 판별은 200 OK만 보면 충분 (비즈니스 success는 불필요)
                Call<CourseListResponse> call = service.getCourseList(null, null, null);
                Response<CourseListResponse> res = call.execute();

                if (res.isSuccessful()) {
                    Log.d(TAG, "✅ 서버 연결 성공: " + baseUrl);
                    return baseUrl;
                } else {
                    Log.w(TAG, "응답 코드 " + res.code() + " @ " + baseUrl);
                }
            } catch (Exception e) {
                Log.w(TAG, "서버 확인 실패: " + baseUrl + " → " + e.getMessage());
            }
        }
        Log.e(TAG, "❌ 사용 가능한 서버 주소 없음");
        return null;
    }

    /** ApiClient와 동일한 OkHttp 구성 (인터셉터/로깅) */
    private static OkHttpClient buildSameHttpClient(Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(new JwtInterceptor(context)) // ApiClient와 동일
                .build();
    }
}
