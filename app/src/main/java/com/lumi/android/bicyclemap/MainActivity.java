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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import retrofit2.Response;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.lumi.android.bicyclemap.service.TrackingService;

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
            List<Route> routes = loadRoutes(coursePoiMap); // 동기
            Map<Integer, POI> poiMap = loadPoi();
            // 전체 데이터 로그로 확인
            for (Route r : routes) {
                Log.d("ROUTE_CHECK",
                        "ID=" + r.id +
                                ", title=" + r.title +
                                ", dist=" + r.dist_km +
                                ", time=" + r.time +
                                ", diff=" + r.diff +
                                ", type=" + r.type +
                                ", poi=" + r.poi +
                                ", path size=" + (r.path != null ? r.path.size() : 0)
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

            if (itemId == R.id.navigation_course) {
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
                Route sel = viewModel.getSelectedRoute().getValue();
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
     * 서버에서 코스 목록을 받아와 Route 리스트로 반환한다.
     * - /api/course/list (GET)
     * - course_poi.json(assets)로부터 courseId → poiId[] 매핑을 읽어, 반환하는 Route에 setPoiIds(...)로 주입한다.
     * - 주의: 동기 호출(execute)이라서 반드시 백그라운드 스레드에서 호출하세요.
     */
    private List<Route> loadRoutes(Map<Integer, List<Integer>> coursePoiMap) {
        List<Route> routes = new ArrayList<>();

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
            // 기본 필드 매핑
            Route r = mapListDtoToRoute(dto);

            // poi id 주입
            r.poi = coursePoiMap.getOrDefault(dto.getCourse_id(), new ArrayList<>());

            // 상세 호출 (path / description / images / tags 등)
            try {
                Response<ApiResponse<CourseDto>> detailResp =
                        api.getCourseDetail(dto.getCourse_id()).execute();
                if (detailResp.isSuccessful()
                        && detailResp.body() != null
                        && detailResp.body().isSuccess()
                        && detailResp.body().getData() != null) {

                    CourseDto detail = detailResp.body().getData();

                    // path 채우기
                    if (detail.getPath() != null && !detail.getPath().isEmpty()) {
                        r.path = toPoints(detail.getPath());
                    }
                } else {
                    // 상세 실패 시: path 비어있을 수 있음(로그만)
                    Log.w("ROUTE_DETAIL", "detail fail id=" + dto.getCourse_id());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            routes.add(r);
        }

        return routes;
    }

    // 목록 DTO → Route 기본 매핑
    private Route mapListDtoToRoute(CourseDto dto) {
        Route r = new Route();
        r.id = dto.getCourse_id();
        r.title = dto.getTitle();
        r.dist_km = dto.getDist_km();
        r.time = dto.getTime();
        r.image = dto.getImage();   // thumbnail_url → image 변경 반영
        r.diff = dto.getDiff();
        r.type = dto.getType();     // "walk" | "bike"
        // 목록 응답에 path가 있을 수도 있지만, 정확성을 위해 상세에서 다시 세팅
        return r;
    }

    // 상세 path [{lat,lng}, ...] → List<Point>
    private List<Point> toPoints(List<CourseDto.PointDto> path) {
        List<Point> out = new ArrayList<>();
        for (CourseDto.PointDto p : path) {
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
    private Map<Integer, POI> loadPoi() {
        Map<Integer, POI> result = new HashMap<>();
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

                    if (poiResp.isSuccessful() && poiResp.body() != null
                            && poiResp.body().isSuccess()
                            && poiResp.body().getData() != null
                            && poiResp.body().getData().getPois() != null) {

                        for (PoiDto d : poiResp.body().getData().getPois()) {
                            POI poi = new POI();
                            poi.id = (int) d.getId();
                            poi.name = d.getName();
                            poi.type = d.getType(); // "biz" | "util" | "tourist"

                            // 좌표 매핑
                            if (d.getPoint() != null) {
                                Point pt = new Point(d.getPoint().getLat(),d.getPoint().getLng());
                                poi.point = pt;
                            }

                            poi.explanation = d.getExplanation();

                            // 같은 id가 여러 코스에 중복 등장할 수 있어 마지막 값으로 덮어씀
                            result.put(poi.id, poi);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
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
