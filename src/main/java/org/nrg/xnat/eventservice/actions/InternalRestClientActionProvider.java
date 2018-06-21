package org.nrg.xnat.eventservice.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.model.Action;
import org.nrg.xnat.eventservice.model.Subscription;
import org.nrg.xnat.eventservice.services.SubscriptionDeliveryEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class InternalRestClientActionProvider extends MultiActionProvider {
    private final String DISPLAY_NAME = "Internal REST Client Action Provider";
    private final String DESCRIPTION = "This Action Provider facilitates linking Event Service events to internal REST endpoints.";

    private static final Logger log = LoggerFactory.getLogger(InternalRestClientActionProvider.class);

    private final ObjectMapper mapper;
    private SubscriptionDeliveryEntityService subscriptionDeliveryEntityService;

    @Autowired
    public InternalRestClientActionProvider(ObjectMapper mapper,
                                            SubscriptionDeliveryEntityService subscriptionDeliveryEntityService) {
        this.mapper = mapper;
        this.subscriptionDeliveryEntityService = subscriptionDeliveryEntityService;
    }

    @Override
    public String getDisplayName() { return DISPLAY_NAME; }

    @Override
    public String getDescription() { return DESCRIPTION; }

    @Override
    public List<Action> getAllActions() {
        return null;
    }

    @Override
    public List<Action> getActions(String projectId, String xnatType, UserI user) {
        return null;
    }

    @Override
    public Boolean isActionAvailable(String actionKey, String projectId, String xnatType, UserI user) {
        return null;
    }

    @Override
    public void processEvent(EventServiceEvent event, Subscription subscription, UserI user,
                             Long deliveryId) {

    }
}
