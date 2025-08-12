package com.lumi.android.bicyclemap.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String PREFS = "tracking_prefs";

    // 배터리 최적화 다이얼로그 관련 키
    private static final String KEY_BATTERY_DIALOG_SHOWN = "battery_dialog_shown";      // 한번만
    private static final String KEY_BATTERY_DIALOG_LAST  = "battery_dialog_last_time";  // 주기 제한용

    // 산책 중 플래그 (부팅복구/서비스 재시작 등에 사용)
    private static final String KEY_WALKING = "walking";

    private AppPrefs() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- 배터리 최적화 다이얼로그: '한번만' 가드 ----------
    public static boolean wasBatteryDialogShown(Context c) {
        return prefs(c).getBoolean(KEY_BATTERY_DIALOG_SHOWN, false);
    }
    public static void markBatteryDialogShown(Context c) {
        prefs(c).edit().putBoolean(KEY_BATTERY_DIALOG_SHOWN, true).apply();
    }

    // ---------- 배터리 최적화 다이얼로그: '주기 제한' 가드 ----------
    public static boolean isBatteryDialogThrottled(Context c, long intervalMs) {
        long last = prefs(c).getLong(KEY_BATTERY_DIALOG_LAST, 0L);
        return (System.currentTimeMillis() - last) < intervalMs;
    }
    public static void stampBatteryDialogNow(Context c) {
        prefs(c).edit().putLong(KEY_BATTERY_DIALOG_LAST, System.currentTimeMillis()).apply();
    }

    // ---------- 산책 중 여부 ----------
    public static boolean isWalking(Context c) {
        return prefs(c).getBoolean(KEY_WALKING, false);
    }
    public static void setWalking(Context c, boolean walking) {
        prefs(c).edit().putBoolean(KEY_WALKING, walking).apply();
    }

    // (개발용) 전체 리셋
    public static void clearAll(Context c) {
        prefs(c).edit().clear().apply();
    }
}
