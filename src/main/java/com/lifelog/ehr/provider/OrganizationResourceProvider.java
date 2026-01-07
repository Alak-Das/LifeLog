package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.OrganizationService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import java.util.List;

@Component
public class OrganizationResourceProvider implements IResourceProvider {

    private final OrganizationService organizationService;

    @Autowired
    public OrganizationResourceProvider(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Override
    public Class<Organization> getResourceType() {
        return Organization.class;
    }

    @Read
    public Organization read(@IdParam IdType theId) {
        Organization organization = organizationService.getOrganization(theId.getIdPart());
        if (organization == null) {
            throw new ResourceNotFoundException(theId);
        }
        return organization;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Organization theOrganization) {
        Organization created = organizationService.createOrganization(theOrganization);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public IBundleProvider search(
            @OptionalParam(name = Organization.SP_NAME) StringParam name,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String nameVal = (name != null) ? name.getValue() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        List<Organization> organizations = organizationService.searchOrganizations(nameVal, offsetVal, countVal);
        return new SimpleBundleProvider(organizations);
    }
}
