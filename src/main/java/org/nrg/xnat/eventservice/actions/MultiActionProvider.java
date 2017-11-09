package org.nrg.xnat.eventservice.actions;

import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.EventServiceActionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MultiActionProvider implements EventServiceActionProvider {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    @Override
    public String getName() {
        return this.getClass().getCanonicalName();
    }

}