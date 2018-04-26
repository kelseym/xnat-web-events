package org.nrg.xnat.eventservice.services;

import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.model.JsonPathFilterNode;
import org.nrg.xnat.eventservice.model.EventPropertyNode;

import javax.annotation.Nonnull;
import java.util.Map;

public interface EventPropertyService {

    Boolean matchFilter(Object eventPayloadObject, String jsonPathFilter);

    String serializePayloadObject(Object eventPayloadObject, UserI user);

    Map<String, JsonPathFilterNode> generateEventFilterNodes(EventServiceEvent event);

    Map<String, EventPropertyNode> generateEventPropertyKeys(EventServiceEvent event);

    Map<String, JsonPathFilterNode> generateEventFilterNodes(@Nonnull Class eventPayloadClass);

    String generateJsonPathFilter(Map<String, JsonPathFilterNode> filterNodeMap);

}
