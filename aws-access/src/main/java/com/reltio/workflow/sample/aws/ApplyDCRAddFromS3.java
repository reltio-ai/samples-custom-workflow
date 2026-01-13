package com.reltio.workflow.sample.aws;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.actions.WorkflowAction;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.services.AwsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplyDCRAddFromS3 implements WorkflowAction {

    private static final Logger logger = LoggerFactory.getLogger(ApplyDCRAddFromS3.class);

    private static final ObjectMapper objectMapper = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

    @WorkflowService
    private ReltioApi reltioApi;

    @WorkflowService
    private AwsService awsService;

    private S3Reader s3Reader = new S3Reader();

    @Override
    public void execute(Execution execution) {
        String changeRequestUri = findChangeRequestUri(execution.getObjectUris());

        // find new entities in this DCR
        List<Map<String, Object>> newEntities = new ArrayList<>();
        Map<String, Object> dcr = getTheDCR(changeRequestUri, execution.getEnvironmentUrl(), execution.getTenantId(), execution.getAccessToken());
        if (dcr != null) {
            Map<String, Object> changes = (Map) dcr.get("changes");
            if (changes != null) {
                for (Object changeObj : changes.values()) {
                    for (Map<String, Object> change : (List<Map<String, Object>>) changeObj) {
                        if ("CREATE_ENTITY".equals(change.get("type"))) {
                            newEntities.add((Map) change.get("newValue"));
                        }
                    }
                }
            }
        }

        // Apply the DCR
        Map<String, Object> result = applyTheDCR(changeRequestUri, execution.getEnvironmentUrl(), execution.getTenantId(), execution.getAccessToken());
        if (!"success".equalsIgnoreCase((String) result.get("status"))) {
            throw new RuntimeException("Failed to apply the DCR");
        }

        // If there are newly created entities - we will add necessary attributes to them
        if (newEntities.isEmpty()) {
            return;
        }
        Map<String, List<Map<String, Object>>> entitiesByType = newEntities.stream().collect(Collectors.groupingBy(entity -> (String) entity.get("type")));

        // Use awsService to get role-based aws credentials
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(3600)
                .roleArn("arn:aws:iam::<accountId>:role/<reltio-workflow-*>")
                .roleSessionName(execution.getTenantId())
                .build();
        Credentials creds = awsService.getTemporaryCredentials(assumeRoleRequest);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsSessionCredentials.create(creds.accessKeyId(), creds.secretAccessKey(), creds.sessionToken())
        );

        // Load the update instructions from the S3 file
        String rawData = s3Reader.loadS3Data(credentialsProvider, "bucketName", "data-to-add.json");
        Map<String, Object> s3Data;
        try {
            s3Data = objectMapper.readValue(rawData, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, Object> entry : s3Data.entrySet()) {
            // check new entities of requested type
            List<Map<String, Object>> entities = entitiesByType.get(entry.getKey());
            if (entities == null) {
                continue;
            }
            for (Map<String, Object> entity : entities) {
                // for each entity add provided attributes
                for (Map.Entry<String, String> attribute : ((Map<String, String>) entry.getValue()).entrySet()) {
                    // update entity - add attribute
                    String updateUrl = buildUpdateUrl(execution.getEnvironmentUrl(), execution.getTenantId(), (String) entity.get("uri"), attribute.getKey());
                    addAttribute(updateUrl, attribute.getValue(), execution.getAccessToken());
                }
            }
        }
    }

    private String findChangeRequestUri(List<String> objectUris) {
        for (String uri : objectUris) {
            if (uri.startsWith("changeRequests/")) {
                return uri;
            }
        }
        return null;
    }

    private Map<String, Object> getTheDCR(String changeRequestUri, String env,  String tenant, String token) {
        String getDcrApi = env + "/reltio/api/" + tenant + "/" + changeRequestUri;
        logger.info("Getting DCR - " + getDcrApi);

        Map<String, Object> responseObj = null;
        try {
            String response = reltioApi.invokeApi(token, getDcrApi, "GET", "");
            responseObj = objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return responseObj;
    }

    private Map<String, Object> applyTheDCR(String changeRequestUri, String env,  String tenant, String token) {
        String url = env + "/reltio/api/" + tenant + "/" + changeRequestUri + "/_apply?ignoreConflicts=true";

        Map<String, Object> response;
        try {
            String strResponse = reltioApi.invokeApi(token, url, "POST", "");
            response = objectMapper.readValue(strResponse, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    private String buildUpdateUrl(String env, String tenant, String entityUri, String attr) {
        return env + "/reltio/api/" + tenant + "/" + entityUri + "/attributes/" + attr;
    }

    private String addAttribute(String url, String value, String token) {
        String body = "[{\"value\":\"" + value + "\"}]";
        String response;
        try {
            response = reltioApi.invokeApi(token, url, "POST", body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
