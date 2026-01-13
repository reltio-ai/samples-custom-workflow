package com.reltio.workflow.sample.utils;

import com.reltio.workflow.api.rest.ReltioException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UtilsTest {

    private HttpClient mockHttpClient;
    private HttpClient originalHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        originalHttpClient = Utils.client;
        Utils.client = mockHttpClient;
    }

    @AfterEach
    void tearDown() {
        Utils.client = originalHttpClient;
    }

    @Test
    void getAccessTokenSuccessfullyReturnsToken() throws Exception {
        String mockResponseBody = "{\"access_token\":\"test-access-token\",\"token_type\":\"Bearer\"}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        String result = Utils.getAccessToken("client-id", "client-secret");

        assertEquals("test-access-token", result);
    }

    @Test
    void getAccessTokenThrowsExceptionWhenAccessTokenMissing() throws Exception {
        String mockResponseBody = "{\"token_type\":\"Bearer\"}"; // missing access_token
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ReltioException exception = assertThrows(ReltioException.class,
                () -> Utils.getAccessToken("client-id", "client-secret"));
        
        assertEquals("Access token not found in response", exception.getMessage());
    }

    @Test
    void getAccessTokenThrowsExceptionWhenSendRequestFails() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertThrows(ReltioException.class,
                () -> Utils.getAccessToken("client-id", "client-secret"));
    }

    @Test
    void getAccessTokenThrowsReltioExceptionOnNetworkError() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network error"));

        assertThrows(ReltioException.class,
                () -> Utils.getAccessToken("client-id", "client-secret"));
    }

    @Test
    void getAccessTokenThrowsInterruptedExceptionOnInterruption() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Thread interrupted"));

        assertThrows(InterruptedException.class,
                () -> Utils.getAccessToken("client-id", "client-secret"));
    }

    @Test
    void getAccessTokenHandlesInvalidJsonResponse() throws Exception {
        String invalidJson = "invalid json response";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(invalidJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertThrows(ReltioException.class,
                () -> Utils.getAccessToken("client-id", "client-secret"));
    }

    // ========== sendRequest Tests ==========

    @Test
    void sendRequestGetMethodSuccess() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("success response");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        String result = Utils.sendRequest("http://example.com", 
                Map.of("Content-Type", "application/json"), "GET", null);
        
        assertEquals("success response", result);
    }

    @Test
    void sendRequestPostMethodWithBodySuccess() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.body()).thenReturn("created response");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        String result = Utils.sendRequest("http://example.com", 
                Map.of("Content-Type", "application/json"), "POST", "{\"test\":\"data\"}");
        
        assertEquals("created response", result);
    }

    @Test
    void sendRequestPutMethodSuccess() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("updated response");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        String result = Utils.sendRequest("http://example.com", 
                Map.of("Content-Type", "application/json"), "PUT", "{\"test\":\"data\"}");
        
        assertEquals("updated response", result);
    }

    @Test
    void sendRequestHandlesNullBodyForPostRequest() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("success");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        String result = Utils.sendRequest("http://example.com", Map.of(), "POST", null);
        
        assertEquals("success", result);
    }

    @Test
    void sendRequestHandlesEmptyHeaders() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("success");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        String result = Utils.sendRequest("http://example.com", Map.of(), "GET", null);
        
        assertEquals("success", result);
    }

    @Test
    void sendRequestThrowsExceptionForUnsupportedMethod() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Utils.sendRequest("http://example.com", Map.of(), "DELETE", null));
        
        assertEquals("Unsupported HTTP method: DELETE", exception.getMessage());
    }

    @Test
    void sendRequestThrowsExceptionForErrorStatusCode() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ReltioException exception = assertThrows(ReltioException.class,
                () -> Utils.sendRequest("http://example.com", Map.of(), "GET", null));
        
        assertTrue(exception.getMessage().contains("failed. Status: 404"));
    }

    @Test
    void sendRequestThrowsExceptionFor500StatusCode() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ReltioException exception = assertThrows(ReltioException.class,
                () -> Utils.sendRequest("http://example.com", Map.of(), "GET", null));
        
        assertTrue(exception.getMessage().contains("failed. Status: 500"));
    }

    @Test
    void sendRequestHandlesBoundaryStatusCodes() throws Exception {
        // Test status code 199 (should fail)
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(199);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertThrows(ReltioException.class,
                () -> Utils.sendRequest("http://example.com", Map.of(), "GET", null));

        // Test status code 300 (should fail)
        when(mockResponse.statusCode()).thenReturn(300);
        assertThrows(ReltioException.class,
                () -> Utils.sendRequest("http://example.com", Map.of(), "GET", null));
    }

    @Test
    void sendRequestThrowsReltioExceptionOnNetworkError() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network timeout"));

        assertThrows(ReltioException.class,
                () -> Utils.sendRequest("http://example.com", Map.of(), "GET", null));
    }

    @Test
    void sendRequestThrowsInterruptedExceptionOnInterruption() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Request interrupted"));

        assertThrows(InterruptedException.class,
                () -> Utils.sendRequest("http://example.com", Map.of(), "GET", null));
    }

    @Test
    void sendRequestHandlesCaseInsensitiveMethod() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("success");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Test lowercase method
        String result = Utils.sendRequest("http://example.com", Map.of(), "get", null);
        assertEquals("success", result);

        // Test mixed case method
        result = Utils.sendRequest("http://example.com", Map.of(), "PoSt", "data");
        assertEquals("success", result);
    }

    // ========== decrypt Tests ==========

    @Test
    void decryptSuccessfullyDecryptsValidString() throws Exception {
        String originalText = "test-client-id";
        String encryptedText = Utils.encrypt(originalText);
        
        String result = Utils.decrypt(encryptedText);
        
        assertEquals(originalText, result);
    }

    @Test
    void decryptThrowsExceptionForInvalidBase64() {
        String invalidBase64 = "invalid-base64-string!@#";
        
        assertThrows(Exception.class, () -> Utils.decrypt(invalidBase64));
    }

    @Test
    void decryptThrowsExceptionForNullInput() {
        assertThrows(Exception.class, () -> Utils.decrypt(null));
    }

    @Test
    void decryptThrowsExceptionForEmptyString() {
        assertThrows(Exception.class, () -> Utils.decrypt(""));
    }

    @Test
    void decryptHandlesSpecialCharacters() throws Exception {
        String originalText = "client-secret!@#$%^&*()";
        String encryptedText = Utils.encrypt(originalText);
        
        String result = Utils.decrypt(encryptedText);
        
        assertEquals(originalText, result);
    }

    @Test
    void decryptHandlesUnicodeCharacters() throws Exception {
        String originalText = "тест-клиент-идентификатор";
        String encryptedText = Utils.encrypt(originalText);
        
        String result = Utils.decrypt(encryptedText);
        
        assertEquals(originalText, result);
    }

    @Test
    void decryptHandlesLongStrings() throws Exception {
        String originalText = "a".repeat(1000); // Long string
        String encryptedText = Utils.encrypt(originalText);
        
        String result = Utils.decrypt(encryptedText);
        
        assertEquals(originalText, result);
    }

    @Test
    void decryptHandlesWhitespaceAndNewlines() throws Exception {
        String originalText = "client\nid\twith\rspecial\u0020chars";
        String encryptedText = Utils.encrypt(originalText);
        
        String result = Utils.decrypt(encryptedText);
        
        assertEquals(originalText, result);
    }

    @Test
    void decryptThrowsExceptionForTamperedData() {
        String validEncrypted = "";
        try {
            validEncrypted = Utils.encrypt("test");
        } catch (Exception e) {
            fail("Setup failed");
        }
        
        // Tamper with the encrypted data (skip IV bytes)
        byte[] tamperedBytes = Base64.getDecoder().decode(validEncrypted);
        if (tamperedBytes.length > 12) { // Make sure we don't tamper with IV
            tamperedBytes[12] = (byte) (tamperedBytes[12] ^ 0xFF); // Flip bits in encrypted data
        }
        String tamperedEncrypted = Base64.getEncoder().encodeToString(tamperedBytes);
        
        assertThrows(Exception.class, () -> Utils.decrypt(tamperedEncrypted));
    }

    @Test
    void decryptThrowsExceptionForInsufficientData() {
        // Create data shorter than IV length (12 bytes for GCM)
        byte[] shortData = new byte[8];
        String shortEncrypted = Base64.getEncoder().encodeToString(shortData);
        
        assertThrows(Exception.class, () -> Utils.decrypt(shortEncrypted));
    }
}