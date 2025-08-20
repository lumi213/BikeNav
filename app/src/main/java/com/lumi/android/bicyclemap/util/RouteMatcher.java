package com.lumi.android.bicyclemap.util;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.lumi.android.bicyclemap.Point;
import com.lumi.android.bicyclemap.api.dto.CourseDto;

import java.util.List;

/**
 * 현재 위치를 경로(폴리라인)에 스냅하고 진행률/세그먼트 등을 계산하는 유틸.
 * 위경도→로컬 평면(m) 변환 후 최근접 투영점으로 계산 (짧은 구간에서 충분히 정확).
 */
public final class RouteMatcher {

    private static final double R = 6371000.0; // 지구 반경 (m)

    private RouteMatcher() {}

    public static class Result {
        /** 최근접 세그먼트의 시작 인덱스 (path[i]~path[i+1]) */
        public final int segmentIndex;
        /** 세그먼트 내 보간 비율 0..1 (0은 시작점, 1은 끝점) */
        public final double tOnSegment;
        /** 경로 위 최근접점 (위경도) */
        public final LatLng snappedLatLng;
        /** 현재 위치에서 경로까지의 수평 거리 (m) */
        public final double distanceToRouteMeters;
        /** 시작점부터 스냅 지점까지 누적 진행 거리 (m) */
        public final double traveledMeters;
        /** 경로 전체 길이 (m) */
        public final double totalMeters;
        /** 남은 거리 (m) = total - traveled */
        public final double remainingMeters;
        /** 진행률 (0~100) */
        public final double progressPercent;

        public Result(int segmentIndex, double tOnSegment, LatLng snappedLatLng,
                      double distanceToRouteMeters, double traveledMeters,
                      double totalMeters) {
            this.segmentIndex = segmentIndex;
            this.tOnSegment = tOnSegment;
            this.snappedLatLng = snappedLatLng;
            this.distanceToRouteMeters = distanceToRouteMeters;
            this.traveledMeters = traveledMeters;
            this.totalMeters = totalMeters;
            this.remainingMeters = Math.max(0, totalMeters - traveledMeters);
            this.progressPercent = (totalMeters > 0) ? (traveledMeters / totalMeters * 100.0) : 0.0;
        }
    }

    /**
     * 현재 위치(currLat, currLng)를 route.path에 스냅해 진행 정보를 계산한다.
     * @param route   선택된 경로 (path가 2점 이상이어야 함)
     * @param currLat 현재 위도
     * @param currLng 현재 경도
     * @return 계산 결과, path가 비정상이면 null
     */
    @Nullable
    public static Result match(CourseDto route, double currLat, double currLng) {
        if (route == null) return null;
        List<Point> path = route.getPath();
        if (path == null || path.size() < 2) return null;

        // 로컬 평면 좌표로 변환 (기준: 현재 위치)
        double latRefRad = Math.toRadians(currLat);
        double lonRefRad = Math.toRadians(currLng);

        // 현재 위치 (m)
        double cx = 0;
        double cy = 0;

        double bestDist = Double.MAX_VALUE;
        int bestSegIdx = -1;
        double bestT = 0;
        double bestPx = 0, bestPy = 0; // 스냅된 XY
        double traveledAtBest = 0.0;

        double totalLen = 0.0;
        double cumLenToSegStart = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            Point a = path.get(i);
            Point b = path.get(i + 1);

            // A,B를 로컬 XY(m)로
            double ax = lonLatToX(a.lng, lonRefRad, latRefRad);
            double ay = latToY(a.lat, latRefRad);
            double bx = lonLatToX(b.lng, lonRefRad, latRefRad);
            double by = latToY(b.lat, latRefRad);

            // 세그먼트 벡터
            double dx = bx - ax;
            double dy = by - ay;
            double segLen2 = dx * dx + dy * dy;
            double segLen = Math.sqrt(segLen2);
            totalLen += segLen;

            if (segLen2 == 0) {
                cumLenToSegStart += segLen; // 0이지만 일관성 위해
                continue;
            }

            // 점 C에서 세그먼트 AB로의 투영 (0..1로 클램프)
            double t = ((cx - ax) * dx + (cy - ay) * dy) / segLen2;
            if (t < 0) t = 0;
            else if (t > 1) t = 1;

            double px = ax + t * dx;
            double py = ay + t * dy;

            double dist = hypot(cx - px, cy - py);

            if (dist < bestDist) {
                bestDist = dist;
                bestSegIdx = i;
                bestT = t;
                bestPx = px;
                bestPy = py;
                traveledAtBest = cumLenToSegStart + t * segLen;
            }

            cumLenToSegStart += segLen;
        }

        if (bestSegIdx < 0) return null;

        LatLng snapped = xyToLatLng(bestPx, bestPy, latRefRad, lonRefRad);

        return new Result(bestSegIdx, bestT, snapped, bestDist, traveledAtBest, totalLen);
    }

    // ====== helpers ======

    private static double lonLatToX(double lonDeg, double lonRefRad, double latRefRad) {
        double lonRad = Math.toRadians(lonDeg);
        return (lonRad - lonRefRad) * Math.cos(latRefRad) * R;
    }

    private static double latToY(double latDeg, double latRefRad) {
        double latRad = Math.toRadians(latDeg);
        return (latRad - latRefRad) * R;
    }

    private static LatLng xyToLatLng(double x, double y, double latRefRad, double lonRefRad) {
        double lat = Math.toDegrees(latRefRad + (y / R));
        double lon = Math.toDegrees(lonRefRad + (x / (R * Math.cos(latRefRad))));
        return new LatLng(lat, lon);
    }

    private static double hypot(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }
}
