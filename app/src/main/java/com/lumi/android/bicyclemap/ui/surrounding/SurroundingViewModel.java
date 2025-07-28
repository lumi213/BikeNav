package com.lumi.android.bicyclemap.ui.surrounding;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.POI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SurroundingViewModel extends ViewModel {

    private final MainViewModel mainViewModel;
    private final LiveData<List<POI>> filteredPoiList;

    public SurroundingViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;

        filteredPoiList = Transformations.map(mainViewModel.getSelectedRoute(), route -> {
            Map<Integer, POI> poiMap = mainViewModel.getPoiMap().getValue();
            List<POI> result = new ArrayList<>();
            if (route != null && poiMap != null) {
                for (int id : route.getPoi()) {
                    POI poi = poiMap.get(id);
                    if (poi != null) result.add(poi);
                }
            }
            return result;
        });
    }

    public LiveData<List<POI>> getFilteredPoiList() {
        return filteredPoiList;
    }
}
