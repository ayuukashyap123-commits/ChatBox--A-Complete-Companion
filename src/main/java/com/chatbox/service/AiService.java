package com.chatbox.service;

import com.chatbox.model.*;
import com.chatbox.repository.KnowledgeRepository;
import com.chatbox.repository.MemoryRepository;
import com.chatbox.repository.SettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * AI Service: Connects to real AI providers (Google Gemini, OpenAI-compatible endpoints)
 * using Java 21 HttpClient and Jackson.
 *
 * Provides:
 * - Secure credential handling without hardcoded secrets.
 * - Persistent local configuration via SettingsRepository.
 * - Strict timeout, error handling, and response validation.
 * - Grounded Second-Brain document question answering with citation enforcement.
 * - Safe personalization using user profiles and explicitly approved memories ONLY.
 * - Complete privacy isolation: zero journal leaks, zero unapproved memory leaks.
 * - Deterministic Offline Demo Mode when offline or requested.
 */
public class AiService {
    private static final Logger LOG = Logger.getLogger(AiService.class.getName());

    public static final String DEMO_LABEL = "[Local Demo Mode — Offline Intelligence]";

    private final ObjectMapper mapper;
    private final HttpTransport transport;
    private final SettingsRepository settingsRepo;
    private final MemoryRepository memoryRepo;
    private final KnowledgeRepository knowledgeRepo;
    private final DocumentService documentService;

    private final AiConfig config = new AiConfig();
    private String runtimeApiKey;
    private final List<Runnable> statusListeners = new CopyOnWriteArrayList<>();

    public AiService() {
        this(null, new SettingsRepository(), new MemoryRepository(), new KnowledgeRepository(), new DocumentService());
    }

    public AiService(HttpTransport transport) {
        this(transport, new SettingsRepository(), new MemoryRepository(), new KnowledgeRepository(), new DocumentService());
    }

    public AiService(HttpTransport transport,
                     SettingsRepository settingsRepo,
                     MemoryRepository memoryRepo,
                     KnowledgeRepository knowledgeRepo,
                     DocumentService documentService) {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.settingsRepo = settingsRepo != null ? settingsRepo : new SettingsRepository();
        this.memoryRepo = memoryRepo != null ? memoryRepo : new MemoryRepository();
        this.knowledgeRepo = knowledgeRepo != null ? knowledgeRepo : new KnowledgeRepository();
        this.documentService = documentService != null ? documentService : new DocumentService();

        if (transport != null) {
            this.transport = transport;
        } else {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
            this.transport = req -> httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        }

        loadPersistedConfig();
    }

    // ─── Configuration & Persistence ──────────────────────────────────────────

    public void loadPersistedConfig() {
        String provStr = settingsRepo.get("ai.provider", "GEMINI");
        try {
            config.setProvider(AiConfig.Provider.valueOf(provStr));
        } catch (Exception e) {
            config.setProvider(AiConfig.Provider.GEMINI);
        }

        config.setModel(settingsRepo.get("ai.model", config.getProvider().getDefaultModel()));
        config.setEndpoint(settingsRepo.get("ai.endpoint", ""));
        config.setTimeoutSeconds(settingsRepo.getInt("ai.timeout", 30));
        config.setForceOffline(settingsRepo.getBoolean("ai.force_offline", false));

        // Read saved key if present
        String savedKey = settingsRepo.get("ai.api_key", "");
        if (!savedKey.isBlank()) {
            config.setApiKey(savedKey);
        }
    }

    public void saveConfig(AiConfig.Provider provider, String model, String endpoint, int timeout, boolean forceOffline, String apiKey) {
        config.setProvider(provider);
        config.setModel(model);
        config.setEndpoint(endpoint);
        config.setTimeoutSeconds(timeout);
        config.setForceOffline(forceOffline);
        if (apiKey != null) config.setApiKey(apiKey);

        settingsRepo.set("ai.provider", config.getProvider().name());
        settingsRepo.set("ai.model", config.getModel());
        settingsRepo.set("ai.endpoint", config.getEndpoint());
        settingsRepo.setInt("ai.timeout", config.getTimeoutSeconds());
        settingsRepo.setBoolean("ai.force_offline", config.isForceOffline());
        if (apiKey != null) {
            settingsRepo.set("ai.api_key", apiKey.trim());
        }
        notifyListeners();
    }

    public AiConfig getConfig() {
        return config;
    }

    public void addStatusListener(Runnable listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(Runnable listener) {
        statusListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : statusListeners) {
            try { r.run(); } catch (Exception e) { LOG.warning("Listener error: " + e.getMessage()); }
        }
    }

    public void setRuntimeApiKey(String key) {
        this.runtimeApiKey = (key != null && !key.isBlank()) ? key.trim() : null;
        if (this.runtimeApiKey != null) {
            config.setForceOffline(false);
        }
        notifyListeners();
    }

    public void setForceOffline(boolean forceOffline) {
        config.setForceOffline(forceOffline);
        settingsRepo.setBoolean("ai.force_offline", forceOffline);
        notifyListeners();
    }

    public boolean isForceOffline() {
        return config.isForceOffline();
    }

    public String getEffectiveKey() {
        if (runtimeApiKey != null && !runtimeApiKey.isBlank()) return runtimeApiKey;
        if (!config.getApiKey().isBlank()) return config.getApiKey();

        // Check environment variables based on configured provider
        if (config.getProvider() == AiConfig.Provider.GEMINI) {
            String geminiEnv = System.getenv("GEMINI_API_KEY");
            if (geminiEnv != null && !geminiEnv.isBlank()) return geminiEnv.trim();
        } else if (config.getProvider() == AiConfig.Provider.OPENAI_COMPATIBLE) {
            String openAiEnv = System.getenv("OPENAI_API_KEY");
            if (openAiEnv != null && !openAiEnv.isBlank()) return openAiEnv.trim();
        }

        String genericEnv = System.getenv("AI_API_KEY");
        if (genericEnv != null && !genericEnv.isBlank()) return genericEnv.trim();

        return null;
    }

    public boolean isLive() {
        if (config.isForceOffline()) return false;
        String key = getEffectiveKey();
        // For OpenAI-compatible endpoints that don't strictly require a key (like local Ollama),
        // we check if it is a local host endpoint
        if (config.getProvider() == AiConfig.Provider.OPENAI_COMPATIBLE && isLocalEndpoint(config.getEffectiveEndpoint())) {
            return true;
        }
        return key != null && !key.isBlank();
    }

    private boolean isLocalEndpoint(String endpoint) {
        if (endpoint == null) return false;
        return endpoint.contains("localhost") || endpoint.contains("127.0.0.1") || endpoint.contains("0.0.0.0");
    }

    public String getStatusLabel() {
        if (config.isForceOffline()) {
            return "Local Demo (Offline Forced)";
        }
        if (isLive()) {
            return "AI Connected";
        }
        return "Local Demo";
    }

    public String getDetailedStatusLabel() {
        if (config.isForceOffline()) {
            return "Local Demo (Offline Forced)";
        }
        if (isLive()) {
            return "AI Connected (" + config.getModel() + ")";
        }
        return "Local Demo";
    }

    public String getSourceInfo() {
        if (config.isForceOffline()) return "Offline Demonstration Mode (User selected)";
        if (runtimeApiKey != null && !runtimeApiKey.isBlank()) return "Runtime Key";
        if (!config.getApiKey().isBlank()) return "Saved Local Settings";
        if (System.getenv("GEMINI_API_KEY") != null) return "Environment Variable (GEMINI_API_KEY)";
        if (System.getenv("OPENAI_API_KEY") != null) return "Environment Variable (OPENAI_API_KEY)";
        if (System.getenv("AI_API_KEY") != null) return "Environment Variable (AI_API_KEY)";
        return "None (Offline Demonstration Mode)";
    }

    // ─── Connection Testing ───────────────────────────────────────────────────

    public boolean testConnection(String candidateKey) throws Exception {
        return testConnection(candidateKey, config.getProvider(), config.getEffectiveEndpoint(), config.getModel(), config.getTimeoutSeconds());
    }

    public boolean testConnection(String candidateKey, AiConfig.Provider provider, String endpoint, String model) throws Exception {
        return testConnection(candidateKey, provider, endpoint, model, config.getTimeoutSeconds());
    }

    public boolean testConnection(String candidateKey, AiConfig.Provider provider, String endpoint, String model, int timeoutSeconds) throws Exception {
        String keyToTest = (candidateKey != null && !candidateKey.isBlank()) ? candidateKey.trim() : getEffectiveKey();
        if ((keyToTest == null || keyToTest.isBlank()) && !isLocalEndpoint(endpoint)) {
            throw new AiException("No API key provided to test.", AiException.ErrorType.AUTHENTICATION_FAILED, 0);
        }

        int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : config.getTimeoutSeconds();

        if (provider == AiConfig.Provider.GEMINI) {
            return testGeminiConnection(keyToTest, model, effectiveTimeout);
        } else {
            return testOpenAiConnection(keyToTest, endpoint, model, effectiveTimeout);
        }
    }

    private boolean testGeminiConnection(String key, String model, int timeoutSeconds) throws Exception {
        var contentsNode = mapper.createArrayNode();
        var msg = mapper.createObjectNode();
        msg.put("role", "user");
        var parts = mapper.createArrayNode();
        parts.addObject().put("text", "Ping test. Reply with 'OK'.");
        msg.set("parts", parts);
        contentsNode.add(msg);

        var requestBody = mapper.createObjectNode();
        requestBody.set("contents", contentsNode);

        int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : config.getTimeoutSeconds();

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(effectiveTimeout));

        if (key != null && !key.isBlank()) {
            builder.header("x-goog-api-key", key);
        }

        HttpRequest req = builder.build();

        HttpResponse<String> resp;
        try {
            resp = transport.send(req);
        } catch (HttpTimeoutException e) {
            throw new AiException("Connection test timed out after " + effectiveTimeout + " seconds.", AiException.ErrorType.TIMEOUT, e);
        } catch (IOException e) {
            throw new AiException("Network connection failed: " + e.getMessage(), AiException.ErrorType.NETWORK_UNREACHABLE, e);
        }

        handleHttpErrors(resp.statusCode(), resp.body());
        return true;
    }

    private boolean testOpenAiConnection(String key, String endpoint, String model, int timeoutSeconds) throws Exception {
        var requestBody = mapper.createObjectNode();
        requestBody.put("model", model);
        var messages = mapper.createArrayNode();
        var msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", "Ping test. Reply with 'OK'.");
        messages.add(msg);
        requestBody.set("messages", messages);
        requestBody.put("max_tokens", 10);

        int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : config.getTimeoutSeconds();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(effectiveTimeout));

        if (key != null && !key.isBlank()) {
            builder.header("Authorization", "Bearer " + key);
        }

        HttpRequest req = builder.build();
        HttpResponse<String> resp;
        try {
            resp = transport.send(req);
        } catch (HttpTimeoutException e) {
            throw new AiException("Connection test timed out after " + effectiveTimeout + " seconds.", AiException.ErrorType.TIMEOUT, e);
        } catch (IOException e) {
            throw new AiException("Network connection failed: " + e.getMessage(), AiException.ErrorType.NETWORK_UNREACHABLE, e);
        }

        handleHttpErrors(resp.statusCode(), resp.body());
        return true;
    }

    // ─── Main Chat Processing ─────────────────────────────────────────────────

    /**
     * Generate an AI or demonstration response given the mode, message, history, and user profile.
     */
    public String chat(Conversation.Mode mode, String userMessage, List<Message> history, UserProfile profile) {
        // Grounded Second Brain Handling
        if (mode == Conversation.Mode.KNOWLEDGE) {
            return handleKnowledgeMode(userMessage, profile);
        }

        if (!isLive()) {
            return buildDemoResponse(mode, userMessage, profile);
        }

        try {
            if (config.getProvider() == AiConfig.Provider.GEMINI) {
                return callGemini(mode, userMessage, history, profile, null);
            } else {
                return callOpenAi(mode, userMessage, history, profile, null);
            }
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            LOG.warning("AI provider call failed: " + e.getMessage());
            throw new AiException("Failed to generate response: " + e.getMessage(), AiException.ErrorType.UNKNOWN, e);
        }
    }

    // ─── Grounded Second-Brain Intelligence ───────────────────────────────────

    private String handleKnowledgeMode(String query, UserProfile profile) {
        String name = (profile != null && profile.getDisplayName() != null) ? profile.getDisplayName() : "User";
        List<KnowledgeDocument> allDocs = knowledgeRepo.findAllWithContent();

        // Insufficient evidence if no documents exist in Second Brain
        if (allDocs.isEmpty()) {
            return (isLive() ? "[Second Brain — Insufficient Evidence]\n" : DEMO_LABEL + "\n[Second Brain — Insufficient Evidence]\n") +
                   "The imported documents do not contain enough evidence to answer this question.\n\n" +
                   "No documents have been imported into your Second Brain yet.\n\n" +
                   "Recommendations for " + name + ":\n" +
                   "• Navigate to the Second Brain tab and import your study guides, notes (.md, .txt), or lecture PDFs.\n" +
                   "• Once imported, your questions will be answered strictly from your verified document passages.";
        }

        // Search for relevant passages
        List<DocumentService.DocumentPassage> passages = documentService.searchPassages(query, allDocs, 3);

        if (passages.isEmpty()) {
            return (isLive() ? "[Second Brain — Insufficient Evidence]\n" : DEMO_LABEL + "\n[Second Brain — Insufficient Evidence]\n") +
                   "The imported documents do not contain enough evidence to answer this question.\n\n" +
                   "No matching passages were found across your " + allDocs.size() + " imported document(s) for query: \"" + query + "\"\n\n" +
                   "Truthful Grounding Guarantee:\n" +
                   "Chatbox never fabricates citations, page numbers, or references. Please verify your query keywords or import additional materials.";
        }

        // Passages found: if not live, generate deterministic grounded demo answer
        if (!isLive()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Local Demo Mode — Second Brain Grounded Answer]\n");
            sb.append("Grounded in your imported document(s) for ").append(name).append(":\n\n");

            for (DocumentService.DocumentPassage p : passages) {
                sb.append("📄 \"").append(p.documentTitle()).append("\" (").append(p.fileType()).append("):\n");
                sb.append("\"").append(p.excerpt()).append("\"\n\n");
            }

            sb.append("Summary & Evidence:\n");
            sb.append("Based on the matching passage(s) above, your document directly discusses this topic. ");
            sb.append("Connect an AI API key in Settings for full generative synthesis across these passages.");
            return sb.toString();
        }

        // Live AI Grounded Call: Instruct model strictly to answer ONLY from retrieved passages
        StringBuilder groundingContext = new StringBuilder();
        groundingContext.append("The user has imported the following verified passages in their Second Brain:\n\n");
        Set<String> titles = new LinkedHashSet<>();
        for (DocumentService.DocumentPassage p : passages) {
            titles.add(p.documentTitle());
            groundingContext.append("--- SOURCE DOCUMENT: ").append(p.documentTitle())
                            .append(" (").append(p.fileType()).append(") ---\n");
            groundingContext.append(p.excerpt()).append("\n\n");
        }

        groundingContext.append("STRICT INSTRUCTIONS:\n")
            .append("1. Answer the user's question based ONLY on the source document passages above.\n")
            .append("2. Explicitly cite the source document title(s) used in your response.\n")
            .append("3. If the provided passages do not contain sufficient evidence to answer, you MUST state: ")
            .append("\"The imported documents do not contain enough evidence to answer this question.\"\n")
            .append("4. Never invent or hallucinate citations, quotes, or facts not present in the excerpts.\n");

        try {
            if (config.getProvider() == AiConfig.Provider.GEMINI) {
                return callGemini(Conversation.Mode.KNOWLEDGE, query, List.of(), profile, groundingContext.toString());
            } else {
                return callOpenAi(Conversation.Mode.KNOWLEDGE, query, List.of(), profile, groundingContext.toString());
            }
        } catch (Exception e) {
            LOG.warning("Live grounded AI call failed: " + e.getMessage());
            throw new AiException("Grounded AI request failed: " + e.getMessage(), AiException.ErrorType.UNKNOWN, e);
        }
    }

    // ─── Provider Request Dispatching ─────────────────────────────────────────

    private String callGemini(Conversation.Mode mode, String userMessage,
                              List<Message> history, UserProfile profile, String customSystemContext) throws Exception {
        String systemPrompt = customSystemContext != null ? customSystemContext : buildSystemPrompt(mode, profile);
        var contentsNode = mapper.createArrayNode();

        // System prompt as first user turn followed by model acknowledgement
        var sysMsg = mapper.createObjectNode();
        sysMsg.put("role", "user");
        var sysParts = mapper.createArrayNode();
        sysParts.addObject().put("text", systemPrompt);
        sysMsg.set("parts", sysParts);
        contentsNode.add(sysMsg);

        var ackMsg = mapper.createObjectNode();
        ackMsg.put("role", "model");
        var ackParts = mapper.createArrayNode();
        ackParts.addObject().put("text", "Understood. I will strictly adhere to these instructions.");
        ackMsg.set("parts", ackParts);
        contentsNode.add(ackMsg);

        // Previous history (last 16 messages)
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 16);
            for (int i = start; i < history.size(); i++) {
                Message m = history.get(i);
                var msgNode = mapper.createObjectNode();
                msgNode.put("role", m.getRole() == Message.Role.ASSISTANT ? "model" : "user");
                var parts = mapper.createArrayNode();
                parts.addObject().put("text", m.getContent());
                msgNode.set("parts", parts);
                contentsNode.add(msgNode);
            }
        }

        // Current user message
        var curMsg = mapper.createObjectNode();
        curMsg.put("role", "user");
        var curParts = mapper.createArrayNode();
        curParts.addObject().put("text", userMessage);
        curMsg.set("parts", curParts);
        contentsNode.add(curMsg);

        var requestBody = mapper.createObjectNode();
        requestBody.set("contents", contentsNode);

        var genConfig = mapper.createObjectNode();
        genConfig.put("temperature", mode == Conversation.Mode.KNOWLEDGE ? 0.2 : 0.7);
        genConfig.put("maxOutputTokens", 1024);
        requestBody.set("generationConfig", genConfig);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + config.getModel() + ":generateContent";
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        String key = getEffectiveKey();
        if (key != null && !key.isBlank()) {
            reqBuilder.header("x-goog-api-key", key);
        }

        HttpRequest req = reqBuilder.build();

        HttpResponse<String> resp;
        try {
            resp = transport.send(req);
        } catch (HttpTimeoutException e) {
            throw new AiException("Request timed out after " + config.getTimeoutSeconds() + " seconds. Please retry.",
                AiException.ErrorType.TIMEOUT, e);
        } catch (IOException e) {
            throw new AiException("Network connection error: " + e.getMessage(),
                AiException.ErrorType.NETWORK_UNREACHABLE, e);
        }

        handleHttpErrors(resp.statusCode(), resp.body());

        JsonNode root = mapper.readTree(resp.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty() || !candidates.isArray()) {
            throw new AiException("The AI provider returned no response candidates.", AiException.ErrorType.INVALID_RESPONSE, 200);
        }

        JsonNode first = candidates.get(0);
        String finishReason = first.path("finishReason").asText("");
        if ("SAFETY".equalsIgnoreCase(finishReason)) {
            throw new AiException("The response was filtered by AI safety guidelines.", AiException.ErrorType.INVALID_RESPONSE, 200);
        }

        String text = first.path("content").path("parts").get(0).path("text").asText("No response text.");
        return "[Live AI — " + config.getModel() + "]\n\n" + text;
    }

    private String callOpenAi(Conversation.Mode mode, String userMessage,
                              List<Message> history, UserProfile profile, String customSystemContext) throws Exception {
        String systemPrompt = customSystemContext != null ? customSystemContext : buildSystemPrompt(mode, profile);

        var requestBody = mapper.createObjectNode();
        requestBody.put("model", config.getModel());
        requestBody.put("temperature", mode == Conversation.Mode.KNOWLEDGE ? 0.2 : 0.7);
        requestBody.put("max_tokens", 1024);

        var messagesNode = mapper.createArrayNode();

        // System prompt
        var sysObj = mapper.createObjectNode();
        sysObj.put("role", "system");
        sysObj.put("content", systemPrompt);
        messagesNode.add(sysObj);

        // History
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 16);
            for (int i = start; i < history.size(); i++) {
                Message m = history.get(i);
                var msgNode = mapper.createObjectNode();
                msgNode.put("role", m.getRole() == Message.Role.ASSISTANT ? "assistant" : "user");
                msgNode.put("content", m.getContent());
                messagesNode.add(msgNode);
            }
        }

        // Current message
        var curObj = mapper.createObjectNode();
        curObj.put("role", "user");
        curObj.put("content", userMessage);
        messagesNode.add(curObj);

        requestBody.set("messages", messagesNode);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(config.getEffectiveEndpoint()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        String key = getEffectiveKey();
        if (key != null && !key.isBlank()) {
            builder.header("Authorization", "Bearer " + key);
        }

        HttpRequest req = builder.build();
        HttpResponse<String> resp;
        try {
            resp = transport.send(req);
        } catch (HttpTimeoutException e) {
            throw new AiException("Request timed out after " + config.getTimeoutSeconds() + " seconds. Please retry.",
                AiException.ErrorType.TIMEOUT, e);
        } catch (IOException e) {
            throw new AiException("Network connection error: " + e.getMessage(),
                AiException.ErrorType.NETWORK_UNREACHABLE, e);
        }

        handleHttpErrors(resp.statusCode(), resp.body());

        JsonNode root = mapper.readTree(resp.body());
        JsonNode choices = root.path("choices");
        if (choices.isEmpty() || !choices.isArray()) {
            throw new AiException("The AI provider returned no choices.", AiException.ErrorType.INVALID_RESPONSE, 200);
        }

        String text = choices.get(0).path("message").path("content").asText("No response text.");
        return "[Live AI — " + config.getModel() + "]\n\n" + text;
    }

    private void handleHttpErrors(int statusCode, String responseBody) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        String detail = "";
        try {
            JsonNode root = mapper.readTree(responseBody);
            if (root.has("error")) {
                JsonNode err = root.path("error");
                if (err.has("message")) {
                    detail = ": " + err.path("message").asText();
                }
            }
        } catch (Exception ignored) {}

        if (statusCode == 401 || statusCode == 403) {
            throw new AiException("Authentication failed (HTTP " + statusCode + "). Please check your API key in Settings" + detail,
                AiException.ErrorType.AUTHENTICATION_FAILED, statusCode);
        } else if (statusCode == 400 && (detail.toLowerCase().contains("api key") || detail.toLowerCase().contains("api_key") || detail.contains("INVALID_ARGUMENT"))) {
            throw new AiException("Invalid API key or request argument (HTTP 400)" + detail,
                AiException.ErrorType.AUTHENTICATION_FAILED, statusCode);
        } else if (statusCode == 404) {
            throw new AiException("Model or endpoint not found (HTTP 404). Check the model name and endpoint in Settings" + detail,
                AiException.ErrorType.MODEL_NOT_FOUND, statusCode);
        } else if (statusCode == 429) {
            throw new AiException("Rate limit or quota exceeded (HTTP 429). Please wait before trying again" + detail,
                AiException.ErrorType.RATE_LIMITED, statusCode);
        } else if (statusCode == 503) {
            throw new AiException("AI provider service unavailable (HTTP 503). The model is temporarily overloaded or down for maintenance" + detail,
                AiException.ErrorType.SERVER_ERROR, statusCode);
        } else if (statusCode >= 500) {
            throw new AiException("AI provider server error (HTTP " + statusCode + "). The service is temporarily unavailable" + detail,
                AiException.ErrorType.SERVER_ERROR, statusCode);
        } else {
            throw new AiException("AI provider error (HTTP " + statusCode + ")" + detail,
                AiException.ErrorType.UNKNOWN, statusCode);
        }
    }

    // ─── Personalization & Privacy Isolation ──────────────────────────────────

    /**
     * Builds system prompt containing user profile and ONLY approved memories.
     * Guaranteed: Private journal entries are NEVER queried or leaked.
     * Guaranteed: Unapproved memories are excluded.
     */
    public String buildSystemPrompt(Conversation.Mode mode, UserProfile profile) {
        String name = profile != null ? profile.getDisplayName() : "User";
        String interests = profile != null && profile.getInterests() != null ? profile.getInterests() : "";
        String style = profile != null && profile.getCommunicationStyle() != null ? profile.getCommunicationStyle() : "";
        String goals = profile != null && profile.getGoals() != null ? profile.getGoals() : "";

        // Query and verify approved memories ONLY
        List<Memory> approvedMemories = memoryRepo.findApproved();
        StringBuilder memStr = new StringBuilder();
        if (approvedMemories != null && !approvedMemories.isEmpty()) {
            memStr.append("\n\nApproved User Memories (Strictly user-verified):\n");
            for (Memory m : approvedMemories) {
                if (m.isApproved()) {
                    memStr.append("- [").append(m.getCategory()).append("] ").append(m.getContent()).append("\n");
                }
            }
        }

        return switch (mode) {
            case GENERAL -> """
                You are a helpful, thoughtful, and private AI assistant named Chatbox.
                The user's name is %s. Interests: %s. Communication style: %s. Long-term goals: %s.
                Be concise, structured, courteous and accurate.%s""".formatted(name, interests, style, goals, memStr);
            case DOPPELGANGER -> """
                You are a digital representation of %s, built from their declared profile.
                You reflect their interests (%s), communication style (%s), and goals (%s).
                Respond as they might, but always maintain honest disclosure that you are an AI simulation.
                Never pretend to be the real biological person.%s""".formatted(name, interests, style, goals, memStr);
            case WELLNESS -> """
                You are a compassionate mental wellness companion for %s.
                Provide supportive, non-judgmental reflections and evidence-based coping strategies.
                CRITICAL SAFEGUARDS: You are NOT a therapist, psychiatrist, or medical professional.
                Never diagnose conditions or prescribe medications.
                Always recommend professional care for serious concerns.%s""".formatted(name, memStr);
            case COACH -> """
                You are an encouraging life coach and accountability partner for %s.
                Help with goal setting, milestone decomposition, habit consistency, and daily momentum.
                Be practical, actionable, and encouraging.%s""".formatted(name, memStr);
            case KNOWLEDGE -> """
                You are a personal knowledge assistant for %s.
                Answer questions strictly based on their imported Second Brain documents.
                Never fabricate facts or citations.%s""".formatted(name, memStr);
        };
    }

    // ─── Offline Demonstration Responder ──────────────────────────────────────

    public String buildDemoResponse(Conversation.Mode mode, String userMessage, UserProfile profile) {
        String name = (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank())
                ? profile.getDisplayName() : "there";
        String lower = (userMessage != null) ? userMessage.trim().toLowerCase() : "";

        String banner = DEMO_LABEL + "\n(Rule-based engine active. Set an AI API key in Settings for generative AI.)\n\n";

        // Supported slash commands
        if (lower.equals("/help") || lower.equals("/commands")) {
            return banner + """
                Here are the offline commands and capabilities currently available in Local Demo mode:
                
                • /help or /commands — Show this offline assistance guide
                • /plan or /study   — Generate a structured 3-phase study & focus blueprint
                • /goals            — Life coaching framework for milestone decomposition
                • /habits           — Behavioral consistency & routine advice
                • /wellness         — Grounding reflection and 4-7-8 breathing instructions
                • /profile          — View your declared Digital Doppelgänger identity blueprint
                
                You can also ask direct questions about study schedules, stress management, goals, and productivity!""";
        }

        if (lower.startsWith("/plan") || lower.startsWith("/study")) {
            return banner + "Study Planning Blueprint for " + name + ":\n\n" +
                "1. Focused Block (50 min):\n" +
                "   • Select ONE primary objective (e.g. solve 3 system design problems or read 1 chapter).\n" +
                "   • Eliminate notifications; use active recall rather than passive re-reading.\n\n" +
                "2. Recovery Interval (10 min):\n" +
                "   • Step away from screens. Hydrate and stretch to prevent cognitive fatigue.\n\n" +
                "3. Knowledge Review (15 min):\n" +
                "   • Summarize core learnings in your own words. Import notes into Second Brain for instant searching.";
        }

        if (lower.startsWith("/wellness") || lower.startsWith("/breathe")) {
            return banner + "Mindful Grounding & Breathing Guidance:\n\n" +
                "• 4-7-8 Breathing Technique:\n" +
                "  1. Inhale quietly through your nose for 4 seconds.\n" +
                "  2. Hold your breath calmly for 7 seconds.\n" +
                "  3. Exhale completely through your mouth for 8 seconds.\n" +
                "  (You can run this animated exercise in the Wellness tab!)\n\n" +
                "• 5-4-3-2-1 Sensory Grounding:\n" +
                "  Notice 5 things you can see, 4 you can touch, 3 you can hear, 2 you can smell, and 1 positive affirmation about yourself.";
        }

        if (lower.startsWith("/goals")) {
            return banner + "Actionable Goal Decomposition (Life Coach):\n\n" +
                "To turn your ambition into reality:\n" +
                "1. Define the Completion Condition: How will you verify this goal is 100% finished?\n" +
                "2. Identify the Next Immediate Action: What can you complete in under 20 minutes today?\n" +
                "3. Track Milestones: Add this to the Life Coach tab where your progress percentage auto-calculates from completed tasks.";
        }

        if (lower.startsWith("/habits")) {
            return banner + "Habit Architecture Guidance:\n\n" +
                "• Anchor to an Existing Trigger: Perform your new habit immediately after a fixed routine (e.g., 'After morning coffee, I will do 45 minutes of deep work').\n" +
                "• Reduce Friction: Prepare all materials in advance.\n" +
                "• Daily Check-Off: Mark your habit complete in the Life Coach tab to build visual consistency streaks.";
        }

        if (lower.startsWith("/profile") || lower.equals("/whoami")) {
            String interests = profile != null && profile.getInterests() != null ? profile.getInterests() : "Not declared";
            String style = profile != null && profile.getCommunicationStyle() != null ? profile.getCommunicationStyle() : "Not declared";
            String goals = profile != null && profile.getGoals() != null ? profile.getGoals() : "Not declared";
            return banner + "Digital Doppelgänger Persona Profile:\n\n" +
                "• User Name: " + name + "\n" +
                "• Declared Interests: " + interests + "\n" +
                "• Communication Style: " + style + "\n" +
                "• Long-Term Goals: " + goals + "\n\n" +
                "You can edit or expand these traits at any time in the Digital Doppelgänger tab.";
        }

        // Rule-based keyword matching
        if (lower.matches(".*\\b(hi|hello|hey|greetings|good morning|good afternoon|good evening)\\b.*")) {
            return banner + "Hello " + name + "! I received your message: \"" + userMessage + "\". I'm Chatbox, your offline companion.\n\n" +
                "I am currently operating in Local Demo mode. I can help organize study sessions, guide breathing exercises, manage your goals, and structure daily plans.\n\n" +
                "Type /help to see all available commands, or navigate using the sidebar to explore your companion modules!";
        }

        if (lower.contains("study") || lower.contains("exam") || lower.contains("revision") || lower.contains("homework")) {
            return banner + "Here is a structured study recommendation for you, " + name + ":\n\n" +
                "• Prioritize High-Yield Topics: Spend 70% of study time actively testing yourself rather than rereading.\n" +
                "• Use 50-Minute Focus Sprints: Work with total concentration for 50 minutes, then take a mandatory 10-minute break.\n" +
                "• Document Intelligence: Import lecture slides or study PDFs into Second Brain to search excerpts and test comprehension.";
        }

        if (lower.contains("stress") || lower.contains("anxious") || lower.contains("anxiety") || lower.contains("overwhelm") || lower.contains("tired")) {
            return banner + "I hear you, " + name + ". It's completely normal to feel this way during demanding periods.\n\n" +
                "• Pause for 2 minutes and try the 4-7-8 breathing exercise in the Wellness tab.\n" +
                "• Break your workload down into smaller steps in the Life Coach tab so you only focus on one task at a time.\n" +
                "• Remember: This is self-reflection guidance, not medical care. Be sure to reach out to loved ones or professionals if you need support.";
        }

        if (lower.contains("goal") || lower.contains("task") || lower.contains("plan") || lower.contains("todo")) {
            return banner + "Great initiative, " + name + ".\n\n" +
                "A successful goal needs three parts:\n" +
                "1. Specific measurable milestone.\n" +
                "2. Realistic target completion date.\n" +
                "3. Linked daily tasks.\n\n" +
                "Head over to the Life Coach tab to register this goal. Whenever you mark tasks done, the goal's progress bar will automatically update!";
        }

        // Default mode-specific fallback
        return switch (mode) {
            case GENERAL -> banner + "Hi " + name + "! I received your message: \"" + userMessage + "\"\n\n" +
                "In Local Demo mode, I provide rule-based study planning, goal structuring, and wellness guidance. Type /help to see available commands.\n\n" +
                "To connect to live generative language models with full reasoning, enter your AI API key in Settings.";
            case DOPPELGANGER -> banner + "Digital Doppelgänger Simulation for " + name + ":\n\n" +
                "Based on your profile, your declared focus includes: " +
                (profile != null && profile.getInterests() != null && !profile.getInterests().isBlank() ? profile.getInterests() : "Engineering & Design") + ".\n\n" +
                "Regarding \"" + userMessage + "\": As your simulation, I reflect your preference for structured, thoughtful approaches. Configure an AI key in Settings to experience dynamic persona mirroring.";
            case WELLNESS -> banner + "Mental Wellness Reflection for " + name + ":\n\n" +
                "Thank you for sharing: \"" + userMessage + "\". Taking a moment to express your thoughts is an important habit.\n\n" +
                "Try logging your current mood score in the Wellness tab or doing a 4-7-8 breathing session. All wellness records remain stored locally on your machine.";
            case COACH -> banner + "Life Coach Action Plan for " + name + ":\n\n" +
                "Regarding: \"" + userMessage + "\"\n\n" +
                "In live mode, I'd help you break down this ambition into actionable steps, milestones, and track your progress. Recommended next steps:\n" +
                "1. Turn this into a specific task in the Life Coach tab.\n" +
                "2. Assign it a priority (High / Medium / Low) and due date.\n" +
                "3. Complete the task today to maintain your momentum!";
            case KNOWLEDGE -> banner + "Personal Second Brain Guidance for " + name + ":\n\n" +
                "To answer questions regarding \"" + userMessage + "\", ensure you have imported relevant .pdf, .md, or .txt documents in the Second Brain section.\n\n" +
                "The search index will find matching passages and display excerpts directly from your files.";
        };
    }
}
