package org.nrg.xnat.eventservice.listeners;

import org.nrg.framework.event.XnatEventServiceListener;
import org.nrg.xnat.eventservice.events.SessionArchiveEvent;
import org.nrg.xnat.eventservice.services.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;

@SuppressWarnings("unused")
@XnatEventServiceListener(name = "SessionArchivedDefaultListener", description = "Placeholder event listener for Session Archive Event")
@Service
public class SessionArchivedDefaultListener implements EventServiceListener<SessionArchiveEvent> {
    private static final Logger log = LoggerFactory.getLogger(SessionArchivedDefaultListener.class);
    private final EventService eventService;

    @Autowired
    public SessionArchivedDefaultListener(final EventService eventService){
        this.eventService = eventService;

    }


    @Override
    public void accept(Event<SessionArchiveEvent> event) {
        try {
            eventService.processEvent(event);
        } catch (Exception e) {
            log.error("Error occurred when trying to process " + event.toString());
        }
    }


    @Override
    public String getEventType() {
        return null;
    }
}
