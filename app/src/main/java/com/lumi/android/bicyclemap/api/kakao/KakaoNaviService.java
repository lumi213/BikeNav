package com.lumi.android.bicyclemap.api.kakao;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface KakaoNaviService {
    @GET("v1/directions") // GET /v1/directions  :contentReference[oaicite:6]{index=6}
    Call<KakaoRouteResponse> directions(
            @Query("origin") String origin,           // "x,y" or "x,y,angle=90"  :contentReference[oaicite:7]{index=7}
            @Query("destination") String destination, // "x,y"                    :contentReference[oaicite:8]{index=8}
            @Query("priority") String priority,       // RECOMMEND/TIME/DISTANCE  :contentReference[oaicite:9]{index=9}
            @Query("alternatives") Boolean alternatives,
            @Query("summary") Boolean summary,
            @Query("road_details") Boolean roadDetails
    );
}
