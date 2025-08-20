package com.lumi.android.bicyclemap.api;

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
import com.lumi.android.bicyclemap.api.dto.VillagesDto;
import com.lumi.android.bicyclemap.api.dto.VillagesListResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    // 1. 인증 API
    @POST("api/auth/register")
    Call<ApiResponse> register(@Body AuthRequest request);
    
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);
    
    // 2. 코스 API
    @GET("api/course/list")
    Call<CourseListResponse> getCourseList(
        @Query("type") String type,
        @Query("diff") String diff,
        @Query("is_recommended") Boolean isRecommended
    );
    
    @GET("api/course/{courseId}")
    Call<ApiResponse<CourseDto>> getCourseDetail(@Path("courseId") int courseId);
    
    // 3-1. POI API
    @GET("api/course/{courseId}/pois")
    Call<PoiListResponse> getPois(
        @Path("courseId") int courseId,
        @Query("category") String category
    );
    
    @GET("api/course/{courseId}/pois/{placeId}")
    Call<ApiResponse<PoiDto>> getPoiDetail(
        @Path("courseId") int courseId,
        @Path("placeId") int placeId
    );
    
    @POST("api/course/{courseId}/pois/{placeId}/reviews")
    Call<ApiResponse> addPoiReview(
        @Path("courseId") int courseId,
        @Path("placeId") int placeId,
        @Body ReviewRequest request
    );

    // 3-2. TOUR API
    @GET("/api/villages/specialties")
    Call<VillagesListResponse> getVillages();

    @GET("/api/villages/{villageId}/specialties/{type}/{id}")
    Call<ApiResponse<VillagesDto>> getVillagesDetail(
            @Path("villageId") int villageId,
            @Path("type") String type,
            @Path("id") int id
    );

    // 4. 위치 API
    @POST("api/user/location")
    Call<ApiResponse> sendLocation(@Body LocationRequest request);
    
    // 5. 트래킹 API
    @POST("api/tracking/start")
    Call<TrackingResponse> startTracking(@Body TrackingRequest request);
    
    @POST("api/tracking/end")
    Call<ApiResponse> endTracking(@Body TrackingRequest request);
    
    // 6. 후기 API
    @POST("api/review/course")
    Call<ApiResponse> addCourseReview(@Body ReviewRequest request);
    
    @GET("api/review/course/{courseId}")
    Call<ApiResponse> getCourseReviews(@Path("courseId") int courseId);
    
    // 7. 편의시설 API
    @GET("api/course/{courseId}/facilities")
    Call<ApiResponse> getFacilities(@Path("courseId") int courseId);
    
    @POST("api/review/facility")
    Call<ApiResponse> addFacilityReview(@Body ReviewRequest request);
} 