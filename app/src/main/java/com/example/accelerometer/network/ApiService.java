package com.example.accelerometer.network;

import com.example.accelerometer.network.request.LoginRequest;
import com.example.accelerometer.network.request.RegisterRequest;
import com.example.accelerometer.network.response.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("/register")
    Call<LoginResponse> register(@Body RegisterRequest registerRequest);
}
