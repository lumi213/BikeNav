package com.lumi.android.bicyclemap;

import android.os.Bundle;
import android.os.Debug;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lumi.android.bicyclemap.api.dto.AuthResponse;
import com.lumi.android.bicyclemap.repository.AuthRepository;
import com.lumi.android.bicyclemap.repository.CourseRepository;
import com.lumi.android.bicyclemap.ui.course.CourseFragment;
import com.lumi.android.bicyclemap.ui.home.MapsFragment;
import com.lumi.android.bicyclemap.ui.surrounding.SurroundingFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
        List<Route> routes = loadRoutesFromJson();
        Map<Integer, POI> poiMap = loadPoiFromJson();

        viewModel.setAllRoutes(routes);
        viewModel.setPoiMap(poiMap);

        // 네비게이션 설정
        bottomNavigationView = findViewById(R.id.nav_view);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_course) {
                viewModel.setMapState(MainViewModel.MapState.GENERAL); // 코스탭에서는 항상 GENERAL
                replaceFragment(new CourseFragment(), false);
                return true;
            } else if (itemId == R.id.navigation_maps) {
                // 상태는 기존 유지(코스선택/산책모드 등)
                replaceFragment(new MapsFragment(), false);
                return true;
            } else if (itemId == R.id.navigation_surrounding) {
                viewModel.setMapState(MainViewModel.MapState.GENERAL); // 주변탭에서도 항상 GENERAL
                replaceFragment(new SurroundingFragment(), false);
                return true;
            }

            return false;
        });

        // 테스트 로그인 (JWT 토큰 자동 저장)
        //authRepository.login("email@example.com", "password", new AuthRepository.RepositoryCallback<AuthResponse>() {
        //    @Override
        //    public void onSuccess(AuthResponse response) {
        //        // 로그인 성공, JWT 토큰이 자동으로 저장됨
        //        // 이후 모든 API 호출에 자동으로 Authorization 헤더 추가
        //    }
        //
        //    @Override
        //    public void onError(String errorMessage) {
        //        // 에러 처리
        //    }
        //});

        // 초기 화면: 지도탭
        bottomNavigationView.setSelectedItemId(R.id.navigation_maps);
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
            long now = System.currentTimeMillis();
            if (now - lastBackPressedTime < BACK_PRESS_INTERVAL) {
                super.onBackPressed();  // 앱 종료
            } else {
                lastBackPressedTime = now;
                Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onBackPressed();  // 디테일 등 → 이전 화면 복귀
        }
    }

    private List<Route> loadRoutesFromJson() {
        try {
            InputStream is = getAssets().open("routes_data.json");
            String json = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));
            Type type = new TypeToken<Map<String, List<Route>>>() {}.getType();
            Map<String, List<Route>> parsed = new Gson().fromJson(json, type);

            List<Route> allRoutes = new ArrayList<>();
            if (parsed.containsKey("walk")) allRoutes.addAll(parsed.get("walk"));
            if (parsed.containsKey("bike")) allRoutes.addAll(parsed.get("bike"));
            return allRoutes;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private Map<Integer, POI> loadPoiFromJson() {
        Map<Integer, POI> result = new HashMap<>();
        try {
            InputStream is = getAssets().open("poi_data.json");
            String json = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));
            JSONObject root = new JSONObject(json);
            String[] categories = {"Toilet", "Cafe", "Restaurant"};

            for (String category : categories) {
                JSONArray array = root.getJSONArray(category);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    POI poi = new Gson().fromJson(item.toString(), POI.class);
                    poi.type = category;
                    result.put(poi.id, poi);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
