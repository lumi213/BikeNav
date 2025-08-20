package com.lumi.android.bicyclemap;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.CourseListResponse;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.dto.VillagesDto;
import com.lumi.android.bicyclemap.data.local.entity.CompletedCourseEntity;
import com.lumi.android.bicyclemap.repository.CourseRepository;

import java.util.List;
import java.util.Map;

public class MainViewModel extends ViewModel {

    // === 앱 전역 상태 ===
    public enum MapState {
        GENERAL,    // 코스 선택 모드
        WALKING     // 산책/진행 모드
    }

    // 전역 상태 관리 LiveData
    private final MutableLiveData<MapState> mapState = new MutableLiveData<>(MapState.GENERAL);
    public LiveData<MapState> getMapState() { return mapState; }
    public void setMapState(MapState state) { mapState.setValue(state); }

    // 전체 경로 리스트
    private final MutableLiveData<List<CourseDto>> allRoutes = new MutableLiveData<>();

    // 전체 POI Map (id → POI)
    private final MutableLiveData<Map<Integer, PoiDto>> poiMap = new MutableLiveData<>();

    // ✅ Villages 전체(상세 포함) LiveData
    private final MutableLiveData<List<VillagesDto>> villages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> villagesLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> villagesError = new MutableLiveData<>(null);

    // 선택된 경로
    private final MutableLiveData<CourseDto> selectedRoute = new MutableLiveData<>();

    // Repository
    private CourseRepository courseRepository;

    // 로딩 상태
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // 에러 메시지
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // === getter/setter ===

    public LiveData<List<CourseDto>> getAllRoutes() {
        return allRoutes;
    }

    public void setAllRoutes(List<CourseDto> routes) {
        allRoutes.setValue(routes);
    }

    public LiveData<Map<Integer, PoiDto>> getPoiMap() {
        return poiMap;
    }

    public void setPoiMap(Map<Integer, PoiDto> poiMap) {
        this.poiMap.setValue(poiMap);
    }

    public LiveData<List<VillagesDto>> getVillages() { return villages; }
    public LiveData<Boolean> getVillagesLoading() { return villagesLoading; }
    public LiveData<String> getVillagesError() { return villagesError; }

    public void setVillages(List<VillagesDto> list) { villages.setValue(list); }
    public void setVillagesLoading(boolean loading) { villagesLoading.setValue(loading); }
    public void setVillagesError(String err) { villagesError.setValue(err); }

    public LiveData<CourseDto> getSelectedRoute() {
        return selectedRoute;
    }

    public void setSelectedRoute(CourseDto route) {
        selectedRoute.setValue(route);
        Log.d("Surrounding","set route: " + route);
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Repository 초기화 및 API 데이터 로드
    public void initRepository(Context context) {
        if (courseRepository == null) {
            courseRepository = new CourseRepository(context);
        }
    }

    // API에서 코스 데이터 로드
    public void loadCoursesFromApi(String type, String diff, Boolean isRecommended) {
        if (courseRepository != null) {
            isLoading.setValue(true);
            courseRepository.getCourses(type, diff, isRecommended, new CourseRepository.RepositoryCallback<CourseListResponse>() {
                @Override
                public void onSuccess(CourseListResponse response) {
                    isLoading.setValue(false);
                    if (response.getData() != null) {
                        // CourseDto를 Route로 변환하는 로직 필요
                        // allRoutes.setValue(convertedRoutes);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    isLoading.setValue(false);
                    MainViewModel.this.errorMessage.setValue(errorMessage);
                }
            });
        }
    }

    // 지도 정리 이벤트 (프래그먼트가 observe해서 폴리라인/마커 지움)
    private final MutableLiveData<Long> clearMapEvent = new MutableLiveData<>();
    public LiveData<Long> getClearMapEvent() { return clearMapEvent; }

    private void emitClearMapEvent() {
        clearMapEvent.setValue(System.nanoTime());
    }

    // === 상태 초기화 ===
    public void resetMapState() {
        mapState.setValue(MapState.GENERAL);
        selectedRoute.setValue(null);
        emitClearMapEvent();
    }

    // 필드 추가
    private com.lumi.android.bicyclemap.data.repository.CompletedCourseRepository completedRepo;

    // 생성자 또는 init에서 Context 주입 시 초기화
    public void init(Context c) {
        if (completedRepo == null) {
            completedRepo = new com.lumi.android.bicyclemap.data.repository.CompletedCourseRepository(c.getApplicationContext());
        }
    }

    // 메소드 추가
    public void markCourseCompleted(int courseId, String title, Runnable onDone) {
        if (completedRepo != null) {
            completedRepo.markCompleted(courseId, title, onDone);
        }
    }
    public void fetchCompleted(com.lumi.android.bicyclemap.data.repository.CompletedCourseRepository.Callback<List<CompletedCourseEntity>> cb) {
        if (completedRepo != null) {
            completedRepo.getAll(cb);
        }
    }
    public void isCourseCompleted(int courseId, com.lumi.android.bicyclemap.data.repository.CompletedCourseRepository.Callback<Boolean> cb) {
        if (completedRepo != null) {
            completedRepo.isCompleted(courseId, cb);
        }
    }
}
