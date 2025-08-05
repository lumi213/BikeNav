package com.lumi.android.bicyclemap.api.dto;

public class TrackingResponse {
    private boolean success;
    private TrackingData data;
    private String message;

    public boolean isSuccess() { return success; }
    public TrackingData getData() { return data; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(TrackingData data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }

    public static class TrackingData {
        private int tracking_id;
        private String started_at;

        public int getTrackingId() { return tracking_id; }
        public String getStartedAt() { return started_at; }

        public void setTrackingId(int tracking_id) { this.tracking_id = tracking_id; }
        public void setStartedAt(String started_at) { this.started_at = started_at; }
    }
} 