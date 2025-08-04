package com.lumi.android.bicyclemap.repository;

import android.content.Context;

import com.lumi.android.bicyclemap.api.ApiManager;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.CourseListResponse;

import java.util.List;

public class CourseRepository {
    private ApiManager apiManager;
    private List<CourseDto> cachedCourses;
    private Context context;

    public CourseRepository(Context context) {
        this.context = context;
        apiManager = ApiManager.getInstance(context);
    }

    public void getCourses(String type, String diff, Boolean isRecommended, RepositoryCallback<CourseListResponse> callback) {
        // 1. 캐시된 데이터가 있으면 먼저 반환
        if (cachedCourses != null && !cachedCourses.isEmpty()) {
            CourseListResponse cachedResponse = new CourseListResponse();
            cachedResponse.setData(cachedCourses);
            cachedResponse.setSuccess(true);
            cachedResponse.setMessage("캐시된 데이터");
            callback.onSuccess(cachedResponse);
        }

        // 2. API 호출
        apiManager.getCourseList(type, diff, isRecommended, new ApiManager.ApiCallback<CourseListResponse>() {
            @Override
            public void onSuccess(CourseListResponse response) {
                // 캐시 업데이트
                if (response.getData() != null) {
                    cachedCourses = response.getData();
                }
                callback.onSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getCourseDetail(int courseId, RepositoryCallback<CourseDto> callback) {
        apiManager.getCourseDetail(courseId, new ApiManager.ApiCallback<com.lumi.android.bicyclemap.api.dto.ApiResponse<CourseDto>>() {
            @Override
            public void onSuccess(com.lumi.android.bicyclemap.api.dto.ApiResponse<CourseDto> response) {
                if (response.getData() != null) {
                    callback.onSuccess(response.getData());
                } else {
                    callback.onError("데이터가 없습니다");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // 간단한 콜백 인터페이스
    public interface RepositoryCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }
} 