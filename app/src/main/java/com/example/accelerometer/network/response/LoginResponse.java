package com.example.accelerometer.network.response;

public class LoginResponse {
    private String access_token;
    private String token_type;
    private UserResponse user;

    public String getAccessToken() {
        return access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public UserResponse getUser() {
        return user;
    }
}

