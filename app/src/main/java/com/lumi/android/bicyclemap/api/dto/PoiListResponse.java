package com.lumi.android.bicyclemap.api.dto;

import java.util.List;

public class PoiListResponse {
    private boolean success;
    private PoiListData data;
    private String message;

    public boolean isSuccess() { return success; }
    public PoiListData getData() { return data; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(PoiListData data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }

    public static class PoiListData {
        private List<PoiDto> pois;

        public List<PoiDto> getPois() { return pois; }
        public void setPois(List<PoiDto> pois) { this.pois = pois; }
    }
} 