package com.lumi.android.bicyclemap;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Point implements Serializable {
    @SerializedName(value = "lat", alternate = {"latitude"})
    public double lat;
    @SerializedName(value = "lng", alternate = {"lon", "longitude"})
    public double lng;

    public Point(double Lat, double Lng)
    {
        this.lat = Lat;
        this.lng = Lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}