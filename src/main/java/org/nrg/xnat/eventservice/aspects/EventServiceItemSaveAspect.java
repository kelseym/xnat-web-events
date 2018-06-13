package org.nrg.xnat.eventservice.aspects;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.EventServiceTrigger;
import org.nrg.xnat.eventservice.events.ProjectCreatedEvent;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.XnatObjectIntrospectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Aspect
@Component
public class EventServiceItemSaveAspect {

    private static final Logger log = LoggerFactory.getLogger(EventServiceTriggerAspect.class);
    private EventService eventService;
    private XnatObjectIntrospectionService xnatObjectIntrospectionService;

    @Autowired
    public EventServiceItemSaveAspect(EventService eventService, XnatObjectIntrospectionService xnatObjectIntrospectionService) {
        this.eventService = eventService;
        this.xnatObjectIntrospectionService = xnatObjectIntrospectionService;
    }

    @Pointcut("execution(* org.nrg.xft.utils.SaveItemHelper.save(..)) && @annotation(eventServiceTrigger)")
    public void eventServiceItemSavePointcut(final EventServiceTrigger eventServiceTrigger) {
    }

    @Around(value = "eventServiceItemSavePointcut(eventServiceTrigger)", argNames = "joinPoint,eventServiceTrigger")
    public Object processItemSaveTrigger(final ProceedingJoinPoint joinPoint,
                                     final EventServiceTrigger eventServiceTrigger) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            List<Object> users = Arrays.stream(args).filter(u -> u instanceof UserI).collect(Collectors.toList());
            UserI user = users == null || users.isEmpty() ? null : (UserI)users.get(0);
            String userLogin = user == null ? "" : user.getLogin();
            List<Object> items = Arrays.stream(args).filter(a -> a instanceof ItemI && !(a instanceof UserI)).collect(Collectors.toList());
            Optional<Object> optional = items.stream().findFirst();
            Object joinPointReturn = null;
            if(optional.isPresent()){
                ItemI item = (ItemI)optional.get();
                if(StringUtils.equals(item.getXSIType(), "arc:project")){
                    // ** New Project Data ** //
                    XnatProjectdataI project = item instanceof XnatProjectdataI ? (XnatProjectdataI)item : new XnatProjectdata((ItemI)item);
                    joinPointReturn = joinPoint.proceed();
                    eventService.triggerEvent(new ProjectCreatedEvent(project, userLogin), project.getId());

                } else if(StringUtils.equals(item.getXSIType(), "xnat:projectData")){
                    // ** Existing Project Data ** //
                    XnatProjectdataI project = item instanceof XnatProjectdataI ? (XnatProjectdataI)item : new XnatProjectdata((ItemI)item);
                    Integer beforeResourceCount = xnatObjectIntrospectionService.getResourceCount(project);
                    joinPoint.proceed();
                    Integer afterResourceCount = xnatObjectIntrospectionService.getResourceCount(project);

                }else if(StringUtils.containsIgnoreCase(item.getXSIType(), "SessionData")){
                    // ** Session Data ** //
                    XnatImagesessiondataI session = item instanceof XnatImagesessiondataI ? (XnatImagesessiondataI) item : new XnatImagesessiondata((ItemI)item);
                    //Integer beforeResourceCount = xnatObjectIntrospectionService.getResourceCount(project);
                    joinPoint.proceed();
                    //Integer afterResourceCount = xnatObjectIntrospectionService.getResourceCount(project);
                } else {
                    joinPointReturn = true;
                }

                return joinPointReturn;
                //if ((StringUtils.equals(item.getXSIType(), "xnat:subjectData")
                //        || StringUtils.containsIgnoreCase(item.getXSIType(), "SessionData")
                //        || StringUtils.equals(item.getXSIType(), "xnat:projectData")
                //        || StringUtils.equals(item.getXSIType(), "arc:project")
                //        || StringUtils.equals(item.getXSIType(), "xnat:experimentData")
                //        || StringUtils.equals(item.getXSIType(), "xnat:subjectAssessorData")
                //        || StringUtils.equals(item.getXSIType(), "xnat:resourceCatalog")
                //        || StringUtils.equals(item.getXSIType(), "xnat:imageAssessorData")
                //        || StringUtils.equals(item.getXSIType(), "icr:roiCollectionData")
                //)){
                //    log.debug("Proceeding with processItemSaveTrigger pointcut on " + item.getXSIType());
                //Hashtable props = item.getProps();
                //props.keySet().stream().forEach(k-> log.debug("Key: " + k.toString() + "\n" + props.get(k).toString()));

            }

        } catch (Throwable e) {
            log.error("Exception in EventServiceItemSaveAspect.processItemSaveTrigger() joinpoint.proceed()", e.getMessage());
        }
        return joinPoint.proceed();
    }

}
