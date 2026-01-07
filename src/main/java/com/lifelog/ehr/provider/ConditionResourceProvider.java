package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.ConditionService;
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
        Condition created = conditionService.createCondition(theCondition);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
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
