package com.company.workflow.parallel.dcr;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MultiTaskCompleteTest {

    private MultiTaskCompleteListener listener;

    @BeforeEach
    public void setUp() {
        listener = new MultiTaskCompleteListener();
        listener.taskService = Mockito.mock(TaskService.class);
    }

    @Test
    public void testOnTaskComplete() {
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getId()).thenReturn("testId");
        Mockito.when(listener.taskService.getVariableLocal(task.getId(), "decision")).thenReturn("Approve");
        Mockito.when(listener.taskService.getVariable(task.getId(), "allDecisions")).thenReturn(null);
        listener.notify(task);
        Mockito.verify(listener.taskService, Mockito.times(1)).setVariable(task.getId(), "allDecisions", "Approve");
    }

    @Test
    public void testOnAllTasksComplete() {
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getId()).thenReturn("testId");
        Mockito.when(listener.taskService.getVariableLocal(task.getId(), "decision")).thenReturn("Approve");
        Mockito.when(listener.taskService.getVariable(task.getId(), "allDecisions")).thenReturn("Approve");
        listener.notify(task);
        Mockito.verify(listener.taskService, Mockito.times(1)).setVariable(task.getId(), "allDecisions", "Approve,Approve");
    }
}