package com.reltio.workflow.custom.notifications;

import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.notification.mail.NotificationSenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OverdueNotificationTest {
    @Mock
    private NotificationSenderService notificationSenderService;

    @Mock
    private ExecutionService executionService;

    @InjectMocks
    private OverdueNotification overdueNotification;

    @Test
    void overdueNotification() {
        when(executionService.getVariable(anyString(), eq("dcrAssignee"), eq(String.class))).thenReturn("user@example.com");

        Execution execution = mock(Execution.class);
        when(execution.getId()).thenReturn("12345");
        when(execution.getTenantId()).thenReturn("tenant");

        overdueNotification.execute(execution);

        verify(notificationSenderService).sendNotification(argThat(notification -> {
                    return notification.getAttachmentList().size() == 1 &&
                            notification.getTenantId().equals("tenant") &&
                            notification.getTo().equals(List.of("firstname.lastname@reltio.com", "user@example.com"));
            }
        ));

    }
}
