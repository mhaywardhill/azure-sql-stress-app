package com.example.sqlstresstool.web;

import com.example.sqlstresstool.service.StressRequest;
import com.example.sqlstresstool.service.StressResult;
import com.example.sqlstresstool.service.SqlRunnerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    private final SqlRunnerService sqlRunnerService;

    public HomeController(SqlRunnerService sqlRunnerService) {
        this.sqlRunnerService = sqlRunnerService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("request", StressRequest.defaultRequest());
        return "index";
    }

    @PostMapping("/run")
    public String run(@ModelAttribute("request") StressRequest request, Model model) {
        StressResult result = sqlRunnerService.run(request);
        model.addAttribute("result", result);
        return "index";
    }

    @PostMapping("/evict")
    public String evict(Model model) {
        String message = sqlRunnerService.evictIdleConnections();
        model.addAttribute("request", StressRequest.defaultRequest());
        model.addAttribute("evictMessage", message);
        return "index";
    }
}
