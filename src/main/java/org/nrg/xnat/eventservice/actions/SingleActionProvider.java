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
import java.util.Map;

public abstract class SingleActionProvider implements  EventServiceActionProvider {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    public abstract Map<String, String> getAttributes();

    public Action getAction() {
        return Action.builder().id(getName())
                        .actionKey(getActionKey() )
                        .displayName(getDisplayName())
                        .description(getDescription())
                        .provider(this)
                        .events(getEvents())
                        .attributes(getAttributes())
                        .build();

    }

    @Override
    public String getName() { return this.getClass().getSimpleName(); }

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
        return Joiner.on(':').join(getName(), this.getClass().getSimpleName());
    }


}
