package com.lifelog.ehr.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SmartWellKnownController {

    @GetMapping("/.well-known/smart-configuration")
    public Map<String, Object> getSmartConfiguration() {
        Map<String, Object> config = new HashMap<>();

        // In a real app, these URLs should be dynamic based on the environment
        String keycloakBase = "http://localhost:8180/realms/lifelog/protocol/openid-connect";

        config.put("authorization_endpoint", keycloakBase + "/auth");
        config.put("token_endpoint", keycloakBase + "/token");
        config.put("introspection_endpoint", keycloakBase + "/token/introspect");
        config.put("revocation_endpoint", keycloakBase + "/revoke");

        config.put("capabilities", List.of(
                "launch-ehr",
                "client-public",
                "client-confidential-symmetric",
                "context-ehr-patient",
                "sso-openid-connect"));

        config.put("response_types_supported", List.of("code", "token", "id_token", "refresh_token"));
        config.put("scopes_supported", List.of(
                "openid", "profile", "email", "launch", "launch/patient",
                "patient/*.read", "patient/*.write",
                "user/*.read", "user/*.write"));

        return config;
    }
}
