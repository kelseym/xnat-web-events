package org.nrg.xnat.eventservice.aspects;


import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.model.XnatSubjectdataI;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.events.ImageAssessorSaveEvent;
import org.nrg.xnat.eventservice.events.ProjectCreatedEvent;
import org.nrg.xnat.eventservice.events.ProjectDeletedEvent;
import org.nrg.xnat.eventservice.events.SessionArchiveEvent;
import org.nrg.xnat.eventservice.events.SessionDeletedEvent;
import org.nrg.xnat.eventservice.events.SessionUpdateEvent;
import org.nrg.xnat.eventservice.events.SubjectCreatedEvent;
import org.nrg.xnat.eventservice.events.SubjectDeletedEvent;
import org.nrg.xnat.eventservice.services.EventService;
import org.nrg.xnat.eventservice.services.XnatObjectIntrospectionService;
import org.nrg.xnat.helpers.resource.direct.ResourceModifierA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

//@Aspect
//@Component
public class EventServiceTriggerAspect {
    private static final Logger log = LoggerFactory.getLogger(EventServiceTriggerAspect.class);

    private EventService eventService;
    private XnatObjectIntrospectionService xnatObjectIntrospectionService;

    @Autowired
    public EventServiceTriggerAspect(EventService eventService, XnatObjectIntrospectionService xnatObjectIntrospectionService) {
        this.eventService = eventService;
        this.xnatObjectIntrospectionService = xnatObjectIntrospectionService;
    }

/*
    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
            "&& args(item, user, ..)" +
            "&& execution(* save(..))")
    public void triggerOnItemSave(final JoinPoint joinPoint, ItemI item,UserI user) throws Throwable{
        if (log.isDebugEnabled()) {
            try {
                String userLogin = user != null ? user.getLogin() : null;
                Object[] args = joinPoint.getArgs();
                Boolean isUpdate = Arrays.stream(args)
                                         .filter(a -> a instanceof EventMetaI)
                                         .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);

                if (!(StringUtils.equals(item.getXSIType(), "xnat:subjectData")
                        || StringUtils.containsIgnoreCase(item.getXSIType(), "SessionData")
                        || StringUtils.equals(item.getXSIType(), "xnat:projectData")
                        || StringUtils.equals(item.getXSIType(), "xnat:experimentData")
                        || StringUtils.equals(item.getXSIType(), "xnat:subjectAssessorData")
                        || StringUtils.equals(item.getXSIType(), "xnat:resourceCatalog")
                        || StringUtils.equals(item.getXSIType(), "xnat:imageAssessorData")
                        || StringUtils.equals(item.getXSIType(), "icr:roiCollectionData")
                ))
                    return;
                item.getClass();

                log.debug("triggerOnItemSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                        "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                        "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                        "  UserI = " + userLogin);
                log.debug("\n\n" + item.getItem().toString() + "\n\n");


            } catch(Throwable e){
                log.error("Exception processing triggerOnItemSave" + e.getMessage());
                throw e;
            }
        }
    }
*/
    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
            "&& args(item, user, ..)" +
            "&& execution(* save(..))")
    public void triggerOnItemSave(final JoinPoint joinPoint, ItemI item,UserI user) throws Throwable {
        try {
            String userLogin = user != null ? user.getLogin() : null;
            Object[] args = joinPoint.getArgs();
            Boolean isUpdate = Arrays.stream(args)
                                     .filter(a -> a instanceof EventMetaI)
                                     .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);

            // ** Session Data ** //
            if(StringUtils.containsIgnoreCase(item.getXSIType(), "SessionData")){
                XnatImagesessiondataI session = null;
                if(item instanceof XnatImagesessiondata){ session = (XnatImagesessiondataI) item; }
                else{ session = new XnatImagesessiondata(item); }

                Boolean isModified = xnatObjectIntrospectionService.isModified(session);
                Boolean hasResource = xnatObjectIntrospectionService.hasResource(session);
                if(isModified || hasResource) {
                        eventService.triggerEvent(new SessionUpdateEvent(session, userLogin), session.getProject());
                } else {
                    eventService.triggerEvent(new SessionArchiveEvent(session, userLogin), session.getProject());
                }
            }

            // ** Project Data ** //
            else if(StringUtils.equals(item.getXSIType(), "arc:project")){

                XnatProjectdataI project = null;
                if(item instanceof XnatProjectdataI){ project = (XnatProjectdataI) item; }
                else{ project = new XnatProjectdata(item); }
                eventService.triggerEvent(new ProjectCreatedEvent(project, userLogin), project.getId());

            }

            // ** Subject Data ** //
            else if(StringUtils.equals(item.getXSIType(), "xnat:subjectData")){
                log.debug("Save called on xnat:subjectData Item.");
                XnatSubjectdataI subject = null;
                if(item instanceof XnatSubjectdataI) {subject = (XnatSubjectdataI) item;}
                else { subject = new XnatSubjectdata(item);}
                Boolean hasHistory = xnatObjectIntrospectionService.hasHistory(subject);
                Boolean hasResource = xnatObjectIntrospectionService.hasResource(subject);
                if(!hasHistory && !hasResource) {
                    eventService.triggerEvent(new SubjectCreatedEvent(subject, userLogin), subject.getProject());
                }

            }

            else if (log.isDebugEnabled()) {
                if ((StringUtils.equals(item.getXSIType(), "xnat:subjectData")
                        || StringUtils.containsIgnoreCase(item.getXSIType(), "SessionData")
                        || StringUtils.equals(item.getXSIType(), "xnat:projectData")
                        || StringUtils.equals(item.getXSIType(), "arc:project")
                        || StringUtils.equals(item.getXSIType(), "xnat:experimentData")
                        || StringUtils.equals(item.getXSIType(), "xnat:subjectAssessorData")
                        || StringUtils.equals(item.getXSIType(), "xnat:resourceCatalog")
                        || StringUtils.equals(item.getXSIType(), "xnat:imageAssessorData")
                        || StringUtils.equals(item.getXSIType(), "icr:roiCollectionData")
                ))
                {
                    log.debug("triggerOnItemSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                            "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                            "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                            "  UserI = " + userLogin);
                    log.debug("\n\n" + item.getItem().toString() + "\n\n");
                }
            }

        } catch (Throwable e) {
            log.error("Exception processing triggerOnItemSave" + e.getMessage());
        }
    }


//    @Pointcut("execution(* org.nrg.xft.utils.SaveItemHelper.save(..))")
//    public void saveItemHelperPointcut() {}
//
//    @Around(value = "saveItemHelperPointcut()")
//    public Object aroundItemSave(final ProceedingJoinPoint joinPoint) throws Throwable {
//        Object[] args = joinPoint.getArgs();
//        return joinPoint.proceed();
//    }



//    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
//            "&& args(item, user, ..)" +
//            "&& execution(* save(..))")
    public void triggerOnProjectSave(final JoinPoint joinPoint, XnatProjectdata item, UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;
            Object[] args = joinPoint.getArgs();
            Boolean isUpdate = Arrays.stream(args)
                                     .filter(a -> a instanceof EventMetaI)
                                     .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);

            log.debug("triggerOnProjectSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
            eventService.triggerEvent(new ProjectCreatedEvent(item, userLogin), item.getId());
        } catch (Throwable e){
            log.error("Exception processing triggerOnProjectSave" + e.getMessage());
            throw e;
        }
    }



//    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
//            "&& args(item, user, ..)" +
//            "&& execution(* save(..))")
    public void triggerOnExperimentDataSave(final JoinPoint joinPoint, XnatExperimentdata item, UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;
            Object[] args = joinPoint.getArgs();
            Boolean isUpdate = Arrays.stream(args)
                                     .filter(a -> a instanceof EventMetaI)
                                     .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);
            log.debug("triggerOnExperimentDataSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
        } catch (Throwable e){
            log.error("Exception processing triggerOnExperimentDataSave" + e.getMessage());
            throw e;
        }
    }



//    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
//            "&& args(item, user, ..)" +
//            "&& execution(* save(..))")
    public void triggerOnSubjectAssessorSave(final JoinPoint joinPoint, XnatSubjectassessordata item, UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;
            Object[] args = joinPoint.getArgs();
            Boolean isUpdate = Arrays.stream(args)
                                     .filter(a -> a instanceof EventMetaI)
                                     .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);
            log.debug("triggerOnSubjectAssessorSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
        } catch (Throwable e){
            log.error("Exception processing triggerOnSubjectSave" + e.getMessage());
            throw e;
        }
    }

//    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
//            "&& args(item, user, ..)" +
//            "&& execution(* save(..))")
    public void triggerOnImageAssessorSave(final JoinPoint joinPoint, XnatImageassessordata item, UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;
            Object[] args = joinPoint.getArgs();
            Boolean isUpdate = Arrays.stream(args)
                                     .filter(a -> a instanceof EventMetaI)
                                     .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);
            log.debug("triggerOnImageAssessorSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
            eventService.triggerEvent(new ImageAssessorSaveEvent(item, userLogin), item.getProject());

        } catch (Throwable e){
            log.error("Exception processing triggerOnSubjectSave" + e.getMessage());
            throw e;
        }
    }

//    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
//            "&& args(item, user, ..)" +
//            "&& execution(* save(..))")
    public void triggerOnResourceCatalogSave(final JoinPoint joinPoint, XnatResourcecatalog item, UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;
            Object[] args = joinPoint.getArgs();
            Boolean isUpdate = Arrays.stream(args)
                                     .filter(a -> a instanceof EventMetaI)
                                     .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);
            log.debug("triggerOnResourceCatalogSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
        } catch (Throwable e){
            log.error("Exception processing triggerOnSubjectSave" + e.getMessage());
            throw e;
        }
    }

//    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
//            "&& args(item, user, ..)" +
//            "&& execution(* save(..))")
    public void triggerOnResourceSave(final JoinPoint joinPoint, XnatResource item, UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;
            Object[] args = joinPoint.getArgs();
            Boolean isUpdate = Arrays.stream(args)
                                     .filter(a -> a instanceof EventMetaI)
                                     .allMatch(a -> a instanceof ResourceModifierA.UpdateMeta);
            log.debug("triggerOnResourceSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
        } catch (Throwable e){
            log.error("Exception processing triggerOnSubjectSave" + e.getMessage());
            throw e;
        }
    }




//    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
//            "&& args(item, user, ..)" +
//            "&& execution(* save(..))")
    public void triggerOnSubjectSave(final JoinPoint joinPoint, XnatSubjectdata item,UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;

            log.debug("triggerOnSubjectSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
            eventService.triggerEvent(new SubjectCreatedEvent(item, userLogin), item.getProject());
        } catch (Throwable e){
            log.error("Exception processing triggerOnSubjectSave" + e.getMessage());
            throw e;
        }
    }

    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
            "&& args(item, user, ..)" +
            "&& execution(* save(..))")
    public void triggerOnUserSave(final JoinPoint joinPoint, XdatUser item, UserI user) throws Throwable{
        try {
            String userLogin = user != null ? user.getLogin() : null;

            log.debug("triggerOnUserSave AfterReturning aspect called after " + joinPoint.getSignature().getName() + "." +
                    "  ItemI type = " + (item != null ? item.getClass().getSimpleName() : "null") +
                    "  ItemI xsiType = " + (item != null ? item.getXSIType() : "null") +
                    "  UserI = " + userLogin);
            //eventService.triggerEvent(new SessionArchiveEvent(item, userLogin), item.getId());

        } catch (Throwable e){
            log.error("Exception processing triggerOnUserSave" + e.getMessage());
            throw e;
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

            if(StringUtils.equals(item.getXSIType(),"xnat:projectData")){
                XnatProjectdataI project = new XnatProjectdata(item);
                eventService.triggerEvent(new ProjectDeletedEvent(project, userLogin), project.getId());

            }else if(StringUtils.containsIgnoreCase(item.getXSIType(),"xnat:") &&
                    StringUtils.containsIgnoreCase(item.getXSIType(),"SessionData")){
                XnatImagesessiondataI session = new XnatImagesessiondata(item);
                eventService.triggerEvent(new SessionDeletedEvent(session, userLogin), session.getProject());

            }else if(StringUtils.equals(item.getXSIType(), "xnat:subjectData")){
                XnatSubjectdataI subject = new XnatSubjectdata(item);
                eventService.triggerEvent(new SubjectDeletedEvent(subject, userLogin), subject.getProject());

            }

        } catch (Throwable e){
            log.error("Exception processing triggerOnItemDelete" + e.getMessage());
            throw e;
        }
    }

}
