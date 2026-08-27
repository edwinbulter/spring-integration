package com.example.demo01.filetokafka.model;

import java.util.List;

public record FileMessage(String payload, String filename, List<LogEntry> logging) {
}
