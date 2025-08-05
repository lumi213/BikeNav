package com.lumi.android.bicyclemap.ui.course;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lumi.android.bicyclemap.api.dto.CourseListResponse;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.repository.CourseRepository;

import java.util.List;

public class CourseViewModel extends ViewModel {

    private final MutableLiveData<List<CourseDto>> courses = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private CourseRepository courseRepository;

    public CourseViewModel() {
        // 기본 생성자
    }

    // Context를 받는 초기화 메서드
    public void init(Context context) {
        if (courseRepository == null) {
            courseRepository = new CourseRepository(context);
        }
    }

    // 코스 목록 로드
    public void loadCourses(String type, String diff, Boolean isRecommended) {
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
                    CourseViewModel.this.errorMessage.setValue(errorMessage);
                }
            });
        }
    }

    // 코스 상세 정보 로드
    public void loadCourseDetail(int courseId) {
        if (courseRepository != null) {
            isLoading.setValue(true);
            courseRepository.getCourseDetail(courseId, new CourseRepository.RepositoryCallback<com.lumi.android.bicyclemap.api.dto.CourseDto>() {
                @Override
                public void onSuccess(com.lumi.android.bicyclemap.api.dto.CourseDto course) {
                    isLoading.setValue(false);
                    // 상세 정보 처리
                }

                @Override
                public void onError(String errorMessage) {
                    isLoading.setValue(false);
                    CourseViewModel.this.errorMessage.setValue(errorMessage);
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