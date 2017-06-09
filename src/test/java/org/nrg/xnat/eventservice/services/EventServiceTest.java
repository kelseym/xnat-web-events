package org.nrg.xnat.eventservice.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.xnat.eventservice.model.EventServiceAction;
import org.nrg.xnat.eventservice.model.EventServiceEvent;
import org.nrg.xnat.eventservice.model.EventSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = EventServiceTestConfig.class)
public class EventServiceTest {
    private static final Logger log = LoggerFactory.getLogger(EventServiceTest.class);

    @Autowired private EventService eventService;

    @Before
    public void setUp() throws Exception {


    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void createSubscription() throws Exception {
        EventSubscription eventSubscription = EventSubscription.builder()
                .id(0)
                .name("Test Subscription")
                .eventType("SessionArchiveEvent")
                .consumerType("SessionArchivedDefaultAction")
                .projectId("ABC123")
                .isEnabled(true)
                .build();
        System.out.println(eventSubscription.toString());
        eventService.createSubscription(eventSubscription);

    }

    @Test
    public void listSubscriptions() throws Exception {

    }


    @Test
    public void getInstalledEvents() throws Exception {
        List<EventServiceEvent> events = eventService.getInstalledEvents();
        Integer eventPropertyFileCount = BasicXnatResourceLocator.getResources("classpath*:META-INF/xnat/event/*-xnateventserviceevent.properties").size();
        System.out.println("\nFound " + events.size() + " Event classes:");
        for (EventServiceEvent event : events) {
            System.out.println(event.toString());
        }
        assert(events != null && events.size() == eventPropertyFileCount);
    }

    @Test
    public void getActionProviders() throws Exception {

    }


    @Test
    public void getInstalledActions() throws Exception {
        List<EventServiceAction> actions = eventService.getInstalledActions();
        Integer actionPropertyFileCount = BasicXnatResourceLocator.getResources("classpath*:META-INF/xnat/event/*-xnateventserviceaction.properties").size();
        System.out.println("\nFound " + actions.size() + " Actions:");
        for (EventServiceAction action : actions) {
            System.out.println(action.toString());
        }
        assert(actions != null && actions.size() == actionPropertyFileCount);
    }


}