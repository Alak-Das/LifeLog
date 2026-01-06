package com.lifelog.ehr.config;

import com.lifelog.ehr.provider.PatientResourceProvider;
import com.lifelog.ehr.provider.ObservationResourceProvider;
import com.lifelog.ehr.provider.ConditionResourceProvider;
import com.lifelog.ehr.provider.EncounterResourceProvider;
import com.lifelog.ehr.provider.MedicationRequestResourceProvider;
import com.lifelog.ehr.provider.AllergyIntoleranceResourceProvider;
import com.lifelog.ehr.provider.AppointmentResourceProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lifelog.ehr.security.SmartOnFhirInterceptor;

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

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public SmartOnFhirInterceptor smartOnFhirInterceptor() {
        return new SmartOnFhirInterceptor();
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
                appointmentResourceProvider));

        // Register Interceptors
        server.registerInterceptor(smartOnFhirInterceptor());

        ServletRegistrationBean<RestfulServer> registration = new ServletRegistrationBean<>(server, "/fhir/*");
        registration.setName("FhirServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
