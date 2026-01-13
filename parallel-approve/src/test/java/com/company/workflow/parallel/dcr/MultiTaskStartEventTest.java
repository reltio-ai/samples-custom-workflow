package com.company.workflow.parallel.dcr;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashSet;

public class MultiTaskStartEventTest {
    private MultiTaskStartEventListener listener;

    @BeforeEach
    public void setUp() {
        listener = new MultiTaskStartEventListener();
        listener.taskService = Mockito.mock(TaskService.class);
    }

    @Test
    public void testTaskIsUnassigned() {
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getId()).thenReturn("testId");
        listener.notify(task);
        Mockito.verify(listener.taskService, Mockito.times(1)).assignTask(task, null);
    }

    @Test
    public void testTaskIsAssigned() {
        Task task = Mockito.mock(Task.class);
        Mockito.when(task.getId()).thenReturn("testId");
        LinkedHashSet set = new LinkedHashSet();
        set.add("reviewer1");
        set.add("reviewer2");
        Mockito.when(listener.taskService.getAllPossibleAssignees(task)).thenReturn(set);
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("nrOfInstances", 2);
        variables.put("loopCounter", 1);
        Mockito.when(listener.taskService.getVariables(task.getId())).thenReturn(variables);
        listener.notify(task);
        Mockito.verify(listener.taskService, Mockito.times(1)).assignTask(task, "reviewer2");
    }
}
