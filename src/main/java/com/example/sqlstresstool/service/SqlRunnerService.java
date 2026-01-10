package com.example.sqlstresstool.service;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

@Service
public class SqlRunnerService {

    private final DataSource dataSource;

    public SqlRunnerService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public StressResult run(StressRequest req) {
        int iterations = Math.max(1, req.getIterations());
        int concurrency = Math.max(1, req.getConcurrency());
        int timeoutSeconds = Math.max(1, req.getTimeoutSeconds());
        int delayMs = Math.max(0, req.getDelayMs());

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<String> errorSamples = Collections.synchronizedList(new ArrayList<>());
        List<List<String>> sampleRows = Collections.synchronizedList(new ArrayList<>());
        final int maxErrorSamples = 5;

        Instant start = Instant.now();
        CountDownLatch latch = new CountDownLatch(iterations);

        for (int i = 0; i < iterations; i++) {
            pool.submit(() -> {
                try {
                    if (delayMs > 0) Thread.sleep(delayMs);
                    long t0 = System.nanoTime();
                    executeOnce(req, timeoutSeconds, sampleRows);
                    long t1 = System.nanoTime();
                    latencies.add(TimeUnit.NANOSECONDS.toMillis(t1 - t0));
                } catch (Exception ex) {
                    synchronized (errorSamples) {
                        if (errorSamples.size() < maxErrorSamples) {
                            errorSamples.add(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
        }

        Instant end = Instant.now();
        long durationMs = Duration.between(start, end).toMillis();
        int successCount = latencies.size();
        int errorCount = iterations - successCount;

        // stats
        Collections.sort(latencies);
        long avg = latencies.isEmpty() ? 0 : (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = percentile(latencies, 50);
        long p95 = percentile(latencies, 95);
        long p99 = percentile(latencies, 99);

        StressResult res = new StressResult();
        res.setTotalIterations(iterations);
        res.setConcurrency(concurrency);
        res.setDurationMs(durationMs);
        res.setSuccessCount(successCount);
        res.setErrorCount(errorCount);
        res.setAvgMs(avg);
        res.setP50Ms(p50);
        res.setP95Ms(p95);
        res.setP99Ms(p99);
        res.setErrorSamples(errorSamples);
        res.setSampleRows(sampleRows);
        return res;
    }

    private static long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

    private void executeOnce(StressRequest req, int timeoutSeconds, List<List<String>> sampleRows) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.setQueryTimeout(timeoutSeconds);

                String sql = req.getSqlText();
                boolean isQuery = isSelect(sql);

                if (isQuery) {
                    if (req.getResultMode() == ResultMode.NONE) {
                        // Execute without fetching rows
                        st.execute(sql);
                        return;
                    } else if (req.getResultMode() == ResultMode.SCALAR) {
                        try (ResultSet rs = st.executeQuery(sql)) {
                            if (rs.next()) {
                                List<String> row = new ArrayList<>();
                                row.add(rs.getString(1));
                                addSampleRow(sampleRows, row, req.getMaxRows());
                            }
                        }
                    } else { // ROWS
                        try (ResultSet rs = st.executeQuery(sql)) {
                            int cols = rs.getMetaData().getColumnCount();
                            int count = 0;
                            while (rs.next() && count < req.getMaxRows()) {
                                List<String> row = new ArrayList<>();
                                for (int c = 1; c <= cols; c++) {
                                    row.add(rs.getString(c));
                                }
                                addSampleRow(sampleRows, row, req.getMaxRows());
                                count++;
                            }
                        }
                    }
                } else {
                    st.executeUpdate(sql);
                }
            }
        }
    }

    private static void addSampleRow(List<List<String>> sampleRows, List<String> row, int maxRows) {
        synchronized (sampleRows) {
            if (sampleRows.size() < maxRows) {
                sampleRows.add(row);
            }
        }
    }

    private static boolean isSelect(String sql) {
        if (sql == null) return false;
        String s = sql.trim().toLowerCase(Locale.ROOT);
        return s.startsWith("select") || s.startsWith("with ");
    }

    public String evictIdleConnections() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            hikari.getHikariPoolMXBean().softEvictConnections();
            return "Idle connections evicted from pool '" + hikari.getPoolName() + "'";
        }
        return "DataSource is not HikariCP, cannot evict connections";
    }
}
