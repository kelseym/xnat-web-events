package org.nrg.xnat.eventservice.events;

import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.springframework.stereotype.Service;

@Service
@XnatEventServiceEvent(name="ProjectCreatedEvent")
public class ProjectCreatedEvent extends CombinedEventServiceEvent<ProjectCreatedEvent, XnatProjectdata>  {
    final String displayName = "Project Created";
    final String description ="New project created.";

    public ProjectCreatedEvent(){};

    public ProjectCreatedEvent(final XnatProjectdata payload, final UserI user) {
        super(payload, user);
    }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getDescription() { return description; }

    @Override
    public String getPayloadXnatType() {
        return "xnat:projectData";
    }

    @Override
    public Boolean isPayloadXsiType() {
        return true;
    }


    @Override
    public EventServiceListener getInstance() {
        return new ProjectCreatedEvent();
    }
}
