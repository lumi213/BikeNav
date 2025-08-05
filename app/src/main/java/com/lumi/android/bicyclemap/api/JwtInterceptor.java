package com.lumi.android.bicyclemap.api;

import android.content.Context;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class JwtInterceptor implements Interceptor {
    private JwtTokenManager tokenManager;
    
    public JwtInterceptor(Context context) {
        this.tokenManager = JwtTokenManager.getInstance(context);
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // JWT 토큰이 있으면 헤더에 추가
        String token = tokenManager.getToken();
        if (token != null) {
            Request newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(newRequest);
        }
        
        // 토큰이 없으면 원래 요청 그대로 진행
        return chain.proceed(originalRequest);
    }
} 