package com.example.sqlstresstool.web;

import com.example.sqlstresstool.service.StressRequest;
import com.example.sqlstresstool.service.StressResult;
import com.example.sqlstresstool.service.SqlRunnerService;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import javax.sql.DataSource;

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
        return "index";
    }

    private void addConnectionInfo(Model model) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            String jdbcUrl = hikari.getJdbcUrl();
            if (jdbcUrl != null) {
                model.addAttribute("serverName", extractServerName(jdbcUrl));
                model.addAttribute("databaseName", extractDatabaseName(jdbcUrl));
            }
            model.addAttribute("currentMinIdle", hikari.getMinimumIdle());
            model.addAttribute("currentMaxPool", hikari.getMaximumPoolSize());
        }
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
