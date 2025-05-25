package com.example.accelerometer.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.accelerometer.network.response.LoginResponse;

import java.util.regex.Pattern;

public class Utils {
    public static final String API = "http://192.168.1.100";
    static public Boolean checkPassword(String password) {
        if (password == null || password.length() != 14) {
            return false;
        }
        Pattern pattern = Pattern.compile("^[A-Za-z0-9_-]{14}$");
        return pattern.matcher(password).matches();
    }

    static public void saveLoginToSharedPref(Context context, LoginResponse response){
        SharedPreferences sharedPref = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("access_token", response.getAccessToken());
        editor.putString("token_type", response.getTokenType());
        editor.putString("username", response.getUser().getUsername());
        editor.putString("registryId", response.getUser().getRegistryId());
        editor.putString("deviceId", response.getUser().getDeviceId());
        editor.putString("devicePassword", response.getUser().getDevicePassword());

        editor.apply();
    }
}


