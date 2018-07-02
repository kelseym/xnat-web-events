package org.nrg.xnat.eventservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.nrg.framework.services.ContextService;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.actions.EventServiceLoggingAction;
import org.nrg.xnat.eventservice.actions.SingleActionProvider;
import org.nrg.xnat.eventservice.actions.TestAction;
import org.nrg.xnat.eventservice.config.EventServiceTestConfig;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.events.ProjectEvent;
import org.nrg.xnat.eventservice.events.SampleEvent;
import org.nrg.xnat.eventservice.events.ScanEvent;
import org.nrg.xnat.eventservice.events.SessionEvent;
import org.nrg.xnat.eventservice.events.TestCombinedEvent;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.listeners.TestListener;
import org.nrg.xnat.eventservice.model.*;
import org.nrg.xnat.eventservice.model.xnat.Scan;
import org.nrg.xnat.eventservice.model.xnat.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static reactor.bus.selector.Selectors.type;


@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = EventServiceTestConfig.class)
public class EventServiceIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(EventServiceIntegrationTest.class);

    private static final String EVENT_RESOURCE_PATTERN ="classpath*:META-INF/xnat/event/*-xnateventserviceevent.properties";

    private UserI mockUser;

    private final String FAKE_USER = "mockUser";
    private final Integer FAKE_USER_ID = 1234;

    @Autowired private EventBus eventBus;
    @Autowired private TestListener testListener;
    @Autowired private EventServiceActionProvider testAction;
    @Autowired @Lazy private EventService eventService;
    @Autowired private EventService mockEventService;
    @Autowired private EventSubscriptionEntityService eventSubscriptionEntityService;
    @Autowired private ContextService contextService;
    @Autowired private EventServiceComponentManager componentManager;
    @Autowired private EventServiceComponentManager mockComponentManager;
    @Autowired private ActionManager actionManager;
    @Autowired private ActionManager mockActionManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EventServiceLoggingAction mockEventServiceLoggingAction;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;
    @Autowired private SubscriptionDeliveryEntityService mockSubscriptionDeliveryEntityService;


    private SubscriptionCreator project1CreatedSubscription;
    private SubscriptionCreator project2CreatedSubscription;
    private Scan mrScan1 = new Scan();
    private Scan mrScan2 = new Scan();
    private Scan ctScan1 = new Scan();

    private Session mrSession = new Session();
    private Session ctSession = new Session();


    //private Project project1 = new Project("PROJECTID-1", mockUser);
    //private Project project2 = new Project("PROJECTID-2", mockUser);

    //private Subject subject1 = new Subject("SUBJECTID-1", mockUser);
    //private Subject subject2 = new Subject("SUBJECTID-2", mockUser);




    @Before
    public void setUp() throws Exception {

        EventFilter eventServiceFilter = EventFilter.builder().eventType("org.nrg.xnat.eventservice.events.ProjectEvent").status("CREATED").build();

        project1CreatedSubscription = SubscriptionCreator.builder()
                                                         .name("TestSubscription")
                                                         .active(true)
                                                         .projectId("PROJECTID-1")
                                                         .eventType("org.nrg.xnat.eventservice.events.ProjectEvent")
                                                         .customListenerId("org.nrg.xnat.eventservice.listeners.TestListener")
                                                         .actionKey("org.nrg.xnat.eventservice.actions.EventServiceLoggingAction:org.nrg.xnat.eventservice.actions.EventServiceLoggingAction")
                                                         .eventFilter(eventServiceFilter)
                                                         .actAsEventUser(false)
                                                         .build();

        project2CreatedSubscription = SubscriptionCreator.builder()
                                                        .name("TestSubscription2")
                                                        .active(true)
                                                        .projectId("PROJECTID-2")
                                                        .eventType("org.nrg.xnat.eventservice.events.ProjectEvent")
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
        mrScan2.setProjectId("PROJECTID-1");
        mrScan2.setSeriesDescription("This is the description of a series which is this one.");

        ctScan1.setId("3333");
        ctScan1.setLabel("TestLabel");
        ctScan1.setXsiType("xnat:Scan");
        ctScan1.setNote("Test note.");
        ctScan1.setModality("CT");
        ctScan1.setIntegerId(3333);
        ctScan1.setProjectId("PROJECTID-1");
        ctScan1.setSeriesDescription("This is the description of a series which is this one.");


        // Mock the userI
        mockUser = Mockito.mock(UserI.class);
        when(mockUser.getLogin()).thenReturn(FAKE_USER);
        when(mockUser.getID()).thenReturn(FAKE_USER_ID);

        // Mock the user management service
        when(mockUserManagementServiceI.getUser(FAKE_USER)).thenReturn(mockUser);
        when(mockUserManagementServiceI.getUser(FAKE_USER_ID)).thenReturn(mockUser);

        when(mockComponentManager.getActionProviders()).thenReturn(new ArrayList<>(Arrays.asList(new MockSingleActionProvider())));

        when(mockComponentManager.getInstalledEvents()).thenReturn(new ArrayList<>(Arrays.asList(new SampleEvent())));

        when(mockComponentManager.getInstalledListeners()).thenReturn(new ArrayList<>(Arrays.asList(new TestListener())));

        // Mock action
        when(mockEventServiceLoggingAction.getName()).thenReturn("org.nrg.xnat.eventservice.actions.EventServiceLoggingAction");
        when(mockEventServiceLoggingAction.getDisplayName()).thenReturn("MockEventServiceLoggingAction");
        when(mockEventServiceLoggingAction.getDescription()).thenReturn("MockEventServiceLoggingAction");
        when(mockEventServiceLoggingAction.getActions(Matchers.any(String.class),Matchers.any(String.class),Matchers.any(UserI.class))).thenReturn(null);


    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void checkContext() throws Exception {
        assertThat(contextService.getBean("eventService"), not(nullValue()));
    }

    @Test
    public void checkDatabaseConnection() throws Exception {
        List<SubscriptionEntity> entities = eventSubscriptionEntityService.getAll();
        assertThat(entities, is(not(nullValue())));
    }

    @Test
    public void getInstalledEvents() throws Exception {
        List<SimpleEvent> events = eventService.getEvents();
        Integer eventPropertyFileCount = BasicXnatResourceLocator.getResources(EVENT_RESOURCE_PATTERN).size();

        assert(events != null && events.size() == eventPropertyFileCount);
    }

    @Test
    public void getInstalledActionProviders() throws Exception {
        assertThat("componentManager.getActionProviders() should not be null.", componentManager.getActionProviders(), notNullValue());
        assertThat("componentManager.getActionProviders() should not be empty.", componentManager.getActionProviders().size(), not(equalTo(0)));
    }

    @Test
    public void getInstalledActions() throws Exception {
        List<Action> actions = eventService.getAllActions();
        assert(actions != null && actions.size() > 0);
    }

    @Test
    public void getInstalledListeners() throws Exception {
        List<EventServiceListener> listeners = componentManager.getInstalledListeners();
        assertThat(listeners, notNullValue());
        assertThat(listeners, not(is(empty())));
    }

    @Test
    @DirtiesContext
    public void createSubscription() throws Exception {
        List<SimpleEvent> events = eventService.getEvents();
        assertThat("eventService.getEvents() should not return a null list", events, notNullValue());
        assertThat("eventService.getEvents() should not return an empty list", events, is(not(empty())));

        List<Action> actions = eventService.getAllActions();
        assertThat("eventService.getAllActions() should not return a null list", actions, notNullValue());
        assertThat("eventService.getAllActions() should not return an empty list", actions, is(not(empty())));

        List<EventServiceListener> listeners = componentManager.getInstalledListeners();
        assertThat("componentManager.getInstalledListeners() should not return a null list", listeners, notNullValue());
        assertThat("componentManager.getInstalledListeners() should not return an empty list", listeners, is(not(empty())));

        String eventId = events.get(0).id();
        String actionId = actions.get(0).id();
        String actionKey = actions.get(0).actionKey();
        String listenerType = listeners.get(0).getClass().getCanonicalName();

        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder().name("Test Subscription")
                                                                     .active(true)
                                                                     .eventType(eventId)
                                                                     .customListenerId(listenerType)
                                                                     .actionKey(actionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();

        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Created subscription should not be null", subscription, notNullValue());

        eventService.validateSubscription(subscription);

        Subscription savedSubscription = eventService.createSubscription(subscription);
        assertThat("eventService.createSubscription() should not return null", savedSubscription, notNullValue());
        assertThat("subscription id should not be null", savedSubscription.id(), notNullValue());
        assertThat("subscription id should not be zero", savedSubscription.id(), not(0));
        assertThat("subscription registration key should not be null", savedSubscription.listenerRegistrationKey(), notNullValue());
        assertThat("subscription registration key should not be empty", savedSubscription.listenerRegistrationKey(), not(""));

        subscriptionCreator = SubscriptionCreator.builder().name("Test 2 Subscription")
                                                                     .active(true)
                                                                     .eventType(eventId)
                                                                     .customListenerId(listenerType)
                                                                     .actionKey(actionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();

        subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());

        eventService.validateSubscription(subscription);

        Subscription secondSavedSubscription = eventService.createSubscription(subscription);

        assertThat("Subscriptions should have unique listener IDs", savedSubscription.listenerRegistrationKey(), not(secondSavedSubscription.listenerRegistrationKey()));
        assertThat("Subscriptions should have unique IDs", savedSubscription.id(), not(secondSavedSubscription.id()));
    }

    @Test
    @DirtiesContext
    public void createSubscriptionWithBlankName() throws Exception {
        EventServiceEvent testCombinedEvent = componentManager.getEvent("org.nrg.xnat.eventservice.events.TestCombinedEvent");
        assertThat("Could not load TestCombinedEvent from componentManager", testCombinedEvent, notNullValue());

        String eventType = "org.nrg.xnat.eventservice.events.TestCombinedEvent";
        String projectId = "PROJECTID-1";
        EventFilter eventServiceFilterWithJson = EventFilter.builder()
                                                            .eventType(eventType)
                                                            .projectIds(Arrays.asList(projectId))
                                                            .jsonPathFilter("$[?(@.modality == \"MR\")]")
                                                            .build();

        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("")
                                                                     .active(true)
                                                                     .projectId(projectId)
                                                                     .eventType(eventType)
                                                                     .actionKey("org.nrg.xnat.eventservice.actions.TestAction:org.nrg.xnat.eventservice.actions.TestAction")
                                                                     .eventFilter(eventServiceFilterWithJson)
                                                                     .actAsEventUser(false)
                                                                     .build();
        assertThat("Json Filtered SubscriptionCreator builder failed :(", subscriptionCreator, notNullValue());

        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Json Filtered Subscription creation failed :(", subscription, notNullValue());

        List<String> names = new ArrayList<>();
        for(int i=1;i<100;i++){
            Subscription createdSubsciption = eventService.createSubscription(subscription);
            assertThat("eventService.createSubscription() returned a null value", createdSubsciption, not(nullValue()));
            assertThat("Expected subscription to have auto-generated name", createdSubsciption.name(), notNullValue());
            assertThat("Expected subscription to have auto-generated name", createdSubsciption.name(), not(""));
            assertThat("Expected subscription name to be unique", names, not(contains(createdSubsciption.name())));
            names.add(createdSubsciption.name());
        }

    }

    @Test
    public void listSubscriptions() throws Exception {
        createSubscription();
        assertThat("No subscriptions found.", eventService.getSubscriptions(), is(not(empty())));
        assertThat("Two subscriptions expected.", eventService.getSubscriptions().size(), is(2));
        for (Subscription subscription:eventService.getSubscriptions()) {
            assertThat("subscription id is null for " + subscription.name(), subscription.id(), notNullValue());
            assertThat("subscription id is zero (0) for " + subscription.name(), subscription.id(), not(0));
        }
    }

    @Test
    @DirtiesContext
    public void saveSubscriptionEntity() throws Exception {
        Subscription subscription = eventSubscriptionEntityService.save(Subscription.create(project1CreatedSubscription, mockUser.getLogin()));
        assertThat("EventSubscriptionEntityService.save should not create a null entity.", subscription, not(nullValue()));
        assertThat("Saved subscription entity should have been assigned a database ID.", subscription.id(), not(nullValue()));
        assertThat("Pojo name mis-match", subscription.name(), containsString(project1CreatedSubscription.name()));
        assertThat("Pojo actionService mis-match", subscription.actionKey(), containsString(project1CreatedSubscription.actionKey()));
        assertThat("Pojo active-status mis-match", subscription.active(), is(project1CreatedSubscription.active()));
        assertThat("Pojo eventListenerFilter should have been assigned an ID", subscription.eventFilter().id(), notNullValue());
        assertThat("Pojo eventListenerFilter should have been assigned a non-zero ID", subscription.eventFilter().id(), not(0));
        assertThat("Pojo eventListenerFilter.name mis-match", subscription.eventFilter().name(), equalTo(project1CreatedSubscription.eventFilter().name()));
        assertThat("Pojo eventListenerFilter.jsonPathFilter mis-match", subscription.eventFilter().jsonPathFilter(), equalTo(project1CreatedSubscription.eventFilter().jsonPathFilter()));

        SubscriptionEntity entity = eventSubscriptionEntityService.get(subscription.id());
        assertThat(entity, not(nullValue()));
    }

    @Test
    public void validateSubscription() throws Exception {
        Subscription subscription = Subscription.create(project1CreatedSubscription, mockUser.getLogin());
        Subscription validatedSubscription = eventSubscriptionEntityService.validate(subscription);
        assertThat("Sample subscription validation failed.", validatedSubscription, notNullValue());
    }

    @Test
    @DirtiesContext
    public void activateAndSaveSubscriptions() throws Exception {
        Subscription subscription1 = eventSubscriptionEntityService.createSubscription(Subscription.create(project1CreatedSubscription, mockUser.getLogin()));
        assertThat(subscription1, not(nullValue()));
        assertThat(subscription1.listenerRegistrationKey(), not(nullValue()));

        Subscription subscription2 = eventSubscriptionEntityService.createSubscription(Subscription.create(project2CreatedSubscription, mockUser.getLogin()));
        assertThat("Subscription 2 needs a non-null ID", subscription2.id(), not(nullValue()));
        assertThat("Subscription 1 and 2 need unique IDs", subscription2.id(), not(is(subscription1.id())));
        assertThat("Subscription 1 and 2 should have unique registration keys.", subscription2.listenerRegistrationKey().toString(), not(containsString(subscription1.listenerRegistrationKey().toString())));
    }

    @Test
    @DirtiesContext
    public void deleteSubscriptionEntity() throws Exception {
        Subscription subscription1 = eventSubscriptionEntityService.createSubscription(Subscription.create(project1CreatedSubscription, mockUser.getLogin()));
        Subscription subscription2 = eventSubscriptionEntityService.createSubscription(Subscription.create(project2CreatedSubscription, mockUser.getLogin()));
        assertThat("Expected two subscriptions in database.", eventSubscriptionEntityService.getAll().size(), equalTo(2));

        eventSubscriptionEntityService.delete(subscription1.id());
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

        List<Action> allActions = eventService.getAllActions();
        assertThat("eventService.getAllActions() should not return a null list", allActions, notNullValue());
        assertThat("eventService.getAllActions() should not return an empty list", allActions, is(not(empty())));


    }


    @Test
    public void testJsonPathFilterSelector() throws Throwable {
        String eventId = "some.test.EventId";
        List<String> filterProjects = Arrays.asList("ProjectId1", "ProjectId2");
        String eventProject = "ProjectId1";
        String status = "CREATED";

        String signatureJson = objectMapper.writeValueAsString(EventSignature.builder().eventType(eventId).projectId(eventProject).status(status).build());
        Filter filter = EventFilter.builder().eventType(eventId).projectIds(filterProjects).status(status).build().buildReactorFilter();

        List match =  JsonPath.read(signatureJson, "$.[?]", filter);

        assertThat(match, notNullValue());
        assertThat(match.size(), is(not(0)));

        String noProjectSignature = objectMapper.writeValueAsString(
                EventSignature.builder().eventType(eventId).status(status).build());
        Filter noProjectEventFilter = EventFilter.builder().eventType(eventId).status(status).build().buildReactorFilter();

        match =  JsonPath.read(noProjectSignature, "$.[?]", noProjectEventFilter);

        assertThat(match, notNullValue());
        assertThat(match.size(), is(not(0)));

        match = JsonPath.read(signatureJson, "$.[?]", noProjectEventFilter);

        assertThat(match, notNullValue());
        assertThat(match.size(), is(not(0)));

        match = JsonPath.read(noProjectSignature, "$.[?]", filter);

        assertThat(match, notNullValue());
        assertThat("JsonPath filter match should be empty. Event signature contains no project, but projects are specified on filter.",match.size(), is(0));

        String otherProjectSignature = objectMapper.writeValueAsString(
                EventSignature.builder().eventType(eventId).projectId("ProjectId").status(status).build());
        Filter otherProjectEventFilter = EventFilter.builder().eventType(eventId).projectIds(Arrays.asList("SomethingElse", "ADifferentOne")).status(status).build().buildReactorFilter();

        match = JsonPath.read(otherProjectSignature, "$.[?]", filter);

        assertThat(match, notNullValue());
        assertThat("JsonPath filter match should be empty. Event signature contains projectId not contained in filter.",match.size(), is(0));

        match = JsonPath.read(signatureJson, "$.[?]", otherProjectEventFilter);

        assertThat(match, notNullValue());
        assertThat("JsonPath filter match should be empty. Event signature contains projectId not contained in filter.",match.size(), is(0));

        Filter otherStatusEventFilter = EventFilter.builder().eventType(eventId).projectIds(filterProjects).status("DIFFERENT_STATUS").build().buildReactorFilter();
        match = JsonPath.read(signatureJson, "$.[?]", otherStatusEventFilter);

        assertThat(match, notNullValue());
        assertThat("JsonPath filter match should be empty. Event signature and filter contain different status.",match.size(), is(0));

        Filter otherEventIdFilter = EventFilter.builder().eventType("SOME_OTHER_EVENT").projectIds(filterProjects).status(status).build().buildReactorFilter();
        match = JsonPath.read(signatureJson, "$.[?]", otherEventIdFilter);

        assertThat(match, notNullValue());
        assertThat("JsonPath filter match should be empty. Event signature and filter contain different eventIds.",match.size(), is(0));
    }

    // ** Async Tests ** //

    @Test
    @DirtiesContext
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

        assertThat("Time-out waiting for eventType", consumer.getEvent(), is(notNullValue()));
    }

    @Test
    @DirtiesContext
    public void catchSubscribedEvent() throws Exception {
        EventServiceEvent event = new SampleEvent();
        String testActionKey = testAction.getAllActions().get(0).actionKey();

        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder().name("Test Subscription")
                                                                     .active(true)
                                                                     .eventType(event.getId())
                                                                     .customListenerId(testListener.getId())
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        eventService.validateSubscription(subscription);
        Subscription savedSubscription = eventService.createSubscription(subscription);

        // Trigger event
        eventService.triggerEvent(event);

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("List of detected events should not be null.",action.getDetectedEvents(), notNullValue());
        assertThat("List of detected events should not be empty.",action.getDetectedEvents().size(), not(0));

    }

    @Test
    @DirtiesContext
    public void checkSubscriptionDeliveryEntry() throws Exception {
        catchSubscribedEvent();
        List<SubscriptionDelivery> subscriptionDeliveries = eventService.getSubscriptionDeliveries(null, null, null);
        assertThat("subscriptionDeliveries table is null. Expected one entry.",subscriptionDeliveries, notNullValue());
        assertThat("subscriptionDeliveries table is empty. Expected one entry.",subscriptionDeliveries.size(), is(1));

        List<TimedEventStatus> eventStatuses = subscriptionDeliveries.get(0).timedEventStatuses();
        assertThat("TimedEventStatus table is null. Expected entries.",eventStatuses, notNullValue());
        assertThat("", eventStatuses.get(eventStatuses.size()-1).status(), is("ACTION_COMPLETE") );
    }

    @Test
    @DirtiesContext
    public void registerMrSessionSubscription() throws Exception {
        EventServiceEvent testCombinedEvent = componentManager.getEvent("org.nrg.xnat.eventservice.events.TestCombinedEvent");
        assertThat("Could not load TestCombinedEvent from componentManager", testCombinedEvent, notNullValue());

        String projectId = "PROJECTID-1";
        String eventType = "org.nrg.xnat.eventservice.events.TestCombinedEvent";
        EventFilter eventServiceFilterWithJson = EventFilter.builder()
                                                            .eventType(eventType)
                                                            .projectIds(Arrays.asList(projectId))
                                                            .jsonPathFilter("$[?(@.modality == \"MR\")]")
                                                            .build();

        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("FilterTestSubscription")
                                                                     .active(true)
                                                                     .projectId(projectId)
                                                                     .eventType(eventType)
                                                                     .actionKey("org.nrg.xnat.eventservice.actions.TestAction:org.nrg.xnat.eventservice.actions.TestAction")
                                                                     .eventFilter(eventServiceFilterWithJson)
                                                                     .actAsEventUser(false)
                                                                     .build();
        assertThat("Json Filtered SubscriptionCreator builder failed :(", subscriptionCreator, notNullValue());

        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Json Filtered Subscription creation failed :(", subscription, notNullValue());

        Subscription createdSubsciption = eventService.createSubscription(subscription);
        assertThat("eventService.createSubscription() returned a null value", createdSubsciption, not(nullValue()));
        assertThat("Created subscription is missing listener registration key.", createdSubsciption.listenerRegistrationKey(), not(nullValue()));
        assertThat("Created subscription is missing DB id.", createdSubsciption.id(), not(nullValue()));

    }

    @Test
    @DirtiesContext
    public void matchMrSubscriptionToMrSession() throws Exception {
        registerMrSessionSubscription();

        // Test MR Project 1 session - match
        Action testAction = actionManager.getActionByKey("org.nrg.xnat.eventservice.actions.TestAction:org.nrg.xnat.eventservice.actions.TestAction", mockUser);
        assertThat("Could not load TestAction from actionManager", testAction, notNullValue());


        XnatImagesessiondataI session = new XnatImagesessiondataBean();
        session.setModality("MR");
        session.setProject("PROJECTID-1");
        session.setSessionType("xnat:imageSessionData");

        TestCombinedEvent combinedEvent = new TestCombinedEvent(session, mockUser.getLogin(), TestCombinedEvent.Status.CREATED, null);

        eventService.triggerEvent(combinedEvent, "PROJECTID-1");

        // wait for async action (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }

        TestAction actionProvider = (TestAction) testAction.provider();
        assertThat("List of detected events should not be null.",actionProvider.getDetectedEvents(), notNullValue());
        assertThat("List of detected events should not be empty.",actionProvider.getDetectedEvents().size(), not(0));
    }

    @Test
    @DirtiesContext
    public void mismatchProjectIdMrSubscriptionToMrSession() throws Exception {
        registerMrSessionSubscription();

        Action testAction = actionManager.getActionByKey("org.nrg.xnat.eventservice.actions.TestAction:org.nrg.xnat.eventservice.actions.TestAction", mockUser);

        XnatImagesessiondataI session = new XnatImagesessiondataBean();
        session.setModality("MR");
        session.setProject("PROJECTID-2");
        session.setSessionType("xnat:imageSessionData");

        TestCombinedEvent combinedEvent = new TestCombinedEvent(session, mockUser.getLogin(), TestCombinedEvent.Status.CREATED, null);

        eventService.triggerEvent(combinedEvent, "PROJECTID-2");

        // wait for async action (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }

        TestAction actionProvider = (TestAction) testAction.provider();
        assertThat("List of detected events should be empty (Mis-matched Project IDs.", actionProvider.getDetectedEvents(), is(empty()));
    }

    @Test
    @DirtiesContext
    public void testReactivateAllActive() throws Exception {
        // Create a working subscription
        matchMrSubscriptionToMrSession();

        List<Subscription> allSubscriptions1 = eventSubscriptionEntityService.getAllSubscriptions();
        assertThat("Expected one subscription to be created.", allSubscriptions1.size(), is(1));

        final Subscription subscription1 = allSubscriptions1.get(0);
        String regKey1 = subscription1.listenerRegistrationKey();

        eventService.reactivateAllSubscriptions();

        List<Subscription> allSubscriptions2 = eventSubscriptionEntityService.getAllSubscriptions();
        assertThat("Expected only a single subscription after reactivation", allSubscriptions2.size(), is(1));
        final Subscription subscription2 = allSubscriptions2.get(0);
        String regKey2 = subscription2.listenerRegistrationKey();

        assertThat("Expected reactivated subscription to have unique registration key.", regKey1, is(not(regKey2)));


    }

    @Test
    @DirtiesContext
    public void mismatchMrSubscriptionToCtSession() throws Exception {
        registerMrSessionSubscription();

        // Test CT Project 1 session - match
        Action testAction = actionManager.getActionByKey("org.nrg.xnat.eventservice.actions.TestAction:org.nrg.xnat.eventservice.actions.TestAction", mockUser);
        assertThat("Could not load TestAction from actionManager", testAction, notNullValue());


        XnatImagesessiondataI session = new XnatImagesessiondataBean();
        session.setModality("CT");
        session.setProject("PROJECTID-1");
        session.setSessionType("xnat:imageSessionData");

        TestCombinedEvent combinedEvent = new TestCombinedEvent(session, mockUser.getLogin(), TestCombinedEvent.Status.CREATED, null);
        eventService.triggerEvent(combinedEvent, session.getProject());

        // wait for async action (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }

        TestAction actionProvider = (TestAction) testAction.provider();
        assertThat("List of detected events should not be null.",actionProvider.getDetectedEvents(), notNullValue());
        assertThat("List of detected events should be empty.",actionProvider.getDetectedEvents().size(), is(0));
    }



    @Test
    @DirtiesContext
    public void catchSubjectEventWithProjectSubscription() throws Exception {
        // Create a user
        String projectId1 = "PROJECTID_1";
        String subjectID = "TestSubject";
        XnatSubjectdata subject = new XnatSubjectdata(mockUser);
        subject.setProject(projectId1);
        subject.setId(subjectID);

        // Subscribe to event
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("Test Subscription")
                                                                     .projectId(projectId1)
                                                                     .active(true)
                                                                     .eventType(new SubjectEvent().getId())
                                                                     .actionKey(testActionKey)
                                                                     .eventStatus(SubjectEvent.Status.CREATED.name())
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Subscription failed validation.",eventService.validateSubscription(subscription), notNullValue());
        assertThat("Subscription failed creation.", eventService.createSubscription(subscription), notNullValue());

        // Trigger Subject Created Event
        eventService.triggerEvent(new SubjectEvent(subject, mockUser.getLogin(), SubjectEvent.Status.CREATED, projectId1));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type SubjectEvent", action.getDetectedEvents().get(0).getId(), containsString("SubjectEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
    }

    @Test
    @DirtiesContext
    public void catchSubjectEventWithSiteSubscription() throws Exception {
        // Create a user
        String projectId1 = "";
        String subjectID = "TestSubject";
        XnatSubjectdata subject = new XnatSubjectdata(mockUser);
        subject.setProject(projectId1);
        subject.setId(subjectID);

        // Subscribe to event
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("Test Subscription")
                                                                     .projectId(projectId1)
                                                                     .active(true)
                                                                     .eventType(new SubjectEvent().getId())
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Subscription failed validation.",eventService.validateSubscription(subscription), notNullValue());
        assertThat("Subscription failed creation.", eventService.createSubscription(subscription), notNullValue());

        // Trigger Subject Created Event
        eventService.triggerEvent(new SubjectEvent(subject, mockUser.getLogin(), SubjectEvent.Status.CREATED, projectId1));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type SubjectEvent", action.getDetectedEvents().get(0).getId(), containsString("SubjectEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));

    }

    @Test
    @DirtiesContext
    public void missSubjectEventWithDifferentProjectSubscription() throws Exception {
        // Create a user
        String projectId1 = "PROJECTID_1";
        String projectId2 = "PROJECTID-2";
        String subjectID = "TestSubject";
        XnatSubjectdata subject = new XnatSubjectdata(mockUser);
        subject.setProject(projectId1);
        subject.setId(subjectID);

        // Subscribe to event
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("Test Subscription")
                                                                     .projectId(projectId2)
                                                                     .active(true)
                                                                     .eventType(new SubjectEvent().getId())
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Subscription failed validation.",eventService.validateSubscription(subscription), notNullValue());
        assertThat("Subscription failed creation.", eventService.createSubscription(subscription), notNullValue());

        // Trigger Subject Created Event
        eventService.triggerEvent(new SubjectEvent(subject, mockUser.getLogin(), SubjectEvent.Status.CREATED, projectId1));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("Expected zero detected events.", action.getDetectedEvents().size(), is(0));
    }

    @Test
    @DirtiesContext
    public void catchSessionArchiveEventWithProjectId() throws Exception {
        String projectId1 = "PROJECTID_1";

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId1);
        session.setSessionType("xnat:imageSessionData");

        // Subscribe to event
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("Test Subscription")
                                                                     .projectId(projectId1)
                                                                     .active(true)
                                                                     .eventType(new SessionEvent().getId())
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Subscription failed validation.",eventService.validateSubscription(subscription), notNullValue());
        assertThat("Subscription failed creation.", eventService.createSubscription(subscription), notNullValue());

        // Trigger SessionEvent
        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(), SessionEvent.Status.CREATED, projectId1));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type SessionEvent", action.getDetectedEvents().get(0).getId(), containsString("SessionEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
    }

    @Test
    @DirtiesContext
    public void catchScanArchiveEventWithProjectId() throws Exception {
        String projectId1 = "PROJECTID_1";

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId1);
        session.setSessionType("xnat:imageSessionData");

        XnatImagescandata scan = new XnatImagescandata();
        scan.setImageSessionData(session);

        // Subscribe to event
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("Test Subscription")
                                                                     .projectId(projectId1)
                                                                     .active(true)
                                                                     .eventType(new ScanEvent().getId())
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Subscription failed validation.",eventService.validateSubscription(subscription), notNullValue());
        assertThat("Subscription failed creation.", eventService.createSubscription(subscription), notNullValue());

        // Trigger ScanArchiveEvent
        eventService.triggerEvent(new ScanEvent(scan, mockUser.getLogin(), ScanEvent.Status.CREATED, projectId1));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type ScanEvent", action.getDetectedEvents().get(0).getId(), containsString("ScanEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
    }


    @Test
    @DirtiesContext
    public void create1000SubscriptionsCatchOneWithDifferentEventType() throws Exception {
        StopWatch sw1 = new StopWatch();
        sw1.start("CreateEvents");
        String projectId = "Test";

        for(Integer i=0; i<999; i++){
            String name = projectId + i.toString();
            createScanSubscription(name, projectId, null);

        }
        createSessionSubscription("SomethingDifferent", projectId, null);
        sw1.stop();
        System.out.print("\n" + Integer.toString(eventService.getSubscriptions().size()) + " Subscriptions created in : " + sw1.getTotalTimeSeconds() + "seconds\n");

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId);
        session.setSessionType("xnat:imageSessionData");
        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(), SessionEvent.Status.CREATED, projectId));

        StopWatch sw2 = new StopWatch();
        sw2.start("eventTriggerToAction");
        synchronized (testAction) {
            testAction.wait(100);
        }
        sw2.stop();
        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type SessionEvent", action.getDetectedEvents().get(0).getId(), containsString("SessionEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
        System.out.print("Event caught in : " + sw2.getTotalTimeSeconds() + "seconds\n");

    }



    @Test
    @DirtiesContext
    public void create1000SubscriptionsCatchTwoWithDifferentProjectId() throws Exception {
        StopWatch sw1 = new StopWatch();
        sw1.start("CreateSubscriptions");
        String projectId = "Test";

        for(Integer i=0; i<1000; i++){
            String name = projectId + i.toString();
            createSessionSubscription(name, name, null);

        }
        sw1.stop();
        System.out.print("\n" + Integer.toString(eventService.getSubscriptions().size()) + " Subscriptions created in : " + sw1.getTotalTimeSeconds() + "seconds\n");

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId);
        session.setSessionType("xnat:imageSessionData");
        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(),SessionEvent.Status.CREATED, projectId + "500"));

        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(), SessionEvent.Status.CREATED, projectId + "600"));

        StopWatch sw2 = new StopWatch();
        sw2.start("eventTriggerToAction");
        synchronized (testAction) {
            testAction.wait(100);
        }
        sw2.stop();
        TestAction action = (TestAction) testAction;
        assertThat("Expected two detected events.", action.getDetectedEvents().size(), is(2));
        assertThat("Expected detected event to be of type SessionEvent", action.getDetectedEvents().get(0).getId(), containsString("SessionEvent"));
        assertThat("Expected detected event to be of type SessionEvent", action.getDetectedEvents().get(1).getId(), containsString("SessionEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
        System.out.print("Two event caught in : " + sw2.getTotalTimeSeconds() + "seconds\n");

    }

    @Test
    @DirtiesContext
    public void createManySubscriptionsTriggerManyEventsCatch1000() throws Exception {
        String projectIdToIgnore = "ProjectIdToIgnore";
        XnatImagesessiondata sessionToIgnore = new XnatImagesessiondata();
        sessionToIgnore.setModality("MR");
        sessionToIgnore.setProject(projectIdToIgnore);
        sessionToIgnore.setSessionType("xnat:imageSessionData");

        String projectIdToCatch = "ProjectIdToCatch";
        XnatImagesessiondata sessionToCatch = new XnatImagesessiondata();
        sessionToCatch.setModality("MR");
        sessionToCatch.setProject(projectIdToCatch);
        sessionToCatch.setSessionType("xnat:imageSessionData");

        StopWatch sw1 = new StopWatch();
        sw1.start("Subscriptions");
        String projectId = "Test";

        for(Integer i=0; i<1000; i++){
            String name = projectId + i.toString();
            assertThat(createSessionSubscription(name  + "_1", projectId + "_1", null), notNullValue());
            assertThat(createSessionSubscription(name  + "_2", projectId + "_2", null), notNullValue());
            assertThat(createScanSubscription(name  +    "_3", projectId + "_3", null), notNullValue());
            assertThat(createScanSubscription(name  +    "_4", projectId + "_4", null), notNullValue());
        }
        for(Integer i=0; i<10; i++){
            String name = projectIdToCatch + i.toString();
            assertThat(createSessionSubscription(name, projectIdToCatch, null), notNullValue());
        }
        sw1.stop();
        System.out.print("\n" + Integer.toString(eventService.getSubscriptions().size()) + " Subscriptions created in : " + sw1.getTotalTimeSeconds() + "seconds\n");

        StopWatch sw2 = new StopWatch();
        sw2.start("eventTriggersToActions");
        for(Integer i=0; i<10000; i++){
            eventService.triggerEvent(new SessionEvent(sessionToIgnore, mockUser.getLogin(), SessionEvent.Status.CREATED, null));
        }
        sw2.stop();
        System.out.print("Triggered 10000 ignored events in : " + sw2.getTotalTimeSeconds() + "seconds\n");

        StopWatch sw3 = new StopWatch();
        sw3.start("eventTriggersToActions");
        for(Integer i=0; i<100; i++){
            eventService.triggerEvent(new SessionEvent(sessionToCatch, mockUser.getLogin(), SessionEvent.Status.CREATED, projectIdToCatch));
        }
        synchronized (testAction) {
            testAction.wait(100);
        }
        sw3.stop();
        TestAction action = (TestAction) testAction;
        System.out.print("Triggered/Caught " + Integer.toString(action.getDetectedEvents().size())+ " detected events in : " + sw3.getTotalTimeSeconds() + "seconds\n");

        List<EventServiceEvent> detectedEvents = action.getDetectedEvents();
        assertThat("Expected 100 detected events.", action.getDetectedEvents().size(), is(1000));

    }

    @Test
    @DirtiesContext
    public void testDisabledSubscriptionHandlingSpeed() throws Exception {
        String projectIdToCatch = "ProjectIdToCatch";
        XnatImagesessiondata sessionToCatch = new XnatImagesessiondata();
        sessionToCatch.setModality("MR");
        sessionToCatch.setProject(projectIdToCatch);
        sessionToCatch.setSessionType("xnat:imageSessionData");

        StopWatch sw1 = new StopWatch();
        sw1.start("Subscriptions");

        Integer i;
        for(i=0; i<1000; i++){
            String name = projectIdToCatch + i.toString();
            Long id = createSessionSubscription(name  + "_1", projectIdToCatch, null).id();
            eventService.deactivateSubscription(id);
        }
        createSessionSubscription(projectIdToCatch, projectIdToCatch, null);

        // time reaction to 1000 disabled subscriptions and 1 enabled
        StopWatch sw3 = new StopWatch();
        sw3.start("disabledEventTriggersToActions");
        eventService.triggerEvent(new SessionEvent(sessionToCatch, mockUser.getLogin(), SessionEvent.Status.CREATED, projectIdToCatch));

        synchronized (testAction) {
            testAction.wait(100);
        }
        sw3.stop();
        TestAction action = (TestAction) testAction;
        System.out.print("Triggered " + Integer.toString(action.getDetectedEvents().size())+ " enabled event and " + i.toString() + " disabled events  in : " + sw3.getTotalTimeSeconds() + "seconds\n");


    }


    @Test
    @DirtiesContext
    public void testSubscriptionDeliveryCreation() throws Exception {
        String projectIdToCatch = "ProjectIdToCatch";
        XnatImagesessiondata sessionToCatch = new XnatImagesessiondata();
        sessionToCatch.setModality("MR");
        sessionToCatch.setProject(projectIdToCatch);
        sessionToCatch.setSessionType("xnat:imageSessionData");

        assertThat(createSessionSubscription("TheOneAndOnly", projectIdToCatch, null), notNullValue());
        StopWatch sw3 = new StopWatch();
        sw3.start("eventTriggersToActions");
        for(Integer i=0; i<10; i++){
            eventService.triggerEvent(new SessionEvent(sessionToCatch, mockUser.getLogin(), SessionEvent.Status.CREATED, projectIdToCatch));
        }
        synchronized (testAction) {
            testAction.wait(100);
        }
        sw3.stop();

        List<SubscriptionDelivery> deliveriesWithProjectId = eventService.getSubscriptionDeliveries(projectIdToCatch, null, false);
        List<SubscriptionDelivery> deliveriesWithSubscriptionId = eventService.getSubscriptionDeliveries(null, 1L, true);
        List<SubscriptionDelivery> deliveriesWithSubscriptionIdAndProjectId = eventService.getSubscriptionDeliveries(projectIdToCatch, 1l, false);
        List<SubscriptionDelivery> deliveries = eventService.getSubscriptionDeliveries(null, null, true);

        assertThat("Expected 10 deliveries.", deliveriesWithProjectId.size(), is(10));
        assertThat("Expected 10 deliveries.", deliveriesWithSubscriptionId.size(), is(10));
        assertThat("Expected 10 deliveries.", deliveriesWithSubscriptionIdAndProjectId.size(), is(10));
        assertThat("Expected 10 deliveries.", deliveries.size(), is(10));

    }

    @Test
    @DirtiesContext
    public void testCreateAndDelete() throws Exception {

        String projectId = "ProjectId";


        List<Subscription> subscriptionsToDelete = new ArrayList<>();
        for(Integer i=0; i<50; i++){
            String name = projectId + i.toString();
            subscriptionsToDelete.add(createSessionSubscription(name, projectId, null));
        }

        Subscription subToCatch = createSessionSubscription("Name to Catch 1", projectId, null);

        for(Integer i=50; i<100; i++){
            String name = projectId + i.toString();
            subscriptionsToDelete.add(createSessionSubscription(name, projectId, null));
        }

        for(Subscription subscription : subscriptionsToDelete){
            eventService.deleteSubscription(subscription.id());
        }

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId);
        session.setSessionType("xnat:imageSessionData");

        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(), SessionEvent.Status.CREATED, projectId));

        synchronized (testAction) {
            testAction.wait(100);
        }

        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type SessionEvent", action.getDetectedEvents().get(0).getId(), containsString("SessionEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
    }


    @Test
    @DirtiesContext
    public void testCreateAndDeactivate() throws Exception {

        String projectId = "ProjectId";

        List<Subscription> subscriptionsToDeactivate = new ArrayList<>();
        for(Integer i=0; i<10; i++){
            String name = projectId + i.toString();
            subscriptionsToDeactivate.add(createSessionSubscription(name, projectId, null));
        }

        Subscription subToCatch = createSessionSubscription("Name to Catch 1", projectId, null);

        for(Integer i=20; i<30; i++){
            String name = projectId + i.toString();
            subscriptionsToDeactivate.add(createSessionSubscription(name, projectId, null));
        }

        for(Subscription subscription : subscriptionsToDeactivate){
            eventService.deactivateSubscription(subscription.id());
        }

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId);
        session.setSessionType("xnat:imageSessionData");

        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(), SessionEvent.Status.CREATED, projectId));

        synchronized (testAction) {
            testAction.wait(100);
        }

        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type SessionEvent", action.getDetectedEvents().get(0).getId(), containsString("SessionEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
    }

    @Test
    @DirtiesContext
    public void testCreateAndDeactivateAndActivate() throws Exception {

        String projectId = "ProjectId";

        List<Subscription> subscriptionsToDeactivate = new ArrayList<>();
        for(Integer i=0; i<5; i++){
            String name = projectId + i.toString();
            subscriptionsToDeactivate.add(createSessionSubscription(name, projectId, null));
        }

        Subscription subscriptionToCatch = createSessionSubscription("Name to Catch 1", projectId, null);
        subscriptionsToDeactivate.add(subscriptionToCatch);

        for(Integer i=6; i<10; i++){
            String name = projectId + i.toString();
            subscriptionsToDeactivate.add(createSessionSubscription(name, projectId, null));
        }

        for(Subscription subscription : subscriptionsToDeactivate){
            eventService.deactivateSubscription(subscription.id());
        }

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId);
        session.setSessionType("xnat:imageSessionData");

        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(), SessionEvent.Status.CREATED, projectId));

        synchronized (testAction) {
            testAction.wait(100);
        }

        TestAction action = (TestAction) testAction;
        assertThat("Expected zero detected events.", action.getDetectedEvents().size(), is(0));

        eventService.activateSubscription(subscriptionToCatch.id());

        eventService.triggerEvent(new SessionEvent(session, mockUser.getLogin(), SessionEvent.Status.CREATED, projectId));

        synchronized (testAction) {
            testAction.wait(100);
        }

        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type SessionEvent", action.getDetectedEvents().get(0).getId(), containsString("SessionEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
    }


    @Test
    @DirtiesContext
    public void catchSpecificEventWithOpenFilter() throws Exception {
        String projectId1 = "PROJECTID_1";

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId1);
        session.setSessionType("xnat:imageSessionData");

        XnatImagescandata scan = new XnatImagescandata();
        scan.setImageSessionData(session);

        // Subscribe to event
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("Test Subscription")
                                                                     .active(true)
                                                                     .eventType(new ScanEvent().getId())
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();

        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Subscription failed validation.",eventService.validateSubscription(subscription), notNullValue());
        assertThat("Subscription failed creation.", eventService.createSubscription(subscription), notNullValue());

        // Trigger ScanArchiveEvent
        eventService.triggerEvent(new ScanEvent(scan, mockUser.getLogin(), ScanEvent.Status.CREATED, projectId1));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("Expected one detected event.", action.getDetectedEvents().size(), is(1));
        assertThat("Expected detected event to be of type ScanEvent", action.getDetectedEvents().get(0).getId(), containsString("ScanEvent"));
        assertThat("Expected Action User to be subscription creator", action.getActionUser(), is(mockUser.getLogin()));
    }


    @Test
    @DirtiesContext
    public void missOpenEventWithSpecificSubscription() throws Exception {
        String projectId1 = "PROJECTID_1";
        String eventType = new ScanEvent().getId();
        Enum status = ScanEvent.Status.CREATED;

        XnatImagesessiondata session = new XnatImagesessiondata();
        session.setModality("MR");
        session.setProject(projectId1);
        session.setSessionType("xnat:imageSessionData");

        XnatImagescandata scan = new XnatImagescandata();
        scan.setImageSessionData(session);

        // Subscribe to event
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        EventFilter filter = EventFilter.builder().projectIds(Arrays.asList(projectId1)).eventType(eventType).status(status.name()).build();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name("Test Subscription")
                                                                     .eventFilter(filter)
                                                                     .active(true)
                                                                     .eventType(eventType)
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        assertThat("Subscription failed validation.",eventService.validateSubscription(subscription), notNullValue());
        assertThat("Subscription failed creation.", eventService.createSubscription(subscription), notNullValue());

        // Trigger ScanArchiveEvent
        eventService.triggerEvent(new ScanEvent(scan, mockUser.getLogin(), null, projectId1));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        TestAction action = (TestAction) testAction;
        assertThat("Expected zero detected events.", action.getDetectedEvents().size(), is(0));


        // Trigger ScanArchiveEvent
        eventService.triggerEvent(new ScanEvent(scan, mockUser.getLogin(), ScanEvent.Status.CREATED, null));

        // wait for listener (max 1 sec.)
        synchronized (testAction) {
            testAction.wait(1000);
        }
        action = (TestAction) testAction;
        assertThat("Expected zero detected events.", action.getDetectedEvents().size(), is(0));
    }


    public Subscription createSessionSubscription(String name, String projectId, EventFilter filter) throws Exception {
        String eventType = new SessionEvent().getId();
        Enum status = SessionEvent.Status.CREATED;
        if(filter == null) {
            filter = EventFilter.builder().projectIds(Arrays.asList(projectId)).eventType(eventType).status(status.name()).build();
        }

        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name(name)
                                                                     .projectId(projectId)
                                                                     .active(true)
                                                                     .eventFilter(filter)
                                                                     .eventType(eventType)
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        subscription = eventService.validateSubscription(subscription);
        return eventService.createSubscription(subscription);
    }

    public Subscription createScanSubscription(String name, String projectId, EventFilter filter) throws Exception{
        String eventType = new ScanEvent().getId();
        Enum status = ScanEvent.Status.CREATED;
        if(filter == null) {
            filter = EventFilter.builder().projectIds(Arrays.asList(projectId)).eventType(eventType).status(status.name()).build();
        }
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name(name)
                                                                     .projectId(projectId)
                                                                     .active(true)
                                                                     .eventFilter(filter)
                                                                     .eventType(eventType)
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();

        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        subscription = eventService.validateSubscription(subscription);
        return eventService.createSubscription(subscription);
    }

    public Subscription createProjectCreatedSubscription(String name, String projectId, EventFilter filter) throws Exception {
        String eventType = new ProjectEvent().getId();
        Enum status = ProjectEvent.Status.CREATED;
        if(filter == null) {
            filter = EventFilter.builder().projectIds(Arrays.asList(projectId)).eventType(eventType).status(status.name()).build();
        }
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name(name)
                                                                     .projectId(projectId)
                                                                     .active(true)
                                                                     .eventFilter(filter)
                                                                     .eventType(eventType)
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        subscription = eventService.validateSubscription(subscription);
        return eventService.createSubscription(subscription);
    }

    public Subscription createSubjectCreatedSubscription(String name, String projectId, EventFilter filter) throws Exception {
        String eventType = new SubjectEvent().getId();
        Enum status = SubjectEvent.Status.CREATED;
        if(filter == null) {
            filter = EventFilter.builder().projectIds(Arrays.asList(projectId)).eventType(eventType).status(status.name()).build();
        }
        String testActionKey = testAction.getAllActions().get(0).actionKey();
        eventService.getAllActions();
        SubscriptionCreator subscriptionCreator = SubscriptionCreator.builder()
                                                                     .name(name)
                                                                     .projectId(projectId)
                                                                     .active(true)
                                                                     .eventFilter(filter)
                                                                     .eventType(eventType)
                                                                     .actionKey(testActionKey)
                                                                     .actAsEventUser(false)
                                                                     .build();
        Subscription subscription = Subscription.create(subscriptionCreator, mockUser.getLogin());
        subscription = eventService.validateSubscription(subscription);
        return eventService.createSubscription(subscription);
    }



    class MockSingleActionProvider extends SingleActionProvider {


        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void processEvent(EventServiceEvent event, Subscription subscription, UserI user,
                                 Long deliveryId) {

        }

        @Override
        public Map<String, ActionAttributeConfiguration> getAttributes(String projectId, String xnatType, UserI user) {
            return null;
        }
    }

    @Service
    class MockConsumer implements EventServiceListener<SampleEvent> {
        private SampleEvent event;

        public EventServiceEvent getEvent() {
            return event;
        }

        @Override
        public String getId() { return this.getClass().getCanonicalName(); }

        @Override
        public String getEventType() {
            return SampleEvent.class.getName();
        }

        @Override
        public EventServiceListener getInstance() {
            return this;
        }

        @Override
        public UUID getInstanceId() {
            return null;
        }

        @Override
        public void setEventService(EventService eventService) { }

        @Override
        public Date getDetectedTimestamp() {
            return null;
        }

        @Override
        public void accept(Event<SampleEvent> event) {
            this.event = event.getData();
            synchronized (this) {
                notifyAll();
            }
        }

    }



}