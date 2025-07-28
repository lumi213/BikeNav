package com.lumi.android.bicyclemap;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // 1. JSON 로드 후 ViewModel에 저장
        List<Route> routes = loadRoutesFromJson();
        Map<Integer, POI> poiMap = loadPoiFromJson();

        viewModel.setAllRoutes(routes);
        viewModel.setPoiMap(poiMap);

        // 2. 네비게이션 설정
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(navView, navController);
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
                    poi.category = category;
                    result.put(poi.id, poi);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
