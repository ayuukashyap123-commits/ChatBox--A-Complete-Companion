package com.chatbox.controller;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.AiConfig;
import com.chatbox.repository.ConversationRepository;
import com.chatbox.service.AiService;
import com.chatbox.util.UIUtil;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings controller providing:
 * - Runtime and persistent AI provider configuration (Google Gemini, OpenAI-compatible).
 * - Live connection testing with latency and error diagnosis.
 * - SQLite storage metrics, table row counts, and maintenance tools.
 * - Privacy information and local data sovereignty guarantees.
 * - Capstone application and runtime version metadata.
 */
public class SettingsController {

    private final AiService aiService;
    private final Runnable onStatusChanged;
    private final ConversationRepository convRepo = new ConversationRepository();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "settings-worker");
        t.setDaemon(true);
        return t;
    });

    private Label statusBadge;
    private Label sourceLabel;
    private Label testResultLabel;

    private ComboBox<AiConfig.Provider> providerCombo;
    private TextField modelField;
    private TextField endpointField;
    private TextField timeoutField;
    private PasswordField keyField;

    private Button testBtn;
    private Button saveBtn;
    private Button demoBtn;

    // Database metrics
    private VBox storageStatsBox;

    public SettingsController(AiService aiService, Runnable onStatusChanged) {
        this.aiService = aiService;
        this.onStatusChanged = onStatusChanged;
    }

    public Node build() {
        VBox root = new VBox();
        root.getStyleClass().add("content-area");

        // Header
        VBox header = new VBox(4);
        header.getStyleClass().add("page-header");

        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");

        Label sub = new Label("Manage AI connections, local database storage, and system preferences.");
        sub.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(title, sub);

        // Body in ScrollPane for laptop display adaptability
        VBox body = new VBox(24);
        body.getStyleClass().add("page-body");
        body.getChildren().addAll(
            buildAiSection(),
            buildStorageSection(),
            buildPrivacySection(),
            buildAboutSection()
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll);
        return root;
    }

    // ─── Section 1: AI Provider & Configuration ───────────────────────────────

    private VBox buildAiSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label heading = new Label("AI ENGINE & CONNECTIVITY");
        heading.getStyleClass().add("section-title");

        // Current status row
        HBox statusRow = new HBox(12);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Current Status:");
        lbl.setStyle("-fx-text-fill: #8891a8; -fx-font-size: 12px;");

        statusBadge = new Label(aiService.getStatusLabel());
        updateStatusBadge();

        sourceLabel = new Label("Source: " + aiService.getSourceInfo());
        sourceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");

        statusRow.getChildren().addAll(lbl, statusBadge, sourceLabel);

        // Form Grid
        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(12);

        // Provider
        formGrid.add(formLabel("Provider:"), 0, 0);
        providerCombo = new ComboBox<>();
        providerCombo.getItems().addAll(AiConfig.Provider.values());
        providerCombo.setValue(aiService.getConfig().getProvider());
        providerCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AiConfig.Provider item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        providerCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AiConfig.Provider item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        providerCombo.setMaxWidth(Double.MAX_VALUE);
        formGrid.add(providerCombo, 1, 0);

        // Model
        formGrid.add(formLabel("Model Identifier:"), 0, 1);
        modelField = new TextField(aiService.getConfig().getModel());
        modelField.setPromptText("e.g. gemini-3.8-flash, gemini-3.5-flash, or gpt-4o-mini");
        formGrid.add(modelField, 1, 1);

        // Endpoint
        formGrid.add(formLabel("Endpoint URL:"), 0, 2);
        endpointField = new TextField(aiService.getConfig().getEndpoint());
        endpointField.setPromptText("Leave blank for default provider endpoint, or enter custom URL (e.g. Ollama)");
        formGrid.add(endpointField, 1, 2);

        // Update defaults when provider changes
        providerCombo.setOnAction(e -> {
            AiConfig.Provider sel = providerCombo.getValue();
            if (sel != null && (modelField.getText().isBlank() || modelField.getText().contains("gemini") || modelField.getText().contains("gpt"))) {
                modelField.setText(sel.getDefaultModel());
            }
        });

        // Timeout
        formGrid.add(formLabel("Timeout (seconds):"), 0, 3);
        timeoutField = new TextField(String.valueOf(aiService.getConfig().getTimeoutSeconds()));
        timeoutField.setMaxWidth(100);
        formGrid.add(timeoutField, 1, 3);

        // API Key
        formGrid.add(formLabel("API Key:"), 0, 4);
        keyField = new PasswordField();
        keyField.setPromptText("Enter API key (stored securely in local SQLite, never committed)");
        String effKey = aiService.getEffectiveKey();
        if (effKey != null && !effKey.isBlank()) {
            keyField.setText(effKey);
        }
        formGrid.add(keyField, 1, 4);

        // Button row
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        saveBtn = new Button("Apply & Save Settings");
        saveBtn.getStyleClass().addAll("btn", "btn-primary");

        testBtn = new Button("Test Connection");
        testBtn.getStyleClass().add("btn");

        demoBtn = new Button(aiService.isForceOffline() ? "Exit Offline Demo" : "Switch to Offline Demo");
        demoBtn.getStyleClass().add("btn");

        buttonRow.getChildren().addAll(saveBtn, testBtn, demoBtn);

        testResultLabel = new Label("");
        testResultLabel.setStyle("-fx-font-size: 11px;");
        testResultLabel.setWrapText(true);

        // Handlers
        saveBtn.setOnAction(e -> {
            AiConfig.Provider prov = providerCombo.getValue() != null ? providerCombo.getValue() : AiConfig.Provider.GEMINI;
            String model = modelField.getText().trim();
            String endpoint = endpointField.getText().trim();
            String key = keyField.getText().trim();
            int timeout = 30;
            try {
                timeout = Integer.parseInt(timeoutField.getText().trim());
            } catch (Exception ignored) {}

            boolean forceOffline = aiService.isForceOffline();
            aiService.saveConfig(prov, model, endpoint, timeout, forceOffline, key);
            aiService.setRuntimeApiKey(key.isBlank() ? null : key);

            updateStatusBadge();
            sourceLabel.setText("Source: " + aiService.getSourceInfo());
            testResultLabel.setText("✓ Configuration saved and applied.");
            testResultLabel.setStyle("-fx-text-fill: #3ecf5e;");
            if (onStatusChanged != null) onStatusChanged.run();
        });

        testBtn.setOnAction(e -> {
            AiConfig.Provider prov = providerCombo.getValue() != null ? providerCombo.getValue() : AiConfig.Provider.GEMINI;
            String model = modelField.getText().trim();
            String endpoint = endpointField.getText().trim();
            String candidateKey = keyField.getText().trim();
            if (candidateKey.isBlank()) candidateKey = aiService.getEffectiveKey();

            if ((candidateKey == null || candidateKey.isBlank()) && !endpoint.contains("localhost") && !endpoint.contains("127.0.0.1")) {
                testResultLabel.setText("Please enter an API key first.");
                testResultLabel.setStyle("-fx-text-fill: #e05555;");
                return;
            }

            int timeout = 30;
            try {
                timeout = Integer.parseInt(timeoutField.getText().trim());
            } catch (Exception ignored) {}

            testResultLabel.setText("Testing connection to " + prov.getDisplayName() + " (" + model + ")…");
            testResultLabel.setStyle("-fx-text-fill: #8891a8;");
            testBtn.setDisable(true);

            final String keyToTest = candidateKey;
            final String endToTest = endpoint;
            final int timeoutToTest = timeout;
            executor.submit(() -> {
                long start = System.currentTimeMillis();
                try {
                    boolean ok = aiService.testConnection(keyToTest, prov, endToTest, model, timeoutToTest);
                    long elapsed = System.currentTimeMillis() - start;
                    Platform.runLater(() -> {
                        testBtn.setDisable(false);
                        testResultLabel.setText("✓ Success! Provider responded in " + elapsed + "ms.");
                        testResultLabel.setStyle("-fx-text-fill: #3ecf5e;");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        testBtn.setDisable(false);
                        testResultLabel.setText("✗ Error: " + ex.getMessage());
                        testResultLabel.setStyle("-fx-text-fill: #e05555;");
                    });
                }
            });
        });

        demoBtn.setOnAction(e -> {
            boolean currentOffline = aiService.isForceOffline();
            boolean newOffline = !currentOffline;
            aiService.setForceOffline(newOffline);
            demoBtn.setText(newOffline ? "Exit Offline Demo" : "Switch to Offline Demo");
            updateStatusBadge();
            sourceLabel.setText("Source: " + aiService.getSourceInfo());
            testResultLabel.setText(newOffline ? "Switched to Offline Demonstration Mode." : "Exited Offline Mode. Live AI active if key is available.");
            testResultLabel.setStyle("-fx-text-fill: #f0a23c;");
            if (onStatusChanged != null) onStatusChanged.run();
        });

        Label note = new Label("Truthful Disclosure: When in Local Demo Mode, all conversation responses are generated locally with rule-based guidance and document-grounded search. When Live AI is active, requests are sent via HTTPS directly to your chosen provider with strict privacy boundaries (zero journal sharing, zero unapproved memory leaks).");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        note.setWrapText(true);

        card.getChildren().addAll(heading, statusRow, formGrid, buttonRow, testResultLabel, note);
        return card;
    }

    private Label formLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #8891a8;");
        return l;
    }

    private void updateStatusBadge() {
        if (statusBadge != null) {
            statusBadge.setText(aiService.getStatusLabel());
            boolean isLive = aiService.isLive();
            statusBadge.setStyle("-fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-background-color: " + (isLive ? "#1a3322" : "#33261a") + "; " +
                "-fx-text-fill: " + (isLive ? "#3ecf5e" : "#f0a23c") + ";");
        }
    }

    // ─── Section 2: Local Storage & SQLite Database ───────────────────────────

    private VBox buildStorageSection() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label heading = new Label("LOCAL STORAGE & SQLITE DATABASE");
        heading.getStyleClass().add("section-title");

        String dbPath = DatabaseManager.getInstance().getDbPath();
        File dbFile = new File(dbPath);
        long sizeBytes = dbFile.exists() ? dbFile.length() : 0;
        String sizeFormatted = "%.1f KB".formatted(sizeBytes / 1024.0);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(16);
        infoGrid.setVgap(8);

        infoGrid.add(metaLabel("Database Path:"), 0, 0);
        Label pathVal = new Label(dbPath);
        pathVal.setStyle("-fx-text-fill: #c4beff; -fx-font-family: monospace; -fx-font-size: 11px;");
        infoGrid.add(pathVal, 1, 0);

        infoGrid.add(metaLabel("Storage Size:"), 0, 1);
        infoGrid.add(valueLabel(sizeFormatted), 1, 1);

        infoGrid.add(metaLabel("Journal Mode:"), 0, 2);
        infoGrid.add(valueLabel("Write-Ahead Logging (WAL) — High-Performance Concurrency"), 1, 2);

        // Record metrics box
        storageStatsBox = new VBox(6);
        refreshStorageCounts();

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnRefresh = new Button("Refresh Stats");
        btnRefresh.getStyleClass().add("btn");
        btnRefresh.setOnAction(e -> refreshStorageCounts());

        Button btnWipeChat = new Button("Clear Chat History");
        btnWipeChat.getStyleClass().addAll("btn", "btn-danger");
        btnWipeChat.setOnAction(e -> {
            if (UIUtil.showConfirm("Clear Conversations", "Are you sure you want to delete all saved conversations and messages?")) {
                try {
                    convRepo.deleteAll();
                    refreshStorageCounts();
                } catch (Exception ex) {
                    UIUtil.showError("Error", "Failed to clear conversations: " + ex.getMessage());
                }
            }
        });

        actions.getChildren().addAll(btnRefresh, btnWipeChat);

        card.getChildren().addAll(heading, infoGrid, storageStatsBox, actions);
        return card;
    }

    private void refreshStorageCounts() {
        storageStatsBox.getChildren().clear();

        Label statsTitle = new Label("Table Record Counts:");
        statsTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(4);

        String[] tables = {
            "conversations", "messages", "goals", "tasks",
            "habits", "mood_entries", "journal_entries", "knowledge_documents", "memories", "app_settings"
        };
        String[] displayNames = {
            "Conversations", "Messages", "Goals", "Tasks",
            "Habits", "Mood Logs", "Journals", "Documents", "Memories", "Settings"
        };

        Map<String, Integer> counts = DatabaseManager.getInstance().getTableRecordCounts();
        for (int i = 0; i < tables.length; i++) {
            int count = counts.getOrDefault(tables[i], 0);
            int col = (i < 5) ? 0 : 2;
            int row = (i < 5) ? i : (i - 5);
            grid.add(metaLabel(displayNames[i] + ":"), col, row);
            grid.add(valueLabel(String.valueOf(count)), col + 1, row);
        }

        storageStatsBox.getChildren().addAll(statsTitle, grid);
    }

    // ─── Section 3: Privacy & Sovereignty ─────────────────────────────────────

    private VBox buildPrivacySection() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label heading = new Label("DATA PRIVACY & LOCAL SOVEREIGNTY");
        heading.getStyleClass().add("section-title");

        String[] guarantees = {
            "100% Local Storage: All goals, journal entries, mood records, and Second Brain documents remain stored on your local disk in SQLite.",
            "Explicit Memory Approval: Only memories you explicitly inspect and toggle to 'Active' are injected into AI prompts.",
            "Strict Journal Isolation: Personal wellness journals are never converted to memories, never indexed, and never sent to any external AI provider.",
            "Grounded Second Brain: Document intelligence queries are grounded strictly in retrieved passages with zero hallucinated citations.",
            "Zero Telemetry: Chatbox has no analytics beacons, user tracking, or third-party advertising SDKs."
        };

        for (String g : guarantees) {
            HBox item = new HBox(8);
            item.setAlignment(Pos.TOP_LEFT);
            Label bullet = new Label("•");
            bullet.setStyle("-fx-text-fill: #7c6ff7; -fx-font-weight: bold;");
            Label text = new Label(g);
            text.setStyle("-fx-font-size: 12px; -fx-text-fill: #8891a8;");
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);
            item.getChildren().addAll(bullet, text);
            card.getChildren().add(item);
        }

        return card;
    }

    // ─── Section 4: About & System Info ───────────────────────────────────────

    private VBox buildAboutSection() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label heading = new Label("ABOUT & RUNTIME ENVIRONMENT");
        heading.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);

        grid.add(metaLabel("Application:"), 0, 0);
        grid.add(valueLabel("Chatbox — A Complete Companion"), 1, 0);

        grid.add(metaLabel("Version:"), 0, 1);
        grid.add(valueLabel("1.0.0 (Senior Engineering Capstone MVP)"), 1, 1);

        grid.add(metaLabel("Runtime JDK:"), 0, 2);
        grid.add(valueLabel(System.getProperty("java.runtime.version") + " (" + System.getProperty("java.vendor") + ")"), 1, 2);

        grid.add(metaLabel("UI Framework:"), 0, 3);
        grid.add(valueLabel("JavaFX 21.0.2 with Custom Charcoal/Violet Design System"), 1, 3);

        grid.add(metaLabel("Architecture:"), 0, 4);
        grid.add(valueLabel("Offline-First Native Desktop Architecture"), 1, 4);

        card.getChildren().addAll(heading, grid);
        return card;
    }

    private Label metaLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #8891a8;");
        return l;
    }

    private Label valueLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
        return l;
    }
}
