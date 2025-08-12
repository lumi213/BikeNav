package com.lumi.android.bicyclemap.repository;

import android.content.Context;

import com.lumi.android.bicyclemap.api.ApiClient;
import com.lumi.android.bicyclemap.api.ApiService;
import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.ReviewRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewRepository {
    private final ApiService api;

    public interface Listener {
        void onSuccess(ApiResponse res);
        void onError(Throwable t);
    }

    public ReviewRepository(Context ctx) {
        this.api = ApiClient.getInstance(ctx).getApiService();
    }

    public void submit(ReviewRequest req, Listener listener) {
        api.addCourseReview(req).enqueue(new Callback<ApiResponse>() {
            @Override public void onResponse(Call<ApiResponse> call, Response<ApiResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    listener.onSuccess(resp.body());
                } else {
                    listener.onError(new RuntimeException("리뷰 전송 실패: " + resp.code()));
                }
            }
            @Override public void onFailure(Call<ApiResponse> call, Throwable t) {
                listener.onError(t);
            }
        });
    }
}
