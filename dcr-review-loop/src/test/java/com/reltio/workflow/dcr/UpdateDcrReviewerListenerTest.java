package com.reltio.workflow.dcr;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateDcrReviewerListenerTest {

    @Test
    void shouldSetDcrReviewerVariableWithAssignee() {
        TaskService taskService = Mockito.mock(TaskService.class);
        Task task = Mockito.mock(Task.class);
        UpdateDcrReviewerListener listener = new UpdateDcrReviewerListener();
        listener.taskService = taskService;
        String taskId = "test-task-id";
        String assignee = "test-assignee";

        when(task.getId()).thenReturn(taskId);
        when(task.getAssignee()).thenReturn(assignee);

        listener.notify(task);

        verify(taskService).setVariable(taskId, "dcrReviewer", assignee);
    }
}