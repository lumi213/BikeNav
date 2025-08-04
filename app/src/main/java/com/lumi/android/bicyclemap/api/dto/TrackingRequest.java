package com.lumi.android.bicyclemap.api.dto;

public class TrackingRequest {
    private int user_id;
    private int course_id;
    private String type;
    private int tracking_id;

    public TrackingRequest(int user_id, int course_id, String type) {
        this.user_id = user_id;
        this.course_id = course_id;
        this.type = type;
    }

    public TrackingRequest(int tracking_id) {
        this.tracking_id = tracking_id;
    }

    public int getUserId() { return user_id; }
    public int getCourseId() { return course_id; }
    public String getType() { return type; }
    public int getTrackingId() { return tracking_id; }

    public void setUserId(int user_id) { this.user_id = user_id; }
    public void setCourseId(int course_id) { this.course_id = course_id; }
    public void setType(String type) { this.type = type; }
    public void setTrackingId(int tracking_id) { this.tracking_id = tracking_id; }
} 