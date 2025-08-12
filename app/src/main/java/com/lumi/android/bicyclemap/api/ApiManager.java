package com.lumi.android.bicyclemap.api;

import android.content.Context;

import androidx.annotation.Nullable;

import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.AuthRequest;
import com.lumi.android.bicyclemap.api.dto.AuthResponse;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.CourseListResponse;
import com.lumi.android.bicyclemap.api.dto.LocationRequest;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.dto.PoiListResponse;
import com.lumi.android.bicyclemap.api.dto.ReviewRequest;
import com.lumi.android.bicyclemap.api.dto.TrackingRequest;
import com.lumi.android.bicyclemap.api.dto.TrackingResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiManager {
    private static ApiManager instance;
    private ApiService apiService;
    private JwtTokenManager tokenManager;
    private Context context;

    private ApiManager(Context context) {
        this.context = context;
        apiService = ApiClient.getInstance(context).getApiService();
        tokenManager = JwtTokenManager.getInstance(context);
    }

    public static synchronized ApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApiManager(context);
        }
        return instance;
    }

    // 1. 인증 API
    public void register(String name, String password, String email, ApiCallback<ApiResponse> callback) {
        AuthRequest request = new AuthRequest(name, password, email);
        apiService.register(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("회원가입 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void login(String email, String password, ApiCallback<AuthResponse> callback) {
        AuthRequest request = new AuthRequest(null, password, email);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.isSuccess() && authResponse.getData() != null) {
                        // JWT 토큰 저장
                        tokenManager.saveToken(
                            authResponse.getData().getToken(),
                            authResponse.getData().getUserId(),
                            authResponse.getData().getName()
                        );
                    }
                    callback.onSuccess(authResponse);
                } else {
                    callback.onError("로그인 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 로그아웃
    public void logout() {
        tokenManager.clearToken();
    }

    // 로그인 상태 확인
    public boolean isLoggedIn() {
        return tokenManager.isLoggedIn();
    }

    // 현재 사용자 ID 가져오기
    public int getCurrentUserId() {
        return tokenManager.getUserId();
    }

    // 2. 코스 API
    public void getCourseList(String type, String diff, Boolean isRecommended, ApiCallback<CourseListResponse> callback) {
        apiService.getCourseList(type, diff, isRecommended).enqueue(new Callback<CourseListResponse>() {
            @Override
            public void onResponse(Call<CourseListResponse> call, Response<CourseListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("코스 목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CourseListResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void getCourseDetail(int courseId, ApiCallback<ApiResponse<CourseDto>> callback) {
        apiService.getCourseDetail(courseId).enqueue(new Callback<ApiResponse<CourseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<CourseDto>> call, Response<ApiResponse<CourseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("코스 상세 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CourseDto>> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 3. POI API
    public void getPois(int courseId, String category, ApiCallback<PoiListResponse> callback) {
        apiService.getPois(courseId, category).enqueue(new Callback<PoiListResponse>() {
            @Override
            public void onResponse(Call<PoiListResponse> call, Response<PoiListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("POI 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PoiListResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void getPoiDetail(int courseId, int placeId, ApiCallback<ApiResponse<PoiDto>> callback) {
        apiService.getPoiDetail(courseId, placeId).enqueue(new Callback<ApiResponse<PoiDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<PoiDto>> call, Response<ApiResponse<PoiDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("POI 상세 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PoiDto>> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 4. 위치 API
    public void sendLocation(int userId, double lat, double lng, ApiCallback<ApiResponse> callback) {
        LocationRequest request = new LocationRequest(userId, lat, lng);
        apiService.sendLocation(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("위치 전송 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 5. 트래킹 API
    public void startTracking(int userId, int courseId, String type, ApiCallback<TrackingResponse> callback) {
        TrackingRequest request = new TrackingRequest(userId, courseId, type);
        apiService.startTracking(request).enqueue(new Callback<TrackingResponse>() {
            @Override
            public void onResponse(Call<TrackingResponse> call, Response<TrackingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("트래킹 시작 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TrackingResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void endTracking(int trackingId, ApiCallback<ApiResponse> callback) {
        TrackingRequest request = new TrackingRequest(trackingId);
        apiService.endTracking(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("트래킹 종료 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 6. 후기 API
    public void addCourseReview(int userId, int courseId, int trackingId, int rating, String content, String imgUrl, String thumbnailUrl, ApiCallback<ApiResponse> callback) {
        ReviewRequest request = ReviewRequest.forCourse(userId, courseId, trackingId, rating, content, imgUrl, thumbnailUrl);
        apiService.addCourseReview(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("코스 후기 등록 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    public void addPoiReview(int courseId, int placeId, int userId, int rating, int diff, String content, String imgUrl, String thumbnailUrl, ApiCallback<ApiResponse> callback) {
        ReviewRequest request = ReviewRequest.forPoi(userId, placeId, rating, diff, content, imgUrl, thumbnailUrl);
        apiService.addPoiReview(courseId, placeId, request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("POI 후기 등록 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 콜백 인터페이스
    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }

    private ApiService getApiService() {
        return apiService;
    }
    // 코스 주변 POI 목록 조회
    public void fetchCoursePois(int courseId, @Nullable String category,
                                retrofit2.Callback<PoiListResponse> callback) {
        ApiService s = getApiService(); // 기존에 있던 getter 사용
        s.getPois(courseId, category).enqueue(callback);
    }

    // POI 상세 조회
    public void fetchPoiDetail(int courseId, int placeId,
                               retrofit2.Callback<ApiResponse<PoiDto>> callback) {
        ApiService s = getApiService();
        s.getPoiDetail(courseId, placeId).enqueue(callback);
    }

} 