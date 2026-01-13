package com.reltio.workflow.custom.notifications;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.notification.mail.Attachment;
import com.reltio.workflow.api.notification.mail.Notification;
import com.reltio.workflow.api.notification.mail.NotificationSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class OverdueNotification implements WorkflowAction {
    private static final Logger LOG = LoggerFactory.getLogger(OverdueNotification.class);
    @WorkflowService
    NotificationSenderService notificationSenderService;
    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) {
        Notification notification = new Notification(execution.getTenantId());
        notification.setSubject("Your task is overdue");
        notification.setBody(
                "<table style=\"width:500px\">" +
                    "<tbody>" +
                        "<tr>" +
                            "<td>" +
                                "<img src=\"cid:overdue\" style=\"height:64px; width:64px\" />" +
                            "</td>" +
                            "<td>" +
                                "<p>REMINDER</p>" +
                                "<p>The task assigned to you is overdue. Please make a decision.</p>" +
                            "</td>" +
                        "</tr>" +
                    "</tbody>" +
                "</table>");

        //Hardcoded email address
        notification.addTo("firstname.lastname@reltio.com");

        //Email address taken from variables
        String dcrAssignee = executionService.getVariable(execution.getId(), "dcrAssignee", String.class);
        if (dcrAssignee != null && !dcrAssignee.isEmpty()) {
            notification.addTo(dcrAssignee);
        }

        try (InputStream stream = getClass().getResourceAsStream("/overdue.png")) {
            if (stream != null) {
                byte[] targetArray = new byte[stream.available()];
                if (stream.read(targetArray) > 0) {
                    Attachment attachment = new Attachment("overdue.png", targetArray);
                    attachment.setContentId("overdue");
                    attachment.setContentType("image/png");
                    attachment.setDisposition("inline");
                    notification.addAttachment(attachment);
                }
            }
        } catch (IOException e) {
            LOG.warn("Cannot attach image overdue.png", e);
        }

        this.notificationSenderService.sendNotification(notification);
    }
}
