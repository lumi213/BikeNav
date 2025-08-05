package com.lumi.android.bicyclemap.api.dto;

public class LocationRequest {
    private int user_id;
    private double lat;
    private double lng;

    public LocationRequest(int user_id, double lat, double lng) {
        this.user_id = user_id;
        this.lat = lat;
        this.lng = lng;
    }

    public int getUserId() { return user_id; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }

    public void setUserId(int user_id) { this.user_id = user_id; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }
} 