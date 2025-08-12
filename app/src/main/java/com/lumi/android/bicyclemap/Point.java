package com.lumi.android.bicyclemap;

import java.io.Serializable;

public class Point implements Serializable {
    public double lat;
    public double lng;

    public Point(double Lat, double Lng)
    {
        lat = Lat;
        lng = Lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}