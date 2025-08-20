package com.lumi.android.bicyclemap;

import android.content.BroadcastReceiver;
import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lumi.android.bicyclemap.api.ApiClient;
import com.lumi.android.bicyclemap.api.ApiService;
import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.CourseListResponse;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.dto.PoiListResponse;
import com.lumi.android.bicyclemap.api.dto.ReviewRequest;
import com.lumi.android.bicyclemap.api.dto.VillagesDto;
import com.lumi.android.bicyclemap.api.dto.VillagesListResponse;
import com.lumi.android.bicyclemap.repository.AuthRepository;
import com.lumi.android.bicyclemap.repository.CourseRepository;
import com.lumi.android.bicyclemap.repository.ReviewRepository;
import com.lumi.android.bicyclemap.service.RouteHolder;
import com.lumi.android.bicyclemap.ui.course.CourseFragment;
import com.lumi.android.bicyclemap.ui.home.MapsFragment;
import com.lumi.android.bicyclemap.ui.setting.SettingFragment;
import com.lumi.android.bicyclemap.ui.surrounding.SurroundingFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import retrofit2.Response;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.lumi.android.bicyclemap.service.TrackingService;
import com.lumi.android.bicyclemap.ui.tour.TourFragment;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private BottomNavigationView bottomNavigationView;
    private long lastBackPressedTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000; // 2초

    // Repository들
    private AuthRepository authRepository;
    private CourseRepository courseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Repository 초기화
        viewModel.initRepository(this);
        authRepository = new AuthRepository(this);
        courseRepository = new CourseRepository(this);

        // 로컬 데이터 로드 (API 실패시 대체용)
        Executors.newSingleThreadExecutor().execute(() -> {
            Map<Integer, List<Integer>> coursePoiMap = readCoursePoiMap();
            List<CourseDto> routes = loadRoutes(coursePoiMap); // 동기
            Map<Integer, PoiDto> poiMap = loadPoi();
            // 전체 데이터 로그로 확인
            for (CourseDto r : routes) {
                Log.d("ROUTE_CHECK",
                        "ID=" + r.getCourse_id() +
                                ", title=" + r.getTitle() +
                                ", dist=" + r.getDist_km() +
                                ", time=" + r.getTime() +
                                ", diff=" + r.getDiff() +
                                ", type=" + r.getType() +
                                ", poi=" + r.poi +
                                ", path size=" + (r.getPath() != null ? r.getPath().size() : 0)
                );
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
                // 상태는 기존 유지(코스선택/산책모드 등)
                replaceFragment(new MapsFragment(), false);
                return true;
            } else if (itemId == R.id.navigation_surrounding) {
                replaceFragment(new SurroundingFragment(), false);
                return true;
            }else if (itemId == R.id.navigation_setting) {
                replaceFragment(new SettingFragment(), false);
                return true;
            }

            return false;
        });

        // MapState 를 사용 중이면 그것을 관찰
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

        // 초기 화면: 지도탭
        bottomNavigationView.setSelectedItemId(R.id.navigation_maps);
    }

    private void startTrackingService() {
        Intent i = new Intent(this, TrackingService.class)
                .setAction(TrackingService.ACTION_START);
        // i.putExtra("routeId", ...); // 필요 시
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

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    public void setBottomNavigationSelected(int itemId) {
        bottomNavigationView.setSelectedItemId(itemId);
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof CourseFragment || currentFragment instanceof SurroundingFragment) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_maps);  // 지도탭으로 이동
        } else if (currentFragment instanceof MapsFragment) {
            // MapsFragmet의 State가 WALKING면 산책종료 질의
            viewModel = new ViewModelProvider(this).get(MainViewModel.class);
            MainViewModel.MapState state = viewModel.getMapState().getValue();
            if (state == MainViewModel.MapState.WALKING) {
                // 산책 종료 확인 팝업
                new AlertDialog.Builder(this)
                        .setTitle("산책 종료")
                        .setMessage("산책을 종료하고 코스 선택 화면으로 돌아갈까요?")
                        .setPositiveButton("종료", (d, w) -> {
                            viewModel.resetMapState(); // 상태 IDLE로
                            // MapsFragment는 상태 관찰로 코스 선택 UI로 자동 전환
                            Toast.makeText(this, "산책을 종료했습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("유지", (d, w) -> d.dismiss())
                        .show();
                return; // 팝업 처리했으니 여기서 종료
            }
            else{
                long now = System.currentTimeMillis();
                if (now - lastBackPressedTime < BACK_PRESS_INTERVAL) {
                    // super.onBackPressed();  // 앱 종료
                    finishAffinity();
                    System.runFinalization();
                    System.exit(0);
                } else {
                    lastBackPressedTime = now;
                    Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onBackPressed();  // 디테일 등 → 이전 화면 복귀
        }
    }

    /**
     * - 주의: 동기 호출(execute)이라서 반드시 백그라운드 스레드에서 호출하세요.
     */
    private List<CourseDto> loadRoutes(Map<Integer, List<Integer>> coursePoiMap) {
        List<CourseDto> routes = new ArrayList<>();

        ApiService api = ApiClient.getInstance(this).getApiService();

        // 1) 목록 동기 호출
        CourseListResponse listBody = null;
        try {
            Response<CourseListResponse> listResp =
                    api.getCourseList(null, null, null).execute();
            if (listResp.isSuccessful()) listBody = listResp.body();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (listBody == null || !listBody.isSuccess() || listBody.getData() == null) {
            return routes; // 빈 리스트
        }

        // 3) 각 코스에 대해 상세 동기 호출 → path 등 채움
        for (CourseDto dto : listBody.getData()) {

            // poi id 주입
            dto.poi = coursePoiMap.getOrDefault(dto.getCourse_id(), new ArrayList<>());

            // 상세 호출 (path / description / images / tags 등)
            try {
                Response<ApiResponse<CourseDto>> detailResp =
                        api.getCourseDetail(dto.getCourse_id()).execute();
                if (detailResp.isSuccessful()
                        && detailResp.body() != null
                        && detailResp.body().isSuccess()
                        && detailResp.body().getData() != null) {
                } else {
                    // 상세 실패 시: path 비어있을 수 있음(로그만)
                    Log.w("ROUTE_DETAIL", "detail fail id=" + dto.getCourse_id());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            routes.add(dto);
        }
        return routes;
    }

    // 상세 path [{lat,lng}, ...] → List<Point>
    private List<Point> toPoints(List<Point> path) {
        List<Point> out = new ArrayList<>();
        for (Point p : path) {
            out.add(new Point(p.getLat(), p.getLng()));
        }
        return out;
    }

    // MainThread에서 호출하지 말 것 (execute() 사용)
    private Map<Integer, List<Integer>> readCoursePoiMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        try {
            ApiService api = ApiClient.getInstance(this).getApiService();

            // 코스 전체 조회 (필터 없음)
            Response<CourseListResponse> courseResp =
                    api.getCourseList(null, null, null).execute();

            if (courseResp.isSuccessful() && courseResp.body() != null
                    && courseResp.body().isSuccess()
                    && courseResp.body().getData() != null) {

                List<CourseDto> courses = courseResp.body().getData(); // data가 List<CourseDto>
                for (CourseDto course : courses) {
                    int courseId = course.getCourse_id(); // 필드명: course_id

                    // 코스 주변 POI 조회 (카테고리 전체)
                    Response<PoiListResponse> poiResp =
                            api.getPois(courseId, null).execute();

                    if (poiResp.isSuccessful() && poiResp.body() != null
                            && poiResp.body().isSuccess()
                            && poiResp.body().getData() != null
                            && poiResp.body().getData().getPois() != null) {

                        List<Integer> ids = new ArrayList<>();
                        for (PoiDto p : poiResp.body().getData().getPois()) {
                            // PoiDto의 id 타입이 long일 수도 있으니 캐스팅
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

    // MainThread에서 호출하지 말 것 (execute() 사용)
    private Map<Integer, PoiDto> loadPoi() {
        Map<Integer, PoiDto> result = new HashMap<>();
        Set<Integer> fetched = new HashSet<>(); // 상세 중복 호출 방지

        try {
            ApiService api = ApiClient.getInstance(this).getApiService();

            // 코스 전체 조회
            Response<CourseListResponse> courseResp =
                    api.getCourseList(null, null, null).execute();

            if (courseResp.isSuccessful() && courseResp.body() != null
                    && courseResp.body().isSuccess()
                    && courseResp.body().getData() != null) {

                List<CourseDto> courses = courseResp.body().getData();
                for (CourseDto course : courses) {
                    int courseId = course.getCourse_id();

                    // 코스 주변 POI 조회 (카테고리 전체)
                    Response<PoiListResponse> poiResp =
                            api.getPois(courseId, null).execute();

                    if (!(poiResp.isSuccessful() && poiResp.body() != null
                            && poiResp.body().isSuccess()
                            && poiResp.body().getData() != null
                            && poiResp.body().getData().getPois() != null)) {
                        continue;
                    }

                    for (PoiDto d : poiResp.body().getData().getPois()) {
                        int pid = d.getId();

                        // 기존/중복 POI 객체 재사용 (전부 PoiDto로만 보관)
                        PoiDto poi = result.getOrDefault(pid, new PoiDto());
                        // 기본 필드 채우기
                        poi.setId(pid);
                        poi.setName(d.getName());
                        poi.setType(d.getType()); // "biz" | "util" | "tourist"

                        if (d.getPoint() != null) {
                            poi.setPoint(new Point(d.getPoint().getLat(), d.getPoint().getLng()));
                        }
                        poi.setExplanation(d.getExplanation());

                        // ===== 상세 정보 (한 번만 호출) =====
                        if (!fetched.contains(pid)) {
                            try {
                                Response<ApiResponse<PoiDto>> detailResp =
                                        api.getPoiDetail(courseId, pid).execute();

                                if (detailResp.isSuccessful()
                                        && detailResp.body() != null
                                        && detailResp.body().isSuccess()
                                        && detailResp.body().getData() != null) {

                                    PoiDto detail = detailResp.body().getData();

                                    // 상세 필드 덮어쓰기/보강
                                    poi.setAddr(detail.getAddr());
                                    poi.setHour(parseHours(detail.getHour())); // 보기 좋게 정리
                                    poi.setRate(detail.getRate());
                                    poi.setTel(detail.getTel());

                                    // 태그: Gson이 유연 파싱한 tags 사용
                                    if (detail.getTag() != null) {
                                        poi.setTags(detail.getTag());
                                    }

                                    // 이미지: DTO 그대로 유지 (main은 필요 시 계산해서 사용)
                                    if (detail.getImages() != null) {
                                        poi.setImages(detail.getImages());
                                    }
                                    // 서버가 별도 mainImages를 주면 그대로 반영
                                    if (detail.getMainImages() != null) {
                                        poi.setMainImages(detail.getMainImages());
                                    }

                                    // 필요 시 아래처럼 계산해서 쓸 수 있음(보관은 안 함)
                                    // String mainUrl = findMainImageUrl(detail);
                                }
                            } catch (Exception e) {
                                e.printStackTrace(); // 개별 실패는 무시
                            }
                            fetched.add(pid);
                        }

                        // 필터: 전화번호 없는 항목 제외 (기존 로직 유지)
                        if (!hasPhone(poi.getTel())) {
                            continue; // result.put() 하지 않음 → 목록에서 제외
                        }

                        // 맵에 반영
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
        // 숫자가 하나라도 있는지 확인 (010-xxxx-xxxx, +82, 공백 포함 등 허용)
        return t.matches(".*\\d+.*");
    }

    /* ================== 보조 파서 ================== */

    // hour 예: "{'운영일': ['매일'], '운영시간': ['08:00-21:00']}"
    private String parseHours(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        try {
            String fixed = raw.replace('\'', '"'); // 서버가 '...' 로 줄 때 대비
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

    private BroadcastReceiver arriveReceiver;

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
                        //showEndDialogAndReview(routeId, routeTitle);
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
                    // 기존 종료 처리 (예: 지도 초기화 등)
                    // new ViewModelProvider(this).get(MainViewModel.class).resetMapState();

                    // ★ trackingId가 아직 없다면 0으로 전달(서버에서 선택 사항이면 OK)
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

                    // ✅ 2번 코드에 맞게 현재 유저 ID 가져오기
                    int userId = authRepository.getCurrentUserId();

                    // ✅ 1번 코드(ReviewRequest) 시그니처에 맞게 바디 생성
                    ReviewRequest body = ReviewRequest.forCourse(
                            userId,
                            (int) courseId,   // ReviewRequest는 int 필드
                            trackingId,       // 없으면 0
                            rating,
                            comment,
                            null,             // imgUrl
                            null              // thumbnailUrl
                    );

                    // ✅ 1번 코드 시그니처: courseId 인자 없이 body만 전달
                    new ReviewRepository(this)
                            .submit(body, new ReviewRepository.Listener() {
                                @Override public void onSuccess(ApiResponse res) {
                                    Toast.makeText(MainActivity.this, "리뷰가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onError(Throwable t) {
                                    Toast.makeText(MainActivity.this, "리뷰 전송 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                    d.dismiss();
                })
                .show();
    }
}
