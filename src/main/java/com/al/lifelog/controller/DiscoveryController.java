package com.al.lifelog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
public class DiscoveryController {

    @GetMapping("/.well-known/smart-configuration")
    public Map<String, Object> getSmartConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("authorization_endpoint", "http://localhost:8080/auth/authorize");
        config.put("token_endpoint", "http://localhost:8080/auth/token");
        config.put("capabilities", List.of(
                "launch-ehr",
                "launch-standalone",
                "client-public",
                "client-confidential-symmetric",
                "sso-openid-connect",
                "context-standalone-patient",
                "permission-patient",
                "permission-user"));
        config.put("scopes_supported", List.of(
                "openid",
                "profile",
                "launch",
                "patient/*.read",
                "patient/*.write",
                "user/*.read",
                "user/*.write",
                "offline_access"));
        return config;
    }
}
