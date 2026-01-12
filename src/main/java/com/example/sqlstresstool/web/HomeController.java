package com.example.sqlstresstool.web;

import com.example.sqlstresstool.service.StressRequest;
import com.example.sqlstresstool.service.StressResult;
import com.example.sqlstresstool.service.SqlRunnerService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HomeController {

    private final SqlRunnerService sqlRunnerService;
    private final DataSource dataSource;

    public HomeController(SqlRunnerService sqlRunnerService, DataSource dataSource) {
        this.sqlRunnerService = sqlRunnerService;
        this.dataSource = dataSource;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("request", StressRequest.defaultRequest());
        addConnectionInfo(model);
        testConnection(model);
        return "index";
    }

    @PostMapping("/run")
    public String run(@ModelAttribute("request") StressRequest request, Model model) {
        StressResult result = sqlRunnerService.run(request);
        model.addAttribute("result", result);
        addConnectionInfo(model);
        return "index";
    }

    @PostMapping("/evict")
    public String evict(Model model) {
        String message = sqlRunnerService.evictIdleConnections();
        model.addAttribute("request", StressRequest.defaultRequest());
        model.addAttribute("evictMessage", message);
        addConnectionInfo(model);
        return "index";
    }

    @PostMapping("/updatePool")
    public String updatePool(@RequestParam(required = false) Integer minIdle, 
                           @RequestParam(required = false) Integer maxPool, 
                           Model model) {
        String message = sqlRunnerService.updatePoolSettings(minIdle, maxPool);
        model.addAttribute("request", StressRequest.defaultRequest());
        model.addAttribute("poolMessage", message);
        addConnectionInfo(model);
        testConnection(model);
        return "index";
    }

    @PostMapping("/testConnection")
    public String testConnectionEndpoint(Model model) {
        model.addAttribute("request", StressRequest.defaultRequest());
        addConnectionInfo(model);
        testConnection(model);
        return "index";
    }

    private void addConnectionInfo(Model model) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            String jdbcUrl = hikari.getJdbcUrl();
            if (jdbcUrl != null) {
                model.addAttribute("serverName", extractServerName(jdbcUrl));
                model.addAttribute("databaseName", extractDatabaseName(jdbcUrl));
                model.addAttribute("jdbcUrl", maskPassword(jdbcUrl));
            }
            model.addAttribute("username", hikari.getUsername());
            model.addAttribute("currentMinIdle", hikari.getMinimumIdle());
            model.addAttribute("currentMaxPool", hikari.getMaximumPoolSize());
            
            // Pool statistics
            try {
                model.addAttribute("poolActive", hikari.getHikariPoolMXBean().getActiveConnections());
                model.addAttribute("poolIdle", hikari.getHikariPoolMXBean().getIdleConnections());
                model.addAttribute("poolTotal", hikari.getHikariPoolMXBean().getTotalConnections());
                model.addAttribute("poolWaiting", hikari.getHikariPoolMXBean().getThreadsAwaitingConnection());
            } catch (Exception e) {
                // Pool not initialized yet
                model.addAttribute("poolActive", 0);
                model.addAttribute("poolIdle", 0);
                model.addAttribute("poolTotal", 0);
                model.addAttribute("poolWaiting", 0);
            }
        }
    }

    private void testConnection(Model model) {
        Map<String, String> connectionStatus = new LinkedHashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            connectionStatus.put("status", "✅ SUCCESS");
            connectionStatus.put("message", "Successfully connected to database");
            
            // Get database metadata
            try {
                String productName = conn.getMetaData().getDatabaseProductName();
                String productVersion = conn.getMetaData().getDatabaseProductVersion();
                String driverVersion = conn.getMetaData().getDriverVersion();
                
                connectionStatus.put("product", productName + " " + productVersion);
                connectionStatus.put("driver", "JDBC Driver " + driverVersion);
                connectionStatus.put("autoCommit", String.valueOf(conn.getAutoCommit()));
                connectionStatus.put("readOnly", String.valueOf(conn.isReadOnly()));
            } catch (SQLException e) {
                connectionStatus.put("metadataError", e.getMessage());
            }
            
            model.addAttribute("connectionSuccess", true);
            
        } catch (SQLException e) {
            connectionStatus.put("status", "❌ FAILED");
            connectionStatus.put("error", e.getClass().getSimpleName());
            connectionStatus.put("message", e.getMessage());
            connectionStatus.put("sqlState", e.getSQLState());
            connectionStatus.put("errorCode", String.valueOf(e.getErrorCode()));
            
            // Add troubleshooting hints
            String hint = getTroubleshootingHint(e);
            if (hint != null) {
                connectionStatus.put("hint", hint);
            }
            
            model.addAttribute("connectionSuccess", false);
            
        } catch (Exception e) {
            connectionStatus.put("status", "❌ FAILED");
            connectionStatus.put("error", e.getClass().getSimpleName());
            connectionStatus.put("message", e.getMessage());
            
            model.addAttribute("connectionSuccess", false);
        }
        
        model.addAttribute("connectionStatus", connectionStatus);
    }

    private String getTroubleshootingHint(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return null;
        
        if (msg.contains("Login failed") || msg.contains("authentication failed")) {
            return "Check DB_USER and DB_PASSWORD environment variables. For Azure AD, ensure authentication=ActiveDirectoryPassword is in the JDBC URL.";
        } else if (msg.contains("Cannot open server") || msg.contains("connect timed out")) {
            return "Check Azure SQL firewall rules. Add your Codespaces IP address or enable 'Allow Azure Services'.";
        } else if (msg.contains("host") && msg.contains("unknown")) {
            return "Verify the server name in DB_URL is correct: jdbc:sqlserver://YOUR-SERVER.database.windows.net:1433;database=YOUR-DB";
        } else if (msg.contains("database") && (msg.contains("not exist") || msg.contains("cannot be opened"))) {
            return "Verify the database name in the JDBC URL is correct.";
        } else if (msg.contains("SSL") || msg.contains("certificate")) {
            return "SSL/TLS issue. Ensure encrypt=true and trustServerCertificate=false in JDBC URL for Azure SQL.";
        } else if (msg.contains("MFA") || msg.contains("multi-factor")) {
            return "MFA is required. Password authentication cannot satisfy MFA requirements. Use a service principal or managed identity instead.";
        }
        
        return null;
    }

    private String maskPassword(String jdbcUrl) {
        // Mask password in JDBC URL for display
        if (jdbcUrl.toLowerCase().contains("password=")) {
            return jdbcUrl.replaceAll("(?i)password=([^;]+)", "password=***");
        }
        return jdbcUrl;
    }

    private String extractServerName(String jdbcUrl) {
        try {
            // Extract server from jdbc:sqlserver://servername:port;...
            int start = jdbcUrl.indexOf("//");
            if (start == -1) return "Unknown";
            start += 2;
            int end = jdbcUrl.indexOf(":", start);
            if (end == -1) end = jdbcUrl.indexOf(";", start);
            if (end == -1) end = jdbcUrl.length();
            return jdbcUrl.substring(start, end);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String extractDatabaseName(String jdbcUrl) {
        try {
            // Extract database from ;database=dbname;
            String marker = "database=";
            int start = jdbcUrl.toLowerCase().indexOf(marker);
            if (start == -1) return "Unknown";
            start += marker.length();
            int end = jdbcUrl.indexOf(";", start);
            if (end == -1) end = jdbcUrl.length();
            return jdbcUrl.substring(start, end);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
