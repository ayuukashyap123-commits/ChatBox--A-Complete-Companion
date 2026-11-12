package com.chatbox.service;

import com.chatbox.model.AiConfig;
import com.chatbox.model.Conversation;
import com.chatbox.model.UserProfile;
import com.chatbox.repository.KnowledgeRepository;
import com.chatbox.repository.MemoryRepository;
import com.chatbox.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AiProviderIntegrationTest {

    private UserProfile testProfile;
    private final SettingsRepository settingsRepo = new SettingsRepository();

    private String originalApiKey;
    private String originalModel;
    private String originalProvider;
    private String originalEndpoint;
    private String originalForceOffline;

    @BeforeEach
    void setUp() {
        backupSettings();
        testProfile = new UserProfile();
        testProfile.setName("Alex Rivera");
        testProfile.setPreferredName("Alex");
        testProfile.setCommunicationStyle("Analytical and concise");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        restoreSettings();
    }

    private void backupSettings() {
        originalApiKey = settingsRepo.get("ai.api_key", null);
        originalModel = settingsRepo.get("ai.model", null);
        originalProvider = settingsRepo.get("ai.provider", null);
        originalEndpoint = settingsRepo.get("ai.endpoint", null);
        originalForceOffline = settingsRepo.get("ai.force_offline", null);

        settingsRepo.remove("ai.force_offline");
        settingsRepo.remove("ai.provider");
        settingsRepo.remove("ai.model");
        settingsRepo.remove("ai.endpoint");
        settingsRepo.remove("ai.api_key");
    }

    private void restoreSettings() {
        restoreSetting("ai.api_key", originalApiKey);
        restoreSetting("ai.model", originalModel);
        restoreSetting("ai.provider", originalProvider);
        restoreSetting("ai.endpoint", originalEndpoint);
        restoreSetting("ai.force_offline", originalForceOffline);
    }

    private void restoreSetting(String key, String value) {
        if (value != null) {
            settingsRepo.set(key, value);
        } else {
            settingsRepo.remove(key);
        }
    }

    /**
     * Deterministic Mock HttpResponse implementation.
     */
    static class MockHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;
        private final HttpRequest request;

        public MockHttpResponse(int statusCode, String body, HttpRequest request) {
            this.statusCode = statusCode;
            this.body = body;
            this.request = request;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public String body() { return body; }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (k, v) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request != null ? request.uri() : URI.create("http://localhost"); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2; }
    }

    @Test
    void testGeminiRequestConstructionAndResponseParsing() {
        AtomicReference<HttpRequest> capturedRequest = new AtomicReference<>();
        String mockGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "This is a verified response from Gemini 1.5 Flash." }
                    ]
                  },
                  "finishReason": "STOP"
                }
              ]
            }
            """;

        HttpTransport transport = req -> {
            capturedRequest.set(req);
            return new MockHttpResponse(200, mockGeminiJson, req);
        };

        AiService service = new AiService(transport);
        service.getConfig().setModel("gemini-3.8-flash");
        service.setRuntimeApiKey("AIzaSyTestGeminiKey123");

        String response = service.chat(Conversation.Mode.GENERAL, "Explain quantum entanglement", List.of(), testProfile);

        assertNotNull(response);
        assertTrue(response.contains("gemini-3.8-flash") || response.contains("Live AI"));
        assertTrue(response.contains("This is a verified response from Gemini 1.5 Flash."));

        HttpRequest req = capturedRequest.get();
        assertNotNull(req);
        assertEquals("POST", req.method());
        assertTrue(req.uri().toString().contains("models/gemini-3.8-flash:generateContent"));
        // Privacy verification: API key must be in x-goog-api-key header, NEVER in URL query string
        assertEquals("AIzaSyTestGeminiKey123", req.headers().firstValue("x-goog-api-key").orElse(""));
        assertFalse(req.uri().toString().contains("AIzaSyTestGeminiKey123"), "API key must not be exposed in URI");
        assertEquals("application/json", req.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void testOpenAiRequestConstructionAndResponseParsing() {
        AtomicReference<HttpRequest> capturedRequest = new AtomicReference<>();
        String mockOpenAiJson = """
            {
              "id": "chatcmpl-123",
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "This is a verified response from OpenAI gpt-4o-mini."
                  },
                  "finish_reason": "stop"
                }
              ]
            }
            """;

        HttpTransport transport = req -> {
            capturedRequest.set(req);
            return new MockHttpResponse(200, mockOpenAiJson, req);
        };

        AiService service = new AiService(transport);
        service.getConfig().setProvider(AiConfig.Provider.OPENAI_COMPATIBLE);
        service.getConfig().setModel("gpt-4o-mini");
        service.getConfig().setEndpoint("https://api.openai.com/v1/chat/completions");
        service.setRuntimeApiKey("sk-test-key-abc456");

        String response = service.chat(Conversation.Mode.GENERAL, "Define ACID properties", List.of(), testProfile);

        assertNotNull(response);
        assertTrue(response.contains("gpt-4o-mini"));
        assertTrue(response.contains("This is a verified response from OpenAI gpt-4o-mini."));

        HttpRequest req = capturedRequest.get();
        assertNotNull(req);
        assertEquals("POST", req.method());
        assertEquals("https://api.openai.com/v1/chat/completions", req.uri().toString());
        assertEquals("Bearer sk-test-key-abc456", req.headers().firstValue("Authorization").orElse(""));
    }

    @Test
    void testMissingCredentialsFallbackToOfflineDemo() {
        HttpTransport transport = req -> {
            fail("Transport should not be called when credentials are missing");
            return null;
        };

        AiService service = new AiService(transport);
        service.setRuntimeApiKey(null);
        service.getConfig().setApiKey("");

        if (System.getenv("GEMINI_API_KEY") == null && System.getenv("OPENAI_API_KEY") == null && System.getenv("AI_API_KEY") == null) {
            assertFalse(service.isLive());
            String response = service.chat(Conversation.Mode.GENERAL, "Hi there", List.of(), testProfile);
            assertTrue(response.contains("Local Demo Mode"));
            assertTrue(response.contains("Alex"));
        }
    }

    @Test
    void testInvalidCredentialsHttp401ThrowsAiException() {
        String errorJson = """
            {
              "error": {
                "message": "API key not valid. Please pass a valid API key.",
                "status": "INVALID_ARGUMENT"
              }
            }
            """;
        HttpTransport transport = req -> new MockHttpResponse(401, errorJson, req);

        AiService service = new AiService(transport);
        service.setRuntimeApiKey("invalid-key-xyz");

        AiException ex = assertThrows(AiException.class, () ->
            service.chat(Conversation.Mode.GENERAL, "Hello", List.of(), testProfile)
        );

        assertEquals(AiException.ErrorType.AUTHENTICATION_FAILED, ex.getErrorType());
        assertTrue(ex.getMessage().contains("Authentication failed (HTTP 401)"));
    }

    @Test
    void testRateLimitHttp429ThrowsAiException() {
        String errorJson = """
            {
              "error": {
                "message": "Resource has been exhausted (e.g. check quota).",
                "status": "RESOURCE_EXHAUSTED"
              }
            }
            """;
        HttpTransport transport = req -> new MockHttpResponse(429, errorJson, req);

        AiService service = new AiService(transport);
        service.setRuntimeApiKey("key-with-exhausted-quota");

        AiException ex = assertThrows(AiException.class, () ->
            service.chat(Conversation.Mode.GENERAL, "Hello", List.of(), testProfile)
        );

        assertEquals(AiException.ErrorType.RATE_LIMITED, ex.getErrorType());
        assertTrue(ex.getMessage().contains("Rate limit or quota exceeded (HTTP 429)"));
    }

    @Test
    void testServerFaultHttp500ThrowsAiException() {
        HttpTransport transport = req -> new MockHttpResponse(500, "Internal Server Error", req);

        AiService service = new AiService(transport);
        service.setRuntimeApiKey("test-key-500");

        AiException ex = assertThrows(AiException.class, () ->
            service.chat(Conversation.Mode.GENERAL, "Hello", List.of(), testProfile)
        );

        assertEquals(AiException.ErrorType.SERVER_ERROR, ex.getErrorType());
        assertTrue(ex.getMessage().contains("AI provider server error (HTTP 500)"));
    }

    @Test
    void testNetworkTimeoutThrowsAiException() {
        HttpTransport transport = req -> {
            throw new HttpTimeoutException("Request timed out");
        };

        AiService service = new AiService(transport);
        service.setRuntimeApiKey("test-timeout-key");
        service.getConfig().setTimeoutSeconds(10);

        AiException ex = assertThrows(AiException.class, () ->
            service.chat(Conversation.Mode.GENERAL, "Hello", List.of(), testProfile)
        );

        assertEquals(AiException.ErrorType.TIMEOUT, ex.getErrorType());
        assertTrue(ex.getMessage().contains("timed out after 10 seconds"));
    }

    @Test
    void testForceOfflineDemoModeTakesPrecedence() {
        HttpTransport transport = req -> {
            fail("Transport must not be contacted when forceOffline is true");
            return null;
        };

        AiService service = new AiService(transport);
        service.setRuntimeApiKey("active-live-key-abc");
        service.setForceOffline(true);

        assertFalse(service.isLive());
        assertTrue(service.getStatusLabel().contains("Offline Forced"));

        String response = service.chat(Conversation.Mode.COACH, "Help me plan my day", List.of(), testProfile);
        assertTrue(response.contains("Local Demo Mode"));
        assertTrue(response.contains("Alex"));
    }

    @Test
    void testTestConnectionSuccessAndFailure() throws Exception {
        AtomicReference<Integer> returnCode = new AtomicReference<>(200);
        HttpTransport transport = req -> new MockHttpResponse(returnCode.get(), "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}", req);

        AiService service = new AiService(transport);

        // Success
        assertTrue(service.testConnection("test-key", AiConfig.Provider.GEMINI, "", "gemini-3.8-flash"));

        // Failure 401
        returnCode.set(401);
        assertThrows(AiException.class, () ->
            service.testConnection("test-key", AiConfig.Provider.GEMINI, "", "gemini-3.8-flash")
        );
    }

    @Test
    void testTestConnectionAppliesConfiguredTimeout() {
        AtomicReference<HttpRequest> capturedReq = new AtomicReference<>();
        HttpTransport transport = req -> {
            capturedReq.set(req);
            throw new HttpTimeoutException("Connection timed out");
        };

        AiService service = new AiService(transport);
        service.getConfig().setTimeoutSeconds(30);

        // Verify that passing 45s applies 45s and exception references 45s
        AiException ex = assertThrows(AiException.class, () ->
            service.testConnection("test-key", AiConfig.Provider.GEMINI, "", "gemini-3.8-flash", 45)
        );

        assertEquals(AiException.ErrorType.TIMEOUT, ex.getErrorType());
        assertTrue(ex.getMessage().contains("timed out after 45 seconds"), "Exception must report configured 45s timeout");
        assertTrue(capturedReq.get().timeout().isPresent());
        assertEquals(45, capturedReq.get().timeout().get().getSeconds(), "Request timeout must be 45s");

        // Verify default fallback to config timeout (30s)
        AiException exDefault = assertThrows(AiException.class, () ->
            service.testConnection("test-key", AiConfig.Provider.GEMINI, "", "gemini-3.8-flash")
        );
        assertEquals(AiException.ErrorType.TIMEOUT, exDefault.getErrorType());
        assertTrue(exDefault.getMessage().contains("timed out after 30 seconds"), "Exception must report configured 30s timeout");
        assertEquals(30, capturedReq.get().timeout().get().getSeconds(), "Request timeout must be 30s");
    }

    @Test
    void testHttp503ServiceUnavailableThrowsAiException() {
        String error503Json = """
            {
              "error": {
                "code": 503,
                "message": "This model is currently experiencing high demand. Spikes in demand are usually temporary. Please try again later.",
                "status": "UNAVAILABLE"
              }
            }
            """;
        HttpTransport transport = req -> new MockHttpResponse(503, error503Json, req);

        AiService service = new AiService(transport);

        AiException ex = assertThrows(AiException.class, () ->
            service.testConnection("test-key", AiConfig.Provider.GEMINI, "", "gemini-3.8-flash")
        );

        assertEquals(AiException.ErrorType.SERVER_ERROR, ex.getErrorType());
        assertEquals(503, ex.getHttpStatusCode());
        assertTrue(ex.getMessage().contains("503"));
        assertTrue(ex.getMessage().contains("service unavailable") || ex.getMessage().contains("experiencing high demand"));
    }

    @Test
    void testHttp404ModelNotFoundThrowsAiException() {
        String error404Json = """
            {
              "error": {
                "code": 404,
                "message": "models/gemini-obsolete is not found for API version v1beta",
                "status": "NOT_FOUND"
              }
            }
            """;
        HttpTransport transport = req -> new MockHttpResponse(404, error404Json, req);

        AiService service = new AiService(transport);

        AiException ex = assertThrows(AiException.class, () ->
            service.testConnection("test-key", AiConfig.Provider.GEMINI, "", "gemini-obsolete")
        );

        assertEquals(AiException.ErrorType.MODEL_NOT_FOUND, ex.getErrorType());
        assertEquals(404, ex.getHttpStatusCode());
        assertTrue(ex.getMessage().contains("404"));
        assertTrue(ex.getMessage().contains("Model or endpoint not found"));
    }

    @Test
    void testHttp400InvalidApiKeyThrowsAiException() {
        String error400Json = """
            {
              "error": {
                "code": 400,
                "message": "API key not valid. Please pass a valid API key.",
                "status": "INVALID_ARGUMENT"
              }
            }
            """;
        HttpTransport transport = req -> new MockHttpResponse(400, error400Json, req);

        AiService service = new AiService(transport);

        AiException ex = assertThrows(AiException.class, () ->
            service.testConnection("bad-api-key", AiConfig.Provider.GEMINI, "", "gemini-3.8-flash")
        );

        assertEquals(AiException.ErrorType.AUTHENTICATION_FAILED, ex.getErrorType());
        assertEquals(400, ex.getHttpStatusCode());
        assertTrue(ex.getMessage().contains("Invalid API key") || ex.getMessage().contains("API key not valid"));
    }
}
