package com.fiftytalk.carddraw.controller;

import com.fiftytalk.carddraw.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemController {
    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of(
                "service", "card-draw-backend",
                "swagger", "/swagger-ui.html",
                "h2Console", "/h2-console",
                "prototype", "/prototype.html",
                "admin", "/admin.html",
                "h5", "/h5.html",
                "ping", "/api/ping"
        );
    }

    @GetMapping("/api/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of("status", "ok", "service", "card-draw-backend"));
    }
}
