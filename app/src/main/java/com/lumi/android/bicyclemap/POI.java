package com.lumi.android.bicyclemap;

import java.io.Serializable;
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
    public String image;
    public String explanation;

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
        return type;
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
