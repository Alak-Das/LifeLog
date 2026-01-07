package com.al.lifelog.interceptor;

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

@Component
@Interceptor
public class AuditLoggingInterceptor {

    @Autowired
    private com.al.lifelog.service.AuditService auditService;

    @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
    public void logRequest(RequestDetails theRequestDetails, HttpServletRequest theServletRequest,
            HttpServletResponse theServletResponse) {
        if (theRequestDetails == null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "anonymous";

        RestOperationTypeEnum operation = theRequestDetails.getRestOperationType();

        // Asynchronously log using the new FHIR-compliant log method
        auditService.log(
                operation != null ? operation.name() : "UNKNOWN",
                theRequestDetails.getResourceName(),
                theRequestDetails.getId() != null ? theRequestDetails.getId().getIdPart() : null,
                theServletResponse != null && theServletResponse.getStatus() < 400 ? "Success" : "Failure",
                username,
                theServletRequest != null ? theServletRequest.getRemoteAddr() : null);
    }
}
