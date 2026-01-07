package com.al.lifelog.provider;

import com.al.lifelog.service.ConditionService;
import com.al.lifelog.service.ValidationService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConditionResourceProvider implements IResourceProvider {

    @Autowired
    private ConditionService conditionService;

    @Autowired
    private ValidationService validationService;

    @Override
    public Class<Condition> getResourceType() {
        return Condition.class;
    }

    @Read
    public Condition read(@IdParam IdType theId) {
        Condition condition = conditionService.getCondition(theId.getIdPart());
        if (condition == null) {
            throw new ResourceNotFoundException(theId);
        }
        return condition;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Condition theCondition) {
        validationService.validate(theCondition);
        Condition created = conditionService.createCondition(theCondition);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Update
    public ca.uhn.fhir.rest.api.MethodOutcome update(@IdParam IdType theId, @ResourceParam Condition theCondition) {
        validationService.validate(theCondition);
        Condition updated = conditionService.updateCondition(theId.getIdPart(), theCondition);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(updated.getId()));
    }

    @Delete
    public ca.uhn.fhir.rest.api.MethodOutcome delete(@IdParam IdType theId) {
        conditionService.deleteCondition(theId.getIdPart());
        return new ca.uhn.fhir.rest.api.MethodOutcome(theId);
    }

    @History
    public List<Condition> getHistory(@IdParam IdType theId) {
        return conditionService.getHistory(theId.getIdPart());
    }

    @Search
    public List<Condition> search(
            @OptionalParam(name = Condition.SP_SUBJECT) ReferenceParam subject,
            @OptionalParam(name = Condition.SP_CODE) TokenParam code,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String subjectVal = (subject != null) ? subject.getIdPart() : null;
        String codeVal = (code != null) ? code.getValue() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        return conditionService.searchConditions(subjectVal, codeVal, offsetVal, countVal);
    }
}
