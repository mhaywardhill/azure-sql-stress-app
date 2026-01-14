package com.example.sqlstresstool.service;

import java.util.List;

public class StressResult {
    private int totalIterations;
    private int concurrency;
    private long durationMs;
    private String startTime;
    private String finishTime;
    private int successCount;
    private int errorCount;
    private long avgMs;
    private long p50Ms;
    private long p95Ms;
    private long p99Ms;
    private List<String> errorSamples;
    private List<List<String>> sampleRows;

    public int getTotalIterations() { return totalIterations; }
    public void setTotalIterations(int totalIterations) { this.totalIterations = totalIterations; }

    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int concurrency) { this.concurrency = concurrency; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getFinishTime() { return finishTime; }
    public void setFinishTime(String finishTime) { this.finishTime = finishTime; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public long getAvgMs() { return avgMs; }
    public void setAvgMs(long avgMs) { this.avgMs = avgMs; }

    public long getP50Ms() { return p50Ms; }
    public void setP50Ms(long p50Ms) { this.p50Ms = p50Ms; }

    public long getP95Ms() { return p95Ms; }
    public void setP95Ms(long p95Ms) { this.p95Ms = p95Ms; }

    public long getP99Ms() { return p99Ms; }
    public void setP99Ms(long p99Ms) { this.p99Ms = p99Ms; }

    public List<String> getErrorSamples() { return errorSamples; }
    public void setErrorSamples(List<String> errorSamples) { this.errorSamples = errorSamples; }

    public List<List<String>> getSampleRows() { return sampleRows; }
    public void setSampleRows(List<List<String>> sampleRows) { this.sampleRows = sampleRows; }
}
