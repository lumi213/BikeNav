package com.lumi.android.bicyclemap.ui.surrounding;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.dto.PoiListResponse;
import com.lumi.android.bicyclemap.repository.PoiRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurroundingViewModel extends ViewModel {

    // 기존: 선택된 POI ID 관리용
    private final MutableLiveData<List<String>> selectedPoiIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, PoiDto>> poiMap = new MutableLiveData<>(new HashMap<>());
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
            poiRepository.getPoiDetail(courseId, placeId, new PoiRepository.RepositoryCallback<PoiDto>() {
                @Override
                public void onSuccess(PoiDto poi) {
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

    // 추가: POI 리스트를 직접 관리하는 LiveData
    private final MutableLiveData<List<PoiDto>> poiList = new MutableLiveData<>(new ArrayList<>());

    // 선택된 POI ID 목록
    public void setSelectedPoiIds(List<String> ids) {
        selectedPoiIds.setValue(ids);
        // id가 갱신되면, poiList는 비워서 아래 getPoiList()가 id 기반으로 동작하게 한다.
        poiList.setValue(new ArrayList<>()); // 혹은 null로 해도 됨
    }

    // 전체 POI Map 설정
    public void setPoiMap(Map<String, PoiDto> map) {
        poiMap.setValue(map);
    }

    // 직접 POI 리스트를 세팅하고 싶을 때 (ex: 1km 필터 POI)
    public void setPoiList(List<PoiDto> list) {
        poiList.setValue(list);
        // poiList가 세팅되면 selectedPoiIds를 비워서 id기반 표시 안하게 한다.
        selectedPoiIds.setValue(new ArrayList<>()); // 혹은 null로 해도 됨
    }

    // POI 리스트를 반환:
    // - poiList가 비어 있지 않으면 이 값을 반환
    // - 비어 있으면 기존 id기반 변환값 반환
    public LiveData<List<PoiDto>> getPoiList() {
        return Transformations.map(poiList, list -> {
            if (list != null && !list.isEmpty()) {
                return list;
            } else {
                Map<String, PoiDto> map = poiMap.getValue();
                List<String> ids = selectedPoiIds.getValue();
                List<PoiDto> result = new ArrayList<>();
                if (map == null || ids == null || ids.isEmpty()) return result;
                for (String id : ids) {
                    PoiDto poi = map.get(id);
                    if (poi != null) result.add(poi);
                }
                return result;
            }
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
