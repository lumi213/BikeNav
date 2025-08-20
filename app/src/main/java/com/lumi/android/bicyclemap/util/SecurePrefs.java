package com.lumi.android.bicyclemap.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public final class SecurePrefs {
    private static final String FILE = "secure_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_PASSWORD = "password"; // 필요 시

    private SecurePrefs() {}

    private static SharedPreferences prefs(Context c) {
        try {
            MasterKey masterKey = new MasterKey.Builder(c)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    c,
                    FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("SecurePrefs init failed", e);
        }
    }

    public static void setToken(Context c, String token) {
        prefs(c).edit().putString(KEY_TOKEN, token).apply();
    }
    public static String getToken(Context c) {
        return prefs(c).getString(KEY_TOKEN, null);
    }

    // 정말 필요한 경우에만 사용 (가능하면 토큰 기반으로 대체)
    public static void setPassword(Context c, String pw) {
        prefs(c).edit().putString(KEY_PASSWORD, pw).apply();
    }
    public static String getPassword(Context c) {
        return prefs(c).getString(KEY_PASSWORD, null);
    }

    public static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }
}
