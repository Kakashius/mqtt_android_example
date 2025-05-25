package com.example.accelerometer;

import static com.example.accelerometer.utils.Utils.API;
import static com.example.accelerometer.utils.Utils.checkPassword;
import static com.example.accelerometer.utils.Utils.saveLoginToSharedPref;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.accelerometer.network.ApiService;
import com.example.accelerometer.network.request.LoginRequest;
import com.example.accelerometer.network.response.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Login extends AppCompatActivity {

    private EditText username;
    private EditText password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.usernameText);
        password = findViewById(R.id.passwordText);
    }

    public void login(View v) {
        if (!checkPassword(password.getText().toString())) {
            Toast.makeText(this, R.string.password_warning, Toast.LENGTH_SHORT).show();

        } else {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            LoginRequest request = new LoginRequest(username.getText().toString(), password.getText().toString());

            apiService.login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        saveLoginToSharedPref(Login.this, response.body());
                        startActivity(new Intent(Login.this, MainActivity.class));
                    } else {
                        Toast.makeText(Login.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(Login.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Login.this, MainActivity.class));
                }
            });
        }
    }

    public void goToRegister(View v) {
        startActivity(new Intent(Login.this, Registration.class));
    }
}
