package com.lumi.android.bicyclemap.api;

import android.content.Context;
import android.content.SharedPreferences;

public class JwtTokenManager {
    private static final String PREF_NAME = "JwtTokenPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    
    private static JwtTokenManager instance;
    private SharedPreferences prefs;
    
    private JwtTokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized JwtTokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new JwtTokenManager(context.getApplicationContext());
        }
        return instance;
    }
    
    // 토큰 저장
    public void saveToken(String token, int userId, String userName) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.apply();
    }
    
    // 토큰 가져오기
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }
    
    // 사용자 ID 가져오기
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }
    
    // 사용자 이름 가져오기
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    // 로그인 상태 확인
    public boolean isLoggedIn() {
        return getToken() != null;
    }
    
    // 토큰 삭제 (로그아웃)
    public void clearToken() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.apply();
    }
} 