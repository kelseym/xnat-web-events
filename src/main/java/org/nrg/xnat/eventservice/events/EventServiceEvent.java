package org.nrg.xnat.eventservice.events;

import org.nrg.framework.event.EventI;
import org.nrg.xnat.eventservice.model.xnat.XnatModelObject;

public interface EventServiceEvent<ObjectT> extends EventI {

    String getId();
    String getDisplayName();
    String getDescription();
    ObjectT getObject();
    XnatModelObject getModelObject();
    String getObjectClass();
    String getPayloadXnatType();
    Boolean isPayloadXsiType();
    Integer getUser();
}
