package com.company.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.expressions.Field;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class StageInfoLoggerTest {
    @Mock
    private ReltioApi reltioApi;

    @Mock
    private ExecutionService executionService;

    @Mock
    private Field stageName;

    @Mock
    private Field logDecision;

    @InjectMocks
    private StageInfoLogger stageInfoLogger;

    @Mock
    private Execution execution;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void notifyLogsDecisionIfRequired() throws Exception {
        when(execution.getObjectUris()).thenReturn(Collections.singletonList("changeRequests/123"));
        when(execution.getEnvironmentUrl()).thenReturn("http://example.com");
        when(execution.getTenantId()).thenReturn("tenantId");
        when(execution.getAccessToken()).thenReturn("accessToken");
        when(stageName.getValue(execution)).thenReturn("stage1");
        when(logDecision.getValue(execution)).thenReturn("true");
        when(executionService.getVariable(execution.getId(), "decision")).thenReturn("Approve");
        when(reltioApi.invokeApi(anyString(), anyString(), anyString(), any())).thenReturn("");

        stageInfoLogger.notify(execution);

        verify(reltioApi).invokeApi(
                eq("accessToken"),
                eq("http://example.com/reltio/api/tenantId/changeRequests/123/_externalInfo"),
                eq("POST"),
                argThat(s -> {
                    try {
                        JsonNode externalInfo = new ObjectMapper().readTree(s);

                        return externalInfo.has("processLog") &&
                                externalInfo.get("processLog").has("stage1") &&
                                externalInfo.get("processLog").get("stage1").has("decision") &&
                                externalInfo.get("processLog").get("stage1").get("decision").asText().equals("Approve") &&
                                externalInfo.get("processLog").get("stage1").has("timeStamp");
                    } catch (Exception e) {
                        return false;
                    }
                })
        );
    }

    @Test
    public void notifyDoesNotInvokeReltioApiIfDcrIsNotFound() {
        when(execution.getObjectUris()).thenReturn(Collections.emptyList());
        stageInfoLogger.notify(execution);
        verifyNoInteractions(reltioApi);

        when(execution.getObjectUris()).thenReturn(List.of("notADcr/uri","alsoNotADcr/uri"));
        stageInfoLogger.notify(execution);
        verifyNoInteractions(reltioApi);
    }

    @Test
    public void testExceptionOnExternalInfoGet() throws Exception {
        when(execution.getObjectUris()).thenReturn(Collections.singletonList("changeRequests/123"));
        when(execution.getEnvironmentUrl()).thenReturn("http://example.com");
        when(execution.getTenantId()).thenReturn("tenantId");
        when(execution.getAccessToken()).thenReturn("accessToken");
        when(stageName.getValue(execution)).thenReturn("stage1");
        when(logDecision.getValue(execution)).thenReturn("true");
        when(executionService.getVariable(execution.getId(), "decision")).thenReturn("Approve");
        when(reltioApi.invokeApi(anyString(), anyString(), eq("GET"), any())).thenThrow(new ReltioException("error"));

        stageInfoLogger.notify(execution);

        verify(reltioApi, times(1)).invokeApi(
                eq("accessToken"),
                eq("http://example.com/reltio/api/tenantId/changeRequests/123/_externalInfo"),
                eq("GET"),
                isNull()
        );
        verifyNoMoreInteractions(reltioApi);
    }

    @Test
    public void testNoLogDecision() throws Exception {
        when(execution.getObjectUris()).thenReturn(Collections.singletonList("changeRequests/123"));
        when(execution.getEnvironmentUrl()).thenReturn("http://example.com");
        when(execution.getTenantId()).thenReturn("tenantId");
        when(execution.getAccessToken()).thenReturn("accessToken");
        when(stageName.getValue(execution)).thenReturn("stage1");
        when(reltioApi.invokeApi(anyString(), anyString(), anyString(), any())).thenReturn("{}");
        when(logDecision.getValue(execution)).thenReturn("false");
        stageInfoLogger.notify(execution);

        verify(reltioApi).invokeApi(
                eq("accessToken"),
                eq("http://example.com/reltio/api/tenantId/changeRequests/123/_externalInfo"),
                eq("POST"),
                argThat(s -> {
                    try {
                        JsonNode externalInfo = new ObjectMapper().readTree(s);

                        return externalInfo.has("processLog") &&
                                externalInfo.get("processLog").has("stage1") &&
                                !externalInfo.get("processLog").get("stage1").has("decision") &&
                                externalInfo.get("processLog").get("stage1").has("timeStamp");
                    } catch (Exception e) {
                        return false;
                    }
                })
        );
    }
}