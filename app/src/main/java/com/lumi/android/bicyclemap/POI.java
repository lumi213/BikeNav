package com.lumi.android.bicyclemap;

import java.io.Serializable;

public class POI implements Serializable {
    public int id;
    public String name;
    public String image;
    public String explanation;
    public Point point;
    public String category;

    // 선택적으로 getter 추가 가능
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getExplanation() {
        return explanation;
    }

    public Point getPoint() {
        return point;
    }

    public String getCategory() {
        return category;
    }
}
