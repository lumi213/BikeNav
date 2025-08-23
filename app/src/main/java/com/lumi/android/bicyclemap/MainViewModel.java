package com.lumi.android.bicyclemap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.dto.VillagesDto;
import com.lumi.android.bicyclemap.data.local.entity.CompletedCourseEntity;
import com.lumi.android.bicyclemap.data.local.repository.CompletedCourseRepository;
import com.lumi.android.bicyclemap.gpt.GptRouteRecommendation;
import com.lumi.android.bicyclemap.repository.AiRepository;
import com.lumi.android.bicyclemap.repository.CourseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainViewModel extends ViewModel {

    // === 앱 전역 상태 ===
    public enum MapState { GENERAL, WALKING }

    // 지도 상태
    private final MutableLiveData<MapState> mapState = new MutableLiveData<>(MapState.GENERAL);
    public LiveData<MapState> getMapState() { return mapState; }
    public void setMapState(MapState state) { mapState.setValue(state); }

    // 전체 코스/POI
    private final MutableLiveData<List<CourseDto>> allRoutes = new MutableLiveData<>();
    public LiveData<List<CourseDto>> getAllRoutes() { return allRoutes; }
    public void setAllRoutes(List<CourseDto> routes) {
        allRoutes.setValue(routes);
        // 코스가 방금 로드됐고 GPT 호출이 보류 중이면 재개
        if (pendingGpt) {
            Log.d("AI_RECO", "allRoutes loaded; firing pending GPT");
            requestGptIfReady();
        }
    }

    private final MutableLiveData<Map<Integer, PoiDto>> poiMap = new MutableLiveData<>();
    public LiveData<Map<Integer, PoiDto>> getPoiMap() { return poiMap; }
    public void setPoiMap(Map<Integer, PoiDto> map) { this.poiMap.setValue(map); }

    // Villages
    private final MutableLiveData<List<VillagesDto>> villages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> villagesLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> villagesError = new MutableLiveData<>(null);
    public LiveData<List<VillagesDto>> getVillages() { return villages; }
    public LiveData<Boolean> getVillagesLoading() { return villagesLoading; }
    public LiveData<String> getVillagesError() { return villagesError; }
    public void setVillages(List<VillagesDto> list) { villages.setValue(list); }
    public void setVillagesLoading(boolean loading) { villagesLoading.setValue(loading); }
    public void setVillagesError(String err) { villagesError.setValue(err); }

    // 선택된 코스
    private final MutableLiveData<CourseDto> selectedRoute = new MutableLiveData<>();
    public LiveData<CourseDto> getSelectedRoute() { return selectedRoute; }
    public void setSelectedRoute(CourseDto route) {
        selectedRoute.setValue(route);
        Log.d("Surrounding","set route: " + route);
    }

    // 공용 에러
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // ★ 스낵바 메시지 (추천 이유 등)
    private final MutableLiveData<String> recommendationToast = new MutableLiveData<>();
    public LiveData<String> getRecommendationToast() { return recommendationToast; }
    public void clearRecommendationToast() { recommendationToast.setValue(null); }

    // Repository
    private CourseRepository courseRepository;
    private AiRepository aiRepository;

    public void initRepository(Context context) {
        if (courseRepository == null) courseRepository = new CourseRepository(context.getApplicationContext());
        if (aiRepository == null)     aiRepository     = new AiRepository(context.getApplicationContext());
    }

    // 지도 정리 이벤트
    private final MutableLiveData<Long> clearMapEvent = new MutableLiveData<>();
    public LiveData<Long> getClearMapEvent() { return clearMapEvent; }
    private void emitClearMapEvent() { clearMapEvent.setValue(System.nanoTime()); }

    // 상태 초기화
    public void resetMapState() {
        mapState.setValue(MapState.GENERAL);
        selectedRoute.setValue(null);
        emitClearMapEvent();
    }

    // 완료 코스 저장/조회
    private CompletedCourseRepository completedRepo;
    public void init(Context c) {
        if (completedRepo == null) {
            completedRepo = new CompletedCourseRepository(c.getApplicationContext());
        }
    }
    public void markCourseCompleted(int courseId, String title, Runnable onDone) {
        if (completedRepo != null) completedRepo.markCompleted(courseId, title, onDone);
    }
    public void fetchCompleted(CompletedCourseRepository.Callback<List<CompletedCourseEntity>> cb) {
        if (completedRepo != null) completedRepo.getAll(cb);
    }
    public void isCourseCompleted(int courseId, CompletedCourseRepository.Callback<Boolean> cb) {
        if (completedRepo != null) completedRepo.isCompleted(courseId, cb);
    }

    // === 설문/추천 상태 ===
    public enum RouteType { WALK, BIKE }
    public enum GroupType { SMALL, LARGE }
    public enum Difficulty { EASY, NORMAL, HARD }

    private final MutableLiveData<Integer>  surveyStep   = new MutableLiveData<>(1);
    private final MutableLiveData<RouteType> selectedRouteType  = new MutableLiveData<>();
    private final MutableLiveData<GroupType> selectedGroupType  = new MutableLiveData<>();
    private final MutableLiveData<Difficulty> selectedDifficulty = new MutableLiveData<>();
    public LiveData<Integer> getSurveyStep() { return surveyStep; }

    // 추천 결과
    private final MutableLiveData<List<CourseDto>> recommendedCourses = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<CourseDto>> getRecommendedCourses() { return recommendedCourses; }

    // 설문 로딩 (오버레이)
    private final MutableLiveData<Boolean> surveyLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getSurveyLoading() { return surveyLoading; }

    // GPT 대기/재개용
    private boolean   pendingGpt = false;
    private RouteType pendingRt;
    private GroupType pendingGt;
    private Difficulty pendingDf;

    // 설문 액션
    public void chooseRouteType(RouteType t){ selectedRouteType.setValue(t); surveyStep.setValue(2); }
    public void chooseGroupType(GroupType g){ selectedGroupType.setValue(g); surveyStep.setValue(3); }

    /**
     * 마지막 선택(난이도) → 코스가 없으면 보류, 들어오면 즉시 GPT 호출
     */
    public void chooseDifficulty(Difficulty d){
        Log.d("AI_RECO", "chooseDifficulty() called with d=" + d
                + ", rt=" + selectedRouteType.getValue()
                + ", gt=" + selectedGroupType.getValue());

        selectedDifficulty.setValue(d);
        surveyLoading.setValue(true);

        // 현재 선택 보관 (보류 시 필요)
        pendingRt = (selectedRouteType.getValue() != null) ? selectedRouteType.getValue() : RouteType.BIKE;
        pendingGt = (selectedGroupType.getValue() != null) ? selectedGroupType.getValue() : GroupType.SMALL;
        pendingDf = (d != null) ? d : Difficulty.NORMAL;

        requestGptIfReady();

        // (옵션) 타임아웃으로 무한로딩 방지
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (pendingGpt) {
                pendingGpt = false;
                surveyLoading.setValue(false);
                errorMessage.setValue("코스 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
                Log.w("AI_RECO", "timeout waiting allRoutes; cancel pending GPT");
            }
        }, 15_000);
    }

    /** 코스/선택값 모두 준비되면 GPT 호출, 아니면 보류 */
    private void requestGptIfReady() {
        List<CourseDto> courses = allRoutes.getValue();
        if (courses != null && !courses.isEmpty()
                && pendingRt != null && pendingGt != null && pendingDf != null) {
            pendingGpt = false;
            doGptRecommend(courses, pendingRt, pendingGt, pendingDf);
        } else {
            pendingGpt = true;
            Log.d("AI_RECO", "allRoutes not ready → GPT pending");
        }
    }

    /** 실제 GPT 호출 */
    private void doGptRecommend(List<CourseDto> courses, RouteType rt, GroupType gt, Difficulty df) {
        if (aiRepository == null) {
            surveyLoading.postValue(false);
            errorMessage.postValue("AI 구성이 초기화되지 않았습니다.");
            return;
        }
        Log.d("AI_RECO", "recommendWithGpt() fire: courses=" + courses.size()
                + ", rt=" + rt + ", gt=" + gt + ", df=" + df);

        aiRepository.recommendWithGpt(
                courses, rt, gt, df,
                new AiRepository.CallbackResult() {
                    @Override
                    public void onSuccess(GptRouteRecommendation result) {
                        List<CourseDto> all = allRoutes.getValue();
                        List<CourseDto> out = new ArrayList<>();
                        Integer firstId = null;
                        String firstReason = null;

                        if (result != null && result.recommendations != null && all != null) {
                            for (GptRouteRecommendation.Item it : result.recommendations) {
                                if (firstId == null) {
                                    firstId = it.course_id;
                                    firstReason = it.reason;
                                }
                                for (CourseDto c : all) {
                                    if (c.getCourse_id() == it.course_id) { out.add(c); break; }
                                }
                            }
                        }

                        recommendedCourses.postValue(out);

                        // ✅ 첫 번째 추천을 자동 선택 + 스낵바 메시지 준비
                        if (!out.isEmpty()) {
                            CourseDto first = out.get(0);
                            setSelectedRoute(first);

                            String reasonTxt = (firstReason != null && !firstReason.trim().isEmpty())
                                    ? firstReason.trim()
                                    : "선택하신 조건에 가장 잘 맞는 코스예요.";
                            String snackMsg = "추천 코스 적용: ‘" + safe(first.getTitle()) + "’\n이유: " + reasonTxt;

                            recommendationToast.postValue(snackMsg);
                        }

                        surveyLoading.postValue(false);
                    }
                    @Override
                    public void onError(String error) {
                        Log.e("AI_RECO", "GPT recommend error: " + error);
                        surveyLoading.postValue(false);
                        errorMessage.postValue("추천을 불러오지 못했습니다: " + error);
                    }
                }
        );
    }

    /** 스킵/취소용: 로딩/보류 해제 */
    public void cancelSurveyAndPending() {
        pendingGpt = false;
        surveyLoading.setValue(false);
    }

    private String safe(String s) { return s == null ? "" : s; }
}
