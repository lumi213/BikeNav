package com.lumi.android.bicyclemap.ui.surrounding;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.POI;
import com.lumi.android.bicyclemap.api.dto.PoiListResponse;
import com.lumi.android.bicyclemap.repository.PoiRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurroundingViewModel extends ViewModel {

    private final MutableLiveData<List<String>> selectedPoiIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, POI>> poiMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private PoiRepository poiRepository;

    public SurroundingViewModel() {
        // 기본 생성자
    }

    // Context를 받는 초기화 메서드
    public void init(Context context) {
        if (poiRepository == null) {
            poiRepository = new PoiRepository(context);
        }
    }

    // POI 목록 로드
    public void loadPois(int courseId, String category) {
        if (poiRepository != null) {
            isLoading.setValue(true);
            poiRepository.getPois(courseId, category, new PoiRepository.RepositoryCallback<PoiListResponse>() {
                @Override
                public void onSuccess(PoiListResponse response) {
                    isLoading.setValue(false);
                    if (response.getData() != null) {
                        // PoiDto를 POI로 변환하는 로직 필요
                        // poiMap.setValue(convertedPois);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    isLoading.setValue(false);
                    SurroundingViewModel.this.errorMessage.setValue(errorMessage);
                }
            });
        }
    }

    // POI 상세 정보 로드
    public void loadPoiDetail(int courseId, int placeId) {
        if (poiRepository != null) {
            isLoading.setValue(true);
            poiRepository.getPoiDetail(courseId, placeId, new PoiRepository.RepositoryCallback<com.lumi.android.bicyclemap.api.dto.PoiDto>() {
                @Override
                public void onSuccess(com.lumi.android.bicyclemap.api.dto.PoiDto poi) {
                    isLoading.setValue(false);
                    // 상세 정보 처리
                }

                @Override
                public void onError(String errorMessage) {
                    isLoading.setValue(false);
                    SurroundingViewModel.this.errorMessage.setValue(errorMessage);
                }
            });
        }
    }

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

    // Getter 메서드들
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
