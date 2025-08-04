package com.lumi.android.bicyclemap.api.dto;

public class AuthResponse {
    private boolean success;
    private AuthData data;
    private String message;

    public boolean isSuccess() { return success; }
    public AuthData getData() { return data; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(AuthData data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }

    public static class AuthData {
        private String token;
        private int user_id;
        private String name;

        public String getToken() { return token; }
        public int getUserId() { return user_id; }
        public String getName() { return name; }

        public void setToken(String token) { this.token = token; }
        public void setUserId(int user_id) { this.user_id = user_id; }
        public void setName(String name) { this.name = name; }
    }
} 