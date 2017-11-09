package org.nrg.xnat.eventservice.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.xnat.eventservice.config.EventServiceTestConfig;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = EventServiceTestConfig.class)
public class EventServiceTest {
    private static final Logger log = LoggerFactory.getLogger(EventServiceTest.class);

    @Autowired private EventService eventService;
    @Autowired private EventSubscriptionEntityService eventSubscriptionEntityService;
    @Autowired private ContextService contextService;
    @Autowired private EventServiceComponentManager componentManager;

    private EventSubscriptionCreator eventSubscription;

    @Before
    public void setUp() throws Exception {
        EventFilter eventServiceFilter = EventFilter.builder()
                                                    .addProjectId("PROJECTID-1")
                                                    .addProjectId("PROJECTID-2")
                                                    .build();

        eventSubscription = EventSubscriptionCreator.builder()
                .name("TestSubscription")
                .eventFilter(eventServiceFilter)
                .eventType("ScanArchiveEvent")
                .actAsEventUser(false)
                .actionProvider("LoggingAction")
                .active(true)
                .build();

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void checkContext() throws Exception {
        assertThat(contextService.getBean("testListener"), not(nullValue()));
    }

    @Test
    public void checkDatabaseConnection() throws Exception {
        List<SubscriptionEntity> entities = eventSubscriptionEntityService.getAll();

    }

    @Test
    public void listSubscriptions() throws Exception {

    }

    @Test
    public void getInstalledEvents() throws Exception {
        List<Event> events = eventService.getInstalledEvents();
        Integer eventPropertyFileCount = BasicXnatResourceLocator.getResources("classpath*:META-INF/xnat/event/*-xnateventserviceevent.properties").size();
        System.out.println("\nFound " + events.size() + " Event classes:");
        for (Event event : events) {
            System.out.println(event.toString());
        }
        assert(events != null && events.size() == eventPropertyFileCount);
    }

    @Test
    public void getActionProviders() throws Exception {
        System.out.println("Installed Action Providers\n"); componentManager.getInstalledActions();
        assertThat("componentManager.getActionProviders() should not be null.", componentManager.getActionProviders(), notNullValue());
        assertThat("componentManager.getActionProviders() should not be empty.", componentManager.getActionProviders().size(), not(equalTo(0)));
        for(Action provider:componentManager.getInstalledActions()) {
            System.out.println(provider.toString());
        }

    }


    @Test
    public void getInstalledActions() throws Exception {
        List<Action> actions = eventService.getInstalledActions();
        Integer actionPropertyFileCount = BasicXnatResourceLocator.getResources("classpath*:META-INF/xnat/event/*-xnateventserviceaction.properties").size();
        System.out.println("\nFound " + actions.size() + " Actions:");
        for (Action action : actions) {
            System.out.println(action.toString());
        }
        assert(actions != null && actions.size() == actionPropertyFileCount);
    }

    @Test
    public void getInstalledListeners() throws Exception {
        List<EventServiceListener> listeners = eventService.getInstalledListeners();
        assertThat(listeners, not(nullValue()));
    }

    @Test
    public void saveSubscriptionEntity() throws Exception {
        EventSubscription subscription = eventSubscriptionEntityService.save(EventSubscription.create(eventSubscription));
        assertThat("EventSubscriptionEntityService.save should not create a null entity.", subscription, not(nullValue()));
        assertThat("Saved subscription entity should have been assigned a database ID.", subscription.id(), not(nullValue()));
        assertThat("Pojo name mis-match", subscription.name(), containsString(eventSubscription.name()));
        assertThat("Pojo actionService mis-match", subscription.actionProvider(), containsString(eventSubscription.actionProvider()));
        assertThat("Pojo active-status mis-match", subscription.active(), is(eventSubscription.active()));
        assertThat("Pojo eventListenerFilter mis-match", subscription.eventFilter(), equalTo(eventSubscription.eventFilter()));

        SubscriptionEntity entity = eventSubscriptionEntityService.get(subscription.id());
        assertThat(entity, not(nullValue()));
    }


    @Test
    public void activateAndSaveSubscriptions() throws Exception {
        EventSubscription subscription1 = eventSubscriptionEntityService.activateAndSave(EventSubscription.create(eventSubscription));
        assertThat(subscription1, not(nullValue()));
        assertThat(subscription1.listenerRegistrationKey(), not(nullValue()));

        EventSubscription subscription2 = eventSubscriptionEntityService.activateAndSave(EventSubscription.create(eventSubscription));
        assertThat("Subscription 2 needs a non-null ID", subscription2.id(), not(nullValue()));
        assertThat("Subscription 1 and 2 need unique IDs", subscription2.id(), not(is(subscription1.id())));
        assertThat("Subscription 1 and 2 should have unique registration keys.", subscription2.listenerRegistrationKey().toString(), not(containsString(subscription1.listenerRegistrationKey().toString())));
    }

    @Test
    public void deleteSubscriptionEntity() throws Exception {
        EventSubscription subscription1 = eventSubscriptionEntityService.activateAndSave(EventSubscription.create(eventSubscription));
        EventSubscription subscription2 = eventSubscriptionEntityService.activateAndSave(EventSubscription.create(eventSubscription));
        assertThat("Expected two subscriptions in database.", eventSubscriptionEntityService.getAll().size(), equalTo(2));

        eventSubscriptionEntityService.delete(subscription1);
        assertThat("Expected one subscription in database after deleting one.", eventSubscriptionEntityService.getAll().size(), equalTo(1));
        assertThat("Expected remaining subscription ID to match entity not deleted.", eventSubscriptionEntityService.get(subscription2.id()).getId(), equalTo(subscription2.id()));
    }

    @Test
    public void updateSubscriptionEntity() throws Exception {

    }



}