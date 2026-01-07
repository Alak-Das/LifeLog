package com.al.lifelog.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class ValidationService {

    private final FhirContext fhirContext;
    private FhirValidator validator;

    public ValidationService(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @PostConstruct
    public void init() {
        validator = fhirContext.newValidator();
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(fhirContext);
        validator.registerValidatorModule(instanceValidator);
    }

    public void validate(IBaseResource resource) {
        ValidationResult result = validator.validateWithResult(resource);
        if (!result.isSuccessful()) {
            // throw new UnprocessableEntityException(fhirContext,
            // result.toOperationOutcome());
            System.err.println("Validation failed for resource: " + result.toString());
        }
    }
}
