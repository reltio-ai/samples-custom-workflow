package com.reltio.workflow.sample.service;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioException;
import com.reltio.workflow.sample.models.AssigneesTO;
import com.reltio.workflow.sample.models.ProcessInstanceTO;
import com.reltio.workflow.sample.models.TaskTO;
import com.reltio.workflow.sample.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class UpdateAssignee implements WorkflowAction {
    private static final Logger logger = LoggerFactory.getLogger(UpdateAssignee.class);

    private static final String WORKFLOW_API_URL = "http://localhost:8080/workflow-adapter/workflow";
    private static final Random random = new Random(); //NOSONAR
    private static final String ASSIGNEE_REQUEST_TEMPLATE = """
            {
                "tasks": [
                    "%s"
                ]
            }
            """;

    private static final String UPDATE_ASSIGNEE_TEMPLATE = """
            {
                "assignee": "%s"
            }
            """;

    private static final String CLIENT_ID_VAR = "encodedClientId";
    private static final String CLIENT_SECRET_VAR = "encodedClientSecret";

    @WorkflowService
    ExecutionService executionService;

    @Override
    public void execute(Execution execution) throws Exception {
        String encodedClientId = executionService.getVariable(execution.getId(), CLIENT_ID_VAR, String.class);
        String encodedClientSecret = executionService.getVariable(execution.getId(), CLIENT_SECRET_VAR, String.class);

        String clientId = Utils.decrypt(encodedClientId);
        String clientSecret = Utils.decrypt(encodedClientSecret);
        String accessToken = Utils.getAccessToken(clientId, clientSecret);

        var workflowHeaders = Map.of("Authorization", "Bearer " + accessToken,
                "EnvironmentURL", execution.getEnvironmentUrl(),
                "Content-Type", "application/json",
                "Accept", "application/json");

        // get process instance details
        String processInstanceData = Utils.sendRequest(
                String.format(WORKFLOW_API_URL + "/%s/processInstances/%s", execution.getTenantId(), execution.getProcessInstanceId()),
                workflowHeaders, "GET", null);
        ProcessInstanceTO processInstance = Utils.parseResponse(processInstanceData, ProcessInstanceTO.class);

        if (processInstance.currentTasks().isEmpty()) {
            throw new ReltioException("There are no active tasks for the process: " + processInstance.processInstanceId());
        }

        // get task details
        TaskTO task = processInstance.currentTasks().get(0);
        String taskData = Utils.sendRequest(
                String.format(WORKFLOW_API_URL + "/%s/tasks/%s", execution.getTenantId(), task.taskId()),
                workflowHeaders, "GET", null);
        TaskTO taskDetails = Utils.parseResponse(taskData, TaskTO.class);

        // get possible assignees
        String assigneesData = Utils.sendRequest(
                String.format(WORKFLOW_API_URL + "/%s/assignee", execution.getTenantId()),
                workflowHeaders, "POST", String.format(ASSIGNEE_REQUEST_TEMPLATE, task.taskId()));
        AssigneesTO assignees = Utils.parseResponse(assigneesData, AssigneesTO.class);
        List<String> possibleAssignees = assignees.data();
        possibleAssignees.remove(taskDetails.assignee());

        // reassign the task if other assignees are available
        if (!possibleAssignees.isEmpty()) {
            String newAssignee = possibleAssignees.get(random.nextInt(possibleAssignees.size()));
            logger.info("Reassign task {} from {} to {}", taskDetails.taskId(), taskDetails.assignee(), newAssignee);
            Utils.sendRequest(
                    String.format(WORKFLOW_API_URL + "/%s/tasks/%s", execution.getTenantId(), task.taskId()),
                    workflowHeaders, "PUT", String.format(UPDATE_ASSIGNEE_TEMPLATE, newAssignee));
        } else {
            logger.info("No other assignees available for task {}", taskDetails.taskId());
        }
    }
}
