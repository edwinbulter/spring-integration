package com.example.demo01.filetokafka.model;

public class LogEntry {

    private String timestamp;
    private String message;

    public LogEntry() {
    }

    public LogEntry(String timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
