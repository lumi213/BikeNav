package com.lumi.android.bicyclemap.gpt;

import java.util.List;

public class GptRouteRecommendation {
    public List<Item> recommendations;

    public static class Item {
        public int course_id;
        public String reason;          // 추천 이유
        public String estimated_time;  // 예: "약 60분"
    }
}
