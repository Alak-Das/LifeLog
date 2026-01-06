package com.lifelog.ehr.interceptor;

import com.lifelog.ehr.model.AuditLog;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Interceptor
public class AuditLoggingInterceptor {

    @Autowired
    private com.lifelog.ehr.service.AuditService auditService;

    @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
    public void logRequest(RequestDetails theRequestDetails, HttpServletRequest theServletRequest,
            HttpServletResponse theServletResponse) {
        if (theRequestDetails == null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "anonymous";

        AuditLog log = new AuditLog();
        log.setTimestamp(Instant.now());
        log.setUsername(username);

        RestOperationTypeEnum operation = theRequestDetails.getRestOperationType();
        log.setOperation(operation != null ? operation.name() : "UNKNOWN");

        log.setResourceType(theRequestDetails.getResourceName());

        if (theRequestDetails.getId() != null) {
            log.setResourceId(theRequestDetails.getId().getIdPart());
        }

        if (theServletResponse != null) {
            log.setStatusCode(String.valueOf(theServletResponse.getStatus()));
        }

        if (theServletRequest != null) {
            log.setClientIp(theServletRequest.getRemoteAddr());
            log.setRequestUrl(theServletRequest.getRequestURI());
        }

        // Asynchronously save log to avoid blocking response
        auditService.saveAuditLog(log);
    }
}
