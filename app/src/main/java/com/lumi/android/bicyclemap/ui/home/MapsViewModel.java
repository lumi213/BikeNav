package com.lumi.android.bicyclemap.ui.home;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.api.dto.CourseListResponse;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.repository.CourseRepository;

import java.util.List;

public class MapsViewModel extends ViewModel {

    private final MutableLiveData<List<CourseDto>> courses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private CourseRepository courseRepository;

    public MapsViewModel() {
        // 기본 생성자
    }

    // Context를 받는 초기화 메서드
    public void init(Context context) {
        if (courseRepository == null) {
            courseRepository = new CourseRepository(context);
        }
    }

    // 코스 목록 로드 (지도에 표시할 코스들)
    public void loadCoursesForMap(String type, String diff, Boolean isRecommended) {
        if (courseRepository != null) {
            isLoading.setValue(true);
            courseRepository.getCourses(type, diff, isRecommended, new CourseRepository.RepositoryCallback<CourseListResponse>() {
                @Override
                public void onSuccess(CourseListResponse response) {
                    isLoading.setValue(false);
                    if (response.getData() != null) {
                        courses.setValue(response.getData());
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    isLoading.setValue(false);
                    MapsViewModel.this.errorMessage.setValue(errorMessage);
                }
            });
        }
    }

    // 특정 코스 상세 정보 로드
    public void loadCourseDetail(int courseId) {
        if (courseRepository != null) {
            isLoading.setValue(true);
            courseRepository.getCourseDetail(courseId, new CourseRepository.RepositoryCallback<com.lumi.android.bicyclemap.api.dto.CourseDto>() {
                @Override
                public void onSuccess(com.lumi.android.bicyclemap.api.dto.CourseDto course) {
                    isLoading.setValue(false);
                    // 지도에 코스 경로 표시 로직
                }

                @Override
                public void onError(String errorMessage) {
                    isLoading.setValue(false);
                    MapsViewModel.this.errorMessage.setValue(errorMessage);
                }
            });
        }
    }

    // Getter 메서드들
    public LiveData<List<CourseDto>> getCourses() {
        return courses;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}