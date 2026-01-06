package com.lifelog.ehr.security;

import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class SmartOnFhirInterceptor extends AuthorizationInterceptor {

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new RuleBuilder().denyAll().build();
        }

        // Allow full access to authenticated users
        return new RuleBuilder()
                .allow().read().allResources().withAnyId().andThen()
                .allow().write().allResources().withAnyId().andThen()
                .allow().metadata().build();
    }
}
