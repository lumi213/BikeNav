package com.lumi.android.bicyclemap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class POI implements Serializable {
    public int id;
    public String name;
    public String type;
    public String addr;
    public Point point;
    public String hour;
    public double rate;
    public String menu;
    public String tel;
    public List<String> tags = new ArrayList<>();
    public List<String> imageUrls = new ArrayList<>();
    public String mainImageUrl; // 카드 썸네일
    public String explanation;

    // 선택적으로 getter 추가
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return mainImageUrl;
    }

    public String getExplanation() {
        return explanation;
    }

    public Point getPoint() {
        return point;
    }

    public String getCategory() {
        return type;
    }

    // ★ 거리계산용 위도/경도 getter 추가
    public double getLatitude() {
        if (point != null) return point.getLat();
        else return 0.0;
    }

    public double getLongitude() {
        if (point != null) return point.getLng();
        else return 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof POI)) return false;
        POI other = (POI) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type);
    }
}
