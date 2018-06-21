package org.nrg.xnat.eventservice.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xnat.eventservice.model.InternalRestEndpoint;
import org.nrg.xnat.eventservice.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Api(description = "API for the XNAT Internal REST Client")
@XapiRestController
public class InternalRestClientApi extends AbstractXapiRestController {


    @Autowired
    public InternalRestClientApi(final EventService eventService,
                               final UserManagementServiceI userManagementService,
                               final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
    }

    @XapiRequestMapping(restrictTo = Admin, value = "restclient/configuration", method = POST)
    @ApiOperation(value = "Create a configured endpoint", code = 201)
    public ResponseEntity<String> createConfiguration(final @RequestBody InternalRestEndpoint endpoint)
            throws NrgServiceRuntimeException, JsonProcessingException {


        return new ResponseEntity<>(endpoint.name() + ":" + Long.toString(endpoint.id()), HttpStatus.CREATED);

    }
}
