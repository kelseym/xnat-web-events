/*
 * web: org.nrg.xapi.rest.notifications.NotificationsApi
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xapi.rest.notifications;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.framework.services.SerializerService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.preferences.NotificationsPreferences;
import org.nrg.xdat.preferences.SmtpServer;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xnat.services.XnatAppInfo;
import org.nrg.xnat.utils.XnatHttpUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.nrg.xdat.preferences.SiteConfigPreferences.SITE_URL;
import static org.nrg.xdat.security.helpers.AccessLevel.Admin;

@Api(description = "XNAT Notifications management API")
@XapiRestController
@RequestMapping(value = "/notifications")
@Slf4j
public class NotificationsApi extends AbstractXapiRestController {
    public static final String POST_PROPERTIES_NOTES = "Sets the mail service host, port, username, password, and protocol. You can set "
                                                       + "extra properties on the mail sender (e.g. for configuring SSL or TLS transport) by "
                                                       + "specifying the property name and value. Any parameters submitted that are not one "
                                                       + "of the standard mail sender attributes is set as a mail sender property. You can "
                                                       + "remove existing properties by setting the property with an empty value. This will "
                                                       + "override any existing configuration. You can change the values of properties by calling "
                                                       + "the API method for that specific property or by calling the PUT version of this method.";
    public static final String PUT_PROPERTIES_NOTES  = "Sets the mail service host, port, username, password, and protocol. You can set "
                                                       + "extra properties on the mail sender (e.g. for configuring SSL or TLS transport) by "
                                                       + "specifying the property name and value. Any parameters submitted that are not one "
                                                       + "of the standard mail sender attributes is set as a mail sender property. You can "
                                                       + "remove existing properties by setting the property with an empty value. This will "
                                                       + "modify the existing server configuration. You can completely replace the configuration "
                                                       + "by calling the POST version of this method.";

    @Inject
    public NotificationsApi(final UserManagementServiceI userManagementService, final RoleHolder roleHolder, final NotificationsPreferences notificationsPrefs, final XnatAppInfo appInfo, final SerializerService serializer) {
        super(userManagementService, roleHolder);
        _notificationsPrefs = notificationsPrefs;
        _appInfo = appInfo;
        _serializer = serializer;
    }

    @ApiOperation(value = "Returns the full map of site configuration properties.", notes = "Complex objects may be returned as encapsulated JSON strings.", response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Site configuration properties successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set site configuration properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<Properties> getNotificationsProperties(@ApiParam(hidden = true) final HttpServletRequest request) {
        final String username = getSessionUser().getUsername();
        log.debug("User {} requested the site configuration.", username);

        final Properties preferences = _notificationsPrefs.asProperties();

        if (!_appInfo.isInitialized()) {
            log.info("The site is being initialized by user {}. Setting default values from context.", username);
            preferences.put(SITE_URL, XnatHttpUtils.getServerRoot(request));
        }

        preferences.put("emailRecipientErrorMessages", _notificationsPrefs.getEmailRecipientErrorMessages());
        preferences.put("emailRecipientIssueReports", _notificationsPrefs.getEmailRecipientIssueReports());
        preferences.put("emailRecipientNewUserAlert", _notificationsPrefs.getEmailRecipientNewUserAlert());
        preferences.put("emailRecipientUpdate", _notificationsPrefs.getEmailRecipientUpdate());

        return new ResponseEntity<>(preferences, HttpStatus.OK);
    }

    @ApiOperation(value = "Sets a map of notifications properties.", notes = "Sets the notifications properties specified in the map.")
    @ApiResponses({@ApiResponse(code = 200, message = "Notifications properties successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set notifications properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Void> setNotificationsProperties(@ApiParam(value = "The map of notifications properties to be set.", required = true) @RequestBody final Properties properties) {
        logSetProperties(properties);

        for (final String name : properties.stringPropertyNames()) {
            try {
                if (StringUtils.equals(name, "emailRecipientErrorMessages")) {
                    _notificationsPrefs.setEmailRecipientErrorMessages(properties.getProperty(name));
                } else if (StringUtils.equals(name, "emailRecipientIssueReports")) {
                    _notificationsPrefs.setEmailRecipientIssueReports(properties.getProperty(name));
                } else if (StringUtils.equals(name, "emailRecipientNewUserAlert")) {
                    _notificationsPrefs.setEmailRecipientNewUserAlert(properties.getProperty(name));
                } else if (StringUtils.equals(name, "emailRecipientUpdate")) {
                    _notificationsPrefs.setEmailRecipientUpdate(properties.getProperty(name));
                } else if (StringUtils.equals(name, "smtpServer")) {
                    _notificationsPrefs.setSmtpServer(_serializer.deserializeJson(properties.getProperty(name), SmtpServer.class));
                } else {
                    _notificationsPrefs.set(properties.getProperty(name), name);
                }
                log.info("Set property {} to value: {}", name, properties.get(name));
            } catch (InvalidPreferenceName invalidPreferenceName) {
                log.error("Got an invalid preference name error for the preference '{}', which is weird because the site configuration is not strict", name);
            } catch (IOException e) {
                log.error("An error occurred deserializing the preference '{}', which is just lame.", name);
            }
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the full SMTP server configuration.",
                  notes = "Returns the configuration as a map of the standard Java mail sender properties&ndash;host, port, protocol, username, and password&ndash;along with any extended properties required for the configuration, e.g. configuring SSL- or TLS-secured SMTP services.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "SMTP service configuration properties successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set site configuration properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = "smtp", produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<Properties> getSmtpServerProperties() {
        log.debug("User {} requested the site SMTP service configuration.", getSessionUser().getUsername());
        return new ResponseEntity<>(_notificationsPrefs.getSmtpServer().asProperties(), HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the mail service properties. This return the SMTP server configuration as it exists after being set.",
                  notes = POST_PROPERTIES_NOTES,
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service properties successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setAllMailProperties(@ApiParam("The value to set for the email host.") @RequestParam(value = "hostname", required = false, defaultValue = NOT_SET) final String hostname,
                                                           @ApiParam("The value to set for the email port.") @RequestParam(value = "port", required = false, defaultValue = "-1") final int port,
                                                           @ApiParam("The value to set for the email username.") @RequestParam(value = "username", required = false, defaultValue = NOT_SET) final String username,
                                                           @ApiParam("The value to set for the email password.") @RequestParam(value = "password", required = false, defaultValue = NOT_SET) final String password,
                                                           @ApiParam("The value to set for the email protocol.") @RequestParam(value = "protocol", required = false, defaultValue = NOT_SET) final String protocol,
                                                           @ApiParam("Values to set for extra mail properties. An empty value indicates that an existing property should be removed.") @RequestParam final Properties properties) {
        cleanProperties(properties);
        logConfigurationSubmit(hostname, port, username, password, protocol, properties);

        _notificationsPrefs.setSmtpServer(new SmtpServer(hostname, port, protocol, username, password, properties));

        return getSmtpServerProperties();
    }

    @ApiOperation(value = "Sets the submitted mail service properties. This returns the SMTP server configuration as it exists after being set.",
                  notes = PUT_PROPERTIES_NOTES,
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service properties successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<Properties> setSubmittedMailProperties(@ApiParam("The value to set for the email host.") @RequestParam(value = "hostname", required = false, defaultValue = NOT_SET) final String host,
                                                                 @ApiParam("The value to set for the email port.") @RequestParam(value = "port", required = false, defaultValue = "-1") final int port,
                                                                 @ApiParam("The value to set for the email username.") @RequestParam(value = "username", required = false, defaultValue = NOT_SET) final String username,
                                                                 @ApiParam("The value to set for the email password.") @RequestParam(value = "password", required = false, defaultValue = NOT_SET) final String password,
                                                                 @ApiParam("The value to set for the email protocol.") @RequestParam(value = "protocol", required = false, defaultValue = NOT_SET) final String protocol,
                                                                 @ApiParam("Values to set for extra mail properties. An empty value indicates that an existing property should be removed.") @RequestParam final Properties properties) {
        logConfigurationSubmit(host, port, username, password, protocol, properties);

        _notificationsPrefs.setSmtpServer(new SmtpServer(host, port, username, password, protocol, properties));

        return getSmtpServerProperties();
    }

    @ApiOperation(value = "Sets the mail service host.",
                  notes = "Sets the mail service host.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service host successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service host."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/host/{host}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<Properties> setHostProperty(@ApiParam(value = "The value to set for the email host.", required = true) @PathVariable final String host) {
        log.info("User {} setting mail host to: {}", getSessionUser().getLogin(), host);
        _notificationsPrefs.setSmtpHostname(host);
        return getSmtpServerProperties();
    }

    @ApiOperation(value = "Sets the mail service port.",
                  notes = "Sets the mail service port.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service port successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service port."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/port/{port}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<Properties> setPortProperty(@ApiParam(value = "The value to set for the email port.", required = true) @PathVariable final int port) {
        log.info("User {} setting mail port to: {}", getSessionUser().getLogin(), port);
        _notificationsPrefs.setSmtpPort(port);
        return getSmtpServerProperties();
    }

    @ApiOperation(value = "Sets the mail service protocol.",
                  notes = "Sets the mail service protocol.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service protocol successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service protocol."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/protocol/{protocol}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<Properties> setProtocolProperty(@ApiParam(value = "The value to set for the email protocol.", required = true) @PathVariable final String protocol) {
        log.info("User {} setting mail protocol to: {}", getSessionUser().getLogin(), protocol);
        _notificationsPrefs.setSmtpProtocol(protocol);
        return getSmtpServerProperties();
    }

    @ApiOperation(value = "Sets the mail service username.",
                  notes = "Sets the mail service username.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service username successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service username."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/username/{username}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<Properties> setUsernameProperty(@ApiParam(value = "The value to set for the email username.", required = true) @PathVariable final String username) {
        log.info("User {} setting mail username to: {}", getSessionUser().getUsername(), username);
        _notificationsPrefs.setSmtpUsername(username);
        return getSmtpServerProperties();
    }

    @ApiOperation(value = "Sets the mail service password.",
                  notes = "Sets the mail service password.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service password successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service password."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/password/{password}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<Properties> setPasswordProperty(@ApiParam(value = "The value to set for the email password.", required = true) @PathVariable final String password) {
        log.info("User {} setting mail password", getSessionUser().getUsername());
        _notificationsPrefs.setSmtpPassword(password);
        return getSmtpServerProperties();
    }

    @ApiOperation(value = "Gets the value for a specified Java mail property.",
                  notes = "The value is always returned as a string.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Property found and returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service properties."),
                   @ApiResponse(code = 404, message = "Specified key not found in the mail service properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/property/{property}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getExtendedProperty(@ApiParam(value = "The mail property to be retrieved.", required = true) @PathVariable final String property) {
        if (!_notificationsPrefs.getSmtpServer().getMailProperties().containsKey(property)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("User {} retrieving value for property {}", getSessionUser().getLogin(), property);
        return new ResponseEntity<>(_notificationsPrefs.getSmtpMailProperty(property), HttpStatus.OK);
    }

    @ApiOperation(value = "Sets a Java mail property with the submitted name and value.",
                  notes = "Setting a property to an existing value will overwrite the existing value. The value returned by this function contains the previous value (if any).",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Property found and returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service properties."),
                   @ApiResponse(code = 404, message = "Specified key not found in the mail service properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/property/{property}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<String> setExtendedProperty(@ApiParam(value = "The name of the extended mail property to set.", required = true) @PathVariable final String property,
                                                      @ApiParam(value = "The value to set for the extended mail property.", required = true) @RequestBody final String value) {
        return setExtendedPropertyFromPath(property, value);
    }

    @ApiOperation(value = "Sets a Java mail property with the submitted name and value.",
                  notes = "Setting a property to an existing value will overwrite the existing value. The value returned by this function contains the previous value (if any).",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Mail service password successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service password."),
                   @ApiResponse(code = 404, message = "Specified key not found in the mail service properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"smtp/property/{property}/{value}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.PUT}, restrictTo = Admin)
    public ResponseEntity<String> setExtendedPropertyFromPath(@ApiParam(value = "The name of the extended mail property to set.", required = true) @PathVariable final String property,
                                                              @ApiParam(value = "The value to set for the extended mail property.", required = true) @PathVariable final String value) {
        log.info("User {} setting mail password", getSessionUser().getUsername());
        return new ResponseEntity<>(_notificationsPrefs.setSmtpMailProperty(property, value), HttpStatus.OK);
    }

    @ApiOperation(value = "Removes the value for a specified Java mail property.",
                  notes = "This completely removes the specified mail property. The value returned by this function contains the previous value (if any).",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Property found and returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the mail service properties."),
                   @ApiResponse(code = 404, message = "Specified key not found in the mail service properties."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = "smtp/property/{property}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    public ResponseEntity<String> removeExtendedProperty(@ApiParam(value = "The mail property to be removed.", required = true) @PathVariable final String property) {
        if (!_notificationsPrefs.getSmtpServer().getMailProperties().containsKey(property)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("User {} removing value for property {}", getSessionUser().getLogin(), property);
        return new ResponseEntity<>(_notificationsPrefs.removeSmtpMailProperty(property), HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email message for contacting help.",
                  notes = "Sets the email message that people should receive when contacting help.")
    @ApiResponses({@ApiResponse(code = 200, message = "Help email message successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the help email message."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/help"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Void> setHelpContactInfo(@ApiParam(value = "The email message for contacting help.", required = true) @RequestParam final String message) {
        _notificationsPrefs.setHelpContactInfo(message);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email message for user registration.",
                  notes = "Sets the email message that people should receive when they register. Link for email validation is auto-populated.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "User registration email message successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the user registration email message."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/registration"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setEmailMessageUserRegistration(@ApiParam(value = "The email message for user registration.", required = true) @RequestParam final String message) {
        _notificationsPrefs.setEmailMessageUserRegistration(message);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email message for forgot username.",
                  notes = "Sets the email message that people should receive when they click that they forgot their username.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Forgot username email message successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the forgot username email message."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/forgotusername"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setEmailMessageForgotUsernameRequest(@ApiParam(value = "The email message for forgot username.", required = true) @RequestParam final String message) {
        _notificationsPrefs.setEmailMessageForgotUsernameRequest(message);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email message for password reset.",
                  notes = "Sets the email message that people should receive when they click to reset their password.  Link for password reset is auto-populated.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Password reset message successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the password reset message."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/passwordreset"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setEmailMessageForgotPasswordReset(@ApiParam(value = "The email message for password reset.", required = true) @RequestParam final String message) {
        _notificationsPrefs.setEmailMessageForgotPasswordReset(message);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the email message for contacting help.",
                  notes = "This returns the email message that people should receive when contacting help.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for contacting help successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for contacting help."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/help"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getHelpContactInfo() {
        return new ResponseEntity<>(_notificationsPrefs.getHelpContactInfo(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the email message for user registration.",
                  notes = "This returns the email message that people should receive when they register. Link for email validation is auto-populated.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for user registration successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for user registration."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/registration"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getEmailMessageUserRegistration() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailMessageUserRegistration(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the email message for forgot username.",
                  notes = "This returns the email message that people should receive when they click that they forgot their username.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for forgot username successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for forgot username."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/forgotusername"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getEmailMessageForgotUsernameRequest() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailMessageForgotUsernameRequest(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns the email message for password reset.",
                  notes = "This returns the email message that people should receive when they click to reset their password.  Link for password reset is auto-populated.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for password reset successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for password reset."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"messages/passwordreset"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getEmailMessageForgotPasswordReset() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailMessageForgotPasswordReset(), HttpStatus.OK);
    }

    @ApiOperation(value = "Sets whether admins should be notified of user registration.",
                  notes = "Sets whether admins should be notified of user registration.")
    @ApiResponses({@ApiResponse(code = 200, message = "Whether admins should be notified of user registration successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set whether admins should be notified of user registration."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/registration"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Void> setNotifyAdminUserRegistration(@ApiParam(value = "Whether admins should be notified of user registration successfully set.", required = true) @RequestParam final boolean notify) {
        _notificationsPrefs.setNotifyAdminUserRegistration(notify);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets whether admins should be notified of pipeline processing submit.",
                  notes = "Sets whether admins should be notified of pipeline processing submit.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether admins should be notified of pipeline processing submit successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set whether admins should be notified of pipeline processing submit."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/pipeline"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setNotifyAdminPipelineEmails(@ApiParam(value = "Whether admins should be notified of pipeline processing submit successfully set.", required = true) @RequestParam final boolean notify) {
        _notificationsPrefs.setNotifyAdminPipelineEmails(notify);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets whether admins should be notified of project access requests.",
                  notes = "Sets whether admins should be notified of project access requests.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether admins should be notified of project access requests successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set whether admins should be notified of project access requests."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/par"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setNotifyAdminProjectAccessRequest(@ApiParam(value = "Whether admins should be notified of project access requests successfully set.", required = true) @RequestParam final boolean notify) {
        _notificationsPrefs.setNotifyAdminProjectAccessRequest(notify);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets whether admins should be notified of session transfer.",
                  notes = "Sets whether admins should be notified of session transfer by user.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether admins should be notified of session transfer successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set whether admins should be notified of session transfer."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/transfer"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setNotifyAdminSessionTransfer(@ApiParam(value = "Whether admins should be notified of session transfer successfully set.", required = true) @RequestParam final boolean notify) {
        _notificationsPrefs.setNotifyAdminSessionTransfer(notify);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Returns whether admins should be notified of user registration.",
                  notes = "This returns whether admins should be notified of user registration.",
                  response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for contacting help successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for contacting help."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/registration"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<Boolean> getNotifyAdminUserRegistration() {
        return new ResponseEntity<>(_notificationsPrefs.getNotifyAdminUserRegistration(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns whether admins should be notified of pipeline processing submit.",
                  notes = "This returns whether admins should be notified of pipeline processing submit.",
                  response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for user registration successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for user registration."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/pipeline"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<Boolean> getNotifyAdminPipelineEmails() {
        return new ResponseEntity<>(_notificationsPrefs.getNotifyAdminPipelineEmails(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns whether admins should be notified of project access requests.",
                  notes = "This returns whether admins should be notified of project access requests.",
                  response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for forgot username successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for forgot username."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/par"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<Boolean> getNotifyAdminProjectAccessRequest() {
        return new ResponseEntity<>(_notificationsPrefs.getNotifyAdminProjectAccessRequest(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns whether admins should be notified of session transfer.",
                  notes = "This returns whether admins should be notified of session transfer.",
                  response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Email message for password reset successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get email message for password reset."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"notify/transfer"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<Boolean> getNotifyAdminSessionTransfer() {
        return new ResponseEntity<>(_notificationsPrefs.getNotifyAdminSessionTransfer(), HttpStatus.OK);
    }

    @ApiOperation(value = "Sets whether non-users should be able to subscribe to notifications.",
                  notes = "Sets whether non-users should be able to subscribe to notifications.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether non-users should be able to subscribe to notifications."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set whether non-users should be able to subscribe to notifications."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"allow/nonusersubscribers/{setting}"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setEmailAllowNonuserSubscribers(@ApiParam(value = "Whether non-users should be able to subscribe to notifications.", required = true) @PathVariable final boolean setting) {
        _notificationsPrefs.setEmailAllowNonuserSubscribers(setting);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Returns whether non-users should be able to subscribe to notifications.",
                  notes = "This returns whether non-users should be able to subscribe to notifications.",
                  response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Whether non-users should be able to subscribe to notifications successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get whether non-users should be able to subscribe to notifications."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"allow/nonusersubscribers"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<Boolean> getEmailAllowNonuserSubscribers() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailAllowNonuserSubscribers(), HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email addresses for error notifications.",
                  notes = "Sets the email addresses that should be subscribed to error notifications.")
    @ApiResponses({@ApiResponse(code = 200, message = "Error subscribers successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the error subscribers."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/error"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Void> setErrorSubscribers(@ApiParam(value = "The values to set for email addresses for error notifications.", required = true) @RequestParam final String subscribers) {
        _notificationsPrefs.setEmailRecipientErrorMessages(subscribers);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email addresses for issue notifications.",
                  notes = "Sets the email addresses that should be subscribed to issue notifications.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Issue subscribers successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the issue subscribers."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/issue"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setIssueSubscribers(@ApiParam(value = "The values to set for email addresses for issue notifications.", required = true) @RequestParam final String subscribers) {
        _notificationsPrefs.setEmailRecipientIssueReports(subscribers);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email addresses for new user notifications.",
                  notes = "Sets the email addresses that should be subscribed to new user notifications.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "New user subscribers successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the new user subscribers."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/newuser"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setNewUserSubscribers(@ApiParam(value = "The values to set for email addresses for new user notifications.", required = true) @RequestParam final String subscribers) {
        _notificationsPrefs.setEmailRecipientNewUserAlert(subscribers);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Sets the email addresses for update notifications.",
                  notes = "Sets the email addresses that should be subscribed to update notifications.",
                  response = Properties.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Update subscribers successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set the update subscribers."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/update"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.POST}, restrictTo = Admin)
    public ResponseEntity<Properties> setUpdateSubscribers(@ApiParam(value = "The values to set for email addresses for update notifications.", required = true) @RequestParam final String subscribers) {
        _notificationsPrefs.setEmailRecipientUpdate(subscribers);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of email addresses subscribed to error notifications.",
                  notes = "This returns a list of all the email addresses that are subscribed to receive error notifications.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Error notification subscribers successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get subscribers for email notifications."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/error"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getErrorSubscribers() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailRecipientErrorMessages(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of email addresses subscribed to issue notifications.",
                  notes = "This returns a list of all the email addresses that are subscribed to receive issue notifications.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Issue notification subscribers successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get subscribers for email notifications."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/issue"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getIssueSubscribers() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailRecipientIssueReports(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of email addresses subscribed to new user notifications.",
                  notes = "This returns a list of all the email addresses that are subscribed to receive new user notifications.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "New user notification subscribers successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get subscribers for email notifications."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/newuser"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getNewUserSubscribers() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailRecipientNewUserAlert(), HttpStatus.OK);
    }

    @ApiOperation(value = "Returns list of email addresses subscribed to update notifications.",
                  notes = "This returns a list of all the email addresses that are subscribed to receive update notifications.",
                  response = String.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Update notification subscribers successfully returned."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to get subscribers for email notifications."),
                   @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(value = {"subscribers/update"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = {RequestMethod.GET}, restrictTo = Admin)
    public ResponseEntity<String> getUpdateSubscribers() {
        return new ResponseEntity<>(_notificationsPrefs.getEmailRecipientUpdate(), HttpStatus.OK);
    }

    private void cleanProperties(final Properties properties) {
        for (final String property : PROPERTIES_TO_CLEAN) {
            properties.remove(property);
        }
    }

    private void logConfigurationSubmit(final String host, final int port, final String username, final String password, final String protocol, final Properties properties) {
        if (log.isInfoEnabled()) {
            final StringBuilder message = new StringBuilder("User ");
            message.append(getSessionUser().getLogin()).append(" setting mail properties to:\n");
            message.append(" * Host:     ").append(StringUtils.equals(NOT_SET, host) ? "No value submitted..." : host).append("\n");
            message.append(" * Port:     ").append(port == -1 ? "No value submitted..." : port).append("\n");
            message.append(" * Protocol: ").append(StringUtils.equals(NOT_SET, protocol) ? "No value submitted..." : protocol).append("\n");
            message.append(" * Username: ").append(StringUtils.equals(NOT_SET, username) ? "No value submitted..." : username).append("\n");
            message.append(" * Password: ").append(StringUtils.equals(NOT_SET, password) ? "No value submitted..." : "********").append("\n");
            if (properties != null && properties.size() > 0) {
                for (final String property : properties.stringPropertyNames()) {
                    message.append(" * ").append(property).append(": ").append(properties.get(property)).append("\n");
                }
            }
            log.info(message.toString());
        }
    }

    private static final String       NOT_SET             = "NotSet";
    private static final List<String> PROPERTIES_TO_CLEAN = Arrays.asList("hostname", "port", "protocol", "username", "password", "smtpHostname", "smtpPort", "smtpProtocol", "smtpUsername", "smtpPassword");

    private final NotificationsPreferences _notificationsPrefs;
    private final XnatAppInfo              _appInfo;
    private final SerializerService        _serializer;
}
