package com.lumi.android.bicyclemap.repository;

import android.content.Context;

import com.lumi.android.bicyclemap.api.ApiManager;
import com.lumi.android.bicyclemap.api.dto.ApiResponse;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.api.dto.PoiListResponse;

import java.util.List;

public class PoiRepository {
    private ApiManager apiManager;
    private Context context;

    public PoiRepository(Context context) {
        this.context = context;
        apiManager = ApiManager.getInstance(context);
    }

    public void getPois(int courseId, String category, RepositoryCallback<PoiListResponse> callback) {
        apiManager.getPois(courseId, category, new ApiManager.ApiCallback<PoiListResponse>() {
            @Override
            public void onSuccess(PoiListResponse response) {
                callback.onSuccess(response);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void getPoiDetail(int courseId, int placeId, RepositoryCallback<PoiDto> callback) {
        apiManager.getPoiDetail(courseId, placeId, new ApiManager.ApiCallback<ApiResponse<PoiDto>>() {
            @Override
            public void onSuccess(ApiResponse<PoiDto> response) {
                if (response.getData() != null) {
                    callback.onSuccess(response.getData());
                } else {
                    callback.onError("POI 데이터가 없습니다");
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