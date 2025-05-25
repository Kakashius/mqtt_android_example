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
import com.example.accelerometer.network.request.RegisterRequest;
import com.example.accelerometer.network.response.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Registration extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private EditText height;
    private EditText weight;
    private EditText age;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        username = findViewById(R.id.loginTextView);
        password = findViewById(R.id.editTextTextPassword2);
        height = findViewById(R.id.heightText);
        weight = findViewById(R.id.weightText);
        age = findViewById(R.id.ageTxt);
    }

    public void register(View v) {
        if (!checkPassword(password.getText().toString())) {
            Toast.makeText(this, R.string.password_warning, Toast.LENGTH_SHORT).show();
        } else {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(API).addConverterFactory(GsonConverterFactory.create()).build();

            ApiService apiService = retrofit.create(ApiService.class);
            float heightFloat = 0f;
            float weightFloat = 0f;
            int ageInt = 0;
            if (!password.getText().toString().isEmpty()
                    || !username.getText().toString().isEmpty()
                    || !height.getText().toString().isEmpty()
                    || !weight.getText().toString().isEmpty()
                    || !age.getText().toString().isEmpty()) {
                heightFloat = Float.parseFloat(height.getText().toString());
                weightFloat = Float.parseFloat(weight.getText().toString());
                ageInt = Integer.parseInt(age.getText().toString());
            } else {
                Toast.makeText(Registration.this, "Some of the fields are empty", Toast.LENGTH_SHORT).show();
            }
            RegisterRequest request = new RegisterRequest(
                    password.getText().toString(),
                    username.getText().toString(),
                    heightFloat,
                    weightFloat,
                    ageInt
            );

            apiService.register(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        saveLoginToSharedPref(Registration.this, response.body());
                        startActivity(new Intent(Registration.this, MainActivity.class));
                    } else {
                        Toast.makeText(Registration.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                    return null;
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(Registration.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void goToLogin(View v) {
        startActivity(new Intent(Registration.this, Login.class));
    }
}
