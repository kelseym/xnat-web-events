package org.nrg.xnat.eventservice.events;

import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import reactor.bus.Event;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Properties;

// ** Extend this class to implement a Reactor Event and Listener in one class ** //
@Service
public abstract class SimpleEventServiceEvent<EventT extends EventServiceEvent, EventObjectT>
        implements EventServiceEvent<EventObjectT>, EventServiceListener<EventT> {

    UserI eventUser;
    EventObjectT object;

    @Autowired
    EventService eventService;

    public SimpleEventServiceEvent(){};

    public SimpleEventServiceEvent(final EventObjectT object, final UserI eventUser) {
        this.object = object;
        this.eventUser = eventUser;
    }

    @Override
    public String getId() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public String getEventType() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public  EventObjectT getObject() {
        return object;
    }

    @Override
    public String getObjectType() {
        return getObject() == null ? null : getObject().getClass().getCanonicalName();
    }

    @Override
    public void accept(Event<EventT> event){
        eventService.processEvent(event);
    }


    public static SimpleEventServiceEvent createFromResource(Resource resource)
            throws IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        SimpleEventServiceEvent event = null;
        final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
        Class<?> clazz = Class.forName(properties.get(XnatEventServiceEvent.EVENT_CLASS).toString());
        if (SimpleEventServiceEvent.class.isAssignableFrom(clazz) &&
                !clazz.isInterface() &&
                !Modifier.isAbstract(clazz.getModifiers())) {
            try {
                event = (SimpleEventServiceEvent) clazz.getConstructor().newInstance();
//                event.setDisplayName(properties.containsKey(XnatEventServiceEvent.EVENT_DISPLAY_NAME) ? properties.get(XnatEventServiceEvent.EVENT_DISPLAY_NAME).toString() : "");
//                event.setDescription(properties.containsKey(XnatEventServiceEvent.EVENT_DESC) ? properties.get(XnatEventServiceEvent.EVENT_DESC).toString() : "");
//                event.setEventObject(properties.containsKey(XnatEventServiceEvent.EVENT_OBJECT) ? properties.get(XnatEventServiceEvent.EVENT_OBJECT).toString() : "");
//                event.setEventOperation(properties.containsKey(XnatEventServiceEvent.EVENT_OPERATION) ? properties.get(XnatEventServiceEvent.EVENT_OPERATION).toString() : "");
            } catch (NoSuchMethodException e){
              throw new NoSuchMethodException("Can't find default constructor for " + clazz.getName());
            }
        }
        return event;
    }

}
