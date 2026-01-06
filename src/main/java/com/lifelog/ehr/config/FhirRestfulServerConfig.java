package com.lifelog.ehr.config;

import com.lifelog.ehr.provider.PatientResourceProvider;
import com.lifelog.ehr.provider.ObservationResourceProvider;
import com.lifelog.ehr.provider.ConditionResourceProvider;
import com.lifelog.ehr.provider.EncounterResourceProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public ServletRegistrationBean<RestfulServer> fhirServlet() {
        RestfulServer server = new RestfulServer(FhirContext.forR4());
        server.setResourceProviders(List.of(patientResourceProvider, observationResourceProvider,
                conditionResourceProvider, encounterResourceProvider));

        ServletRegistrationBean<RestfulServer> registration = new ServletRegistrationBean<>(server, "/fhir/*");
        registration.setName("FhirServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
