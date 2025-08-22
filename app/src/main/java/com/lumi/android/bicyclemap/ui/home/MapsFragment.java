package com.lumi.android.bicyclemap.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.Point;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.kakao.KakaoNaviClient;
import com.lumi.android.bicyclemap.api.kakao.KakaoNaviService;
import com.lumi.android.bicyclemap.api.kakao.KakaoRouteResponse;
import com.lumi.android.bicyclemap.util.AppPrefs;
import com.lumi.android.bicyclemap.util.RouteMatcher;
import com.lumi.android.bicyclemap.util.PolylineUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    // === LOG 태그 ===
    public static final String TAG = "MapsFragmentDebug";

    private GoogleMap map;
    private MainViewModel viewModel;
    private RecyclerView routeRecyclerView;
    private RouteAdapter adapter;
    private RadioButton radioWalk, radioBike;
    private List<CourseDto> walkRoutes = new ArrayList<>();
    private List<CourseDto> bikeRoutes = new ArrayList<>();
    private FusedLocationProviderClient fusedClient;
    private LocationCallback walkCallback;
    private boolean autoNavCameraEnabled = true;          // 자동 카메라 on/off
    private final Handler cameraHandler = new Handler(Looper.getMainLooper());
    private final Runnable resumeNavRunnable = () -> autoNavCameraEnabled = true;

    private int mapTopPaddingPx = 0;     // 내비 모드용 패딩(유저를 아래에 위치시킴)
    private int mapBottomPaddingPx = 0;  // 필요시 하단 패널 높이 만큼
    private boolean navPaddingApplied = false;
    private Polyline fullRoutePolyline;   // 전체 경로(일반 모드용)
    private Polyline remainingPolyline;   // 남은 경로 선
    private Polyline deviationPolyline;   // 경로 이탈 시 스냅→내위치 보조선
    private Polyline routeToSnapPolyline;       // 사용자→스냅 경로(도로 경로)
    private long lastKakaoCallTime = 0L;
    private Call<KakaoRouteResponse> inflightCall;
    private static final double OFFROUTE_THRESHOLD_METERS = 30.0;
    private static final long KAKAO_THROTTLE_MS = 5000; // 5초 디바운스

    // 전역 상태 연동용
    public enum State {
        GENERAL,      // 코스 선택 모드
        WALKING       // 산책/진행 모드
    }
    private State currentState = State.GENERAL;

    private LinearLayout routeInfoLayout;
    private TextView textRouteTitle, textRouteDistTime, textRemainingDistance;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 뷰 참조
        routeRecyclerView = view.findViewById(R.id.routeRecyclerView);
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new RouteAdapter(viewModel);
        routeRecyclerView.setAdapter(adapter);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(routeRecyclerView);

        routeRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                Log.d("Maps","ScrollStateChanged");
                if (newState == RecyclerView.SCROLL_STATE_IDLE && snapHelper != null) {
                    View centerView = snapHelper.findSnapView(recyclerView.getLayoutManager());
                    if (centerView != null) {
                        int pos = recyclerView.getLayoutManager().getPosition(centerView);
                        CourseDto route = adapter.getCurrentList().get(pos);
                        viewModel.setSelectedRoute(route);
                        // GENERAL 모드에서 슬라이드 변경 시 routeInfoLayout은 자동 GONE 유지
                    }
                }
            }
        });

        adapter.setOnRouteClickListener((route, pos) -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("산책 시작")
                    .setMessage("이 경로로 산책을 시작할까요?")
                    .setPositiveButton("예", (d, w) -> {
                        viewModel.setSelectedRoute(route);
                        viewModel.setMapState(MainViewModel.MapState.WALKING);
                    })
                    .setNegativeButton("아니오", (d, w) -> d.dismiss())
                    .show();
        });

        radioWalk = view.findViewById(R.id.radio_walk);
        radioBike = view.findViewById(R.id.radio_bike);

        radioWalk.setOnClickListener(v -> {
            routeRecyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(walkRoutes);
        });

        radioBike.setOnClickListener(v -> {
            routeRecyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(bikeRoutes);
        });

        // 산책상태 정보창
        routeInfoLayout = view.findViewById(R.id.routeInfoLayout);
        textRouteTitle = view.findViewById(R.id.text_route_title);
        textRouteDistTime = view.findViewById(R.id.text_route_dist_time);
        textRemainingDistance = view.findViewById(R.id.text_remaining_distance);

        // === 상태 observe (ViewModel의 mapState만 observe)
        viewModel.getMapState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                currentState = (state == MainViewModel.MapState.WALKING) ? State.WALKING : State.GENERAL;
            } else {
                currentState = State.GENERAL;
            }
            Log.d(TAG, "getMapState.observe: currentState = " + currentState);
            updateUIByState();
        });

        // 최초 진입시 리스트 GONE
        routeRecyclerView.setVisibility(View.GONE);

        // --- UI 상태 최초 갱신
        updateUIByState();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // resetMapState()가 이벤트를 쏘면 여기서 모두 지움
        viewModel.getClearMapEvent().observe(getViewLifecycleOwner(), tick -> {
            if (tick == null) return;
            clearAllMapOverlays(); // 지도 선/마커 정리
        });

        return view;
    }

    /** 지도 위 모든 선/마커를 정리하고 내부 참조도 null로 */
    private void clearAllMapOverlays() {
        if (map != null) {
            map.clear(); // 마커/폴리라인/폴리곤 등 한 번에 제거
        }
        // 내부 참조도 깔끔히 null 처리
        clearFullRoute();
        if (remainingPolyline != null)   { remainingPolyline.remove();   remainingPolyline = null; }
        if (deviationPolyline != null)   { deviationPolyline.remove();   deviationPolyline = null; }
        if (routeToSnapPolyline != null) { routeToSnapPolyline.remove(); routeToSnapPolyline = null; }
    }

    private void updateUIByState() {
        Log.d(TAG, "updateUIByState: currentState = " + currentState);
        if (currentState == State.GENERAL) {
            routeInfoLayout.setVisibility(View.GONE);
            radioWalk.setVisibility(View.VISIBLE);
            radioBike.setVisibility(View.VISIBLE);
            routeRecyclerView.setVisibility(View.GONE);
            if (remainingPolyline != null) { remainingPolyline.remove(); remainingPolyline = null; }
            if (deviationPolyline != null) { deviationPolyline.remove(); deviationPolyline = null; }
            stopWalkingLocationUpdates();
            //if (map != null) map.setPadding(0, 0, 0, 0);
            navPaddingApplied = false;
        } else if (currentState == State.WALKING) {
            routeInfoLayout.setVisibility(View.VISIBLE);
            radioWalk.setVisibility(View.GONE);
            radioBike.setVisibility(View.GONE);
            routeRecyclerView.setVisibility(View.GONE);
            clearFullRoute();
            updateRouteInfo();
            applyNavPaddingIfNeeded();
            startWalkingLocationUpdates();
        }
    }

    // 위치 업데이트 시작
    private void startWalkingLocationUpdates() {
        ensurePermissionsThenStartWalking(() -> {

            requestIgnoreBatteryOptimizationsIfNeeded();

            if (walkCallback != null) return; // 이미 시작됨

            LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                    .setMinUpdateIntervalMillis(1000)
                    .build();

            walkCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    if (result.getLastLocation() == null) return;

                    CourseDto route = viewModel.getSelectedRoute().getValue();
                    if (route == null) return;

                    double lat = result.getLastLocation().getLatitude();
                    double lng = result.getLastLocation().getLongitude();

                    RouteMatcher.Result match = RouteMatcher.match(route, lat, lng);
                    if (match == null) return;

                    // ① 남은거리 = 경로 남은거리 + 스냅→내위치 직선거리
                    double effectiveRemainingMeters = match.remainingMeters + match.distanceToRouteMeters;

                    // 예시: UI 갱신
                    if (effectiveRemainingMeters>=1000) {
                        textRemainingDistance.setText("남은 거리 " + String.format("%.2f", effectiveRemainingMeters / 1000.0) + "Km");
                    } else {
                        textRemainingDistance.setText("남은 거리 " + String.format("%.2f", effectiveRemainingMeters) + "m");
                    }
                    textRouteDistTime.setText(
                            "진행률 " + String.format("%.1f", match.progressPercent) + "%");

                    // ② 지나간 경로 제거하고 "남은 경로"만 그리기
                    updateRemainingPathOnMap(route, match);

                    if (match.remainingMeters <= 10) return;

                    // ③ 경로 이탈 시 스냅→내위치 보조선 표시, 아니면 제거
                    LatLng user = new LatLng(lat, lng);
                    updateOffRoutePath(user, match.snappedLatLng, match.distanceToRouteMeters);

                    // 경로 이탈 감지 (예: 임계값 이상 벗어나면 알림)
                    if (match.distanceToRouteMeters > OFFROUTE_THRESHOLD_METERS) {
                        Log.w(TAG, "Route off by " + (int) match.distanceToRouteMeters + " m");
                        // TODO: TTS/진동/안내 로직 등
                    }

                    // 내비 카메라 업데이트
                    if (autoNavCameraEnabled) {
                        updateNavCamera(route, match, lat, lng);
                    }
                }
            };
            try {
                fusedClient.requestLocationUpdates(req, walkCallback, requireActivity().getMainLooper());
            } catch (SecurityException se) {
                // 권한이 중간에 회수된 경우 대비
                Log.e(TAG, "Location permission missing", se);
            }
        });
    }

    private void updateOffRoutePath(@NonNull LatLng user, @NonNull LatLng snapped, double offDistMeters) {
        if (map == null) return;

        boolean offRoute = offDistMeters >= OFFROUTE_THRESHOLD_METERS;

        if (!offRoute) {
            // 경로 복귀 → 보조 경로 제거
            if (routeToSnapPolyline != null) { routeToSnapPolyline.remove(); routeToSnapPolyline = null; }
            // (직선 보조선도 쓰지 않음)
            // 진행 카메라만 유지
            return;
        }

        // 너무 잦은 호출 방지
        long now = System.currentTimeMillis();
        if (now - lastKakaoCallTime < KAKAO_THROTTLE_MS) return;
        lastKakaoCallTime = now;

        // 진행 중인 요청 취소
        if (inflightCall != null) inflightCall.cancel();

        fetchAndDrawKakaoRoute(user, snapped, new Runnable() {
            @Override public void run() {
                // 실패 시 Fallback: 직선 빨간선
                if (routeToSnapPolyline != null) { routeToSnapPolyline.remove(); routeToSnapPolyline = null; }
                routeToSnapPolyline = map.addPolyline(new PolylineOptions()
                        .add(user, snapped).width(8f).color(Color.RED).zIndex(7f).geodesic(true));
            }
        });
    }

    private void fetchAndDrawKakaoRoute(LatLng user, LatLng snapped, Runnable onFailFallback) {
        String apiKey = getString(R.string.kakao_rest_api_key); // strings.xml에 넣어두자
        KakaoNaviService svc = KakaoNaviClient.get(apiKey).create(KakaoNaviService.class);

        // Kakao는 X=경도, Y=위도. origin은 "x,y" 또는 "x,y,angle=90" 형식. :contentReference[oaicite:11]{index=11}
        String origin = String.format(Locale.US, "%.8f,%.8f,angle=%d",
                user.longitude, user.latitude, Math.round(bearingBetween(user, snapped)));
        String dest = String.format(Locale.US, "%.8f,%.8f", snapped.longitude, snapped.latitude);

        inflightCall = svc.directions(
                origin, dest,
                "RECOMMEND",   // 우선순위  :contentReference[oaicite:12]{index=12}
                false,         // alternatives
                false,         // summary=false → roads.vertexes 제공  :contentReference[oaicite:13]{index=13}
                true           // road_details=true → roads 포함
        );

        inflightCall.enqueue(new Callback<KakaoRouteResponse>() {
            @Override public void onResponse(Call<KakaoRouteResponse> call, Response<KakaoRouteResponse> res) {
                if (!res.isSuccessful() || res.body() == null || res.body().routes == null || res.body().routes.isEmpty()) {
                    if (onFailFallback != null) onFailFallback.run();
                    return;
                }
                // 첫 경로만 사용
                List<LatLng> latLngs = new ArrayList<>();
                try {
                    KakaoRouteResponse.Route r = res.body().routes.get(0);
                    if (r.sections != null) {
                        for (KakaoRouteResponse.Section s : r.sections) {
                            if (s.roads == null) continue;
                            for (KakaoRouteResponse.Road rd : s.roads) {
                                List<Double> v = rd.vertexes;
                                if (v == null || v.size() < 4) continue;
                                for (int i = 0; i < v.size() - 1; i += 2) {
                                    double x = v.get(i);     // 경도
                                    double y = v.get(i + 1); // 위도
                                    latLngs.add(new LatLng(y, x));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (onFailFallback != null) onFailFallback.run();
                    return;
                }

                if (latLngs.size() < 2) {
                    if (onFailFallback != null) onFailFallback.run();
                    return;
                }

                // 1) 왕복/중복 제거 + 압축
                List<LatLng> cleaned = PolylineUtils.cleanRoute(latLngs, /*toleranceMeters=*/10.0);

                // 2) 안전장치: 2점 미만이면 fallback
                if (cleaned.size() < 2) cleaned = latLngs;

                // 기존 보조경로 제거 후 갱신
                if (routeToSnapPolyline != null) { routeToSnapPolyline.remove(); routeToSnapPolyline = null; }
                routeToSnapPolyline = map.addPolyline(new PolylineOptions()
                        .addAll(cleaned)
                        .width(9f)
                        .color(Color.RED)
                        .zIndex(8f));
            }

            @Override public void onFailure(Call<KakaoRouteResponse> call, Throwable t) {
                if (onFailFallback != null) onFailFallback.run();
            }
        });
    }

    private void updateRemainingPathOnMap(@NonNull CourseDto route, @NonNull RouteMatcher.Result match) {
        if (map == null) return;

        clearFullRoute();

        // 이전 선 제거
        if (remainingPolyline != null) {
            remainingPolyline.remove();
            remainingPolyline = null;
        }

        // 스냅지점부터 목표까지 좌표 구성
        List<LatLng> pts = new ArrayList<>();
        pts.add(match.snappedLatLng); // 스냅지점이 항상 첫 점

        List<Point> path = route.getPath();
        int i = match.segmentIndex;
        if (path != null && path.size() >= 2 && i >= 0 && i < path.size() - 1) {
            // 현재 세그먼트의 다음 점부터 끝까지
            for (int idx = i + 1; idx < path.size(); idx++) {
                Point p = path.get(idx);
                pts.add(new LatLng(p.lat, p.lng));
            }
        }

        // 유효점이 2개 이상일 때만 라인 그림
        if (pts.size() >= 2) {
            remainingPolyline = map.addPolyline(
                    new PolylineOptions()
                            .addAll(pts)
                            .width(10f)
                            .color(Color.BLUE)
                            .zIndex(5f)
            );
        }
    }

    private void clearFullRoute() {
        if (fullRoutePolyline != null) {
            fullRoutePolyline.remove();
            fullRoutePolyline = null;
        }
    }

    // 위치 업데이트 중지
    private void stopWalkingLocationUpdates() {
        if (walkCallback != null && fusedClient != null) {
            fusedClient.removeLocationUpdates(walkCallback);
            walkCallback = null;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        map.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                pauseAutoNavFor(5_000);
            }
        });

        requireView().post(() -> {
            int h = requireView().getHeight();
            // 유저를 화면 아래쪽으로 내리기 위해 "상단 패딩"을 크게 준다.
            // 예) 상단 40%, 하단은 정보패널 높이(없으면 16dp)
            mapTopPaddingPx = dp(170);
            mapBottomPaddingPx = dp(16);
            navPaddingApplied = false; // 상태 전환 시 한 번만 setPadding 하도록 플래그
            applyNavPaddingIfNeeded(); // WALKING일 때 적용됨
        });

        viewModel.getAllRoutes().observe(getViewLifecycleOwner(), routes -> {
            walkRoutes.clear();
            bikeRoutes.clear();
            for (CourseDto r : routes) {
                if ("walk".equalsIgnoreCase(r.getType())) {
                    walkRoutes.add(r);
                } else if ("bike".equalsIgnoreCase(r.getType())) {
                    bikeRoutes.add(r);
                } else {
                    Log.w(TAG, "Unknown type for route: " + r.getTitle() + " -> " + r.getType());
                }
            }
            Log.d(TAG, "getAllRoutes: walk=" + walkRoutes.size() + ", bike=" + bikeRoutes.size());
        });

        // 1. observe 전에 현재 값이 있는지 먼저 체크
        CourseDto selected = viewModel.getSelectedRoute().getValue();
        if (selected != null) {
            Log.d(TAG, "onMapReady: selectedRoute exists on init = " + selected.getTitle());
            drawRouteOnMap(selected);
            enableMyLocation(false);
            moveCameraToRoute(selected);
        } else
        {
            enableMyLocation(true);
        }

        viewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            Log.d(TAG, "selectedRoute.observe: currentState = " + currentState + ", route = " + (route != null ? route.getTitle() : "null"));
            if (route == null) return;

            if (currentState == State.WALKING) {
                updateRouteInfo();
                clearFullRoute();
            } else {
                drawRouteOnMap(route);
                moveCameraToRoute(route);
            }
        });

        map.setOnMarkerClickListener(marker -> {
            PoiDto poi = (PoiDto) marker.getTag();
            if (poi != null) {
                POIDetailFragment.newInstance(poi).show(getParentFragmentManager(), "poi_detail");
                showPOIDetail(poi);
                return true;
            }
            return false;
        });
    }

    private void applyNavPaddingIfNeeded() {
        if (map != null && !navPaddingApplied && mapTopPaddingPx > 0) {
            map.setPadding(0, mapTopPaddingPx, 0, mapBottomPaddingPx);
            navPaddingApplied = true;
        }
    }

    private void pauseAutoNavFor(long millis) {
        autoNavCameraEnabled = false;
        cameraHandler.removeCallbacks(resumeNavRunnable);
        cameraHandler.postDelayed(resumeNavRunnable, millis);
    }

    /** 산책 내비 카메라: 유저는 아래쪽, 진행방향(또는 200m 앞 포인트)은 위쪽에 오도록
     *  - bearing: 경로 진행 방향
     *  - zoom: 유저↔앞포인트 거리를 화면 높이의 60% 정도에 맞춤
     *  - padding: 상단 큰 패딩으로 유저를 화면 아래로 내림
     */
    private void updateNavCamera(CourseDto route, RouteMatcher.Result match, double currLat, double currLng) {
        if (map == null) return;
        applyNavPaddingIfNeeded(); // 패딩이 아직이면 적용

        LatLng user = new LatLng(currLat, currLng);

        // 1) 200m 앞 포인트(UTurn 감지시 보수적으로 짧게)
        LatLng ahead = computeAheadPoint(route, match, 200.0 /*meters*/, 120.0 /*UTurn deg*/);
        if (ahead == null) {
            // fallback: 다음 점이 없으면 bearing만 디바이스 베어링 or 0
            ahead = user;
        }

        // 2) bearing: 유저→앞포인트
        float bearing = bearingBetween(user, ahead);

        // 3) zoom: 유저↔앞포인트 거리를 화면 높이의 60%로 보이도록
        double distanceMeters = haversine(user.latitude, user.longitude, ahead.latitude, ahead.longitude);
        int viewHeight = requireView().getHeight();
        float zoom = computeZoomForDistance(distanceMeters, (int) (viewHeight * 0.60f), user.latitude);
        // 적당한 범위로 클램프 (도심 내비 느낌)
        zoom = clamp(zoom, 16f, 19.5f);

        // 4) tilt: 적당히 45도 내외 (원하는 값으로)
        float tilt = 45f;

        CameraPosition cp = new CameraPosition.Builder()
                .target(user)     // 유저를 "패딩이 적용된 화면의 중심"에 둠 → 실제로는 아래쪽에 보임
                .zoom(zoom)
                .bearing(bearing)
                //.tilt(tilt)
                .build();

        map.animateCamera(CameraUpdateFactory.newCameraPosition(cp), 500, null);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float bearingBetween(LatLng a, LatLng b) {
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);
        double dLon = Math.toRadians(b.longitude - a.longitude);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (float)((brng + 360.0) % 360.0);
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    /** 거리(m)가 화면 픽셀(px)에 차지하도록 줌 추정 */
    private static float computeZoomForDistance(double meters, int pixels, double latitude) {
        meters = Math.max(1.0, meters);
        pixels = Math.max(1, pixels);
        double latRad = Math.toRadians(latitude);
        double metersPerPixelAtZoom0 = 156543.03392 * Math.cos(latRad); // 구글타일 해상도 근사
        double scale = (meters / pixels);
        double zoom = (float) (Math.log(metersPerPixelAtZoom0 / scale) / Math.log(2));
        return (float) zoom;
    }

    /** 경로상 현재 위치(match)에서 앞쪽으로 aheadMeters만큼 진행한 좌표(UTurn 예외 처리) */
    @Nullable
    private LatLng computeAheadPoint(CourseDto route, RouteMatcher.Result match, double aheadMeters, double uTurnThresholdDeg) {
        List<Point> path = route.getPath();
        if (path == null || path.size() < 2) return null;

        // 매치 지점 (세그먼트 i, 보간 t)
        int i = match.segmentIndex;
        double t = match.tOnSegment;
        if (i < 0 || i >= path.size()-1) return null;

        // 매치 지점의 실제 LatLng 보간
        Point A = path.get(i);
        Point B = path.get(i+1);
        LatLng P = lerp(A, B, t);

        double remain = aheadMeters;
        LatLng curr = P;

        // 기준 진행 방향 (현재 세그먼트)
        float baseBearing = bearingBetween(curr, new LatLng(B.lat, B.lng));

        // 앞으로 진행하며 U턴 감지
        // (간단한 휴리스틱: 누적 200m 안에서 어떤 세그먼트의 진행 각이 base와 'uTurnThresholdDeg' 이상 벌어지면
        //  U턴으로 보고 너무 멀리 잡지 않고, 바로 앞 세그먼트 끝점(B) 근처를 반환)
        for (int seg = i; seg < path.size()-1 && remain > 0; seg++) {
            Point S = path.get(seg);
            Point T = path.get(seg+1);

            LatLng sLatLng = (seg == i) ? P : new LatLng(S.lat, S.lng);
            LatLng tLatLng = new LatLng(T.lat, T.lng);

            double segLen = haversine(sLatLng.latitude, sLatLng.longitude, tLatLng.latitude, tLatLng.longitude);
            float segBearing = bearingBetween(sLatLng, tLatLng);
            double delta = angleDiffDeg(baseBearing, segBearing);

            if (delta > uTurnThresholdDeg) {
                // U턴 전으로 보수적 반환: 바로 다음 점 방향으로 50m 정도만
                return moveAlong(sLatLng, segBearing, Math.min(50.0, remain));
            }

            if (segLen >= remain) {
                return moveAlong(sLatLng, segBearing, remain);
            } else {
                remain -= segLen;
                curr = tLatLng;
            }
        }

        // 경로 끝에 도달
        return curr;
    }

    private static double angleDiffDeg(float a, float b) {
        double d = Math.abs(a - b) % 360.0;
        return d > 180.0 ? 360.0 - d : d;
    }

    private static LatLng lerp(Point a, Point b, double t) {
        double lat = a.lat + (b.lat - a.lat) * t;
        double lng = a.lng + (b.lng - a.lng) * t;
        return new LatLng(lat, lng);
    }

    /** 시작점(latLng)에서 bearing(도) 방향으로 dist(m) 이동한 좌표(단순 구면) */
    private static LatLng moveAlong(LatLng start, float bearingDeg, double distMeters) {
        double R = 6371000.0;
        double br = Math.toRadians(bearingDeg);
        double lat1 = Math.toRadians(start.latitude);
        double lon1 = Math.toRadians(start.longitude);
        double dr = distMeters / R;

        double lat2 = Math.asin(Math.sin(lat1)*Math.cos(dr) + Math.cos(lat1)*Math.sin(dr)*Math.cos(br));
        double lon2 = lon1 + Math.atan2(Math.sin(br)*Math.sin(dr)*Math.cos(lat1),
                Math.cos(dr)-Math.sin(lat1)*Math.sin(lat2));

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    private void updateRouteInfo() {
        CourseDto route = viewModel.getSelectedRoute().getValue();
        Log.d(TAG, "updateRouteInfo: called. route = " + (route != null ? route.getTitle() : "null"));
        if (route != null) {
            textRouteTitle.setText(route.getTitle());
            textRouteDistTime.setText("약 " + route.getDist_km() + "km · " + route.getTime() + "분");
            textRemainingDistance.setText("남은 거리 " + route.getDist_km() + "Km");
        }
    }

    private void enableMyLocation(boolean moveCameraToCurrent) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);

            if (moveCameraToCurrent) {
                FusedLocationProviderClient fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(requireActivity());
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16));
                    }
                });
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }


    private void drawRouteOnMap(CourseDto route) {
        if (map == null || route == null) return;
        map.clear();
        if (currentState != State.WALKING){
            List<Point> path = route.getPath();
            if (path == null || path.isEmpty()) return;

            PolylineOptions polylineOptions = new PolylineOptions().width(10).color(Color.BLUE);
            for (Point p : path) {
                polylineOptions.add(new LatLng(p.lat, p.lng));
            }
            fullRoutePolyline = map.addPolyline(polylineOptions);
        }

        Map<Integer, PoiDto> poiMap = viewModel.getPoiMap().getValue();
        if (poiMap == null) return;

        // poi marker 추가하는 부분
        if(true) return;
        for (int poiId : route.getPoi()) {
            PoiDto poi = poiMap.get(poiId);
            if (poi != null) {
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(new LatLng(poi.getPoint().lat, poi.getPoint().lng))
                        .title(poi.getName())
                        .snippet(poi.getExplanation())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                );
                if (marker != null) marker.setTag(poi);
            }
        }
    }

    private void moveCameraToRoute(CourseDto route) {
        List<Point> path = route.getPath();
        if (path == null || path.isEmpty() || map == null) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Point p : path) {
            builder.include(new LatLng(p.lat, p.lng));
        }
        try {
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            Log.e(TAG, "moveCameraToRoute error: " + e.getMessage());
        }
    }

    private static final int REQ_FINE = 1001;
    private static final int REQ_BG   = 1002;
    private static final int REQ_POST = 1003;

    private void ensurePermissionsThenStartWalking(Runnable onGranted) {
        // 1) 알림 권한 (Android 13+)
        if (Build.VERSION.SDK_INT >= 33 &&
                requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_POST);
            return;
        }

        // 2) FINE/COARSE
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_FINE);
            return;
        }

        // 3) 백그라운드 위치 (Android 10+ 에서 필요할 때만)
        if (Build.VERSION.SDK_INT >= 29 &&
                requireContext().checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQ_BG);
            return;
        }

        // 모두 허용됨
        onGranted.run();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);
        // 간단 처리: 모두 허용되었는지 다시 검사
        if (requestCode == REQ_FINE || requestCode == REQ_BG || requestCode == REQ_POST) {
            ensurePermissionsThenStartWalking(() -> {
                // 여기서 WALKING 상태로 전환 or 산책 시작 로직
                viewModel.setMapState(MainViewModel.MapState.WALKING);
            });
        }
    }

    private void requestIgnoreBatteryOptimizationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        PowerManager pm = (PowerManager) requireContext().getSystemService(android.content.Context.POWER_SERVICE);
        String pkg = requireContext().getPackageName();

        // 이미 최적화 제외 상태라면 아무 것도 하지 않음
        if (pm != null && pm.isIgnoringBatteryOptimizations(pkg)) return;

        // ------ [A안] 한번만 띄우기 ------
        if (AppPrefs.wasBatteryDialogShown(requireContext())) return;

        // ------ [B안] 하루 1회만 띄우기 (원하면 A안 대신 사용) ------
        // long ONE_DAY = 24L * 60L * 60L * 1000L;
        // if (AppPrefs.isBatteryDialogThrottled(requireContext(), ONE_DAY)) return;

        try {
            // 인텐트 띄우기
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + pkg));
            startActivity(i);

            // 가드 마킹(원하는 정책(A/B)에 맞게 한쪽만 사용)
            AppPrefs.markBatteryDialogShown(requireContext()); // A안
            // AppPrefs.stampBatteryDialogNow(requireContext()); // B안
        } catch (Exception ignore) {
            // 일부 기기에서 미지원일 수 있음. 조용히 무시
        }
    }

    private void showPOIDetail(PoiDto poi) {
        // TODO: 상세정보 UI 연결
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopWalkingLocationUpdates();
    }
}
