package com.al.lifelog.provider;

import com.al.lifelog.service.SubscriptionService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionResourceProvider implements IResourceProvider {

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public Class<Subscription> getResourceType() {
        return Subscription.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Subscription theSubscription) {
        subscriptionService.registerSubscription(theSubscription);
        return new MethodOutcome(new IdType("Subscription", theSubscription.getIdElement().getIdPart()));
    }

    @Delete
    public void delete(@IdParam IdType theId) {
        subscriptionService.removeSubscription(theId.getIdPart());
    }
}
