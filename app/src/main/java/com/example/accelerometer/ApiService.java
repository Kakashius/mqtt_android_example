package com.example.accelerometer;

import static com.example.accelerometer.utils.Utils.API;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("start")
    Call<StartResponse> startSession(
            @Header("Authorization") String bearerToken,
            @Body StartRequest request
    );

    @GET("stop")
    Call<ResponseBody> stopSession(
            @Query("sessionId") String sessionId
    );
}

public class StartResponse {
    private boolean success;
    private String message;
}

public class StartRequest {
    private String session_id;
    private long timestamp;

    public StartRequest(String session_id, long timestamp) {
        this.session_id = session_id;
        this.timestamp = timestamp;
    }
}


public class ApiClient {
    private final ApiService apiService;
    private final SharedPreferences prefs;
    String html = "";

    public ApiClient(Context context) {
        prefs = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);

        // Строим Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    /**
     * Отправляет запрос на /start с заголовком Authorization: Bearer <token>
     * @param sessionId  сгенерированный UUID сессии
     * @param timestamp  время старта в миллисекундах System.currentTimeMillis()
     */
    public void sendStartSession(@NonNull String sessionId, long timestamp) {
        // 1) Читаем токен
        String token = prefs.getString("authToken", "");
        String bearer = "Bearer " + token;

        StartRequest req = new StartRequest(sessionId, timestamp);

        apiService.startSession(bearer, req).enqueue(new Callback<StartResponse>() {
            @Override
            public void onResponse(Call<StartResponse> call, Response<StartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Успешно: response.body().getSuccess(), .getMessage() и т.д.
                    System.out.println("Start session success: " + response.body().getMessage());
                } else {
                    // Сервер вернул ошибку (4xx, 5xx)
                    System.err.println("Start session failed: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<StartResponse> call, Throwable t) {
                // Сетевая ошибка или ошибка парсинга
                t.printStackTrace();
            }
        });
    }

    public void sendStopSession(@NonNull String sessionId) {
        html = "";
        apiService.stopSession(sessionId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        html = response.body().string();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Stop failed: " + response.code());
                    html = "";
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Сетевая ошибка
                t.printStackTrace();
            }
        });
    }
}
