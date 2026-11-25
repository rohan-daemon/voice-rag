package com.voicerag.model;

public class TranscriptionStatus {
    private String fileName;
    private String status;
    private String message;

    public TranscriptionStatus(String fileName, String status, String message) {
        this.fileName = fileName;
        this.status = status;
        this.message = message;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
