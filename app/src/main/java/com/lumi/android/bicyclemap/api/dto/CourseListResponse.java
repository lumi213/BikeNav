package com.lumi.android.bicyclemap.api.dto;

import java.util.List;

public class CourseListResponse {
    private boolean success;
    private List<CourseDto> data;
    private String message;

    public boolean isSuccess() { return success; }
    public List<CourseDto> getData() { return data; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(List<CourseDto> data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }
} 