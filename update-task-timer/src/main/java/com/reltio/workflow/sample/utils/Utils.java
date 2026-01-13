package com.reltio.workflow.sample.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reltio.workflow.api.rest.ReltioException;
import com.reltio.workflow.api.rest.Response;
import com.reltio.workflow.api.rest.beans.ReltioResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

public class Utils {

    private static final String AUTH_ENDPOINT = "https://auth.reltio.com/oauth/token";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static HttpClient client = HttpClient.newHttpClient();

    private static final String ENCRYPTION_KEY = "WorkflowSecretEncryptionKey";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int AES_KEY_LENGTH_BYTES = 16;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private Utils() {
        // hiding the constructor
    }

    public static String getAccessToken(String clientId, String clientSecret) throws ReltioException, InterruptedException {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        var headers = Map.of("Content-Type", "application/x-www-form-urlencoded",
                "Authorization", String.format("Basic %s", encodedCredentials));

        try {
            String response = sendRequest(AUTH_ENDPOINT, headers, "POST", "grant_type=client_credentials");

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode accessTokenNode = jsonNode.get("access_token");
            if (accessTokenNode == null) {
                throw new ReltioException("Access token not found in response");
            }
            return accessTokenNode.asText();
        } catch (IOException e) {
            throw new ReltioException("Failed to get access token", e);
        }
    }

    public static String sendRequest(String url, Map<String, String> headers, String method, String body) throws ReltioException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

        switch (method.toUpperCase(Locale.ENGLISH)) {
            case "GET" -> builder.GET();
            case "POST", "PUT" -> builder.method(method.toUpperCase(Locale.ENGLISH), HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpURLConnection.HTTP_OK || response.statusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
                throw new ReltioException("Request to " + url + " failed. Status: " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new ReltioException("Failed to send request to " + url, e);
        }
    }

    public static <T> T parseResponse(String response, Class<T> clazz) throws IOException, ReltioException {
        Response reltioResponse = objectMapper.readValue(response, ReltioResponse.class);
        if (reltioResponse.getError() != null) {
            throw new ReltioException(response);
        }
        return objectMapper.readValue(response, clazz);
    }

    public static String decrypt(String stringToDecrypt) throws Exception {
        SecretKeySpec secretKey = createKey();
        byte[] combined = Base64.getDecoder().decode(stringToDecrypt);
        
        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);
        
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        
        byte[] decrypted = cipher.doFinal(encryptedData);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static String encrypt(String strToEncrypt) throws Exception {
        SecretKeySpec secretKey = createKey();

        // Encrypt with AES/GCM/NoPadding
        byte[] iv = new byte[GCM_IV_LENGTH]; // GCM standard IV length
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));

        // Combine IV + encrypted data (with authentication tag)
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private static SecretKeySpec createKey() throws NoSuchAlgorithmException {
        byte[] key = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, AES_KEY_LENGTH_BYTES);
        return new SecretKeySpec(key, "AES");
    }
}
