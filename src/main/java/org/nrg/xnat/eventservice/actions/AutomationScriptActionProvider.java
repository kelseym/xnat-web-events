package org.nrg.xnat.eventservice.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nrg.automation.entities.Script;
import org.nrg.automation.services.ScriptRunnerService;
import org.nrg.automation.services.ScriptService;
import org.nrg.automation.services.ScriptTriggerService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.model.Action;
import org.nrg.xnat.eventservice.model.Subscription;
import org.nrg.xnat.eventservice.services.SubscriptionDeliveryEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AutomationScriptActionProvider extends MultiActionProvider{
    private final String DISPLAY_NAME = "Automation Script";
    private final String DESCRIPTION = "This Action Provider facilitates linking Event Service to configured Automation scripts.";

    private final ScriptService scriptService;
    private final ScriptTriggerService scriptTriggerService;
    private final ScriptRunnerService scriptRunnerService;
    private final ObjectMapper mapper;
    private SubscriptionDeliveryEntityService subscriptionDeliveryEntityService;

    @Autowired
    public AutomationScriptActionProvider(ScriptService scriptService,
                                          ScriptTriggerService scriptTriggerService,
                                          ScriptRunnerService scriptRunnerService,
                                          ObjectMapper mapper,
                                          SubscriptionDeliveryEntityService subscriptionDeliveryEntityService) {
        this.scriptService = scriptService;
        this.scriptTriggerService = scriptTriggerService;
        this.scriptRunnerService = scriptRunnerService;
        this.mapper = mapper;
        this.subscriptionDeliveryEntityService = subscriptionDeliveryEntityService;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public List<Action> getAllActions() {
        List<Action> actions = new ArrayList<>();
        List<Script> scripts = scriptService.getAll();
        for (Script script: scripts) {
            actions.add(Action.builder().id(script.getScriptId())
                              .displayName(script.getScriptLabel())
                              .description(script.getDescription())
                              .provider(this)
                              .actionKey(actionIdToActionKey(script.getScriptId()))
                              .build());
        }
        return actions;
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
