package org.nrg.xnat.eventservice.listeners;

import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.services.NrgEventService;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.event.entities.WorkflowStatusEvent;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.ProjectCreatedEvent;
import org.nrg.xnat.eventservice.events.SessionArchiveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import static reactor.bus.selector.Selectors.type;

@Service
@SuppressWarnings("unused")
public class TmpSessionTransferredListener implements Consumer<Event<WorkflowStatusEvent>> {
    private static final Logger log = LoggerFactory.getLogger(TmpSessionTransferredListener.class);

    private final NrgEventService eventService;

    @Autowired
    public TmpSessionTransferredListener(final EventBus eventBus, final NrgEventService eventService) {
        eventBus.on(type(WorkflowStatusEvent.class), this);
        this.eventService = eventService;
    }

    //*
    // Translate "Transferred" workflow event into SessionArchivedEvent for workflow events containing Session type
    //*
    @Override
    public void accept(Event<WorkflowStatusEvent> event) {
        final WorkflowStatusEvent wfsEvent = event.getData();

        if (StringUtils.equals(wfsEvent.getEventId(), "Transferred") && wfsEvent.getEntityType().contains("Session")) {
            try {
                final UserI user = Users.getUser(wfsEvent.getUserId());
                final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(wfsEvent.getEntityId(), user, true);

                // Trigger Session Archived Lifecycle event from here until we figure out where to launch the event.
                // Manually build event label
                String eventLabel = "ProjectId:" + session.getProject();
                eventService.triggerEvent(eventLabel, new SessionArchiveEvent(session, user), false);
                log.error("Firing SessionArchiveEvent for EventLabel: " + eventLabel);


            } catch (UserNotFoundException e) {
                log.warn("The specified user was not found: {}", wfsEvent.getUserId());
            } catch (UserInitException e) {
                log.error("An error occurred trying to retrieve the user for a workflow event: " + wfsEvent.getUserId(), e);
            }
        } else if (StringUtils.equals(wfsEvent.getEventId(), "Added Project") && wfsEvent.getEntityType().contains("projectData")) {
            try {
                final UserI user = Users.getUser(wfsEvent.getUserId());
                final String projectId = wfsEvent.getEntityId();
                final XnatProjectdata projectData = XnatProjectdata.getProjectByIDorAlias(projectId, user, false);

                // Trigger Session Archived Lifecycle event from here until we figure out where to launch the event.
                // Manually build event label
                String eventLabel = "ProjectId:" + projectId;
                eventService.triggerEvent(eventLabel, new ProjectCreatedEvent(projectData, user), false);
                log.error("Firing New Project for EventLabel: " + "XXX");


            } catch (UserNotFoundException e) {
                log.warn("The specified user was not found: {}", wfsEvent.getUserId());
            } catch (UserInitException e) {
                log.error("An error occurred trying to retrieve the user for a workflow event: " + wfsEvent.getUserId(), e);
            }

        }
    }
}