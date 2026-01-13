package com.reltio.workflow.sample.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class S3Reader {

    public String loadS3Data(StaticCredentialsProvider credentialsProvider, String bucketName, String fileName) {
        try (S3Client s3client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();
             ResponseInputStream<GetObjectResponse> inputStream = s3client.getObject(
                     GetObjectRequest.builder()
                             .bucket(bucketName)
                             .key(fileName)
                             .build())) {

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                bytes.write(buffer, 0, length);
            }
            return bytes.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
