package com.lumi.android.bicyclemap.ui.surrounding;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.PoiDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurroundingFragment extends Fragment {

    private SurroundingViewModel viewModel;
    private MainViewModel mainViewModel;
    private RecyclerView recyclerView;
    private POIAdapter adapter;

    private enum FilterCategory {
        ALL, BIZ, UTIL, TOURIST
    }

    private FilterCategory currentFilter = FilterCategory.ALL;
    private List<PoiDto> latestPoiList = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1201;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_surrounding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SurroundingViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        recyclerView = view.findViewById(R.id.poi_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new POIAdapter();
        recyclerView.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        mainViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            Log.d("Surrounding", " route: " + route);
            if (route != null) {
                Log.d("Surrounding","check getSelectedRoute()");
                // 기존 로직: 경로가 선택된 경우 POI 매핑
                List<String> poiIdStrings = new ArrayList<>();
                if (route.getPoi() != null) {
                    for (Integer id : route.getPoi()) {
                        poiIdStrings.add(String.valueOf(id));
                    }
                }
                Map<Integer, PoiDto> intMap = mainViewModel.getPoiMap().getValue();
                if (intMap != null) {
                    Map<String, PoiDto> stringMap = new HashMap<>();
                    for (Map.Entry<Integer, PoiDto> entry : intMap.entrySet()) {
                        stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    viewModel.setPoiMap(stringMap);
                }
                Log.d("Surrounding","check setSelectedPoiIds()");
                viewModel.setSelectedPoiIds(poiIdStrings);

            } else {
                // 경로가 선택되지 않은 경우 → 현 위치 기준 1km POI만 ViewModel에 넣기
                Log.d("Surrounding","check getCurrentLocationAndShowNearbyPOI()");
                getCurrentLocationAndShowNearbyPOI();
            }
        });

        // 중요: 옵저버 등록만으로는 "현재 값"이 null이어도 emit이 안 될 수 있으므로 직접 한 번 체크
        if (mainViewModel.getSelectedRoute().getValue() == null) {
            Log.d("Surrounding","initial selectedRoute is NULL -> getCurrentLocationAndShowNearbyPOI()");
            getCurrentLocationAndShowNearbyPOI();
        } else {
            Log.d("Surrounding","initial selectedRoute is NOT null");
        }

        viewModel.getPoiList().observe(getViewLifecycleOwner(), poiList -> {
            if (poiList != null) {
                latestPoiList = poiList;
                Log.d("Surrounding","check filterPOIList()");
                filterPOIList();
            }
        });

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.category_toggle_group);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_filter_biz) {
                    currentFilter = FilterCategory.BIZ;
                } else if (checkedId == R.id.btn_filter_util) {
                    currentFilter = FilterCategory.UTIL;
                } else if (checkedId == R.id.btn_filter_tourist) {
                    currentFilter = FilterCategory.TOURIST;
                } else {
                    currentFilter = FilterCategory.ALL;
                }
                filterPOIList();
            } else {
                if (group.getCheckedButtonId() == View.NO_ID) {
                    currentFilter = FilterCategory.ALL;
                    filterPOIList();
                }
            }
        });
    }

    /** 현 위치 기준 1km 이내 POI만 ViewModel로 전달 */
    private void getCurrentLocationAndShowNearbyPOI() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double userLat = location.getLatitude();
                    double userLng = location.getLongitude();

                    // 전체 POI 리스트 (POI Map이 Integer, POI로 되어 있다고 가정)
                    Map<Integer, PoiDto> poiMap = mainViewModel.getPoiMap().getValue();
                    List<PoiDto> allPoiList = new ArrayList<>();
                    if (poiMap != null) {
                        allPoiList.addAll(poiMap.values());
                    }

                    List<PoiDto> nearbyPoiList = new ArrayList<>();
                    for (PoiDto poi : allPoiList) {
                        double dist = distance(userLat, userLng, poi.getPoint().getLat(), poi.getPoint().getLng());
                        if (dist <= 1.0) { // 1km 이내
                            nearbyPoiList.add(poi);
                        }
                    }
                    // ViewModel에 세팅 (filterPOIList() 통해 표시됨)
                    viewModel.setPoiList(nearbyPoiList);
                }
            });

        } else {
            // 권한 요청 (처음 한 번만)
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            // 또는: "위치 권한이 필요합니다" 안내
        }
    }

    // Haversine 거리 계산 (반환: km)
    private double distance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Log.d("Surrounding", "POI 거리(km): " + (earthRadius * c));
        return earthRadius * c;
    }

    private void filterPOIList() {
        List<PoiDto> filteredList = new ArrayList<>();
        switch (currentFilter) {
            case BIZ:
                for (PoiDto poi : latestPoiList)
                    if ("biz".equalsIgnoreCase(poi.getType()) || "상권".equals(poi.getType()))
                        filteredList.add(poi);
                break;
            case UTIL:
                for (PoiDto poi : latestPoiList)
                    if ("util".equalsIgnoreCase(poi.getType()) || "편의 시설".equals(poi.getType()))
                        filteredList.add(poi);
                break;
            case TOURIST:
                for (PoiDto poi : latestPoiList)
                    if ("tourist".equalsIgnoreCase(poi.getType()) || "관광지".equals(poi.getType()))
                        filteredList.add(poi);
                break;
            case ALL:
            default:
                filteredList = latestPoiList;
                break;
        }
        adapter.submitList(filteredList);
    }
}
