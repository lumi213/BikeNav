package com.lumi.android.bicyclemap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lumi.android.bicyclemap.api.ApiClient;
import com.lumi.android.bicyclemap.api.ApiService;
import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.CourseListResponse;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.dto.PoiListResponse;
import com.lumi.android.bicyclemap.api.dto.ReviewRequest;
import com.lumi.android.bicyclemap.repository.AuthRepository;
import com.lumi.android.bicyclemap.repository.CourseRepository;
import com.lumi.android.bicyclemap.repository.ReviewRepository;
import com.lumi.android.bicyclemap.service.RouteHolder;
import com.lumi.android.bicyclemap.service.TrackingService;
import com.lumi.android.bicyclemap.ui.course.CourseFragment;
import com.lumi.android.bicyclemap.ui.home.MapsFragment;
import com.lumi.android.bicyclemap.ui.setting.SettingFragment;
import com.lumi.android.bicyclemap.ui.survey.StartupSurveyFragment;
import com.lumi.android.bicyclemap.ui.surrounding.SurroundingFragment;
import com.lumi.android.bicyclemap.ui.tour.TourFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private BottomNavigationView bottomNavigationView;
    private long lastBackPressedTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000; // 2초

    // Repository들
    private AuthRepository authRepository;
    private CourseRepository courseRepository;

    private BroadcastReceiver arriveReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Repository 초기화
        viewModel.initRepository(this);
        authRepository = AuthRepository.getInstance(this);
        courseRepository = new CourseRepository(this);

        // 로컬 데이터 로드 (API 실패시 대체용)
        Executors.newSingleThreadExecutor().execute(() -> {
            Map<Integer, List<Integer>> coursePoiMap = readCoursePoiMap();
            List<CourseDto> routes = loadRoutes(coursePoiMap); // 동기
            Map<Integer, PoiDto> poiMap = loadPoi();

            for (CourseDto r : routes) {
                Log.d("ROUTE_CHECK",
                        "ID=" + r.getCourse_id() +
                                ", title=" + r.getTitle() +
                                ", dist=" + r.getDist_km() +
                                ", time=" + r.getTime() +
                                ", diff=" + r.getDiff() +
                                ", type=" + r.getType() +
                                ", poi=" + r.poi +
                                ", path size=" + (r.getPath() != null ? r.getPath().size() : 0));
            }

            runOnUiThread(() -> {
                viewModel.setAllRoutes(routes);
                viewModel.setPoiMap(poiMap);
            });
        });

        // 네비게이션 설정
        bottomNavigationView = findViewById(R.id.nav_view);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_tour) {
                replaceFragment(new TourFragment(), false);
                return true;
            } else if (itemId == R.id.navigation_course) {
                replaceFragment(new CourseFragment(), false);
                return true;
            } else if (itemId == R.id.navigation_maps) {
                replaceFragment(new MapsFragment(), false);
                return true;
            } else if (itemId == R.id.navigation_surrounding) {
                replaceFragment(new SurroundingFragment(), false);
                return true;
            } else if (itemId == R.id.navigation_setting) {
                replaceFragment(new SettingFragment(), false);
                return true;
            }
            return false;
        });

        // MapState 관찰 → 트래킹 서비스 관리
        viewModel.getMapState().observe(this, state -> {
            if (state == MainViewModel.MapState.WALKING) {
                CourseDto sel = viewModel.getSelectedRoute().getValue();
                RouteHolder.set(sel);
                startTrackingService();
            } else {
                stopTrackingService();
                RouteHolder.clear();
            }
        });

        // ✅ 추천 완료 후 ViewModel이 코스를 선택하면 지도 탭으로 포커스 이동
        viewModel.getSelectedRoute().observe(this, route -> {
            if (route != null && bottomNavigationView.getSelectedItemId() != R.id.navigation_maps) {
                setBottomNavigationSelected(R.id.navigation_maps);
            }
        });

        // 초기 화면: 지도탭
        bottomNavigationView.setSelectedItemId(R.id.navigation_maps);

        // =========================[ 설문 오버레이 띄우기 ]========================
        if (savedInstanceState == null &&
                getSupportFragmentManager().findFragmentByTag("StartupSurvey") == null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    // android.R.id.content에 add → 탭 replace 영향 없음
                    .add(android.R.id.content, new StartupSurveyFragment(), "StartupSurvey")
                    .commitAllowingStateLoss();
        }
        // ========================================================================
    }

    private void startTrackingService() {
        Intent i = new Intent(this, TrackingService.class)
                .setAction(TrackingService.ACTION_START);
        ContextCompat.startForegroundService(this, i);
    }

    private void stopTrackingService() {
        Intent i = new Intent(this, TrackingService.class)
                .setAction(TrackingService.ACTION_STOP);
        stopService(i);
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack(null);
        transaction.commit();
    }

    public void setBottomNavigationSelected(int itemId) {
        bottomNavigationView.setSelectedItemId(itemId);
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        // 1) 설문 오버레이가 열려있으면 먼저 닫기
        Fragment f = getSupportFragmentManager().findFragmentByTag("StartupSurvey");
        if (f != null && f.isAdded()) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .remove(f)
                    .commitAllowingStateLoss();
            return;
        }

        if (currentFragment instanceof CourseFragment || currentFragment instanceof SurroundingFragment) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_maps);  // 지도탭으로 이동
        } else if (currentFragment instanceof MapsFragment) {
            MainViewModel.MapState state = viewModel.getMapState().getValue();
            if (state == MainViewModel.MapState.WALKING) {
                new AlertDialog.Builder(this)
                        .setTitle("산책 종료")
                        .setMessage("산책을 종료하고 코스 선택 화면으로 돌아갈까요?")
                        .setPositiveButton("종료", (d, w) -> {
                            viewModel.resetMapState();
                            Toast.makeText(this, "산책을 종료했습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("유지", (d, w) -> d.dismiss())
                        .show();
                return;
            } else {
                long now = System.currentTimeMillis();
                if (now - lastBackPressedTime < BACK_PRESS_INTERVAL) {
                    finishAffinity();
                    System.runFinalization();
                    System.exit(0);
                } else {
                    lastBackPressedTime = now;
                    Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 동기 호출(execute)이므로 반드시 백그라운드 스레드에서 사용
     */
    private List<CourseDto> loadRoutes(Map<Integer, List<Integer>> coursePoiMap) {
        List<CourseDto> routes = new ArrayList<>();
        ApiService api = ApiClient.getInstance(this).getApiService();

        try {
            // 1) 코스 목록
            Response<CourseListResponse> listResp = api.getCourseList(null, null, null).execute();
            if (!(listResp.isSuccessful()
                    && listResp.body() != null
                    && listResp.body().isSuccess()
                    && listResp.body().getData() != null)) {
                Log.w("ROUTE_LIST", "list fetch failed");
                return routes;
            }

            // 2) 상세 병합
            for (CourseDto dto : listResp.body().getData()) {
                dto.poi = coursePoiMap.getOrDefault(dto.getCourse_id(), new ArrayList<>());

                try {
                    Response<ApiResponse<CourseDto>> detailResp =
                            api.getCourseDetail(dto.getCourse_id()).execute();

                    if (detailResp.isSuccessful()
                            && detailResp.body() != null
                            && detailResp.body().isSuccess()
                            && detailResp.body().getData() != null) {

                        CourseDto detail = detailResp.body().getData();
                        mergeCourseDetail(dto, detail);
                    } else {
                        Log.w("ROUTE_DETAIL", "detail fail id=" + dto.getCourse_id());
                    }
                } catch (IOException e) {
                    Log.e("ROUTE_DETAIL", "detail io error id=" + dto.getCourse_id(), e);
                }

                routes.add(dto);
            }
        } catch (IOException e) {
            Log.e("ROUTE_LIST", "list io error", e);
        }

        return routes;
    }

    private void mergeCourseDetail(CourseDto base, CourseDto detail) {
        try {
            if (detail.getPath() != null && !detail.getPath().isEmpty()) {
                base.setPath(detail.getPath());
            }
            if (detail.getDescription() != null && !detail.getDescription().trim().isEmpty()) {
                base.setDescription(detail.getDescription());
            }
            if (detail.getImage() != null && !detail.getImage().trim().isEmpty()) {
                base.setImage(detail.getImage());
            }
            if (detail.getImages() != null && !detail.getImages().isEmpty()) {
                base.setImages(detail.getImages());
            }
            if (detail.getTags() != null && !detail.getTags().isEmpty()) {
                base.setTags(detail.getTags());
            }
            if (detail.getTourist_spots() != null && !detail.getTourist_spots().isEmpty()) {
                base.setTourist_spots(detail.getTourist_spots());
            }
            if (detail.getNearby_businesses() != null && !detail.getNearby_businesses().isEmpty()) {
                base.setNearby_businesses(detail.getNearby_businesses());
            }
        } catch (Exception e) {
            Log.e("ROUTE_MERGE", "merge error id=" + base.getCourse_id(), e);
        }
    }

    // MainThread에서 호출하지 말 것 (execute 사용)
    private Map<Integer, List<Integer>> readCoursePoiMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        try {
            ApiService api = ApiClient.getInstance(this).getApiService();

            Response<CourseListResponse> courseResp = api.getCourseList(null, null, null).execute();
            if (courseResp.isSuccessful() && courseResp.body() != null
                    && courseResp.body().isSuccess()
                    && courseResp.body().getData() != null) {

                List<CourseDto> courses = courseResp.body().getData();
                for (CourseDto course : courses) {
                    int courseId = course.getCourse_id();

                    Response<PoiListResponse> poiResp = api.getPois(courseId, null).execute();
                    if (poiResp.isSuccessful() && poiResp.body() != null
                            && poiResp.body().isSuccess()
                            && poiResp.body().getData() != null
                            && poiResp.body().getData().getPois() != null) {

                        List<Integer> ids = new ArrayList<>();
                        for (PoiDto p : poiResp.body().getData().getPois()) {
                            ids.add((int) p.getId());
                        }
                        map.put(courseId, ids);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    // MainThread에서 호출하지 말 것 (execute 사용)
    private Map<Integer, PoiDto> loadPoi() {
        Map<Integer, PoiDto> result = new HashMap<>();
        Set<Integer> fetched = new HashSet<>();

        try {
            ApiService api = ApiClient.getInstance(this).getApiService();

            Response<CourseListResponse> courseResp = api.getCourseList(null, null, null).execute();
            if (courseResp.isSuccessful() && courseResp.body() != null
                    && courseResp.body().isSuccess()
                    && courseResp.body().getData() != null) {

                List<CourseDto> courses = courseResp.body().getData();
                for (CourseDto course : courses) {
                    int courseId = course.getCourse_id();

                    Response<PoiListResponse> poiResp = api.getPois(courseId, null).execute();
                    if (!(poiResp.isSuccessful() && poiResp.body() != null
                            && poiResp.body().isSuccess()
                            && poiResp.body().getData() != null
                            && poiResp.body().getData().getPois() != null)) {
                        continue;
                    }

                    for (PoiDto d : poiResp.body().getData().getPois()) {
                        int pid = d.getId();

                        PoiDto poi = result.getOrDefault(pid, new PoiDto());
                        poi.setId(pid);
                        poi.setName(d.getName());
                        poi.setType(d.getType()); // "biz" | "util" | "tourist"

                        if (d.getPoint() != null) {
                            poi.setPoint(new Point(d.getPoint().getLat(), d.getPoint().getLng()));
                        }
                        poi.setExplanation(d.getExplanation());

                        if (!fetched.contains(pid)) {
                            try {
                                Response<ApiResponse<PoiDto>> detailResp =
                                        api.getPoiDetail(courseId, pid).execute();

                                if (detailResp.isSuccessful()
                                        && detailResp.body() != null
                                        && detailResp.body().isSuccess()
                                        && detailResp.body().getData() != null) {
                                    PoiDto detail = detailResp.body().getData();

                                    poi.setAddr(detail.getAddr());
                                    poi.setHour(parseHours(detail.getHour()));
                                    poi.setRate(detail.getRate());
                                    poi.setTel(detail.getTel());

                                    if (detail.getTag() != null) {
                                        poi.setTags(detail.getTag());
                                    }
                                    if (detail.getImages() != null) {
                                        poi.setImages(detail.getImages());
                                    }
                                    if (detail.getMainImages() != null) {
                                        poi.setMainImages(detail.getMainImages());
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            fetched.add(pid);
                        }

                        if (!hasPhone(poi.getTel())) {
                            continue;
                        }
                        result.put(pid, poi);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** tel이 유효한지(숫자 하나 이상 포함) 체크 */
    private boolean hasPhone(String tel) {
        if (tel == null) return false;
        String t = tel.trim();
        if (t.isEmpty()) return false;
        return t.matches(".*\\d+.*");
    }

    /* ================== 보조 파서 ================== */

    // hour 예: "{'운영일': ['매일'], '운영시간': ['08:00-21:00']}"
    private String parseHours(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        try {
            String fixed = raw.replace('\'', '"');
            org.json.JSONObject o = new org.json.JSONObject(fixed);
            String day  = joinJsonArray(o.optJSONArray("운영일"));
            String time = joinJsonArray(o.optJSONArray("운영시간"));
            if (!day.isEmpty() && !time.isEmpty()) return day + " " + time;
            if (!time.isEmpty()) return time;
            return day;
        } catch (Exception e) {
            return raw;
        }
    }

    private String joinJsonArray(org.json.JSONArray arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr.optString(i));
        }
        return sb.toString();
    }

    // tag 예: "[{\"tag1\":\"한식\"},{\"tag2\":\"면요리\"}]"
    private List<String> parseTags(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        try {
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                for (java.util.Iterator<String> it = o.keys(); it.hasNext();) {
                    String k = it.next();
                    String v = o.optString(k, null);
                    if (v != null && !v.isEmpty()) out.add(v);
                }
            }
        } catch (Exception ignore) {}
        return out;
    }

    private String joinWithDot(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(" · ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (arriveReceiver == null) {
            arriveReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    if (TrackingService.ACTION_ARRIVED.equals(intent.getAction())) {
                        long routeId = intent.getLongExtra(TrackingService.EXTRA_ROUTE_ID, -1);
                        String routeTitle = intent.getStringExtra(TrackingService.EXTRA_ROUTE_TITLE);
                        showCourseReviewDialog(routeId, routeTitle, /*trackingId=*/0);
                    }
                }
            };
        }
        registerReceiver(arriveReceiver, new IntentFilter(TrackingService.ACTION_ARRIVED),
                Context.RECEIVER_NOT_EXPORTED); // 앱 내부만
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (arriveReceiver != null) {
            unregisterReceiver(arriveReceiver);
        }
    }

    private void showEndDialogAndReview(long routeId, String routeTitle) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("산책 종료")
                .setMessage("목적지에 도착했습니다. 산책을 종료할까요?")
                .setCancelable(false)
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .setPositiveButton("종료", (d, w) -> {
                    d.dismiss();
                    showCourseReviewDialog(routeId, routeTitle, /*trackingId=*/0);
                })
                .show();
    }

    private void showCourseReviewDialog(long courseId, String title, int trackingId) {
        // 기존 종료 처리 (예: 지도 초기화 등)
        viewModel.resetMapState();

        View view = getLayoutInflater().inflate(R.layout.dialog_course_review, null, false);

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText etComment = view.findViewById(R.id.etComment);
        TextView tvTitle = view.findViewById(R.id.tvCourseTitle);
        tvTitle.setText("‘" + title + "’ 코스는 어땠나요?");

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton("건너뛰기", (d, w) -> d.dismiss())
                .setPositiveButton("보내기", (d, w) -> {
                    int rating = Math.max(1, Math.round(ratingBar.getRating()));
                    String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";

                    int userId = authRepository.getCurrentUserId();

                    ReviewRequest body = ReviewRequest.forCourse(
                            userId,
                            (int) courseId,
                            trackingId,
                            rating,
                            comment,
                            null,
                            null
                    );

                    new ReviewRepository(this)
                            .submit(body, new ReviewRepository.Listener() {
                                @Override public void onSuccess(ApiResponse res) {
                                    Toast.makeText(MainActivity.this, "리뷰가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onError(Throwable t) {
                                    Toast.makeText(MainActivity.this, "리뷰 전송 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                })
                .show();
    }
}
