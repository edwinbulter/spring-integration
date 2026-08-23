package com.example.demo01.filetokafka.model;

import java.util.List;

public class FileMessage {

    private String payload;
    private String filename;
    private List<LogEntry> logging;

    public FileMessage() {
    }

    public FileMessage(String payload, String filename, List<LogEntry> logging) {
        this.payload = payload;
        this.filename = filename;
        this.logging = logging;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<LogEntry> getLogging() {
        return logging;
    }

    public void setLogging(List<LogEntry> logging) {
        this.logging = logging;
    }
}
