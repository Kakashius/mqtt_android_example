package com.example.accelerometer;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor accelerometer;

    MqttAndroidClient client;


    private TextView xVal, yVal, zVal;
    private Button settings,btnStart, btnStop, btnAboutUs;

    Handler mHandler = new Handler();

    Runnable mRunnableTask;

    private static String publishTopic = "$registries/ares0a5dq95860qifkgv/events";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Accelerometer Sensor
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Register Sensor Listener
        sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);

        //Text View
        xVal = findViewById(R.id.xValue);
        yVal = findViewById(R.id.yVlaue);
        zVal = findViewById(R.id.zValue);

       //Buttons
        settings = findViewById(R.id.btnSettings);
        btnStart = findViewById(R.id.btnStart);
        btnStop =findViewById(R.id.btnStop);
        btnAboutUs = findViewById((R.id.btnAboutUs));

    }




    @Override
    public void onSensorChanged(SensorEvent event) {
        xVal.setText("X: "+event.values[0]);
        yVal.setText("Y: "+event.values[1]);
        zVal.setText("Z: "+event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
              //not in use
    }

    public void startPublish(View v){

        SharedPreferences sharedPref = this.getSharedPreferences("MySharedPref",Context.MODE_PRIVATE);
        String serverURI = sharedPref.getString("serverURI","");
        String userID = sharedPref.getString("Username", "");
        String userPassword = sharedPref.getString("Password", "");
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), serverURI,clientId);
        MqttConnectOptions options = new MqttConnectOptions();

        try {
            options.setUserName(userID);
            options.setPassword(userPassword.toCharArray());
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(MainActivity.this,"Connected",Toast.LENGTH_LONG).show();
                    publish();


                    mRunnableTask = new Runnable()
                    {
                        @Override
                        public void run() {
                            publish();
                            // this will repeat this task again at specified time interval
                            mHandler.postDelayed(this, 5000);
                        }
                    };

                    // Call this to start the task first time
                    mHandler.postDelayed(mRunnableTask, 5000);
                }


                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    System.out.print("Connection Failed");
                    Log.e("MQTT", "Connection Failed", exception); // Лог ошибки
                    Toast.makeText(MainActivity.this,"Connection Failed",Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
//
//        new java.util.Timer().schedule(
//                new java.util.TimerTask() {
//                    @Override
//                    public void run() {
//                    publish();
//                    }
//                },
//                5000
//        );
//

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                publish();
//            }
//        }, 5000);


    }

    public void publish(){
        SharedPreferences sharedPref = this.getSharedPreferences("MySharedPref",Context.MODE_PRIVATE);
        String mqttTopic = sharedPref.getString("Topic","");
        Log.d("Topic",mqttTopic);
        String message =xVal.getText().toString()+","+yVal.getText().toString()+","+zVal.getText().toString();
        try{
            client.publish(publishTopic, new MqttMessage(message.getBytes()));
        }catch (MqttException e){
            e.printStackTrace();
        }


    }

    public void stopPublish(View v){
        mHandler.removeCallbacks(mRunnableTask);

    }
    public void handleClick(View v){
        startActivity(new Intent(MainActivity.this, MqttSettings.class));

    }

    public void handleAboutUs(View v){
        startActivity(new Intent(MainActivity.this, AboutUs.class));
    }

}