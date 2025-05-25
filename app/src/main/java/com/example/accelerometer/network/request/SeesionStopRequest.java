package com.example.accelerometer.network.request;

import java.sql.Timestamp;

public class SeesionStopRequest {
    private String session_id;
    private Timestamp datetime;

    public SeesionStopRequest(String session_id, Timestamp datetime) {
        this.session_id = session_id;
        this.datetime = datetime;
    }
}
