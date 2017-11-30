package org.nrg.xnat.eventservice.events;

import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xnat.eventservice.model.xnat.XnatModelObject;

@XnatEventServiceEvent
public class SampleEvent implements EventServiceEvent {

    @Override
    public String getId() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Object getObject() {
        return null;
    }

    @Override
    public XnatModelObject getModelObject() {
        return null;
    }

    @Override
    public String getObjectClass() {
        return null;
    }

    @Override
    public String getPayloadXnatType() {
        return null;
    }

    @Override
    public Boolean isPayloadXsiType() {
        return null;
    }
}