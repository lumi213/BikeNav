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
// ⬇️ GPT 추천을 위한 Repo 추가
import com.lumi.android.bicyclemap.repository.AiRepository;
import com.lumi.android.bicyclemap.gpt.GptRouteRecommendation;

import java.util.ArrayList;
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
    public LiveData<List<CourseDto>> getAllRoutes() { return allRoutes; }
    public void setAllRoutes(List<CourseDto> routes) { allRoutes.setValue(routes); }

    // 전체 POI Map (id → POI)
    private final MutableLiveData<Map<Integer, PoiDto>> poiMap = new MutableLiveData<>();
    public LiveData<Map<Integer, PoiDto>> getPoiMap() { return poiMap; }
    public void setPoiMap(Map<Integer, PoiDto> poiMap) { this.poiMap.setValue(poiMap); }

    // ✅ Villages 전체(상세 포함) LiveData
    private final MutableLiveData<List<VillagesDto>> villages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> villagesLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> villagesError = new MutableLiveData<>(null);
    public LiveData<List<VillagesDto>> getVillages() { return villages; }
    public LiveData<Boolean> getVillagesLoading() { return villagesLoading; }
    public LiveData<String> getVillagesError() { return villagesError; }
    public void setVillages(List<VillagesDto> list) { villages.setValue(list); }
    public void setVillagesLoading(boolean loading) { villagesLoading.setValue(loading); }
    public void setVillagesError(String err) { villagesError.setValue(err); }

    // 선택된 경로
    private final MutableLiveData<CourseDto> selectedRoute = new MutableLiveData<>();
    public LiveData<CourseDto> getSelectedRoute() { return selectedRoute; }
    public void setSelectedRoute(CourseDto route) {
        selectedRoute.setValue(route);
        Log.d("Surrounding","set route: " + route);
    }

    // 공용 로딩/에러
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // Repository
    private CourseRepository courseRepository;
    // ⬇️ GPT 추천용 Repo 필드 추가
    private AiRepository aiRepository;

    // Repository 초기화
    public void initRepository(Context context) {
        if (courseRepository == null) courseRepository = new CourseRepository(context.getApplicationContext());
        if (aiRepository == null)     aiRepository     = new AiRepository(context.getApplicationContext()); // ✅ 추가
    }

    // 지도 정리 이벤트 (프래그먼트가 observe해서 폴리라인/마커 지움)
    private final MutableLiveData<Long> clearMapEvent = new MutableLiveData<>();
    public LiveData<Long> getClearMapEvent() { return clearMapEvent; }
    private void emitClearMapEvent() { clearMapEvent.setValue(System.nanoTime()); }

    // === 상태 초기화 ===
    public void resetMapState() {
        mapState.setValue(MapState.GENERAL);
        selectedRoute.setValue(null);
        emitClearMapEvent();
    }

    // 완료 코스 저장/조회
    private com.lumi.android.bicyclemap.data.repository.CompletedCourseRepository completedRepo;
    public void init(Context c) {
        if (completedRepo == null) {
            completedRepo = new com.lumi.android.bicyclemap.data.repository.CompletedCourseRepository(c.getApplicationContext());
        }
    }

    // === 설문/추천 상태 ===
    public enum RouteType { WALK, BIKE }
    public enum GroupType { SMALL, LARGE }
    public enum Difficulty { EASY, NORMAL, HARD }

    // 설문 단계/선택값
    private final MutableLiveData<Integer> surveyStep = new MutableLiveData<>(1);
    private final MutableLiveData<RouteType> selectedRouteType = new MutableLiveData<>();
    private final MutableLiveData<GroupType> selectedGroupType = new MutableLiveData<>();
    private final MutableLiveData<Difficulty> selectedDifficulty = new MutableLiveData<>();
    public LiveData<Integer> getSurveyStep() { return surveyStep; }
    public LiveData<Boolean> getSurveyLoading() { return surveyLoading; }

    // GPT 추천 결과 목록 (UI가 관찰)
    private final MutableLiveData<List<CourseDto>> recommendedCourses = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<CourseDto>> getRecommendedCourses() { return recommendedCourses; }

    // 설문 중 로딩(오버레이에 표시)
    private final MutableLiveData<Boolean> surveyLoading = new MutableLiveData<>(false);

    // 설문 액션
    public void chooseRouteType(RouteType t){ selectedRouteType.setValue(t); surveyStep.setValue(2); }
    public void chooseGroupType(GroupType g){ selectedGroupType.setValue(g); surveyStep.setValue(3); }

    /**
     * 마지막 난이도 선택 시: GPT에 (전체코스 + 선택값) 전달 → 추천 수신 → 첫 코스 자동 선택
     */
    public void chooseDifficulty(Difficulty d){
        Log.d("AI_RECO", "chooseDifficulty() called with d=" + d
                + ", rt=" + selectedRouteType.getValue()
                + ", gt=" + selectedGroupType.getValue());

        selectedDifficulty.setValue(d);
        surveyLoading.setValue(true);

        List<CourseDto> courses = allRoutes.getValue();
        if (courses == null || courses.isEmpty()) {
            Log.w("AI_RECO", "allRoutes is empty → GPT 추천 건너뜀");
            surveyLoading.setValue(false);
            return;
        }

        // NPE 방지를 위한 기본값
        RouteType rt = selectedRouteType.getValue() != null ? selectedRouteType.getValue() : RouteType.BIKE;
        GroupType gt = selectedGroupType.getValue() != null ? selectedGroupType.getValue() : GroupType.SMALL;
        Difficulty df = d != null ? d : Difficulty.NORMAL;

        aiRepository.recommendWithGpt(
                courses,
                rt,
                gt,
                df,
                new AiRepository.CallbackResult() {
                    @Override
                    public void onSuccess(GptRouteRecommendation result) {
                        Log.d("AI_RECO", "onSuccess() with items="
                                + (result!=null && result.recommendations!=null ? result.recommendations.size() : 0));

                        List<CourseDto> all = allRoutes.getValue();
                        List<CourseDto> out = new ArrayList<>();

                        if (result != null && result.recommendations != null && all != null) {
                            for (GptRouteRecommendation.Item it : result.recommendations) {
                                for (CourseDto c : all) {
                                    if (c.getCourse_id() == it.course_id) {
                                        out.add(c);
                                        break;
                                    }
                                }
                            }
                        }
                        // 추천 목록 반영
                        recommendedCourses.postValue(out);

                        // ✅ 첫 번째 추천 코스를 자동 선택 (MapsFragment가 observe하여 표시)
                        if (!out.isEmpty()) {
                            setSelectedRoute(out.get(0));
                        }

                        surveyLoading.postValue(false);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("AI_RECO", "GPT recommend error: " + error);
                        surveyLoading.postValue(false);
                    }
                }
        );
    }
}
