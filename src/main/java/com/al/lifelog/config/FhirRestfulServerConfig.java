package com.al.lifelog.config;

import com.al.lifelog.provider.PatientResourceProvider;
import com.al.lifelog.provider.ObservationResourceProvider;
import com.al.lifelog.provider.ConditionResourceProvider;
import com.al.lifelog.provider.EncounterResourceProvider;
import com.al.lifelog.provider.MedicationRequestResourceProvider;
import com.al.lifelog.provider.AllergyIntoleranceResourceProvider;
import com.al.lifelog.provider.AppointmentResourceProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;

import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.context.annotation.Configuration;

import com.al.lifelog.security.SmartOnFhirInterceptor;

import com.al.lifelog.interceptor.AuditLoggingInterceptor;
import java.util.List;

@Configuration
public class FhirRestfulServerConfig {

    @Autowired
    private PatientResourceProvider patientResourceProvider;

    @Autowired
    private ObservationResourceProvider observationResourceProvider;

    @Autowired
    private ConditionResourceProvider conditionResourceProvider;

    @Autowired
    private EncounterResourceProvider encounterResourceProvider;

    @Autowired
    private MedicationRequestResourceProvider medicationRequestResourceProvider;

    @Autowired
    private AllergyIntoleranceResourceProvider allergyIntoleranceResourceProvider;

    @Autowired
    private AppointmentResourceProvider appointmentResourceProvider;

    @Autowired
    private AuditLoggingInterceptor auditLoggingInterceptor;

    @Autowired
    private com.al.lifelog.provider.PractitionerResourceProvider practitionerResourceProvider;

    @Autowired
    private com.al.lifelog.provider.OrganizationResourceProvider organizationResourceProvider;

    @Autowired
    private com.al.lifelog.provider.DiagnosticReportResourceProvider diagnosticReportResourceProvider;

    @Autowired
    private com.al.lifelog.provider.ImmunizationResourceProvider immunizationResourceProvider;

    @Autowired
    private com.al.lifelog.provider.SubscriptionResourceProvider subscriptionResourceProvider;

    @Bean
    public SmartOnFhirInterceptor smartOnFhirInterceptor() {
        return new SmartOnFhirInterceptor();
    }

    @Bean
    public RequestValidatingInterceptor requestValidatingInterceptor(FhirContext fhirContext) {
        RequestValidatingInterceptor interceptor = new RequestValidatingInterceptor();

        // Create a validation support chain
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(fhirContext);
        ValidationSupportChain supportChain = new ValidationSupportChain(defaultSupport);

        // Create the validator
        FhirInstanceValidator validatorModule = new FhirInstanceValidator(supportChain);
        interceptor.addValidatorModule(validatorModule);

        return interceptor;
    }

    @Bean
    public ServletRegistrationBean<RestfulServer> fhirServlet(FhirContext fhirContext) {
        RestfulServer server = new RestfulServer(fhirContext);
        server.setResourceProviders(List.of(
                patientResourceProvider,
                observationResourceProvider,
                conditionResourceProvider,
                encounterResourceProvider,
                medicationRequestResourceProvider,
                allergyIntoleranceResourceProvider,
                appointmentResourceProvider,
                practitionerResourceProvider,
                organizationResourceProvider,
                diagnosticReportResourceProvider,
                immunizationResourceProvider,
                subscriptionResourceProvider));

        // Register Interceptors
        server.registerInterceptor(smartOnFhirInterceptor());
        // server.registerInterceptor(requestValidatingInterceptor(fhirContext));
        server.registerInterceptor(auditLoggingInterceptor);

        ServletRegistrationBean<RestfulServer> registration = new ServletRegistrationBean<>(server, "/fhir/*");
        registration.setName("FhirServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
