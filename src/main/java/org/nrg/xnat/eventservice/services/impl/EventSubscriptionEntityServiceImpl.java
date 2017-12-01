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

    @Autowired
    public EventSubscriptionEntityServiceImpl(final EventBus eventBus, final ContextService contextService, final ActionManager actionManager, final EventServiceComponentManager componentManager, @Lazy final EventService eventService, ObjectMapper mapper) {
        this.eventBus = eventBus;
        this.contextService = contextService;
        this.actionManager = actionManager;
        this.componentManager = componentManager;
        this.eventService = eventService;
        this.mapper = mapper;
    }


    @Override
    public Subscription validate(Subscription subscription) throws SubscriptionValidationException {
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

            if (! actionManager.validateAction(actionManager.getActionByKey(subscription.actionKey()))) {
                throw new SubscriptionValidationException("Could not validate Action Provider Class " + subscription.actionKey() != null ? subscription.actionKey() : "unknown");
            }
        } catch (NoSuchBeanDefinitionException e) {
            throw new SubscriptionValidationException("Could not load default Listener/Consumer: " + subscription.eventId() != null ? subscription.eventId() : "unknown" + ", from application context.");
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
                if(subscription.eventFilter() != null && !Strings.isNullOrEmpty(subscription.eventFilter().toRegexPattern()))
                    selector = R(subscription.eventFilter().toRegexPattern());
                Registration registration = eventBus.on(selector, uniqueListener);

                log.info("Created registrationKey: " + registration.hashCode());
                subscription = subscription.toBuilder()
                       .listenerRegistrationKey(uniqueListener.getInstanceId() == null ? "" : uniqueListener.getInstanceId().toString())
                        .build();
            } else throw new SubscriptionValidationException("Could not activate subscription. No appropriate listener found.");
            subscription = save(subscription);
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
        SubscriptionEntity entity;
/*        if(subscription.id() != null) {
            entity = retrieve(subscription.id());
            if(entity != null) {

                Registration registration = entity.getListenerRegistrationKey().pause();
                entity.setListenerRegistrationKey(registration);
            }
            else
                throw new EntityNotFoundException("Could not retrieve EventSubscriptionEntity from id: " + subscription.id());
        } else
            throw new NotFoundException("Invalid or missing subscription ID");
        return entity.toPojo();*/
        return null;

    }

    @Transactional
    @Override
    public Subscription save(@Nonnull Subscription subscription) {
        return toPojo(create(fromPojo(subscription)));
    }

    @Transactional
    public void delete(@Nonnull Subscription subscription) throws NotFoundException, Exception{
        if(subscription.id() != null) {
            SubscriptionEntity entity = retrieve(subscription.id());
            if(entity.getActive() == false)
                delete(entity);
            else
                throw new Exception("Cannot delete an active subscription. Deactivate first.");
        } else
            throw new NotFoundException("Invalid or missing subsription.ID");
    }

    @Override
    public Subscription activateAndSave(Subscription subscription) throws SubscriptionValidationException {
        //subscription = validate(subscription);
        subscription = activate(subscription);
        return subscription;
    }

    @Override
    public Subscription update(Subscription subscription) throws SubscriptionValidationException, NotFoundException {
        subscription = deactivate(subscription);
        subscription = activate(subscription);
        update(fromPojoWithTemplate(subscription));
        return validate(subscription);
    }


    @Override
    public List<Subscription> getAllSubscriptions() {
        return SubscriptionEntity.toPojo(super.getAll());
    }

    @Override
    public List<Subscription> getSubscriptionsByKey(String key) throws NotFoundException {
        return SubscriptionEntity.toPojo(getDao().findByKey(key));
    }

    @Override
    public Subscription getSubscription(Long id) throws NotFoundException {
        return super.get(id).toPojo();
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
                    XnatModelObject modelObject = esEvent.getModelObject();
                    if(modelObject != null && mapper.canSerialize(modelObject.getClass())) {

                        // Serialize data object
                        jsonObject = mapper.writeValueAsString(esEvent.getModelObject());
                    } else if(esEvent.getObject() != null && mapper.canSerialize(esEvent.getObject().getClass())) {
                        jsonObject = mapper.writeValueAsString(esEvent.getObject());
                    } else {
                        log.error("Could not serialize event object in: " + esEvent.toString());
                        return;
                    }
                    log.error("Serialized Object: " + jsonObject);

                    //Filter on data object (if filter exists)
                    if( subscription.eventFilter() != null && subscription.eventFilter().jsonPathFilter() != null ) {
                        String jsonFilter = subscription.eventFilter().jsonPathFilter();
                        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);
                        List<String> filterResult =  JsonPath.using(conf).parse(jsonObject).read(jsonFilter);
                        if(filterResult.isEmpty()){
                            log.error("Serialized event:\n" + jsonObject + "\ndidn't match JsonPath Filter:\n" + jsonFilter);
                            return;
                        }
                    }
                    // call Action Manager with payload
                    SubscriptionEntity subscriptionEntity = super.get(subscription.id());
                    actionManager.processEvent(subscriptionEntity, esEvent);
                    subscriptionEntity.incCounter();
                } catch (JsonProcessingException e) {
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
