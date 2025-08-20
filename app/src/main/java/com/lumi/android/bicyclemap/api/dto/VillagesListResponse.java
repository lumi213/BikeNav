package com.lumi.android.bicyclemap.api.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VillagesListResponse {
    private boolean success;
    private VillagesListData data;
    private String message;

    public boolean isSuccess() { return success; }
    public VillagesListData getData() { return data; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(VillagesListData data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }

    public static class VillagesListData {
        @SerializedName("specialties")
        private List<VillagesDto> specialties;

        public List<VillagesDto> getSpecialties() { return specialties; }
        public void setSpecialties(List<VillagesDto> specialties) { this.specialties = specialties; }
    }

}
