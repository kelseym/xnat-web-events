package org.nrg.xnat.eventservice.actions;

import com.google.common.base.Joiner;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.model.Action;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.EventServiceActionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SingleActionProvider implements  EventServiceActionProvider {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    public abstract List<String> getAttributeKeys();

    public Action getAction() {
        return Action.builder().id(getName())
                        .actionKey(getActionKey() )
                        .displayName(getDisplayName())
                        .description(getDescription())
                        .provider(this)
                        .events(getEvents())
                        .attributes(getAttributeKeys())
                        .build();

    }

    @Override
    public String getName() { return this.getClass().getName(); }

    @Override
    public List<Action> getActions(UserI user) {
        return new ArrayList<>(Arrays.asList(getAction()));
    }

    @Override
    public List<Action> getActions(String xnatType, UserI user) {
        return getActions(user);
    }

    @Override
    public List<Action> getActions(String projectId, String xnatType, UserI user) {
        return getActions(user);
    }

    public String getActionKey() {
        return Joiner.on(':').join(getName(), this.getName()
        );
    }


}
