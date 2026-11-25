package com.voicerag.model;

public class SearchResult {
    private String content;
    private String source;
    private double score;

    public SearchResult(String content, String source, double score) {
        this.content = content;
        this.source = source;
        this.score = score;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
