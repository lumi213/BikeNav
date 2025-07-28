package com.lumi.android.bicyclemap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Map;

public class MainViewModel extends ViewModel {

    // 전체 경로 리스트
    private final MutableLiveData<List<Route>> allRoutes = new MutableLiveData<>();

    // 전체 POI Map (id → POI)
    private final MutableLiveData<Map<Integer, POI>> poiMap = new MutableLiveData<>();

    // 선택된 경로
    private final MutableLiveData<Route> selectedRoute = new MutableLiveData<>();

    // === getter/setter ===

    public LiveData<List<Route>> getAllRoutes() {
        return allRoutes;
    }

    public void setAllRoutes(List<Route> routes) {
        allRoutes.setValue(routes);
    }

    public LiveData<Map<Integer, POI>> getPoiMap() {
        return poiMap;
    }

    public void setPoiMap(Map<Integer, POI> poiMap) {
        this.poiMap.setValue(poiMap);
    }

    public LiveData<Route> getSelectedRoute() {
        return selectedRoute;
    }

    public void setSelectedRoute(Route route) {
        selectedRoute.setValue(route);
    }
}
