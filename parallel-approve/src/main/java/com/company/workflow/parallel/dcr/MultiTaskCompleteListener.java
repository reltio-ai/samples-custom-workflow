package com.company.workflow.parallel.dcr;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;

public class MultiTaskCompleteListener implements WorkflowTaskListener {
    @WorkflowService
    TaskService taskService;

    public MultiTaskCompleteListener() {
    }

    public void notify(Task task) {
        String taskDecision = (String)taskService.getVariableLocal(task.getId(), "decision");
        String allDecisions = (String)taskService.getVariable(task.getId(), "allDecisions");
        if (allDecisions == null) {
            allDecisions = taskDecision;
        } else {
            allDecisions = allDecisions + "," + taskDecision;
        }
        taskService.setVariable(task.getId(), "allDecisions", allDecisions);
    }
}
