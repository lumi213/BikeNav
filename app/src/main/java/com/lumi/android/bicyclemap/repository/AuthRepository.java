package com.lumi.android.bicyclemap.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.lumi.android.bicyclemap.api.ApiManager;
import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.AuthResponse;

/**
 * AuthRepository (싱글턴)
 * - /api/auth/login 응답 스펙에 맞춰 토큰/유저ID/이름 저장 (email은 로그인 요청값 저장)
 * - 앱 재실행 후에도 SharedPreferences로 상태 유지
 */
public class AuthRepository {

    // ===== 싱글턴 =====
    private static AuthRepository instance;

    public static synchronized AuthRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AuthRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ===== 의존성/상태 =====
    private final ApiManager apiManager;
    private final Context appContext;

    // ===== 로컬 저장 키 =====
    private static final String PREFS            = "auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_EMAIL   = "user_email";
    private static final String KEY_USER_NAME    = "user_name";
    private static final String KEY_USER_ID      = "user_id";

    private AuthRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.apiManager = ApiManager.getInstance(this.appContext);

        // 저장된 토큰을 ApiManager에 반영해야 한다면 여기서 연결
        String existing = getAccessToken();
        if (existing != null) {
            // apiManager.setAccessToken(existing);
        }
    }

    // ========================
    // API 호출
    // ========================

    /**
     * 로그인
     * Request: { "email": "...", "password": "..." }
     * Response:
     * {
     *   "success": true,
     *   "data": { "token": "string", "user_id": number, "name": "string" },
     *   "message": "로그인 성공"
     * }
     */
    public void login(String email, String password, RepositoryCallback<AuthResponse> callback) {
        apiManager.login(email, password, new ApiManager.ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse response) {
                // success 플래그가 있다면 체크 (선택)
                // if (!response.isSuccess()) { callback.onError(response.getMessage()); return; }

                String token = null;
                Integer userId = null;
                String name = null;

                // 스펙: data.token / data.user_id / data.name
                try { token  = response.getData().getToken();   } catch (Exception ignore) {}
                try { userId = response.getData().getUserId();  } catch (Exception ignore) {}
                try { name   = response.getData().getName();    } catch (Exception ignore) {}

                // email은 응답에 없으므로 요청 파라미터 값을 저장
                saveLogin(token, email, name, userId);

                // ApiManager 인터셉터 사용 시
                // apiManager.setAccessToken(token);

                callback.onSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * 회원가입
     * Request: { "name": "string", "password": "string", "email": "string" }
     * Response: { "success": true, "message": "string" }
     */
    public void register(String name, String password, String email, RepositoryCallback<ApiResponse> callback) {
        apiManager.register(name, password, email, new ApiManager.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                callback.onSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /** 로그아웃: 서버 호출(가능 시) + 로컬 세션 삭제 */
    public void logout() {
        try {
            apiManager.logout();
        } catch (Exception ignore) {}
        clearLocal();
        // apiManager.setAccessToken(null);
    }

    // ========================
    // 세션/조회
    // ========================

    /** 토큰 존재 여부로 로그인 상태 판단 */
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    /** 저장된 AccessToken (없으면 null) */
    @Nullable
    public String getAccessToken() {
        String token = sp().getString(KEY_ACCESS_TOKEN, null);
        return (token == null || token.isEmpty()) ? null : token;
    }

    /** 저장된 사용자 이메일 (없으면 null) */
    @Nullable
    public String getUserEmail() {
        String v = sp().getString(KEY_USER_EMAIL, null);
        return (v == null || v.isEmpty()) ? null : v;
    }

    /** 저장된 사용자 이름 (없으면 null) */
    @Nullable
    public String getUserName() {
        String v = sp().getString(KEY_USER_NAME, null);
        return (v == null || v.isEmpty()) ? null : v;
    }

    /** 저장된 사용자 ID (없으면 -1) */
    public int getCurrentUserId() {
        return sp().getInt(KEY_USER_ID, -1);
    }

    // ========================
    // 내부 저장 유틸
    // ========================

    /** 로그인 성공 시 세션 저장 */
    private void saveLogin(@Nullable String accessToken,
                           @Nullable String email,
                           @Nullable String name,
                           @Nullable Integer userId) {
        SharedPreferences.Editor e = sp().edit();
        if (accessToken != null) e.putString(KEY_ACCESS_TOKEN, accessToken);
        if (email != null)       e.putString(KEY_USER_EMAIL, email);
        if (name != null)        e.putString(KEY_USER_NAME, name);
        if (userId != null)      e.putInt(KEY_USER_ID, userId);
        e.apply();
    }

    /** 로컬 세션 전체 삭제 */
    private void clearLocal() {
        sp().edit().clear().apply();
    }

    private SharedPreferences sp() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // 공용 콜백
    public interface RepositoryCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }
}
