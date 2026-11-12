package com.chatbox.model;

/**
 * Encapsulates AI provider configuration, model, endpoint, timeout,
 * and offline demonstration mode options.
 */
public class AiConfig {
    public enum Provider {
        GEMINI("Google Gemini", "gemini-3.8-flash", "https://generativelanguage.googleapis.com/v1beta/models/"),
        OPENAI_COMPATIBLE("OpenAI-Compatible", "gpt-4o-mini", "https://api.openai.com/v1/chat/completions");

        private final String displayName;
        private final String defaultModel;
        private final String defaultEndpoint;

        Provider(String displayName, String defaultModel, String defaultEndpoint) {
            this.displayName = displayName;
            this.defaultModel = defaultModel;
            this.defaultEndpoint = defaultEndpoint;
        }

        public String getDisplayName() { return displayName; }
        public String getDefaultModel() { return defaultModel; }
        public String getDefaultEndpoint() { return defaultEndpoint; }
    }

    private Provider provider = Provider.GEMINI;
    private String apiKey = "";
    private String endpoint = "";
    private String model = "gemini-3.8-flash";
    private int timeoutSeconds = 30;
    private boolean forceOffline = false;

    public AiConfig() {}

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider != null ? provider : Provider.GEMINI; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey != null ? apiKey.trim() : ""; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint != null ? endpoint.trim() : ""; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = (model != null && !model.isBlank()) ? model.trim() : provider.getDefaultModel(); }

    public int getTimeoutSeconds() { return timeoutSeconds > 0 ? timeoutSeconds : 30; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30; }

    public boolean isForceOffline() { return forceOffline; }
    public void setForceOffline(boolean forceOffline) { this.forceOffline = forceOffline; }

    /**
     * Resolves the effective API endpoint URL.
     */
    public String getEffectiveEndpoint() {
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint;
        }
        return provider.getDefaultEndpoint();
    }
}
