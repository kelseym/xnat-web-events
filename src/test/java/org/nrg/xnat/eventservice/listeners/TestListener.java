package org.nrg.xnat.eventservice.listeners;

import org.nrg.framework.event.EventI;
import org.nrg.framework.event.XnatEventServiceListener;
import org.nrg.xnat.eventservice.services.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;

@SuppressWarnings("unused")
@XnatEventServiceListener(name = "TestListener", description = "Listener to support testing")
@Service
public class TestListener implements EventServiceListener<EventI> {
    private static final Logger log = LoggerFactory.getLogger(TestListener.class);
    private final EventService eventService;

    @Autowired
    public TestListener(final EventService eventService){
        this.eventService = eventService;

    }


    @Override
    public void accept(Event<EventI> event) {
        try {
            eventService.processEvent(event);
        } catch (Exception e) {
            log.error("Error occurred when trying to process " + event.toString());
        }
    }


}
