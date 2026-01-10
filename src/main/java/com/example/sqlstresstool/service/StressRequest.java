package com.example.sqlstresstool.service;

public class StressRequest {
    private String sqlText;
    private int iterations;
    private int concurrency;
    private int delayMs;
    private int timeoutSeconds;
    private ResultMode resultMode;
    private int maxRows;

    public static StressRequest defaultRequest() {
        StressRequest r = new StressRequest();
        r.sqlText = "SELECT SYSDATETIME() as now";
        r.iterations = 50;
        r.concurrency = 10;
        r.delayMs = 0;
        r.timeoutSeconds = 30;
        r.resultMode = ResultMode.SCALAR;
        r.maxRows = 10;
        return r;
    }

    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }

    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }

    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int concurrency) { this.concurrency = concurrency; }

    public int getDelayMs() { return delayMs; }
    public void setDelayMs(int delayMs) { this.delayMs = delayMs; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public ResultMode getResultMode() { return resultMode; }
    public void setResultMode(ResultMode resultMode) { this.resultMode = resultMode; }

    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
}
