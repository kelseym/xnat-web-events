package org.nrg.xnat.eventservice.actions;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.EventServiceActionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MultiActionProvider implements EventServiceActionProvider {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public String actionKeyToActionId(String actionKey) {
        return Splitter.on(':').splitToList(actionKey).get(0);
    }

    @Override
    public String actionIdToActionKey(String actionId) {
        return Joiner.on(':').join(actionId, this.getName() );
    }
}