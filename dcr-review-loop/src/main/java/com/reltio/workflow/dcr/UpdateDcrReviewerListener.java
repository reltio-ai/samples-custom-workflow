package com.reltio.workflow.dcr;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;

public class UpdateDcrReviewerListener implements WorkflowTaskListener {
    private static final String DCR_REVIEWER_VARIABLE = "dcrReviewer";

    @WorkflowService
    TaskService taskService;

    @Override
    public void notify(Task task) {
        taskService.setVariable(task.getId(), DCR_REVIEWER_VARIABLE, task.getAssignee());
    }
}
