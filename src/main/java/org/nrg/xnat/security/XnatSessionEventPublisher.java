/*
 * org.nrg.xnat.security.XnatSessionEventPublisher
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class XnatSessionEventPublisher implements HttpSessionListener, ServletContextListener {
    /**
     * Handles the HttpSessionEvent by publishing a {@link HttpSessionCreatedEvent} to the application
     * appContext.
     *
     * @param event HttpSessionEvent passed in by the container
     */
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession             session = event.getSession();
        HttpSessionCreatedEvent e       = new HttpSessionCreatedEvent(session);

        if (_log.isDebugEnabled()) {
            _log.debug("Publishing event: " + e);
        }

        // TODO: This should be wired to a database setting so that the admin can change the session timeout value.
        session.setMaxInactiveInterval(900);
        session.setAttribute("XNAT_CSRF", UUID.randomUUID().toString());

        getContext(session.getServletContext()).publishEvent(e);
    }

    /**
     * Handles the HttpSessionEvent by publishing a {@link HttpSessionDestroyedEvent} to the application
     * appContext.
     *
     * @param event The HttpSessionEvent pass in by the container
     */
    public void sessionDestroyed(final HttpSessionEvent event) {
        final String sessionId = event.getSession().getId();
        final Date   today     = Calendar.getInstance(TimeZone.getDefault()).getTime();

        try {
            final UserI user = (UserI) event.getSession().getAttribute(SecureResource.USER_ATTRIBUTE);
            if (user != null) {
                final String    userId = user.getID().toString();
                final Timestamp stamp  = new Timestamp(today.getTime());
                //sessionId's aren't guaranteed to be unique forever. But, the likelihood of sessionId and userId not forming a unique combo with a null logout_date is slim.
                //noinspection SqlDialectInspection,SqlNoDataSourceInspection,SqlResolve
                _template.execute("UPDATE xdat_user_login SET logout_date='" + stamp + "' WHERE logout_date is null and session_id='" + sessionId + "' and user_xdat_user_id='" + userId + "';");
            }
        } catch (Exception e) {
            //remember, anonymous gets a session, too. Those won't be in the table. Fail silently.
        }
        HttpSessionDestroyedEvent e = new HttpSessionDestroyedEvent(event.getSession());
        if (_log.isDebugEnabled()) {
            _log.debug("Publishing event: " + e);
        }
        getContext(event.getSession().getServletContext()).publishEvent(e);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        if (_log.isDebugEnabled()) {
            final ServletContext context   = event.getServletContext();
            _log.debug("Context destroyed: {}", context.getContextPath());
        }
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        if (_log.isDebugEnabled()) {
            final ServletContext context   = event.getServletContext();
            _log.debug("Context initialized: {}", context.getContextPath());
        }
    }

    private ApplicationContext getContext(ServletContext servletContext) {
        return WebApplicationContextUtils.findWebApplicationContext(servletContext);  // contextAttribute in xnat's case will always be "org.springframework.web.servlet.FrameworkServlet.CONTEXT.spring-mvc");
    }

    private static final Logger _log = LoggerFactory.getLogger(XnatSessionEventPublisher.class);

    @Inject
    private JdbcTemplate _template;
}