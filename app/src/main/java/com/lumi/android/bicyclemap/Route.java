package com.lumi.android.bicyclemap;

import java.util.List;
import java.io.Serializable;

public class Route implements Serializable {
    public int id;
    public String title;
    public String image;
    public float dist_km;
    public int time;
    public String diff;               // 난이도 (상/중/하)
    public List<Point> path;
    public String type;               // 코스유형 (자전거/산책)
    public List<String> category;     // 추천유형 (예: 계절/경험 등 카테고리)
    public boolean is_recommended;    // 코스추천 여부

    public List<Integer> poi;   // 코스 주변 poi id
    public String explanation;  // 상세 설명
    public List<String> tourist_point;  // 관광 포인트

    // 선택적으로 getter를 정의
    public String getName() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public float getDistance() {
        return dist_km;
    }

    public int getTime() {
        return time;
    }

    public List<Point> getPath() {
        return path;
    }

    public List<Integer> getPoi() {
        return poi;
    }
}
