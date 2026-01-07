package com.al.lifelog.service;

import org.hl7.fhir.r4.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

@Service
public class SubscriptionService {

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;

    @Autowired
    public SubscriptionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void registerSubscription(Subscription subscription) {
        if (!subscription.hasId()) {
            subscription.setId(java.util.UUID.randomUUID().toString());
        }
        subscriptions.put(subscription.getIdElement().getIdPart(), subscription);
    }

    public void removeSubscription(String id) {
        subscriptions.remove(id);
    }

    @Async("taskExecutor")
    public void notifySubscribers(String resourceType, String action, String jsonBody) {
        subscriptions.values().stream()
                .filter(sub -> sub.getStatus() == Subscription.SubscriptionStatus.ACTIVE)
                .filter(sub -> matchesCriteria(sub.getCriteria(), resourceType))
                .forEach(sub -> sendNotification(sub, action, jsonBody));
    }

    private boolean matchesCriteria(String criteria, String resourceType) {
        if (criteria == null)
            return false;
        return criteria.startsWith(resourceType);
    }

    private void sendNotification(Subscription sub, String action, String jsonBody) {
        if (sub.getChannel().getType() == Subscription.SubscriptionChannelType.RESTHOOK) {
            String url = sub.getChannel().getEndpoint();
            try {
                restTemplate.postForEntity(url, jsonBody, String.class);
            } catch (Exception e) {
                // Log failure (placeholder for real logging)
                System.err.println("Failed to notify subscription " + sub.getId() + ": " + e.getMessage());
            }
        }
    }
}
