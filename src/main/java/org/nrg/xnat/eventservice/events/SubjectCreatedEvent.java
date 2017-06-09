package org.nrg.xnat.eventservice.events;

import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;

@Service
@XnatEventServiceEvent(
        name="SubjectCreatedEvent",
        displayName = "Subject Created Event",
        description="Subject Created Event",
        object = "Subject",
        operation = "Created")
public class SubjectCreatedEvent extends SimpleEventServiceEvent<SubjectCreatedEvent, XnatSubjectdata> {

    public SubjectCreatedEvent(){};

    public SubjectCreatedEvent(XnatSubjectdata payload, UserI eventUser) {
        super(payload, eventUser);
    }


    @Override
    public String getDisplayName() {
        return "SubjectCreatedEvent";
    }

    @Override
    public String getDescription() {
        return "Subject Created Event";
    }
}
