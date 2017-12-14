package org.nrg.xnat.eventservice.initialization.tasks;

import org.nrg.xnat.eventservice.services.EventSubscriptionEntityService;
import org.nrg.xnat.initialization.tasks.AbstractInitializingTask;
import org.nrg.xnat.initialization.tasks.InitializingTaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InitializeSubscriptions extends AbstractInitializingTask {
    private static final Logger log = LoggerFactory.getLogger(InitializeSubscriptions.class);

    private final EventSubscriptionEntityService eventSubscriptionEntityService;

    @Autowired
    public InitializeSubscriptions(final EventSubscriptionEntityService eventSubscriptionEntityService){
        this.eventSubscriptionEntityService = eventSubscriptionEntityService;
    }

    @Override
    public String getTaskName() { return "Register active subscriptions with Reactor."; }

    @Override
    protected void callImpl() throws InitializingTaskException {

        log.debug("Registering all active event subscriptions from SubscriptionEntity table to Reactor.EventBus.");
        eventSubscriptionEntityService.reregisterAllActive();

    }
}
