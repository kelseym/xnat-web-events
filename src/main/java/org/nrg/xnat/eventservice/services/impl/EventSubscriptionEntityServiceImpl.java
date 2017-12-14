package org.nrg.xnat.eventservice.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.daos.EventSubscriptionEntityDao;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.exceptions.SubscriptionValidationException;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.model.Subscription;
import org.nrg.xnat.eventservice.model.xnat.XnatModelObject;
import org.nrg.xnat.eventservice.services.ActionManager;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.EventServiceComponentManager;
import org.nrg.xnat.eventservice.services.EventSubscriptionEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.registry.Registration;
import reactor.bus.selector.Selector;

import javax.annotation.Nonnull;
import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static reactor.bus.selector.Selectors.R;
import static reactor.bus.selector.Selectors.T;

@Service
@Transactional
public class EventSubscriptionEntityServiceImpl
        extends AbstractHibernateEntityService<SubscriptionEntity, EventSubscriptionEntityDao>
        implements EventSubscriptionEntityService {

    private static final Logger log = LoggerFactory.getLogger(EventSubscriptionEntityService.class);
    private EventBus eventBus;
    private ContextService contextService;
    private ActionManager actionManager;
    private EventServiceComponentManager componentManager;
    private EventService eventService;
    private ObjectMapper mapper;
    private UserManagementServiceI userManagementService;


    @Autowired
    public EventSubscriptionEntityServiceImpl(final EventBus eventBus,
                                              final ContextService contextService,
                                              final ActionManager actionManager,
                                              final EventServiceComponentManager componentManager,
                                              @Lazy final EventService eventService,
                                              final ObjectMapper mapper,
                                              final UserManagementServiceI userManagementService) {
        this.eventBus = eventBus;
        this.contextService = contextService;
        this.actionManager = actionManager;
        this.componentManager = componentManager;
        this.eventService = eventService;
        this.mapper = mapper;
        this.userManagementService = userManagementService;
        log.debug("EventSubscriptionService started normally.");
    }


    @Override
    public Subscription validate(Subscription subscription) throws SubscriptionValidationException {
        UserI actionUser = null;
        try {
            actionUser = userManagementService.getUser(subscription.subscriptionOwner());
        } catch (UserNotFoundException|UserInitException e) {
            throw new SubscriptionValidationException("Could not load Subscription Owner for userID: " + subscription.subscriptionOwner() != null ? Integer.toString(subscription.subscriptionOwner()) : "null" + "\n" + e.getMessage());
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(subscription.eventId());
            if (clazz == null || !EventServiceEvent.class.isAssignableFrom(clazz)) {
                throw new SubscriptionValidationException("Event class does not have a default listener: " + subscription.eventId() != null ? subscription.eventId() : "unknown");
            }
        } catch (NoSuchBeanDefinitionException|ClassNotFoundException e) {
            throw new SubscriptionValidationException("Could not load Event class: " + subscription.eventId() != null ? subscription.eventId() : "unknown");
        }
        try {
            // Check that event class has a default listener loaded into the application context
            if(!EventServiceListener.class.isAssignableFrom(clazz) || contextService.getBean(clazz) == null){
                throw new NoSuchBeanDefinitionException("Could not load Bean of type " + subscription.eventId() != null ? subscription.eventId() : "unknown" + ", from application context.");
            }
            // Check that Action is valid and service is accessible

            if (! actionManager.validateAction(actionManager.getActionByKey(subscription.actionKey(), actionUser), actionUser)) {
                throw new SubscriptionValidationException("Could not validate Action Provider Class " + subscription.actionKey() != null ? subscription.actionKey() : "unknown");
            }
        } catch (NoSuchBeanDefinitionException e) {
            throw new SubscriptionValidationException("Could not load default Listener/Consumer: " + subscription.eventId() != null ? subscription.eventId() : "unknown" + ", from application context. \n" + e.getMessage());
        }
        return subscription;
    }

    @Transactional
    @Override
    public Subscription activate(Subscription subscription) {
        try {
            Class<?> eventClazz = Class.forName(subscription.eventId());
            EventServiceListener listener = null;
            // Is a custom listener defined and valid
            if(!Strings.isNullOrEmpty(subscription.customListenerId())){
                listener = componentManager.getListener(subscription.customListenerId());
            }
            if(listener == null && EventServiceListener.class.isAssignableFrom(eventClazz)) {
            // Is event class a combined event/listener
                listener = componentManager.getListener(subscription.eventId());
            }
            if(listener != null) {
                EventServiceListener uniqueListener = listener.getInstance();
                uniqueListener.setEventService(eventService);
                Selector selector = T(eventClazz);
                if(subscription.eventFilter() != null && !Strings.isNullOrEmpty(subscription.eventFilter().toRegexMatcher()))
                    selector = R(subscription.eventFilter().toRegexMatcher());
                Registration registration = eventBus.on(selector, uniqueListener);

                log.debug("Activated Reactor Registration: " + registration.hashCode() + "  RegistrationKey: " + (uniqueListener.getInstanceId() == null ? "" : uniqueListener.getInstanceId().toString()));
                subscription = subscription.toBuilder()
                                           .listenerRegistrationKey(uniqueListener.getInstanceId() == null ? "" : uniqueListener.getInstanceId().toString())
                                           .active(true)
                                           .build();
            } else {
                log.error("Could not activate subscription:" + Long.toString(subscription.id()) + ". No appropriate listener found.");
                throw new SubscriptionValidationException("Could not activate subscription. No appropriate listener found.");
            }
        }
        catch (SubscriptionValidationException | ClassNotFoundException e) {
            log.error("Event subscription failed for " + subscription.toString());
            log.error(e.getMessage());
        }
        return subscription;
    }



    @Transactional
    @Override
    public Subscription deactivate(@Nonnull Subscription subscription) throws NotFoundException, EntityNotFoundException{
        Subscription deactivatedSubscription = null;
        try {
            if(subscription.id() != null) {
                throw new NotFoundException("Failed to deactivate subscription - Missing subscription ID");
            }
            log.debug("Deactivating subscription:" + Long.toString(subscription.id()));
            SubscriptionEntity entity = SubscriptionEntity.fromPojo(subscription);
            if(entity != null) {
                entity.setActive(false);
                entity.setListenerRegistrationKey(null);
                deactivatedSubscription = entity.toPojo();
                log.debug("Deactivated subscription:" + Long.toString(subscription.id()));
            }
            else {
                log.error("Failed to deactivate subscription - no entity found for id:" + Long.toString(subscription.id()));
                throw new EntityNotFoundException("Could not retrieve EventSubscriptionEntity from id: " + subscription.id());
            }
        } catch(NotFoundException|EntityNotFoundException e){
            log.error(e.getMessage());
        }
        return deactivatedSubscription;
    }

    @Transactional
    @Override
    public Subscription save(@Nonnull Subscription subscription) {
        Subscription saved = toPojo(create(fromPojo(subscription)));
        log.debug("Saved subscription with ID:" + Long.toString(saved.id()));
        return saved;
    }

    @Transactional
    @Override
    public void delete(@Nonnull Long subscriptionId) throws NotFoundException {
        if(subscriptionId != null) {
            Subscription subscription = getSubscription(subscriptionId);
            deactivate(subscription);
            SubscriptionEntity entity = retrieve(subscriptionId);
            if(entity.getActive() == false) {
                delete(entity);
            }
            else {
                deactivate(subscription);
                delete(entity);
            }
            log.debug("Deleted subscription:" + Long.toString(subscription.id()));
        } else {
            log.error("Failed to delete subscription. Invalid or missing subscription ID.");
            throw new NotFoundException("Failed to delete subscription. Invalid or missing subscription ID");
        }
    }

    @Override
    public Subscription createSubscription(Subscription subscription) throws SubscriptionValidationException {
        log.debug("Validating subscription: " + subscription.name());
        subscription = validate(subscription);
        if(subscription.active()) {
            log.debug("Activating subscription: " + subscription.name());
            subscription = activate(subscription);
        }
        else {
            log.debug("Subscription set to not active. Skipping activation.");
        }
        log.debug("Saving subscription: " + subscription.name());
        subscription = save(subscription);
        return subscription;
    }

    @Override
    public Subscription update(Subscription subscription) throws SubscriptionValidationException, NotFoundException {
        SubscriptionEntity storedEntity = retrieve(subscription.id());
        SubscriptionEntity subscriptionEntity = fromPojoWithTemplate(subscription);
        Subscription vSubscription = validate(toPojo(subscriptionEntity));
        if(storedEntity.getActive() && !subscription.active()){
            deactivate(subscription);
        } else if(!storedEntity.getActive() && subscription.active()){
            activate(subscription);
        }
        update(subscriptionEntity);
        log.debug("Updated subscription: " + subscription.name());
        return toPojo(subscriptionEntity);
    }


    @Override
    public List<Subscription> getAllSubscriptions() {
        return SubscriptionEntity.toPojo(super.getAll());
    }

    @Override
    public List<Subscription> getSubscriptionsByKey(String key) throws NotFoundException {
        List<SubscriptionEntity> subscriptionEntities = getDao().findByKey(key);
        return SubscriptionEntity.toPojo(getDao().findByKey(key));
    }

    @Override
    public Subscription getSubscription(Long id) throws NotFoundException {
        return super.get(id).toPojo();
    }

    @Override
    public void reregisterAllActive() {
        List<Subscription> failedReactivations = new ArrayList<>();
        for (Subscription subscription:getAllSubscriptions()) {
            if(subscription.active()) {
                log.debug("Attempting reactivation of subscription: " + Long.toString(subscription.id()));
                try {
                    Subscription reactivated = activate(subscription);
                    reactivated = update(reactivated);
                    if(reactivated == null || !reactivated.active()){
                        failedReactivations.add(subscription);
                    }
                } catch (SubscriptionValidationException|NotFoundException e) {
                    log.error("Failed to reactivate subscription: " + Long.toString(subscription.id()));
                    log.error(e.getMessage());
                }
            }
        }
        if(!failedReactivations.isEmpty()){
            log.error("Failed to re-activate %i event subscriptions.", failedReactivations.size());
            for (Subscription fs:failedReactivations) {
                log.error("Subscription activation: <" + fs.toString() + "> failed.");
            }
        }
    }

    @Override
    public void processEvent(EventServiceListener listener, Event event) throws NotFoundException {
        String jsonObject = null;
        if(event.getData() instanceof EventServiceEvent) {
            EventServiceEvent esEvent = (EventServiceEvent) event.getData();
            for (Subscription subscription : getSubscriptionsByKey(listener.getInstanceId().toString())) {
                log.debug("RegKey matched for " + subscription.listenerRegistrationKey() + "  " + subscription.name());
                try {

                    // Is subscription enabled
                    if (!subscription.active()) {
                        log.debug("Inactive subscription: " + subscription.name() != null ? subscription.name() : "" + " skipped.");
                        return;
                    }
                    log.debug("Resolving action user (subscription owner or event user).");
                    UserI actionUser = subscription.actAsEventUser() ?
                            userManagementService.getUser(esEvent.getUser()) :
                            userManagementService.getUser(subscription.subscriptionOwner());
                    log.debug("Action User: " + actionUser.getUsername());

                    XnatModelObject modelObject = componentManager.getModelObject(esEvent.getObject(), actionUser);
                    if(modelObject != null && mapper.canSerialize(modelObject.getClass())) {
                        // Serialize data object
                        log.debug("Serializing event object as known Model Object.");
                        jsonObject = mapper.writeValueAsString(modelObject);
                    } else if(esEvent.getObject() != null && mapper.canSerialize(esEvent.getObject().getClass())) {
                        log.debug("Serializing event object as unknown object type.");
                        jsonObject = mapper.writeValueAsString(esEvent.getObject());
                    } else {
                        log.error("Could not serialize event object in: " + esEvent.toString());
                        return;
                    }
                    log.debug("Serialized Object: " + jsonObject);

                    //Filter on data object (if filter exists)
                    if( subscription.eventFilter() != null && subscription.eventFilter().jsonPathFilter() != null ) {
                        String jsonFilter = subscription.eventFilter().jsonPathFilter();
                        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);
                        List<String> filterResult =  JsonPath.using(conf).parse(jsonObject).read(jsonFilter);
                        if(filterResult.isEmpty()){
                            log.debug("Aborting event pipeline - Serialized event:\n" + jsonObject + "\ndidn't match JSONPath Filter:\n" + jsonFilter);
                            return;
                        } else {
                            log.debug("JSONPath Filter Match - Serialized event:\n" + jsonObject + "\nJSONPath Filter:\n" + jsonFilter);
                        }

                    }
                    // call Action Manager with payload
                    SubscriptionEntity subscriptionEntity = super.get(subscription.id());

                    actionManager.processEvent(subscriptionEntity, esEvent, actionUser);
                    subscriptionEntity.incCounter();
                } catch (JsonProcessingException|UserNotFoundException|UserInitException e) {
                    log.error("Failed to process subscription:" + subscription.name());
                    log.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }





    private Subscription toPojo(final SubscriptionEntity eventSubscriptionEntity) {
        return eventSubscriptionEntity == null ? null : eventSubscriptionEntity.toPojo();
    }

    private SubscriptionEntity fromPojo(final Subscription eventSubscription) {
        return eventSubscription == null ? null : SubscriptionEntity.fromPojo(eventSubscription);
    }

    private SubscriptionEntity fromPojoWithTemplate(final Subscription eventSubscription) {
        if (eventSubscription == null) {
            return null;
        }
        return SubscriptionEntity.fromPojoWithTemplate(eventSubscription, retrieve(eventSubscription.id()));
    }



}
