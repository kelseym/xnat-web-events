package org.nrg.xnat.eventservice.events;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;

@Service
public class ProjectCreatedEvent extends SimpleEventServiceEvent<ProjectCreatedEvent, XnatProjectdata>  {
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


}
