/*
 * web: org.nrg.xnat.services.messaging.file.MoveStoredFileRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.services.messaging.file;

import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.event.XftItemEventI;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.utils.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.List;

public class MoveStoredFileRequestListener {
    @SuppressWarnings("unused")
    public void onRequest(final MoveStoredFileRequest request) {
        boolean success = true;
        final List<String> duplicates = new ArrayList<>();

        final PersistentWorkflowI wrk = WorkflowUtils.getUniqueWorkflow(request.getUser(), request.getWorkflowId());
        assert wrk != null;
        wrk.setStatus(PersistentWorkflowUtils.IN_PROGRESS);

        try {
            duplicates.addAll(request.getResourceModifier().addFile(request.getWriters(),
                                                                    request.getResourceIdentifier(),
                                                                    request.getType(),
                                                                    request.getFilePath(),
                                                                    request.getResourceInfo(),
                                                                    request.isExtract()));
        } catch (Exception e) {
            log.error("Unable to perform move operation on file.", e);
            success = false;
        }

        if (success)
            try {
                if (StringUtils.isNotBlank(request.getProject())) {
                    XDAT.triggerXftItemEvent(XnatProjectdata.SCHEMA_ELEMENT_NAME, request.getProject(), XftItemEventI.UPDATE);
                }
                WorkflowUtils.complete(wrk, wrk.buildEvent());
            } catch (Exception e) {
                log.error("Could not mark workflow " + wrk.getWorkflowId() + " complete.", e);
                success = false;
            }

        if (success && request.isDelete())
            for (FileWriterWrapperI file : request.getWriters()) {
                file.delete();
            }

        if (request.getNotifyList().length > 0) {
            final StringBuilder message = new StringBuilder();

            final String subject;
            if (success) {
                subject = "Upload by reference complete";
                message.append("<p>The upload by reference requested by ").append(wrk.getUsername()).append(" has finished successfully.</p>");
                if (!duplicates.isEmpty()) {
                    message.append("<p>The following files were not uploaded because they already exist on the server:<br><ul>");
                    for (final String duplicate : duplicates) {
                        message.append("<li>").append(duplicate).append("</li>");
                    }
                    message.append("</ul></p>");
                }
            }
            else {
                subject = "Upload by reference error";
                message.append("<p>The upload by reference requested by ").append(request.getUser().getUsername()).append(" has encountered an error.</p>").append("<p>Please contact your IT staff or the application logs for more information.</p>");
            }

            try {
                XDAT.getMailService().sendHtmlMessage(XDAT.getSiteConfigPreferences().getAdminEmail(), request.getNotifyList(), subject, message.toString());
            } catch (MessagingException e) {
                log.error("Failed to send email.", e);
            }
        }
    }

    private final static Logger log = LoggerFactory.getLogger(MoveStoredFileRequestListener.class);
}
