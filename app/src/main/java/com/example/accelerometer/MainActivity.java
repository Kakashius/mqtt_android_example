package com.example.accelerometer;

import static com.example.accelerometer.utils.Utils.API;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.accelerometer.network.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import info.mqtt.android.service.MqttAndroidClient;
import kotlin.text.Charsets;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // UI
    private Button btnStart, btnStop;

    // Sensors
    private SensorManager sensorManager;
    private Sensor accelerometer, stepCounter;
    private long initialStepCount = -1;
    private long lastStepCount = 0;

    // Location
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    // Data lists
    private List<Location> locationList = new ArrayList<>();
    private List<StepEvent> stepList = new ArrayList<>();

    // Session metadata
    private String sessionId;
    private long startTimeMs;

    // MQTT (optional)
    private info.mqtt.android.service.MqttAndroidClient mqttClient;
    private Handler mqttHandler = new Handler(Looper.getMainLooper());
    private Runnable mqttRunnable;

    public String getDeviceId () {
        SharedPreferences sp = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String deviceId = "devices/" + sp.getDevideId() + "/events";
        return deviceId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI
        btnStart = findViewById(R.id.btnStart);
        btnStop  = findViewById(R.id.btnStop);
        btnStop.setEnabled(false);

        // Sensors
        sensorManager   = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer   = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepCounter     = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // Location Client
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                for (Location loc : result.getLocations()) {
                    locationList.add(loc);
                }
            }
        };

        // Request permissions
        String[] perms = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION };
        ActivityCompat.requestPermissions(this, perms, 1001);

        // Start/Stop Button handlers
        btnStart.setOnClickListener(v -> startPublish());
        btnStop.setOnClickListener(v -> stopPublish());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long ts = System.currentTimeMillis();
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            long count = (long) event.values[0];
            if (initialStepCount < 0) initialStepCount = count;
            long steps = count - initialStepCount;
            stepList.add(new StepEvent(steps, ts));
            lastStepCount = steps;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // not used
    }

    public void startPublish(View v) {
        // UI
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);

        // Reset data
        locationList.clear();
        stepList.clear();
        initialStepCount = -1;

        // Generate session metadata
        sessionId    = UUID.randomUUID().toString();
        startTimeMs  = System.currentTimeMillis();


        ApiClient apiClient = new ApiClient(this);
        apiClient.sendStartSession(sessionId, startTimeMs);

        // Register sensors
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, stepCounter,   SensorManager.SENSOR_DELAY_NORMAL);

        // Start location updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest req = LocationRequest.create()
                    .setInterval(1000)
                    .setFastestInterval(500)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
        }

        // Init and connect MQTT
        SharedPreferences sp = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String userID    = sp.getString("Username", "");
        String clientId  = MqttClient.generateClientId();
        mqttClient       = new MqttAndroidClient(getApplicationContext(), API, clientId);
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setUserName(userID);
        mqttClient.connect(opts, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Toast.makeText(MainActivity.this, "MQTT Connected", Toast.LENGTH_SHORT).show();
                // Start periodic publish
                mqttRunnable = new Runnable() {
                    @Override
                    public void run() {
                        publishMqtt();
                        mqttHandler.postDelayed(this, 5000);
                    }
                };
                mqttHandler.post(mqttRunnable);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e("MQTT", "Connection Failed", exception);
            }
        });
    }

    private void publishMqtt() {
        // Build JSON payload

        JsonArray locArr = new JsonArray();
        for (Location loc : locationList) {
            JsonObject o = new JsonObject();
            o.addProperty("ts", loc.getTime());
            o.addProperty("lat", loc.getLatitude());
            o.addProperty("lon", loc.getLongitude());
            o.addProperty("alt", loc.getAltitude());
            o.addProperty("acc", loc.getAccuracy());
            locArr.add(o);
        }

        JsonArray stepArr = new JsonArray();
        for (StepEvent st : stepList) {
            JsonObject o = new JsonObject();
            o.addProperty("count", st.count);
            stepArr.add(o);
        }

        String jsonString = payload.toString();
        MqttMessage msg = new MqttMessage(jsonString.getBytes(Charsets.UTF_8));
        try {
            mqttClient.publish(PUBLISH_TOPIC, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void stopPublish() {
        // UI
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);

        ApiClient apiClient = new ApiClient(this);
        apiClient.sendStopSession(sessionId);

        // Stop sensors & location
        sensorManager.unregisterListener(this);
        fusedClient.removeLocationUpdates(locationCallback);

        // Stop MQTT publishing
        if (mqttRunnable != null) {
            mqttHandler.removeCallbacks(mqttRunnable);
        }

        // Optionally disconnect
        if (mqttClient.isConnected()) mqttClient.disconnect();

        // Proceed to next screen
        startActivity(new Intent(MainActivity.this, Analytics.class));
    }

    // Helper classes
    private static class StepEvent {
        long count;
        long ts;
        StepEvent(long count, long ts) { this.count = count; this.ts = ts; }
    }
}

