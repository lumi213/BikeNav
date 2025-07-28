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
import com.lumi.android.bicyclemap.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SurroundingFragment extends Fragment {

    private MainViewModel viewModel;
    private POIAdapter adapter;
    private RecyclerView recyclerView;
    private String currentCategory = null; // 전체 보기 상태
    private MaterialButtonToggleGroup toggleGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surrounding, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        recyclerView = view.findViewById(R.id.poi_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new POIAdapter();
        recyclerView.setAdapter(adapter);

        toggleGroup = view.findViewById(R.id.category_toggle_group);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                currentCategory = null; // 전체 보기
                filterPOIList();
                return;
            }

            if (checkedId == R.id.btn_filter_restaurant) {
                currentCategory = "Restaurant";
            } else if (checkedId == R.id.btn_filter_cafe) {
                currentCategory = "Cafe";
            } else if (checkedId == R.id.btn_filter_toilet) {
                currentCategory = "Toilet";
            }
            filterPOIList();
        });

        // 선택된 경로가 바뀌면 다시 필터 적용
        viewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                filterPOIList();
            }
        });

        return view;
    }

    private void filterPOIList() {
        Route route = viewModel.getSelectedRoute().getValue();
        Map<Integer, POI> poiMap = viewModel.getPoiMap().getValue();
        if (route == null || poiMap == null) return;

        List<POI> result = new ArrayList<>();
        for (int id : route.getPoi()) {
            POI poi = poiMap.get(id);
            if (poi != null && (currentCategory == null || poi.category.equals(currentCategory))) {
                result.add(poi);
            }
        }
        adapter.submitList(result);
    }
}
