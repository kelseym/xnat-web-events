package org.nrg.xnat.eventservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.xnat.eventservice.actions.SingleActionProvider;
import org.nrg.xnat.eventservice.config.EventServiceTestConfig;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.events.SampleEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.listeners.TestListener;
import org.nrg.xnat.eventservice.model.*;
import org.nrg.xnat.eventservice.model.xnat.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static reactor.bus.selector.Selectors.matchAll;
import static reactor.bus.selector.Selectors.type;


@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = EventServiceTestConfig.class)
public class EventServiceTest {
    private static final Logger log = LoggerFactory.getLogger(EventServiceTest.class);

    @Autowired private EventBus eventBus;
    @Autowired private TestListener testListener;
    @Autowired @Lazy private EventService eventService;
    @Autowired private EventService mockEventService;
    @Autowired private EventSubscriptionEntityService eventSubscriptionEntityService;
    @Autowired private ContextService contextService;
    @Autowired private EventServiceComponentManager componentManager;
    @Autowired private EventServiceComponentManager mockComponentManager;
    @Autowired private ActionManager actionManager;
    @Autowired private ActionManager mockActionManager;
    @Autowired private ObjectMapper objectMapper;

    private SubscriptionCreator projectCreatedSubscription;
    private Scan mrScan1 = new Scan();
    private Scan mrScan2 = new Scan();
    private Scan ctScan1 = new Scan();


    @Before
    public void setUp() throws Exception {
        EventFilter eventServiceFilter = EventFilter.builder()
                                                    .addProjectId("PROJECTID-1")
                                                    .addProjectId("PROJECTID-2")
                                                    .build();

        projectCreatedSubscription = SubscriptionCreator.builder()
                                                        .name("TestSubscription")
                                                        .active(true)
                                                        .eventId("org.nrg.xnat.eventservice.events.ProjectCreatedEvent")
                                                        .customListenerId("org.nrg.xnat.eventservice.listeners.TestListener")
                                                        .actionKey("org.nrg.xnat.eventservice.actions.EventServiceLoggingAction:org.nrg.xnat.eventservice.actions.EventServiceLoggingAction")
                                                        .eventFilter(eventServiceFilter)
                                                        .actAsEventUser(false)
                                                        .build();
        mrScan1.setId("1111");
        mrScan1.setLabel("TestLabel");
        mrScan1.setXsiType("xnat:Scan");
        mrScan1.setNote("Test note.");
        mrScan1.setModality("MR");
        mrScan1.setIntegerId(1111);
        mrScan1.setProjectId("PROJECTID-1");
        mrScan1.setSeriesDescription("This is the description of a series which is this one.");

        mrScan2.setId("2222");
        mrScan2.setLabel("TestLabel");
        mrScan2.setXsiType("xnat:Scan");
        mrScan2.setNote("Test note.");
        mrScan2.setModality("MR");
        mrScan2.setIntegerId(2222);
        mrScan2.setProjectId("PROJECTID-2");
        mrScan2.setSeriesDescription("This is the description of a series which is this one.");

        ctScan1.setId("3333");
        ctScan1.setLabel("TestLabel");
        ctScan1.setXsiType("xnat:Scan");
        ctScan1.setNote("Test note.");
        ctScan1.setModality("CT");
        ctScan1.setIntegerId(3333);
        ctScan1.setProjectId("PROJECTID-1");
        ctScan1.setSeriesDescription("This is the description of a series which is this one.");


        List<EventServiceActionProvider> mockProviders = new ArrayList<>();
        mockProviders.add(new MockSingleActionProvider());
        when(mockComponentManager.getActionProviders()).thenReturn(mockProviders);

        when(mockComponentManager.getInstalledEvents()).thenReturn(new ArrayList<>(Arrays.asList(new SampleEvent())));

        when(mockComponentManager.getInstalledListeners()).thenReturn(new ArrayList<>(Arrays.asList(new TestListener())));

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
        assertThat(entities, is(not(nullValue())));
    }

    @Test
    public void listSubscriptions() throws Exception {

    }

    @Test
    public void filterSerializedModelObjects() throws Exception {
        String mrFilter = "$[?(@.modality == \"MR\")]";
        String ctFilter = "$[?(@.modality == \"CT\")]";
        String mrCtProj2Filter = "$[?(@.project-id == \"PROJECTID-2\" && (@.modality == \"MR\" || @.modality == \"CT\"))]";
        String proj2Filter = "$[?(@.project-id == \"PROJECTID-2\" && (@.modality == \"MR\" || @.modality == \"CT\"))]";

        assertThat(objectMapper.canSerialize(Scan.class), is(true));
        String jsonMrScan1 = objectMapper.writeValueAsString(mrScan1);
        String jsonMrScan2 = objectMapper.writeValueAsString(mrScan2);
        String jsonCtScan1 = objectMapper.writeValueAsString(ctScan1);


        List<String> match = JsonPath.parse(jsonMrScan1).read(mrFilter);
        assertThat("JsonPath result should not be null", match, notNullValue());
        assertThat("JsonPath match result should not be empty", match, is(not(empty())));

        List<String> mismatch = JsonPath.parse(jsonCtScan1).read(mrFilter);
        assertThat("JsonPath result should not be null", mismatch, notNullValue());
        assertThat("JsonPath mismatch result should be empty" + mismatch, mismatch, is(empty()));

        match = JsonPath.parse(jsonMrScan2).read(mrCtProj2Filter);
        assertThat("JsonPath result should not be null", match, notNullValue());
        assertThat("JsonPath match result should not be empty", match, is(not(empty())));

        mismatch = JsonPath.parse(jsonMrScan1).read(mrCtProj2Filter);
        assertThat("JsonPath result should not be null", mismatch, notNullValue());
        assertThat("JsonPath mismatch result should be empty: " + mismatch, mismatch, is(empty()));


    }

    @Test
    public void listenForEverything() throws Exception {

        // Detect all EventServiceEvent type events
        Selector selector = matchAll();
        eventBus.on(selector, testListener);
    }

    @Test
    public void getInstalledEvents() throws Exception {
        List<SimpleEvent> events = eventService.getEvents();
        Integer eventPropertyFileCount = BasicXnatResourceLocator.getResources("classpath*:META-INF/xnat/eventId/*-xnateventserviceevent.properties").size();
        System.out.println("\nFound " + events.size() + " Event classes:");
        for (SimpleEvent event : events) {
            System.out.println(event.toString());
        }
        assert(events != null && events.size() == eventPropertyFileCount);
    }

    @Test
    public void getInstalledActionProviders() throws Exception {
        System.out.println("Installed Action Providers\n");
        assertThat("componentManager.getActionProviders() should not be null.", componentManager.getActionProviders(), notNullValue());
        assertThat("componentManager.getActionProviders() should not be empty.", componentManager.getActionProviders().size(), not(equalTo(0)));
        for(EventServiceActionProvider provider:componentManager.getActionProviders()) {
            System.out.println(provider.toString());
        }

    }

    @Test
    public void getInstalledActions() throws Exception {
        List<Action> actions = eventService.getAllActions(null);
        System.out.println("\nFound " + actions.size() + " Actions:");
        for (Action action : actions) {
            System.out.println(action.toString());
        }
        assert(actions != null && actions.size() > 0);
    }

    @Test
    public void createSubscription() throws Exception {
        List<SimpleEvent> events = mockEventService.getEvents();
        assertThat("eventService.getEvents() should not return a null list", events, notNullValue());
        assertThat("eventService.getEvents() should not return an empty list", events, is(not(empty())));

        List<Action> actions = mockEventService.getAllActions(null);
        assertThat("eventService.getAllActions() should not return a null list", actions, notNullValue());
        assertThat("eventService.getAllActions() should not return an empty list", actions, is(not(empty())));

        List<EventServiceListener> listeners = mockComponentManager.getInstalledListeners();
        assertThat("componentManager.getInstalledListeners() should not return a null list", listeners, notNullValue());
        assertThat("componentManager.getInstalledListeners() should not return an empty list", listeners, is(not(empty())));

        String eventId = events.get(0).id();
        String actionId = actions.get(0).id();
        String actionKey = actions.get(0).actionKey();
        String listenerType = listeners.get(0).getClass().getCanonicalName();

        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder().name("Test Subscription")
                                                                     .active(true)
                                                                     .eventId(eventId)
                                                                     .customListenerId(listenerType)
                                                                     .actionKey(actionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();

        Subscription subscription = Subscription.create(subscriptionCreator);
        assertThat("Created subscription should not be null", subscription, notNullValue());

        Subscription savedSubscription = mockEventService.createSubscription(subscription);
        assertThat("eventService.createSubscription() should not return null", savedSubscription, notNullValue());
        assertThat("subscription id should not be null", savedSubscription.id(), notNullValue());
        assertThat("subscription id should not be zero", savedSubscription.id(), not(0));
        assertThat("subscription registration key should not be null", savedSubscription.listenerRegistrationKey(), notNullValue());
        assertThat("subscription registration key should not be empty", savedSubscription.listenerRegistrationKey(), not(""));

    }

    @Test
    public void saveSubscriptionEntity() throws Exception {
        Subscription subscription = eventSubscriptionEntityService.save(Subscription.create(projectCreatedSubscription));
        assertThat("EventSubscriptionEntityService.save should not create a null entity.", subscription, not(nullValue()));
        assertThat("Saved subscription entity should have been assigned a database ID.", subscription.id(), not(nullValue()));
        assertThat("Pojo name mis-match", subscription.name(), containsString(projectCreatedSubscription.name()));
        assertThat("Pojo actionService mis-match", subscription.actionKey(), containsString(projectCreatedSubscription.actionKey()));
        assertThat("Pojo active-status mis-match", subscription.active(), is(projectCreatedSubscription.active()));
        assertThat("Pojo eventListenerFilter mis-match", subscription.eventFilter(), equalTo(projectCreatedSubscription.eventFilter()));

        SubscriptionEntity entity = eventSubscriptionEntityService.get(subscription.id());
        assertThat(entity, not(nullValue()));
    }


    @Test
    public void activateAndSaveSubscriptions() throws Exception {
        Subscription subscription1 = eventSubscriptionEntityService.activateAndSave(Subscription.create(projectCreatedSubscription));
        assertThat(subscription1, not(nullValue()));
        assertThat(subscription1.listenerRegistrationKey(), not(nullValue()));

        Subscription subscription2 = eventSubscriptionEntityService.activateAndSave(Subscription.create(projectCreatedSubscription));
        assertThat("Subscription 2 needs a non-null ID", subscription2.id(), not(nullValue()));
        assertThat("Subscription 1 and 2 need unique IDs", subscription2.id(), not(is(subscription1.id())));
        assertThat("Subscription 1 and 2 should have unique registration keys.", subscription2.listenerRegistrationKey().toString(), not(containsString(subscription1.listenerRegistrationKey().toString())));
    }

    @Test
    public void deleteSubscriptionEntity() throws Exception {
        Subscription subscription1 = eventSubscriptionEntityService.activateAndSave(Subscription.create(projectCreatedSubscription));
        Subscription subscription2 = eventSubscriptionEntityService.activateAndSave(Subscription.create(projectCreatedSubscription));
        assertThat("Expected two subscriptions in database.", eventSubscriptionEntityService.getAll().size(), equalTo(2));

        eventSubscriptionEntityService.delete(subscription1);
        assertThat("Expected one subscription in database after deleting one.", eventSubscriptionEntityService.getAll().size(), equalTo(1));
        assertThat("Expected remaining subscription ID to match entity not deleted.", eventSubscriptionEntityService.get(subscription2.id()).getId(), equalTo(subscription2.id()));
    }

    @Test
    public void updateSubscriptionEntity() throws Exception {

    }

    @Test
    public void testGetComponents() throws Exception {
        List<EventServiceEvent> installedEvents = componentManager.getInstalledEvents();
        assertThat("componentManager.getInstalledEvents should not return a null list", installedEvents, notNullValue());
        assertThat("componentManager.getInstalledEvents should not return an empty list", installedEvents, is(not(empty())));

        List<EventServiceActionProvider> actionProviders = componentManager.getActionProviders();
        assertThat("componentManger.getActionProviders() should not return null list of action providers", actionProviders, notNullValue());
        assertThat("componentManger.getActionProviders() should not return empty list of action providers", actionProviders, is(not(empty())));

        actionProviders = actionManager.getActionProviders();
        assertThat("actionManager.getActionProviders() should not return null list of action providers", actionProviders, notNullValue());
        assertThat("actionManager.getActionProviders() should not return empty list of action providers", actionProviders, is(not(empty())));

        List<SimpleEvent> events = eventService.getEvents();
        assertThat("eventService.getEvents() should not return a null list", events, notNullValue());
        assertThat("eventService.getEvents() should not return an empty list", events, is(not(empty())));

        List<ActionProvider> providers = eventService.getActionProviders();
        assertThat("eventService.getActionProviders() should not return a null list", providers, notNullValue());
        assertThat("eventService.getActionProviders() should not return an empty list", providers, is(not(empty())));

        List<Action> allActions = eventService.getAllActions(null);
        assertThat("eventService.getAllActions() should not return a null list", allActions, notNullValue());
        assertThat("eventService.getAllActions() should not return an empty list", allActions, is(not(empty())));


    }

    // ** Async Tests ** //

    @Test
    public void testSampleEvent() throws InterruptedException {
        MockConsumer consumer = new MockConsumer();

        Selector selector = type(SampleEvent.class);
        // Register with Reactor
        eventBus.on(selector, consumer);

        // Trigger event
        EventServiceEvent event = new SampleEvent();
        eventBus.notify(event, Event.wrap(event));

        // wait for consumer (max 1 sec.)
        synchronized (consumer) {
            consumer.wait(1000);
        }

        assertThat("Time-out waiting for eventId", consumer.getEvent(), is(notNullValue()));
    }

    @Test
    public void catchSubscribedEvent() throws Exception {
        createSubscription();

        // Trigger event
        EventServiceEvent event = new SampleEvent();
        eventBus.notify(event, Event.wrap(event));

        // wait for listener (max 1 sec.)
        synchronized (testListener) {
            testListener.wait(1000);
        }

        assertThat("List of detected events should not be null.",testListener.getDetectedEvents(), notNullValue());
        assertThat("List of detected events should not be empty.",testListener.getDetectedEvents().size(), not(0));

    }


    class MockSingleActionProvider extends SingleActionProvider {

        @Override
        public List<String> getAttributeKeys() {
            return null;
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
        public List<String> getEvents() {
            return null;
        }

        @Override
        public void processEvent(EventServiceEvent event, SubscriptionEntity subscription) {

        }
    }

    @Service
    class MockConsumer implements EventServiceListener<SampleEvent> {
        private SampleEvent event;

        public EventServiceEvent getEvent() {
            return event;
        }

        @Override
        public String getEventType() {
            return event.getObjectClass();
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
        public void setEventService(EventService eventService) { }

        @Override
        public void accept(Event<SampleEvent> event) {
            this.event = event.getData();
            synchronized (this) {
                notifyAll();
            }
        }

    }



}