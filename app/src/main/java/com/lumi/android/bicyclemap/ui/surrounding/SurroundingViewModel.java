package com.lumi.android.bicyclemap.ui.surrounding;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.POI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurroundingViewModel extends ViewModel {

    private final MutableLiveData<List<String>> selectedPoiIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, POI>> poiMap = new MutableLiveData<>(new HashMap<>());

    // 선택된 POI ID 목록
    public void setSelectedPoiIds(List<String> ids) {
        selectedPoiIds.setValue(ids);
    }

    // 전체 POI Map 설정
    public void setPoiMap(Map<String, POI> map) {
        poiMap.setValue(map);
    }

    // 선택된 POI ID에 해당하는 POI 객체들만 추출
    public LiveData<List<POI>> getPoiList() {
        return Transformations.map(selectedPoiIds, ids -> {
            Map<String, POI> map = poiMap.getValue();
            List<POI> result = new ArrayList<>();
            if (map == null || ids == null) return result;
            for (String id : ids) {
                POI poi = map.get(id);
                if (poi != null) result.add(poi);
            }
            Log.d("Surrounding", "POI 리스트 크기: " + (result != null ? result.size() : -1));
            return result;
        });
    }
}
