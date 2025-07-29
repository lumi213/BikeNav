package com.lumi.android.bicyclemap.ui.surrounding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.POI;
import com.lumi.android.bicyclemap.POIAdapter;
import com.lumi.android.bicyclemap.R;

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
        ALL, RESTAURANT, CAFE, TOILET
    }

    private FilterCategory currentFilter = FilterCategory.ALL;
    private List<POI> latestPoiList = new ArrayList<>();

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

        // ★ POIAdapter 사용 (item_poi.xml)
        adapter = new POIAdapter();
        recyclerView.setAdapter(adapter);

        // 선택된 Route의 POI 정보 ViewModel에 전달
        mainViewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                List<String> poiIdStrings = new ArrayList<>();
                if (route.getPoi() != null) {
                    for (Integer id : route.getPoi()) {
                        poiIdStrings.add(String.valueOf(id));
                    }
                }
                Map<Integer, POI> intMap = mainViewModel.getPoiMap().getValue();
                if (intMap != null) {
                    Map<String, POI> stringMap = new HashMap<>();
                    for (Map.Entry<Integer, POI> entry : intMap.entrySet()) {
                        stringMap.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    viewModel.setPoiMap(stringMap);
                }
                viewModel.setSelectedPoiIds(poiIdStrings);
            }
        });

        viewModel.getPoiList().observe(getViewLifecycleOwner(), poiList -> {
            if (poiList != null) {
                latestPoiList = poiList;
                filterPOIList();
            }
        });

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.category_toggle_group);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_filter_restaurant) {
                    currentFilter = FilterCategory.RESTAURANT;
                } else if (checkedId == R.id.btn_filter_cafe) {
                    currentFilter = FilterCategory.CAFE;
                } else if (checkedId == R.id.btn_filter_toilet) {
                    currentFilter = FilterCategory.TOILET;
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

    private void filterPOIList() {
        List<POI> filteredList = new ArrayList<>();
        switch (currentFilter) {
            case RESTAURANT:
                for (POI poi : latestPoiList)
                    if ("Restaurant".equalsIgnoreCase(poi.getCategory()) || "음식점".equals(poi.getCategory()))
                        filteredList.add(poi);
                break;
            case CAFE:
                for (POI poi : latestPoiList)
                    if ("Cafe".equalsIgnoreCase(poi.getCategory()) || "카페".equals(poi.getCategory()))
                        filteredList.add(poi);
                break;
            case TOILET:
                for (POI poi : latestPoiList)
                    if ("Toilet".equalsIgnoreCase(poi.getCategory()) || "화장실".equals(poi.getCategory()))
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
