package com.voicerag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "voicerag")
public class VoiceRagConfig {
    private String recordingsDir;
    private String transcriptsDir;
    private String whisperModel = "base";
    private int chunkSize = 500;
    private int chunkOverlap = 50;

    public String getRecordingsDir() { return recordingsDir; }
    public void setRecordingsDir(String recordingsDir) { this.recordingsDir = recordingsDir; }

    public String getTranscriptsDir() { return transcriptsDir; }
    public void setTranscriptsDir(String transcriptsDir) { this.transcriptsDir = transcriptsDir; }

    public String getWhisperModel() { return whisperModel; }
    public void setWhisperModel(String whisperModel) { this.whisperModel = whisperModel; }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
}
