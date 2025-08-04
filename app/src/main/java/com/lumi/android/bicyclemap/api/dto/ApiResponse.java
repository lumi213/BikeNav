package com.lumi.android.bicyclemap.api.dto;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private int errorCode;

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public int getErrorCode() { return errorCode; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(T data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
} 