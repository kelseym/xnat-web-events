package org.nrg.xnat.eventservice.events;

import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatSubjectdataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.nrg.xnat.eventservice.model.xnat.*;
import org.nrg.xnat.eventservice.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import reactor.bus.Event;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.UUID;

// ** Extend this class to implement a Reactor Event and Listener in one class ** //
@Service
public abstract class CombinedEventServiceEvent<EventT extends EventServiceEvent, EventObjectT>
        implements EventServiceEvent<EventObjectT>, EventServiceListener<EventT> {

    UserI eventUser;
    EventObjectT object;
    UUID listenerId = UUID.randomUUID();

    @Autowired
    EventService eventService;

    public CombinedEventServiceEvent() {};

    public CombinedEventServiceEvent(final EventObjectT object, final UserI eventUser) {
        this.object = object;
        this.eventUser = eventUser;
    }

    @Override
    public String getId() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public UUID getListenerId() {return listenerId;}

    @Override
    public String getEventType() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public  EventObjectT getObject() {
        return object;
    }


    @Override
    public String getObjectClass() {
        return getObject() == null ? null : getObject().getClass().getCanonicalName();
    }

    public void setEventService(EventService eventService){
        this.eventService = eventService;
    }

    @Override
    public void accept(Event<EventT> event){
        if( event.getData() instanceof EventServiceEvent)
            eventService.processEvent(this, event);
    }


    public static CombinedEventServiceEvent createFromResource(org.springframework.core.io.Resource resource)
            throws IOException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        CombinedEventServiceEvent event = null;
        final Properties properties = PropertiesLoaderUtils.loadProperties(resource);
        Class<?> clazz = Class.forName(properties.get(XnatEventServiceEvent.EVENT_CLASS).toString());
        if (CombinedEventServiceEvent.class.isAssignableFrom(clazz) &&
                !clazz.isInterface() &&
                !Modifier.isAbstract(clazz.getModifiers())) {
            try {
                event = (CombinedEventServiceEvent) clazz.getConstructor().newInstance();
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

    @Override
    public XnatModelObject getModelObject() {

        if(object.getClass() == XnatImageassessordataI.class) {
            return new Assessor((XnatImageassessordataI) object);
        }
        else if(object.getClass() == XnatProjectdata.class) {
            return new Project((XnatProjectdata) object);
        }
        else if(object.getClass() == XnatResourcecatalog.class) {
            return new Resource((XnatResourcecatalog) object);
        }
//        else if(object.getClass() == XnatImagescandataI.class) {
//            return new Scan((XnatImagescandataI) object);
//        }
        else if(object.getClass() == XnatImagesessiondataI.class) {
            return new Session((XnatImagesessiondataI) object);
        }
        else if(object.getClass() == XnatSubjectdataI.class) {
            return new Subject((XnatSubjectdataI) object);
        }
        return null;
    }




}
