package com.company.workflow.custom.dcr;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DueDateAdjustListenerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private DueDateAdjustListener dateAdjustListener;

    @Test
    void testAdjustDueDate() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("12345");
        Date date = new Date(1753454679813L);
        when(task.getDueDate()).thenReturn(date);

        dateAdjustListener.notify(task);

        verify(taskService).setDueDate(eq("12345"), eq(date));
    }
}
