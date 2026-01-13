package com.company.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reltio.workflow.api.Utility;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.ExecutionService;
import com.reltio.workflow.api.expressions.Field;
import com.reltio.workflow.api.listeners.WorkflowExecutionListener;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.rest.ReltioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Listener that logs stage info to the external info of changeRequest object.
 * Used in different user tasks in the BPMN scheme.
 * - 'stageName' and 'logDecision' fields are injected from BPMN scheme.
 */

public class StageInfoLogger implements WorkflowExecutionListener {
    private static final String DCR_PREFIX = "changeRequests/";

    private static final Logger LOG = LoggerFactory.getLogger(StageInfoLogger.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @WorkflowService
    ReltioApi reltioApi;
    @WorkflowService
    ExecutionService executionService;

    private Field stageName;
    private Field logDecision;

    @Override
    public void notify(Execution execution) {
        //current stage name (injected)
        String stage = stageName == null ? "UNKNOWN" : String.valueOf(stageName.getValue(execution));

        try {
            String dcrUri = getDcrUri(execution);

            if (dcrUri != null) {

                //get actual changRequest's External Info
                String uri = Utility.generateTenantUrl(execution.getEnvironmentUrl(), execution.getTenantId()) + "/" + dcrUri + "/_externalInfo";
                JsonNode externalInfoNode = objectMapper.readTree(reltioApi.invokeApi(execution.getAccessToken(), uri, "GET", null));
                if (externalInfoNode.isMissingNode() || !externalInfoNode.isObject()) {
                    externalInfoNode = objectMapper.createObjectNode();
                }

                JsonNode processLogNode = externalInfoNode.get("processLog");
                if (processLogNode == null || !processLogNode.isObject()) {
                    processLogNode = objectMapper.createObjectNode();
                    ((ObjectNode) externalInfoNode).set("processLog", processLogNode);
                }

                // add stage info to processLogNode
                ((ObjectNode) processLogNode).set(stage, makeStageInfo(execution));

                // save External Info
                reltioApi.invokeApi(execution.getAccessToken(), uri, "POST", objectMapper.writeValueAsString(externalInfoNode));
            } else {
                LOG.warn("Couldn't update stage info. ChangeRequest URI was not found for process {}", execution.getProcessInstanceId());
            }
        } catch (ReltioException | JsonProcessingException e) {
            //log error
            LOG.error("Couldn't save stage info ({})", stage, e);
        }
    }

    private ObjectNode makeStageInfo(Execution execution) {
        ObjectNode stageInfo = objectMapper.createObjectNode();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = LocalDateTime.now().format(formatter);
        stageInfo.put("timeStamp", formattedDateTime);

        //flag to log decision is injected
        if (logDecision != null && Boolean.parseBoolean(String.valueOf(logDecision.getValue(execution)))) {
            // if decision logging is needed - get it from variables
            String decision = String.valueOf(executionService.getVariable(execution.getId(), "decision"));
            stageInfo.put("decision", decision);
        }
        return stageInfo;
    }

    private String getDcrUri(Execution execution) {
        List<String> uris = execution.getObjectUris();
        for (String uri : uris) {
            if (uri.startsWith(DCR_PREFIX)) {
                return uri;
            }
        }
        return null;
    }
}
