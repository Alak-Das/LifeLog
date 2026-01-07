package com.lifelog.ehr.interceptor;

import com.lifelog.ehr.model.AuditLog;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.hl7.fhir.r4.model.IdType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLoggingInterceptorTest {

    @Mock
    private com.lifelog.ehr.service.AuditService auditService;

    // Remove AuditLogRepository mock as it's not used directly
    // @Mock
    // private AuditLogRepository auditLogRepository;

    @Mock
    private RequestDetails requestDetails;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuditLoggingInterceptor interceptor;

    @Test
    public void testLogRequest_ShouldSaveLog() {
        // Setup Security Context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        // Setup Request Details
        when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.READ);
        when(requestDetails.getResourceName()).thenReturn("Patient");
        when(requestDetails.getId()).thenReturn(new IdType("123"));

        // Setup Servlet Objects
        when(servletResponse.getStatus()).thenReturn(200);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(servletRequest.getRequestURI()).thenReturn("/fhir/Patient/123");

        // Execute
        interceptor.logRequest(requestDetails, servletRequest, servletResponse);

        // Verify
        verify(auditService).saveAuditLog(any(AuditLog.class));
    }
}
