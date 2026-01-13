package com.company.workflow.parallel.dcr;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;

import java.util.*;

public class MultiTaskStartEventListener  implements WorkflowTaskListener {
    @WorkflowService
    TaskService taskService;

    private Random random = new Random(); //NOSONAR

    public MultiTaskStartEventListener() {
    }

    public void notify(Task task) {
        String reltioUser = task.getReltioUser();
        this.taskService.setOwner(task, reltioUser);
        Set<String> possibleAssignees = this.taskService.getAllPossibleAssignees(task);
        if (possibleAssignees.isEmpty()) {
            this.taskService.assignTask(task, null);
        } else {
            assignTaskRandomly(task, possibleAssignees);
        }
    }

    private void assignTaskRandomly(Task task, Set<String> possibleAssignees) {
        Map<String, Object> variables = taskService.getVariables(task.getId());
        Integer nrOfInstances = (Integer) variables.get("nrOfInstances");
        Integer loopCounter = (Integer) variables.get("loopCounter");
        List<String> assignees = new ArrayList<>(possibleAssignees);
        String randomAssignee = getRandomAssignee(assignees, nrOfInstances, loopCounter);
        this.taskService.assignTask(task, randomAssignee);
    }

    private String getRandomAssignee(List<String> assignees, Integer nrOfInstances, Integer loopCounter) {
        int randomNumber = random.nextInt(assignees.size());
        randomNumber = ((randomNumber / nrOfInstances) * nrOfInstances + loopCounter) % assignees.size();
        return assignees.get(randomNumber);
    }
}
