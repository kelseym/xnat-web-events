package org.nrg.xnat.eventservice.services.impl;

import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.framework.services.ContextService;
import org.nrg.xnat.eventservice.daos.EventSubscriptionEntityDao;
import org.nrg.xnat.eventservice.entities.SubscriptionEntity;
import org.nrg.xnat.eventservice.events.EventServiceEvent;
import org.nrg.xnat.eventservice.exceptions.SubscriptionValidationException;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.model.Subscription;
import org.nrg.xnat.eventservice.services.ActionManager;
import org.nrg.xnat.eventservice.services.EventServiceComponentManager;
import org.nrg.xnat.eventservice.services.EventSubscriptionEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.bus.EventBus;
import reactor.bus.registry.Registration;
import reactor.fn.Consumer;

import javax.annotation.Nonnull;
import javax.persistence.EntityNotFoundException;
import java.util.List;

import static reactor.bus.selector.Selectors.regex;

@Service
@Transactional
public class EventSubscriptionEntityServiceImpl
        extends AbstractHibernateEntityService<SubscriptionEntity, EventSubscriptionEntityDao>
        implements EventSubscriptionEntityService {

    private static final Logger log = LoggerFactory.getLogger(org.nrg.xnat.eventservice.services.EventSubscriptionEntityService.class);
    private EventBus eventBus;
    private ContextService contextService;
    private ActionManager actionManager;
    private EventServiceComponentManager componentManager;


    @Autowired
    public EventSubscriptionEntityServiceImpl(final EventBus eventBus, final ContextService contextService, final ActionManager actionManager, final EventServiceComponentManager componentManager) {
        this.eventBus = eventBus;
        this.contextService = contextService;
        this.actionManager = actionManager;
        this.componentManager = componentManager;
    }


    @Override
    public Subscription validate(Subscription subscription) throws SubscriptionValidationException {
        Class<?> clazz;
        try {
            clazz = Class.forName(subscription.event());
            if (clazz == null || !EventServiceEvent.class.isAssignableFrom(clazz)) {
                throw new SubscriptionValidationException("Event class does not have a default listener: " + subscription.event() != null ? subscription.event() : "unknown");
            }
        } catch (NoSuchBeanDefinitionException|ClassNotFoundException e) {
            throw new SubscriptionValidationException("Could not load Event class: " + subscription.event() != null ? subscription.event() : "unknown");
        }
        try {
            // Check that event class has a default listener loaded into the application context
            if(!EventServiceListener.class.isAssignableFrom(clazz) || contextService.getBean(clazz) == null){
                throw new NoSuchBeanDefinitionException("Could not load Bean of type " + subscription.event() != null ? subscription.event() : "unknown" + ", from application context.");
            }
            // Check that Action is valid and service is accessible

            if (! actionManager.validateAction(actionManager.getActionByKey(subscription.actionKey()))) {
                throw new SubscriptionValidationException("Could not validate Action Provider Class " + subscription.actionKey() != null ? subscription.actionKey() : "unknown");
            }
        } catch (NoSuchBeanDefinitionException e) {
            throw new SubscriptionValidationException("Could not load default Listener/Consumer: " + subscription.event() != null ? subscription.event() : "unknown" + ", from application context.");
        }
        return subscription;
    }

    @Transactional
    @Override
    public Subscription activate(Subscription subscription) {
        //TODO
        try {
            Consumer consumer = componentManager.getListener(subscription.event());
            Class<?> clazz = Class.forName(subscription.event());
            if(clazz == null || !EventServiceListener.class.isAssignableFrom(clazz)) {
                  Registration registration = eventBus.on(
                  regex(subscription.eventFilter() != null ? subscription.eventFilter().toRegexPattern() : "*"), consumer);

           log.info("Created registrationKey: " + registration.hashCode());
           subscription = subscription.toBuilder()
                   .listenerRegistrationKey(registration.hashCode())
                   .build();
            } else throw new SubscriptionValidationException("Could not activate subscription.");
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
        subscription = validate(subscription);
        subscription = activate(subscription);
        subscription = save(subscription);
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
    public Subscription getSubscription(Long id) throws NotFoundException {
        return super.get(id).toPojo();
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
