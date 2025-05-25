package com.example.accelerometer.network.request;

import java.sql.Timestamp;

public class SessionStartRequest {
    private String session_id;
    private Timestamp datetime;

    public SessionStartRequest(String session_id, Timestamp datetime) {
        this.session_id = session_id;
        this.datetime = datetime;
    }
}
