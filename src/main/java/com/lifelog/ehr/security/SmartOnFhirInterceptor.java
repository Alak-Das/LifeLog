package com.lifelog.ehr.security;

import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class SmartOnFhirInterceptor extends AuthorizationInterceptor {

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            // If not authenticated (and not caught by Spring Security for some reason),
            // deny all
            return new RuleBuilder().denyAll().build();
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String scopeString = jwt.getClaimAsString("scope");
        if (scopeString == null) {
            scopeString = "";
        }
        String[] scopes = scopeString.split(" ");

        RuleBuilder ruleBuilder = new RuleBuilder();

        // Simple mapping of SMART scopes to Rules
        for (String scope : scopes) {
            if ("patient/*.read".equals(scope) || "user/*.read".equals(scope)) {
                // Allow reading everything for MVP (In real world, restrict to specific patient
                // if launch context exists)
                ruleBuilder.allow().read().allResources().withAnyId().build();
            }
            if ("patient/*.write".equals(scope)) {
                ruleBuilder.allow().write().allResources().withAnyId().build();
            }
        }

        // Always allow metadata
        ruleBuilder.allow().metadata().build();

        return ruleBuilder.build();
    }
}
