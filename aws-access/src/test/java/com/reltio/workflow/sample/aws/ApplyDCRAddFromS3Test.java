package com.reltio.workflow.sample.aws;

import com.reltio.workflow.api.actions.Execution;
import com.reltio.workflow.api.rest.ReltioApi;
import com.reltio.workflow.api.services.AwsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplyDCRAddFromS3Test {

    @Mock
    AwsService awsService;
    @Mock
    ReltioApi reltioApi;
    @Mock
    S3Reader s3Reader;

    @InjectMocks
    ApplyDCRAddFromS3 applier;

    @Test
    void testAttributeAdding() throws Exception {
        String s3data = "{\"configuration/entityTypes/HCP\": {\"MiddleName\": \"New Middle\"} }";
        doReturn(s3data).when(s3Reader).loadS3Data(any(), eq("bucketName"), eq("data-to-add.json"));

        Execution execution = mock(Execution.class);
        when(execution.getTenantId()).thenReturn("tenant");
        when(execution.getEnvironmentUrl()).thenReturn("https://env.reltio.com");
        when(execution.getAccessToken()).thenReturn("token");
        when(execution.getObjectUris()).thenReturn(List.of("changeRequests/ABC", "entities/XYZ", "entities/0BP3ZSn"));

        when(awsService.getTemporaryCredentials(any())).thenReturn(Credentials.builder()
                .accessKeyId("accessKey")
                .secretAccessKey("secret")
                .sessionToken("token")
                .expiration(Instant.now())
                .build());

        String dcrUrl = "https://env.reltio.com/reltio/api/tenant/changeRequests/ABC";
        String applyResponse = "{\"status\": \"success\"}";
        doReturn(applyResponse).when(reltioApi).invokeApi(eq("token"), eq(dcrUrl + "/_apply?ignoreConflicts=true"), eq("POST"), eq(""));

        String dcrJson = Files.readString(Path.of(getClass().getResource("/dcr.json").toURI()));
        doReturn(dcrJson).when(reltioApi).invokeApi(eq("token"), eq(dcrUrl), eq("GET"), eq(""));


        applier.execute(execution);

        String addAttrUrl = "https://env.reltio.com/reltio/api/tenant/entities/XYZ/attributes/MiddleName";
        verify(reltioApi).invokeApi(eq("token"), eq(addAttrUrl), eq("POST"), eq("[{\"value\":\"New Middle\"}]"));
    }
}
