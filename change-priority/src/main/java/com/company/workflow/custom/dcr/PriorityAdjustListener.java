package com.company.workflow.custom.dcr;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityAdjustListener implements WorkflowTaskListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriorityAdjustListener.class);

    @WorkflowService
    TaskService taskService;

    @Override
    public void notify(Task task) {
        LOGGER.info("Change priority depending on dcrType");

        //get field variable 'dcrType'
        String dcrType = String.valueOf(taskService.getVariable(task.getId(), "dcrType"));

        //if variable is CREATE_ENTITY - set priority to URGENT (>=1000)
        if (dcrType.contains("CREATE_ENTITY")) {
            taskService.setPriority(task.getId(), 1000);
        }
    }
}
