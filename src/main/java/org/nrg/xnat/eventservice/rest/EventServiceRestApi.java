package org.nrg.xnat.eventservice.rest;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.exceptions.SubscriptionValidationException;
import org.nrg.xnat.eventservice.exceptions.UnauthorizedException;
import org.nrg.xnat.eventservice.model.*;
import org.nrg.xnat.eventservice.services.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Api(description = "The (new) XNAT Event Handler API")
@XapiRestController
@RequestMapping(value = "/events")
public class EventServiceRestApi extends AbstractXapiRestController {
    private static final Logger log = LoggerFactory.getLogger(EventServiceRestApi.class);

    private static final String ID_REGEX = "\\d+";
    private static final String NAME_REGEX = "\\d*[^\\d]+\\d*";

    private static final String JSON = MediaType.APPLICATION_JSON_UTF8_VALUE;
    private static final String TEXT = MediaType.TEXT_PLAIN_VALUE;

    private EventService eventService;

    @Autowired
    public EventServiceRestApi(final EventService eventService,
                               final UserManagementServiceI userManagementService,
                               final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.eventService = eventService;
    }

//    @XapiRequestMapping(value = "/test", method = POST)
//    @ApiOperation(value = "Test OK")
//    public ResponseEntity<Void> getOk() {
//        return ResponseEntity.ok().build();
//    }
//
//
//    @XapiRequestMapping(value = "admintest", method = POST)
//    @ApiOperation(value = "Test Admin OK")
//    public ResponseEntity<Void> getAdminOk() throws UnauthorizedException {
//        final UserI userI = XDAT.getUserDetails();
//        checkCreateOrThrow(userI);
//        return ResponseEntity.ok().build();
//    }

    @XapiRequestMapping(value = "/subscription", method = POST)
    @ApiOperation(value = "Create a Subscription", code = 201)
    public ResponseEntity<Long> createSubscription(final @RequestBody SubscriptionCreator subscription)
            throws NrgServiceRuntimeException, UnauthorizedException, SubscriptionValidationException, JsonProcessingException {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        Subscription created = eventService.createSubscription(Subscription.create(subscription));
        return new ResponseEntity<>(created.id(), HttpStatus.CREATED);

    }

    @XapiRequestMapping(value = "/subscription{id}", method = POST)
    @ApiOperation(value = "Update an existing Subscription")
    public ResponseEntity<Void> updateSubscription(final @PathVariable long id, final @RequestBody Subscription subscription)
            throws NrgServiceRuntimeException, UnauthorizedException, SubscriptionValidationException, NotFoundException {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        final Subscription toUpdate =
                subscription.id() != null && subscription.id() == id
                        ? subscription
                        : subscription.toBuilder().id(id).build();
        eventService.updateSubscription(toUpdate);
        return ResponseEntity.ok().build();
    }

    @XapiRequestMapping(value = "/subscriptions", method = GET, produces = JSON)
    @ResponseBody
    public List<Subscription> getAllSubscriptions()
            throws NrgServiceRuntimeException, UnauthorizedException {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        return eventService.getSubscriptions();
    }

    @XapiRequestMapping(value = {"/subscription/{id}"}, method = GET, produces = JSON)
    @ApiOperation(value = "Get a Subscription by ID")
    @ResponseBody
    public Subscription retrieveSubscription(final @PathVariable long id) throws NotFoundException {
        return eventService.getSubscription(id);
    }



    @XapiRequestMapping(value = "/events", method = GET)
    @ResponseBody
    public List<SimpleEvent> getEvents() throws Exception {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        return eventService.getEvents();
    }

//    @Deprecated
//    @XapiRequestMapping(value = "/listeners", method = GET)
//    @ResponseBody
//    public List<Listener> getInstalledListeners()
//            throws NrgServiceRuntimeException, UnauthorizedException {
//        final UserI userI = XDAT.getUserDetails();
//        checkCreateOrThrow(userI);
//        return eventService.getInstalledListeners();
//    }


    @XapiRequestMapping(value = "/actionproviders", method = GET)
    @ApiOperation(value = "Get Action Providers and associated Actions")
    @ResponseBody
    public List<ActionProvider> getActionProviders()
            throws NrgServiceRuntimeException, UnauthorizedException {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        return eventService.getActionProviders();
    }

    @XapiRequestMapping(value = "/actions", method = GET)
    @ResponseBody
    public List<Action> getAllActions(final @RequestParam(value = "projectid", required = false) String projectId,
                                      final @RequestParam(value = "xnattype", required = false) String xnatType)
            throws NrgServiceRuntimeException, UnauthorizedException {
        final UserI user = XDAT.getUserDetails();
        checkCreateOrThrow(user);
        if(projectId != null && xnatType != null)
            return eventService.getAllActions(projectId, xnatType, user);
        else if(xnatType != null)
            return eventService.getAllActions(xnatType, user);
        else
            return eventService.getAllActions(user);
            
    }


    @XapiRequestMapping(value = "/actions/{provider}", method = GET)
    @ApiOperation(value = "Get a actions by provider")
    @ResponseBody
    public List<Action> getActions(final @PathVariable String provider)
            throws NrgServiceRuntimeException, UnauthorizedException {
        final UserI userI = XDAT.getUserDetails();
        checkCreateOrThrow(userI);
        return eventService.getActionsByProvider(provider);
    }


/*
    @XapiRequestMapping(value = "/action", method = POST)
    @ApiOperation(value = "Create an Event Service Action")
    public ResponseEntity<String> createAction(final @RequestBody EventServiceAction action)
            throws NrgServiceRuntimeException, UnauthorizedException {
        return new ResponseEntity<>(eventService.createAction(action).toString(), HttpStatus.CREATED);
    }
*/

    private void checkCreateOrThrow() throws UnauthorizedException {
        checkCreateOrThrow(XDAT.getUserDetails());
    }

    private void checkCreateOrThrow(final UserI userI) throws UnauthorizedException {
        if (!isAdmin(userI)) {
            throw new UnauthorizedException(String.format("User %s not authorize.", userI == null ? "" : userI.getLogin()));
        }
    }

    private boolean isAdmin(final UserI userI) {
        return getRoleHolder().isSiteAdmin(userI);
    }


    @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
    @ExceptionHandler(value = {SubscriptionValidationException.class})
    public String handleFailedSubscriptionValidation() {
        return "Subscription format failed to validate.";
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = {UnauthorizedException.class})
    public String handleUnauthorized(final Exception e) {
        return "Unauthorized.\n" + e.getMessage();
    }

}
