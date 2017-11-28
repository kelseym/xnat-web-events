package org.nrg.xnat.eventservice.listeners;

import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.services.EventService;
import org.springframework.stereotype.Service;
import reactor.bus.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TestListener implements EventServiceListener<EventServiceEvent> {
    List<EventServiceEvent> detectedEvents = new ArrayList();


    @Override
    public String getEventType() {
        return null;
    }

    @Override
    public EventServiceListener getInstance() {
        return this;
    }

    @Override
    public UUID getListenerId() {
        return null;
    }

    @Override
    public void setEventService(EventService eventService) {

    }

    @Override
    public void accept(Event<EventServiceEvent> event) {
        if( event.getData() instanceof EventServiceEvent)
            detectedEvents.add((EventServiceEvent) event);
    }
}
