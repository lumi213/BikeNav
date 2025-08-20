package com.lumi.android.bicyclemap.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.*;
import com.lumi.android.bicyclemap.MainActivity;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.util.RouteMatcher;

import java.util.Locale;

public class TrackingService extends Service {
    public static final String ACTION_START = "com.lumi.android.bicyclemap.action.START";
    public static final String ACTION_STOP  = "com.lumi.android.bicyclemap.action.STOP";

    public static final String TAG = "TrackingService";
    public static final String CHANNEL_ID = "tracking_channel";
    public static final String ACTION_ARRIVED = "com.lumi.android.bicyclemap.action.ARRIVED";
    public static final String EXTRA_ROUTE_ID = "extra_route_id";
    public static final String EXTRA_ROUTE_TITLE = "extra_route_title";
    private boolean arrivedBroadcastSent = false;
    private static final double ARRIVE_THRESHOLD_METERS = 10.0; // 5m 이하면 도착 처리

    public static final int NOTI_ID = 1001;

    private FusedLocationProviderClient fused;
    private LocationCallback callback;
    private CourseDto route;

    @Override
    public void onCreate() {
        super.onCreate();
        fused = LocationServices.getFusedLocationProviderClient(this);
        createChannel();
        startForeground(NOTI_ID, buildNotification("산책 모드 진행 중", "현재 위치를 추적하고 있어요"));
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (route == null) route = RouteHolder.get();
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        startLocationUpdates(); // 이미 시작되어 있으면 내부에서 무시
        // 홈으로 가도 유지
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }

    private void startLocationUpdates() {
        if (callback != null) return;

        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000 /*2s*/)
                .setMinUpdateIntervalMillis(1000)
                .setWaitForAccurateLocation(false)
                .build();

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || result.getLastLocation() == null) return;

                // 최신 경로 재확보(중간에 바뀌었을 수 있음)
                if (route == null) route = RouteHolder.get();
                if (route == null || route.getPath() == null || route.getPath().size() < 2) {
                    updateNotification("산책 중", "경로 없음");
                    return;
                }

                double lat = result.getLastLocation().getLatitude();
                double lng = result.getLastLocation().getLongitude();

                RouteMatcher.Result m = RouteMatcher.match(route, lat, lng);
                if (m == null) {
                    updateNotification("산책 중", "경로 매칭 실패");
                    return;
                }

                // 남은거리 = 경로 남은거리 + 스냅→내 위치
                double effectiveRemain = m.remainingMeters + m.distanceToRouteMeters;

                // 진행률 = (총거리-실제남은)/총거리
                double progress = (m.totalMeters > 0)
                        ? Math.max(0, Math.min(100, (1.0 - (m.remainingMeters / m.totalMeters)) * 100.0))
                        : 0.0;

                String text = String.format(
                        Locale.KOREA, "남은 거리 %s · 진행률 %.1f%%",
                        formatKm(effectiveRemain), progress
                );

                // 알림 갱신(텍스트 + 프로그레스바)
                updateNotification("산책 중", text);

                // 여기서 도착 판단 → 브로드캐스트
                if (!arrivedBroadcastSent && m.remainingMeters <= ARRIVE_THRESHOLD_METERS) {
                    arrivedBroadcastSent = true;
                    sendArrivedBroadcast();
                    // 원하면 서비스 종료:
                    // stopSelf();
                }
            }
        };

        try {
            fused.requestLocationUpdates(req, callback, getMainLooper());
        } catch (SecurityException se) {
            Log.e(TAG, "Location permission missing", se);
        }
    }

    private void stopLocationUpdates() {
        if (callback != null) {
            fused.removeLocationUpdates(callback);
            callback = null;
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Tracking", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        // 알림의 “정지” 액션
        Intent stop = new Intent(this, TrackingService.class).setAction(ACTION_STOP);
        PendingIntent stopPI = PendingIntent.getService(
                this, 1, stop,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.noimg) // 존재하는 아이콘으로 교체
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setOngoing(true)
                .addAction(R.drawable.noimg, "정지", stopPI)
                .setProgress(0, 0, false);
        return b.build();
    }

    private void updateNotification(String title, String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID, buildNotification(title, text));
    }

    private static String formatKm(double meters) {
        if (meters >= 1000) return String.format(Locale.KOREA, "%.2fKm", meters / 1000.0);
        else return String.format(Locale.KOREA, "%.0fm", meters);
    }

    private void sendArrivedBroadcast() {
        if (route == null) return;
        Intent i = new Intent(ACTION_ARRIVED);
        i.setPackage(getPackageName()); // 앱 내부로만
        i.putExtra(EXTRA_ROUTE_ID, route.getCourse_id());      // Route의 id getter에 맞게 수정
        i.putExtra(EXTRA_ROUTE_TITLE, route.getTitle()); // Route의 title getter에 맞게 수정
        sendBroadcast(i);
        updateNotification("도착", "목적지에 도착했습니다");
    }
}
