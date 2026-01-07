package com.al.lifelog.security;

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

        RuleBuilder builder = new RuleBuilder();

        // Simple implementation: check authorities for SMART scopes
        // In a real OAuth2 setup, these would come from the 'scope' claim
        boolean hasPatientAllRead = hasScope(authentication, "patient/*.read");
        boolean hasUserAllRead = hasScope(authentication, "user/*.read");
        boolean hasWrite = hasScope(authentication, "user/*.write") || hasScope(authentication, "patient/*.write");

        if (hasPatientAllRead || hasUserAllRead) {
            builder.allow().read().allResources().withAnyId().andThen();
        }

        if (hasWrite) {
            builder.allow().write().allResources().withAnyId().andThen();
        }

        // If no specific scopes found, default to full access for authenticated users
        // (legacy)
        // This ensures we don't break existing functionality while transitioning
        if (!hasPatientAllRead && !hasUserAllRead && !hasWrite) {
            return builder
                    .allow().read().allResources().withAnyId().andThen()
                    .allow().write().allResources().withAnyId().andThen()
                    .allow().delete().allResources().withAnyId().andThen()
                    .allow().metadata().build();
        }

        return builder.allow().metadata().build();
    }

    private boolean hasScope(Authentication auth, String scope) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_" + scope) || a.getAuthority().equals(scope));
    }
}
