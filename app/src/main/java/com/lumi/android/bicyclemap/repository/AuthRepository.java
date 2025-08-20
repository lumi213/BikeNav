package com.lumi.android.bicyclemap.repository;

import android.content.Context;

import com.lumi.android.bicyclemap.api.ApiManager;
import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.AuthResponse;

public class AuthRepository {
    private ApiManager apiManager;
    private Context context;

    public AuthRepository(Context context) {
        this.context = context;
        apiManager = ApiManager.getInstance(context);
    }

    public void login(String email, String password, RepositoryCallback<AuthResponse> callback) {
        apiManager.login(email, password, new ApiManager.ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse response) {
                callback.onSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

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

    // 로그아웃
    public void logout() {
        apiManager.logout();
    }

    // 로그인 상태 확인
    public boolean isLoggedIn() {
        return apiManager.isLoggedIn();
    }

    // 현재 사용자 ID 가져오기
    public int getCurrentUserId() {
        return apiManager.getCurrentUserId();
    }

    // 간단한 콜백 인터페이스
    public interface RepositoryCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }
} 