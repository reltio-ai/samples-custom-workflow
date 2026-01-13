package com.reltio.workflow.sample.service;

import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.rest.ReltioException;
import com.reltio.workflow.sample.models.AssigneesTO;
import com.reltio.workflow.sample.models.ProcessInstanceTO;
import com.reltio.workflow.sample.models.TaskTO;
import com.reltio.workflow.sample.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateAssigneeTest {

    private UpdateAssignee updateAssignee;
    private Execution mockExecution;
    private ExecutionService mockExecutionService;
    private ProcessInstanceTO mockProcessInstance;
    private TaskTO mockTaskDetails;
    private AssigneesTO mockAssignees;

    @BeforeEach
    void setUp() throws Exception {
        updateAssignee = new UpdateAssignee();
        setupMockObjects();
        injectExecutionService();
    }

    private void setupMockObjects() {
        mockExecution = mock(Execution.class);
        when(mockExecution.getId()).thenReturn("exec-123");
        when(mockExecution.getParentId()).thenReturn("parent-456");
        when(mockExecution.getProcessInstanceId()).thenReturn("process-789");
        when(mockExecution.getTenantId()).thenReturn("tenant-abc");
        when(mockExecution.getEnvironmentUrl()).thenReturn("https://env.example.com");

        mockExecutionService = mock(ExecutionService.class);
        when(mockExecutionService.getVariable("exec-123", "encodedClientId", String.class))
                .thenReturn("encrypted-client-id");
        when(mockExecutionService.getVariable("exec-123", "encodedClientSecret", String.class))
                .thenReturn("encrypted-client-secret");

        TaskTO mockTask = mock(TaskTO.class);
        when(mockTask.taskId()).thenReturn("task-123");

        mockTaskDetails = mock(TaskTO.class);
        when(mockTaskDetails.taskId()).thenReturn("task-123");
        when(mockTaskDetails.assignee()).thenReturn("current-assignee");

        mockProcessInstance = mock(ProcessInstanceTO.class);
        when(mockProcessInstance.processInstanceId()).thenReturn("process-789");
        when(mockProcessInstance.currentTasks()).thenReturn(List.of(mockTask));

        List<String> assigneesList = new ArrayList<>();
        assigneesList.add("current-assignee");
        assigneesList.add("assignee1");
        assigneesList.add("assignee2");

        mockAssignees = mock(AssigneesTO.class);
        when(mockAssignees.data()).thenReturn(assigneesList);
    }

    private void injectExecutionService() throws Exception {
        Field executionServiceField = UpdateAssignee.class.getDeclaredField("executionService");
        executionServiceField.setAccessible(true);
        executionServiceField.set(updateAssignee, mockExecutionService);
    }

    @Test
    void successfulReassignment() throws Exception {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            setupUtilsMocks(mockedUtils);

            // Execute
            updateAssignee.execute(mockExecution);

            // Verify encryption/decryption flow
            verify(mockExecutionService).getVariable("exec-123", "encodedClientId", String.class);
            verify(mockExecutionService).getVariable("exec-123", "encodedClientSecret", String.class);
            mockedUtils.verify(() -> Utils.decrypt("encrypted-client-id"));
            mockedUtils.verify(() -> Utils.decrypt("encrypted-client-secret"));
            mockedUtils.verify(() -> Utils.getAccessToken("decrypted-client-id", "decrypted-client-secret"));

            // Verify API calls
            mockedUtils.verify(() -> Utils.sendRequest(contains("processInstances"), any(), eq("GET"), isNull()));
            mockedUtils.verify(() -> Utils.sendRequest(contains("tasks/task-123"), any(), eq("GET"), isNull()));
            mockedUtils.verify(() -> Utils.sendRequest(contains("assignee"), any(), eq("POST"), anyString()));
            mockedUtils.verify(() -> Utils.sendRequest(contains("tasks/task-123"), any(), eq("PUT"), anyString()));
        }
    }

    @Test
    void noAlternativeAssigneesShouldNotReassign() throws Exception {
        // Setup assignees list with only current assignee
        List<String> singleAssigneeList = new ArrayList<>();
        singleAssigneeList.add("current-assignee");
        when(mockAssignees.data()).thenReturn(singleAssigneeList);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            setupUtilsMocks(mockedUtils);

            // Execute
            updateAssignee.execute(mockExecution);

            // Verify PUT request was not called (no reassignment)
            mockedUtils.verify(() -> Utils.sendRequest(contains("tasks/task-123"), any(), eq("PUT"), anyString()), never());
        }
    }

    @Test
    void noActiveTasksShouldThrowReltioException() {
        // Setup empty task list
        when(mockProcessInstance.currentTasks()).thenReturn(List.of());

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            setupUtilsMocksForProcessInstance(mockedUtils);

            // Execute and Assert
            ReltioException exception = assertThrows(ReltioException.class,
                    () -> updateAssignee.execute(mockExecution));

            assertTrue(exception.getMessage().contains("There are no active tasks"));
        }
    }

    @Test
    void missingClientIdVariableShouldThrowException() {
        when(mockExecutionService.getVariable("exec-123", "encodedClientId", String.class))
                .thenReturn(null);

        try (MockedStatic<Utils> ignored = mockStatic(Utils.class)) {
            // Execute and Assert - should fail when trying to decrypt null
            assertThrows(Exception.class, () -> updateAssignee.execute(mockExecution));
        }
    }

    @Test
    void missingClientSecretVariableShouldThrowException() {
        when(mockExecutionService.getVariable("exec-123", "encodedClientSecret", String.class))
                .thenReturn(null);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.decrypt("encrypted-client-id"))
                    .thenReturn("decrypted-client-id");

            // Execute and Assert - should fail when trying to decrypt null
            assertThrows(Exception.class, () -> updateAssignee.execute(mockExecution));
        }
    }

    @Test
    void decryptionFailureShouldPropagateException() {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.decrypt("encrypted-client-id"))
                    .thenThrow(new ReltioException("Decryption failed"));

            // Execute and Assert
            assertThrows(ReltioException.class, () -> updateAssignee.execute(mockExecution));
        }
    }

    @Test
    void sendRequestThrowsExceptionShouldPropagateException() {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.decrypt(anyString()))
                    .thenReturn("decrypted-value");
            mockedUtils.when(() -> Utils.getAccessToken(anyString(), anyString()))
                    .thenReturn("mock-access-token");
            mockedUtils.when(() -> Utils.sendRequest(anyString(), any(), anyString(), any()))
                    .thenThrow(new ReltioException("API call failed"));

            // Execute and Assert
            assertThrows(ReltioException.class, () -> updateAssignee.execute(mockExecution));
        }
    }

    @Test
    void parseResponseThrowsExceptionShouldPropagateException() {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.decrypt(anyString()))
                    .thenReturn("decrypted-value");
            mockedUtils.when(() -> Utils.getAccessToken(anyString(), anyString()))
                    .thenReturn("mock-access-token");
            mockedUtils.when(() -> Utils.sendRequest(anyString(), any(), anyString(), any()))
                    .thenReturn("invalid-response");
            mockedUtils.when(() -> Utils.parseResponse(anyString(), any()))
                    .thenThrow(new ReltioException("Parse error"));

            // Execute and Assert
            assertThrows(ReltioException.class, () -> updateAssignee.execute(mockExecution));
        }
    }

    @Test
    void verifyCorrectHeaders() throws Exception {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            setupUtilsMocks(mockedUtils);

            // Execute
            updateAssignee.execute(mockExecution);

            // Verify headers contain expected values
            mockedUtils.verify(() -> Utils.sendRequest(anyString(), argThat(headers ->
                headers.containsKey("Authorization") &&
                headers.get("Authorization").equals("Bearer mock-access-token") &&
                headers.containsKey("EnvironmentURL") &&
                headers.get("EnvironmentURL").equals("https://env.example.com") &&
                headers.containsKey("Content-Type") &&
                headers.get("Content-Type").equals("application/json") &&
                headers.containsKey("Accept") &&
                headers.get("Accept").equals("application/json")
            ), anyString(), any()), atLeastOnce());
        }
    }

    @Test
    void verifyCorrectUrlFormats() throws Exception {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            setupUtilsMocks(mockedUtils);

            // Execute
            updateAssignee.execute(mockExecution);

            // Verify correct URL patterns
            mockedUtils.verify(() -> Utils.sendRequest(
                    eq("http://localhost:8080/workflow-adapter/workflow/tenant-abc/processInstances/process-789"),
                    any(), eq("GET"), isNull()));

            mockedUtils.verify(() -> Utils.sendRequest(
                    eq("http://localhost:8080/workflow-adapter/workflow/tenant-abc/tasks/task-123"),
                    any(), eq("GET"), isNull()));

            mockedUtils.verify(() -> Utils.sendRequest(
                    eq("http://localhost:8080/workflow-adapter/workflow/tenant-abc/assignee"),
                    any(), eq("POST"), anyString()));
        }
    }

    @Test
    void verifyExecutionServiceInteraction() throws Exception {
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            setupUtilsMocks(mockedUtils);

            // Execute
            updateAssignee.execute(mockExecution);

            // Verify ExecutionService was called with correct parameters
            verify(mockExecutionService, times(1))
                    .getVariable("exec-123", "encodedClientId", String.class);
            verify(mockExecutionService, times(1))
                    .getVariable("exec-123", "encodedClientSecret", String.class);
            verifyNoMoreInteractions(mockExecutionService);
        }
    }

    private void setupUtilsMocks(MockedStatic<Utils> mockedUtils) {
        mockedUtils.when(() -> Utils.decrypt("encrypted-client-id"))
                .thenReturn("decrypted-client-id");
        mockedUtils.when(() -> Utils.decrypt("encrypted-client-secret"))
                .thenReturn("decrypted-client-secret");
        mockedUtils.when(() -> Utils.getAccessToken("decrypted-client-id", "decrypted-client-secret"))
                .thenReturn("mock-access-token");

        mockedUtils.when(() -> Utils.sendRequest(contains("processInstances"), any(), eq("GET"), isNull()))
                .thenReturn("process-instance-response");
        mockedUtils.when(() -> Utils.parseResponse("process-instance-response", ProcessInstanceTO.class))
                .thenReturn(mockProcessInstance);

        mockedUtils.when(() -> Utils.sendRequest(contains("tasks/task-123"), any(), eq("GET"), isNull()))
                .thenReturn("task-details-response");
        mockedUtils.when(() -> Utils.parseResponse("task-details-response", TaskTO.class))
                .thenReturn(mockTaskDetails);

        mockedUtils.when(() -> Utils.sendRequest(contains("assignee"), any(), eq("POST"), anyString()))
                .thenReturn("assignees-response");
        mockedUtils.when(() -> Utils.parseResponse("assignees-response", AssigneesTO.class))
                .thenReturn(mockAssignees);

        mockedUtils.when(() -> Utils.sendRequest(contains("tasks/task-123"), any(), eq("PUT"), anyString()))
                .thenReturn("update-response");
    }

    private void setupUtilsMocksForProcessInstance(MockedStatic<Utils> mockedUtils) {
        mockedUtils.when(() -> Utils.decrypt("encrypted-client-id"))
                .thenReturn("decrypted-client-id");
        mockedUtils.when(() -> Utils.decrypt("encrypted-client-secret"))
                .thenReturn("decrypted-client-secret");
        mockedUtils.when(() -> Utils.getAccessToken("decrypted-client-id", "decrypted-client-secret"))
                .thenReturn("mock-access-token");

        mockedUtils.when(() -> Utils.sendRequest(contains("processInstances"), any(), eq("GET"), isNull()))
                .thenReturn("process-instance-response");
        mockedUtils.when(() -> Utils.parseResponse("process-instance-response", ProcessInstanceTO.class))
                .thenReturn(mockProcessInstance);
    }
}