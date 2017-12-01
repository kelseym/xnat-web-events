package org.nrg.xnat.eventservice.listeners;

import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.services.NrgEventService;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.event.entities.WorkflowStatusEvent;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.ProjectCreatedEvent;
import org.nrg.xnat.eventservice.events.SessionArchiveEvent;
import org.nrg.xnat.eventservice.events.SubjectCreatedEvent;
import org.nrg.xnat.eventservice.model.EventFilter;
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
public class TmpWorkflowStatusTapListener implements Consumer<Event<WorkflowStatusEvent>> {
    private static final Logger log = LoggerFactory.getLogger(TmpWorkflowStatusTapListener.class);

    private final NrgEventService nrgEventService;

    @Autowired
    public TmpWorkflowStatusTapListener(final EventBus eventBus, final NrgEventService nrgEventService) {
        eventBus.on(type(WorkflowStatusEvent.class), this);
        this.nrgEventService = nrgEventService;
    }

    //*
    // Translate workflow status events into Event Service events for workflow events containing appropriate labels
    //*
    @Override
    public void accept(Event<WorkflowStatusEvent> event) {
        final WorkflowStatusEvent wfsEvent = event.getData();

        if (StringUtils.equals(wfsEvent.getEventId(), "Transferred") && wfsEvent.getEntityType().contains("SessionData")) {
            try {
                final UserI user = Users.getUser(wfsEvent.getUserId());
                final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(wfsEvent.getEntityId(), user, true);

                // Trigger Session Archived Lifecycle event from here until we figure out where to launch the event.
                // Manually build event label
                String filter = EventFilter.builder().addProjectId(session.getProject()).build().toRegexPattern();
                nrgEventService.triggerEvent(filter, new SessionArchiveEvent(session, user), false);
                log.error("Firing SessionArchiveEvent for EventLabel: " + filter);


            } catch (UserNotFoundException e) {
                log.warn("The specified user was not found: {}", wfsEvent.getUserId());
            } catch (UserInitException e) {
                log.error("An error occurred trying to retrieve the user for a workflow event: " + wfsEvent.getUserId(), e);
            }
        } else if (StringUtils.equals(wfsEvent.getEventId(), "Created Resource") && wfsEvent.getEntityType().contains("ScanData")) {
            try {
                final UserI user = Users.getUser(wfsEvent.getUserId());
                final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(wfsEvent.getEntityId(), user, true);

                // Trigger Session Archived Lifecycle event from here until we figure out where to launch the event.
                // Manually build event label
                String filter = EventFilter.builder().addProjectId(session.getProject()).build().toRegexPattern();
                nrgEventService.triggerEvent(filter, new SessionArchiveEvent(session, user), false);
                log.error("Firing SessionArchiveEvent for EventLabel: " + filter);


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
                String filter = EventFilter.builder().addProjectId(projectId).build().toRegexPattern();
                nrgEventService.triggerEvent(filter, new ProjectCreatedEvent(projectData, user), false);
                log.error("Firing New Project for EventLabel: " + filter);


            } catch (UserNotFoundException e) {
                log.warn("The specified user was not found: {}", wfsEvent.getUserId());
            } catch (UserInitException e) {
                log.error("An error occurred trying to retrieve the user for a workflow event: " + wfsEvent.getUserId(), e);
            }

        } else if (StringUtils.equals(wfsEvent.getEventId(), "Added Subject") && wfsEvent.getEntityType().contains("SubjectData")) {
            try {
                final UserI user = Users.getUser(wfsEvent.getUserId());
                final String subjectId = wfsEvent.getEntityId();
                final XnatSubjectdata subjectdata = XnatSubjectdata.getXnatSubjectdatasById(subjectId, user, false);
                String projectId = subjectdata.getFirstProject().getId();

                // Trigger Session Archived Lifecycle event from here until we figure out where to launch the event.
                // Manually build event label
                String filter = EventFilter.builder().addProjectId(projectId).build().toRegexPattern();
                nrgEventService.triggerEvent(filter, new SubjectCreatedEvent(subjectdata, user), false);
                log.error("Firing New Project for EventLabel: " + filter);


            } catch (UserNotFoundException e) {
                log.warn("The specified user was not found: {}", wfsEvent.getUserId());
            } catch (UserInitException e) {
                log.error("An error occurred trying to retrieve the user for a workflow event: " + wfsEvent.getUserId(), e);
            }

        }
    }
}