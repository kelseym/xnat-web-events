package org.nrg.xnat.eventservice.aspects;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.model.XnatResourceI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.model.XnatSubjectdataI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.ProjectCreatedEvent;
import org.nrg.xnat.eventservice.events.ResourceSavedEvent;
import org.nrg.xnat.eventservice.events.ScanArchiveEvent;
import org.nrg.xnat.eventservice.events.SessionArchiveEvent;
import org.nrg.xnat.eventservice.events.SessionUpdateEvent;
import org.nrg.xnat.eventservice.events.SubjectCreatedEvent;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.SubjectUpdatedEvent;
import org.nrg.xnat.eventservice.services.XnatObjectIntrospectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Around(value = "execution(* org.nrg.xft.utils.SaveItemHelper.save(..)) && @annotation(org.nrg.xft.utils.EventServiceTrigger) && args(item, user,..)")
    public Object processItemSaveTrigger(final ProceedingJoinPoint joinPoint, ItemI item, UserI user) throws Throwable {
        Object retVal = null;
        try {
            String userLogin = user.getLogin();
            if(StringUtils.contains(item.getXSIType(), "xdat:user")){
                return joinPoint.proceed();

            }else if(StringUtils.equals(item.getXSIType(), "arc:project")){
                log.debug("New Project Data Save" + " : xsiType:" + item.getXSIType());
                XnatProjectdataI project = item instanceof XnatProjectdataI ? (XnatProjectdataI) item : new XnatProjectdata(item);
                eventService.triggerEvent(new ProjectCreatedEvent(project, userLogin), project.getId());

            } else if(StringUtils.equals(item.getXSIType(), "xnat:projectData")){
                log.debug("Existing Project Data Save" + " : xsiType:" + item.getXSIType());
                XnatProjectdataI project = item instanceof XnatProjectdataI ? (XnatProjectdataI) item : new XnatProjectdata(item);

            }else if(StringUtils.equals(item.getXSIType(), "xnat:subjectData")){
                XnatSubjectdataI subject = item instanceof XnatSubjectdataI ? (XnatSubjectdataI) item : new XnatSubjectdata(item);
                Boolean alreadyStored = xnatObjectIntrospectionService.storedInDatabase(subject);
                retVal = joinPoint.proceed();
                if(!alreadyStored && xnatObjectIntrospectionService.storedInDatabase(subject)){
                    log.debug("New Subject Data Save" + " : xsiType:" + item.getXSIType());
                    eventService.triggerEvent(new SubjectCreatedEvent(subject, userLogin), subject.getProject());
                } else{
                    log.debug("Existing Subject Data Save" + " : xsiType:" + item.getXSIType());
                    eventService.triggerEvent(new SubjectUpdatedEvent(subject, userLogin), subject.getProject());

                }

            }else if(item instanceof XnatImagesessiondataI || StringUtils.containsIgnoreCase(item.getXSIType(), "SessionData")) {
                log.debug("Session Data Save" + " : xsiType:" + item.getXSIType());
                XnatImagesessiondataI session = item instanceof XnatImagesessiondataI ? (XnatImagesessiondataI) item : new XnatImagesessiondata(item);
                Boolean alreadyStored = xnatObjectIntrospectionService.storedInDatabase((XnatExperimentdata) session);

                if (!alreadyStored) {
                    log.debug("New Session Data Save" + " : xsiType:" + item.getXSIType());
                    retVal = joinPoint.proceed();
                    eventService.triggerEvent(new SessionArchiveEvent(session, userLogin), session.getProject());
                    List<XnatImagescandataI> scans = session.getScans_scan();
                    if (scans != null && !scans.isEmpty()) {
                        scans.forEach(sc -> eventService.triggerEvent(new ScanArchiveEvent(sc, userLogin), session.getProject()));
                    }
                } else {
                    log.debug("Existing Session Data Save" + " : xsiType:" + item.getXSIType());
                    List<String> preScanIds = xnatObjectIntrospectionService.getScanIds((XnatExperimentdata) session);
                    retVal = joinPoint.proceed();
                    List<String> postScanIds = xnatObjectIntrospectionService.getScanIds((XnatExperimentdata) session);
                    if (postScanIds.size() > preScanIds.size()) {
                        postScanIds.removeAll(preScanIds);
                        List<XnatImagescandataI> newScans = session.getScans_scan().stream().filter(scn -> postScanIds.contains(scn.getId())).collect(Collectors.toList());

                    }
                    eventService.triggerEvent(new SessionUpdateEvent(session, userLogin), session.getProject());

                }
            } else if(item instanceof XnatResource || StringUtils.equals(item.getXSIType(), "xnat:resourceCatalog")){
                log.debug("Resource Data Save" + " : xsiType:" + item.getXSIType());
                XnatResourceI resource = item instanceof XnatResourceI ? (XnatResourceI) item : new XnatResource(item);
                String project = (String)(item.getProperty("project"));
                if((project == null || project.isEmpty() && item.getParent() != null)) {
                    project = (String)(item.getParent().getProperty("project"));
                }
                eventService.triggerEvent(new ResourceSavedEvent((XnatResourcecatalogI) resource, userLogin), project);
            }




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


        } catch (Throwable e) {
            log.error("Exception in EventServiceItemSaveAspect.processItemSaveTrigger() joinpoint.proceed(): " + joinPoint.toString()
                    + "item: " + item.toString()
                    + "user: " + user.toString()
                    + "\n" + e.getMessage());
        } finally {
            return retVal == null ? joinPoint.proceed() : retVal;
        }
    }


    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
            "&& args(item, user, ..)" +
            "&& execution(* delete(..))")
    public void triggerOnItemDelete(final JoinPoint joinPoint, ItemI item, UserI user) throws Throwable{
        try {


            String userLogin = user != null ? user.getLogin() : null;

            log.debug("triggerOnItemDelete AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);

        } catch (Throwable e){
            log.error("Exception processing triggerOnItemDelete" + e.getMessage());
            throw e;
        }
    }


}
