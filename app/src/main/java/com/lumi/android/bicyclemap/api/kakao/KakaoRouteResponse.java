package com.lumi.android.bicyclemap.api.kakao;

import java.util.List;

// 응답 필드 중 필요한 것만 축약
public class KakaoRouteResponse {
    public List<Route> routes;
    public static class Route {
        public Summary summary;
        public List<Section> sections;
    }
    public static class Summary {
        public int distance; // m
        public int duration; // s
    }
    public static class Section {
        public int distance;
        public int duration;
        public List<Road> roads;
    }
    public static class Road {
        public String name;
        public int distance;
        public int duration;
        public List<Double> vertexes; // x,y,x,y,...
    }
}
