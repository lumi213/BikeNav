package com.lumi.android.bicyclemap.util;

import androidx.annotation.NonNull;
import com.google.android.gms.maps.model.LatLng;
import java.util.*;

/** 카카오 길찾기 등 폴리라인 후처리(중복/왕복 구간 제거, 포인트 압축) */
public final class PolylineUtils {
    private PolylineUtils() {}

    /** 메인 엔트리: 루프 제거 + 포인트 압축 */
    @NonNull
    public static List<LatLng> cleanRoute(@NonNull List<LatLng> pts,
                                          double toleranceMeters) {
        if (pts.size() < 3) return new ArrayList<>(pts);
        double lat0 = pts.get(0).latitude;

        // 1) 포인트 압축: 너무 가까운 포인트 제거(기본 3m)
        List<LatLng> compressed = compressClosePoints(pts, Math.max(1.5, toleranceMeters * 0.6));

        // 2) 루프/왕복 제거: 이전 방문 지점 재방문 시 사이 구간 삭제
        List<LatLng> loopless = removeLoops(compressed, toleranceMeters, lat0);

        // 3) 한번 더 살짝 압축 (연속 중복 제거)
        return compressClosePoints(loopless, Math.max(1.5, toleranceMeters * 0.6));
    }

    /** tolerance 이내의 연속 포인트는 하나로 합치기 */
    @NonNull
    private static List<LatLng> compressClosePoints(@NonNull List<LatLng> pts, double tolMeters) {
        ArrayList<LatLng> out = new ArrayList<>(pts.size());
        LatLng prev = null;
        for (LatLng p : pts) {
            if (prev == null || distMeters(prev, p) >= tolMeters) {
                out.add(p);
                prev = p;
            }
        }
        // 마지막 점 보장
        if (!out.isEmpty() && !out.get(out.size()-1).equals(pts.get(pts.size()-1))) {
            out.add(pts.get(pts.size()-1));
        }
        return out;
    }

    /** 같은 지점(격자) 재방문 시 그 사이 구간 삭제 */
    @NonNull
    private static List<LatLng> removeLoops(@NonNull List<LatLng> pts,
                                            double tolMeters, double latRef) {
        // 위경도를 정사각 격자에 양자화해서 빠르게 '같은 지점' 검출
        double metersPerDegLat = 111320.0;
        double metersPerDegLon = 111320.0 * Math.cos(Math.toRadians(latRef));
        double scaleLat = metersPerDegLat / tolMeters;
        double scaleLon = metersPerDegLon / tolMeters;

        // 격자키 -> 처음 나타난 인덱스
        HashMap<Long, Integer> firstIndex = new HashMap<>();
        ArrayList<LatLng> list = new ArrayList<>(pts);

        int i = 0;
        while (i < list.size()) {
            LatLng p = list.get(i);
            long key = gridKey(p, scaleLon, scaleLat);
            Integer prevIdx = firstIndex.get(key);

            if (prevIdx == null) {
                firstIndex.put(key, i);
                i++;
                continue;
            }

            // 같은 격자 재방문 → 실제 거리로도 검증
            if (distMeters(list.get(prevIdx), p) <= tolMeters) {
                // prevIdx..i 사이를 잘라내고 i 위치를 prevIdx 로 ‘축소’ (루프 제거)
                if (i - prevIdx >= 2) { // 최소 두 점 이상일 때만 의미 있음
                    // prevIdx+1 ~ i 삭제
                    for (int k = i; k > prevIdx; k--) list.remove(k);
                    // 테이블을 prevIdx 이후로 다시 만들어야 함
                    // 간단히 처음부터 재스캔(보통 좌표 수가 수백~수천으로 충분히 빠름)
                    firstIndex.clear();
                    i = 0;
                    continue;
                }
            }

            // 다른 이유(근접X)면 현 위치 등록 갱신
            firstIndex.put(key, i);
            i++;
        }

        return list;
    }

    private static long gridKey(LatLng p, double scaleLon, double scaleLat) {
        long gx = Math.round(p.longitude * scaleLon);
        long gy = Math.round(p.latitude  * scaleLat);
        return (gx << 32) ^ (gy & 0xffffffffL);
    }

    /** 하버사인 거리(m) */
    public static double distMeters(LatLng a, LatLng b) {
        double R = 6371000.0;
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLon = Math.toRadians(b.longitude - a.longitude);
        double s = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(a.latitude))*Math.cos(Math.toRadians(b.latitude))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1.0 - s));
        return R * c;
    }
}
