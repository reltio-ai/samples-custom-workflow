package com.company.workflow.custom.dcr;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PriorityAdjustListenerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private PriorityAdjustListener priorityAdjustListener;

    @Test
    void testAdjustPriority() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("12345");

        when(taskService.getVariable(eq("12345"), eq("dcrType"))).thenReturn("CREATE_RELATIONSHIP");
        priorityAdjustListener.notify(task);
        verify(taskService, never()).setPriority(any(), anyInt());

        when(taskService.getVariable(eq("12345"), eq("dcrType"))).thenReturn("CREATE_ENTITY");
        priorityAdjustListener.notify(task);
        verify(taskService).setPriority(eq("12345"), eq(1000));

    }
}
