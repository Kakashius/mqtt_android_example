package com.example.accelerometer.network.request;

public class RegisterRequest {
    private String password;
    private String username;
    private Float height;
    private Float weight;
    private Integer age;

    public RegisterRequest(String password, String username, Float height, Float weight, Integer age){
        this.password = password;
        this.username = username;
        this.height = height;
        this.weight = weight;
        this.age = age;
    }
}
