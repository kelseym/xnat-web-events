package org.nrg.xnat.eventservice.events;


import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@XnatEventServiceEvent(name="SessionEvent")
public class SessionEvent extends CombinedEventServiceEvent<SessionEvent, XnatImagesessiondataI> {

    public enum Status {CREATED, UPDATED, DELETED};

    public SessionEvent(){};

    public SessionEvent(final XnatImagesessiondataI payload, final String eventUser, final Status status, final String projectId) {
        super(payload, eventUser, status, projectId);
    }


    @Override
    public String getDisplayName() {
        return "Session Event";
    }

    @Override
    public String getDescription() {
        return "Session created, updated, or deleted.";
    }

    @Override
    public String getPayloadXnatType() {
        return "xnat:imageSessionData";
    }

    @Override
    public Boolean isPayloadXsiType() {
        return true;
    }

    @Override
    public List<String> getStatiStates() { return Arrays.stream(Status.values()).map(Status::name).collect(Collectors.toList()); }

    @Override
    public EventServiceListener getInstance() {
        return new SessionEvent();
    }
}
