package com.chatbox.controller;

import com.chatbox.model.*;
import com.chatbox.repository.*;
import com.chatbox.service.AiService;
import com.chatbox.util.UIUtil;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * AI Chatbox controller — supports multiple conversation modes,
 * persistent message history, duplicate submission prevention,
 * request retry without duplicate messages, and background AI execution.
 */
public class ChatController {

    private final AiService aiService;
    private final ConversationRepository convRepo = new ConversationRepository();
    private final UserProfileRepository profileRepo = new UserProfileRepository();

    private ListView<Conversation> convList;
    private VBox messagesBox;
    private ScrollPane messagesScroll;
    private TextArea inputField;
    private Button sendBtn;
    private ComboBox<Conversation.Mode> modeCombo;
    private Label statusLabel;
    private HBox modeButtons;

    private Conversation currentConv;
    private List<Message> currentMessages = new ArrayList<>();
    private volatile boolean isSending = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-worker");
        t.setDaemon(true);
        return t;
    });

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    public Node build() {
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.28);
        split.getStyleClass().add("split-pane");

        split.getItems().addAll(buildConvSidebar(), buildChatArea());
        return split;
    }

    // ─── Conversation sidebar ─────────────────────────────────────────────

    private VBox buildConvSidebar() {
        VBox sidebar = new VBox();
        sidebar.setStyle("-fx-background-color: #13161d; -fx-border-color: #1e2334; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(240);

        // Header
        HBox header = new HBox(8);
        header.setPadding(new Insets(16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color: transparent transparent #1e2334 transparent; -fx-border-width: 0 0 1 0;");

        Label title = new Label("Conversations");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnNew = new Button("+");
        btnNew.getStyleClass().addAll("btn");
        btnNew.setStyle("-fx-font-size: 16px; -fx-padding: 2 10; -fx-font-weight: bold;");
        btnNew.setTooltip(new Tooltip("New Conversation"));
        btnNew.setOnAction(e -> newConversation());

        header.getChildren().addAll(title, btnNew);

        // Mode filter
        modeCombo = new ComboBox<>();
        modeCombo.getItems().add(null);
        modeCombo.getItems().addAll(Conversation.Mode.values());
        modeCombo.setValue(null);
        modeCombo.setPromptText("All Modes");
        modeCombo.getStyleClass().add("mode-selector");
        modeCombo.setMaxWidth(Double.MAX_VALUE);
        modeCombo.setPadding(new Insets(8, 12, 0, 12));
        modeCombo.setOnAction(e -> refreshConvList());

        // List
        convList = new ListView<>();
        convList.getStyleClass().add("conv-list");
        VBox.setVgrow(convList, Priority.ALWAYS);

        convList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                VBox cell = new VBox(2);
                Label titleLbl = new Label(UIUtil.truncate(item.getTitle(), 28));
                titleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
                Label modeLbl = new Label(item.getMode().getLabel() + " · " +
                    UIUtil.formatShort(item.getUpdatedAt()));
                modeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #4d5468;");
                cell.getChildren().addAll(titleLbl, modeLbl);
                setGraphic(cell);
                setText(null);
            }
        });

        convList.getSelectionModel().selectedItemProperty().addListener((obs, old, conv) -> {
            if (conv != null) loadConversation(conv);
        });

        Button btnDelete = new Button("Delete Conversation");
        btnDelete.getStyleClass().addAll("btn", "btn-danger");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setPadding(new Insets(8, 12, 8, 12));
        btnDelete.setStyle("-fx-margin: 8px;");
        btnDelete.setOnAction(e -> deleteCurrentConversation());

        VBox bottomBar = new VBox(btnDelete);
        bottomBar.setPadding(new Insets(8));
        bottomBar.setStyle("-fx-border-color: #1e2334 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        sidebar.getChildren().addAll(header, modeCombo, convList, bottomBar);
        refreshConvList();
        return sidebar;
    }

    // ─── Chat area ────────────────────────────────────────────────────────

    private VBox buildChatArea() {
        VBox chatArea = new VBox();
        chatArea.getStyleClass().add("content-area");

        // Chat header with mode selector
        HBox chatHeader = new HBox(12);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(14, 20, 14, 20));
        chatHeader.setStyle("-fx-background-color: #13161d; -fx-border-color: transparent transparent #1e2334 transparent; -fx-border-width: 0 0 1 0;");

        Label chatTitle = new Label("AI Chat");
        chatTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        HBox.setHgrow(chatTitle, Priority.ALWAYS);

        statusLabel = new Label(aiService.getStatusLabel());
        updateChatStatusLabel();
        aiService.addStatusListener(() -> Platform.runLater(this::updateChatStatusLabel));

        chatHeader.getChildren().addAll(chatTitle, statusLabel);

        // Messages area
        messagesBox = new VBox(10);
        messagesBox.getStyleClass().add("chat-container");
        messagesBox.setFillWidth(true);

        messagesScroll = new ScrollPane(messagesBox);
        messagesScroll.setFitToWidth(true);
        messagesScroll.getStyleClass().add("scroll-pane");
        messagesScroll.setStyle("-fx-background-color: #0d0f14;");
        VBox.setVgrow(messagesScroll, Priority.ALWAYS);

        // Empty state
        showEmptyState();

        // Input area
        VBox inputArea = new VBox(10);
        inputArea.getStyleClass().add("chat-input-area");

        // Mode selector bar
        HBox modeBar = new HBox(8);
        modeBar.setAlignment(Pos.CENTER_LEFT);
        Label modeLabel = new Label("Mode:");
        modeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");

        ToggleGroup modeGroup = new ToggleGroup();
        modeButtons = new HBox(4);
        for (Conversation.Mode m : Conversation.Mode.values()) {
            ToggleButton tb = new ToggleButton(m.getLabel());
            tb.setToggleGroup(modeGroup);
            tb.getStyleClass().add("btn");
            tb.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
            if (m == Conversation.Mode.GENERAL) {
                tb.setSelected(true);
                tb.getStyleClass().add("btn-primary");
            }
            tb.selectedProperty().addListener((obs, old, sel) -> {
                if (sel) {
                    tb.getStyleClass().add("btn-primary");
                    if (currentConv != null) currentConv.setMode(m);
                } else {
                    tb.getStyleClass().remove("btn-primary");
                }
            });
            modeButtons.getChildren().add(tb);
        }

        modeBar.getChildren().addAll(modeLabel, modeButtons);

        // Text input row
        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.BOTTOM_CENTER);

        inputField = new TextArea();
        inputField.setPromptText("Type your message… (Shift+Enter for new line, Enter to send)");
        inputField.getStyleClass().add("chat-input-field");
        inputField.setPrefHeight(60);
        inputField.setMaxHeight(120);
        inputField.setWrapText(true);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendMessage(modeGroup);
            }
        });

        sendBtn = new Button("Send");
        sendBtn.getStyleClass().addAll("btn", "btn-primary");
        sendBtn.setPrefHeight(60);
        sendBtn.setPrefWidth(80);
        sendBtn.setOnAction(e -> sendMessage(modeGroup));

        inputRow.getChildren().addAll(inputField, sendBtn);
        inputArea.getChildren().addAll(modeBar, inputRow);

        chatArea.getChildren().addAll(chatHeader, messagesScroll, inputArea);
        return chatArea;
    }

    // ─── Actions ──────────────────────────────────────────────────────────

    private void newConversation() {
        Conversation conv = new Conversation("New Conversation", Conversation.Mode.GENERAL);
        convRepo.save(conv);
        refreshConvList();
        convList.getSelectionModel().select(conv);
        loadConversation(conv);
    }

    private void loadConversation(Conversation conv) {
        currentConv = conv;
        currentMessages = convRepo.findMessages(conv.getId());
        renderMessages();
    }

    private void deleteCurrentConversation() {
        if (currentConv == null) return;
        if (!UIUtil.showConfirm("Delete Conversation", "Delete this conversation? This cannot be undone.")) return;
        convRepo.delete(currentConv.getId());
        currentConv = null;
        currentMessages = new ArrayList<>();
        refreshConvList();
        showEmptyState();
    }

    private void sendMessage(ToggleGroup modeGroup) {
        if (isSending) return; // Prevent duplicate concurrent submissions

        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        // Get selected mode
        Conversation.Mode selectedMode = Conversation.Mode.GENERAL;
        if (modeGroup.getSelectedToggle() != null) {
            ToggleButton sel = (ToggleButton) modeGroup.getSelectedToggle();
            for (Conversation.Mode m : Conversation.Mode.values()) {
                if (m.getLabel().equals(sel.getText())) { selectedMode = m; break; }
            }
        }

        // Ensure conversation exists
        if (currentConv == null) {
            currentConv = new Conversation(UIUtil.truncate(text, 40), selectedMode);
            convRepo.save(currentConv);
            refreshConvList();
        }

        // Update title if it's the first message
        if (currentMessages.isEmpty()) {
            String title = UIUtil.truncate(text, 40);
            convRepo.updateTitle(currentConv.getId(), title);
            currentConv.setTitle(title);
            refreshConvList();
        }

        isSending = true;
        inputField.clear();
        setControlsDisabled(true);

        // Save user message
        Message userMsg = new Message(currentConv.getId(), Message.Role.USER, text);
        convRepo.saveMessage(userMsg);
        currentMessages.add(userMsg);
        renderMessages();

        dispatchAiRequest(selectedMode, userMsg);
    }

    private void dispatchAiRequest(Conversation.Mode mode, Message userMsg) {
        // Visible loading indicator bubble
        Node thinkingBubble = buildThinkingBubble();
        messagesBox.getChildren().add(thinkingBubble);
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));

        final List<Message> historySnapshot = new ArrayList<>(currentMessages);
        final UserProfile profile = profileRepo.getProfile();

        executor.submit(() -> {
            try {
                String response = aiService.chat(mode, userMsg.getContent(), historySnapshot, profile);
                Platform.runLater(() -> {
                    messagesBox.getChildren().remove(thinkingBubble);
                    Message aiMsg = new Message(currentConv.getId(), Message.Role.ASSISTANT, response);
                    convRepo.saveMessage(aiMsg);
                    currentMessages.add(aiMsg);
                    renderMessages();
                    isSending = false;
                    setControlsDisabled(false);
                    inputField.requestFocus();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    messagesBox.getChildren().remove(thinkingBubble);
                    isSending = false;
                    setControlsDisabled(false);
                    messagesBox.getChildren().add(buildErrorCard(ex.getMessage(), () -> retryMessage(mode, userMsg)));
                    Platform.runLater(() -> messagesScroll.setVvalue(1.0));
                });
            }
        });
    }

    /**
     * Retries a failed AI request WITHOUT duplicating the saved user message in database.
     */
    private void retryMessage(Conversation.Mode mode, Message existingUserMsg) {
        if (isSending) return;
        isSending = true;
        setControlsDisabled(true);
        dispatchAiRequest(mode, existingUserMsg);
    }

    private void setControlsDisabled(boolean disabled) {
        inputField.setDisable(disabled);
        sendBtn.setDisable(disabled);
        sendBtn.setText(disabled ? "…" : "Send");
        if (modeButtons != null) modeButtons.setDisable(disabled);
    }

    private void updateChatStatusLabel() {
        if (statusLabel != null) {
            boolean isLive = aiService.isLive();
            statusLabel.setText(aiService.getStatusLabel());
            statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; " +
                "-fx-background-color: " + (isLive ? "#1a3322" : "#33261a") + "; " +
                "-fx-text-fill: " + (isLive ? "#3ecf5e" : "#f0a23c") + ";");
        }
    }

    private Node buildThinkingBubble() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 4, 0));

        VBox bubble = new VBox(4);
        bubble.setStyle("-fx-background-color: #1a1e28; -fx-border-color: #1e2334; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");

        String indicatorText = aiService.isLive()
            ? "Generating response with " + aiService.getConfig().getModel() + "…"
            : "Formulating response with offline demo engine…";

        Label thinking = new Label(indicatorText);
        thinking.setStyle("-fx-font-size: 12px; -fx-text-fill: #8891a8; -fx-font-style: italic;");
        bubble.getChildren().add(thinking);

        row.getChildren().add(bubble);
        return row;
    }

    private Node buildErrorCard(String errorMsg, Runnable retryAction) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #2e1616; -fx-border-color: #e05555; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10 14;");

        VBox textBox = new VBox(2);
        Label errTitle = new Label("Failed to generate response");
        errTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e05555;");
        Label errMsg = new Label(errorMsg != null ? errorMsg : "Network or provider error.");
        errMsg.setStyle("-fx-font-size: 11px; -fx-text-fill: #e8eaf0;");
        errMsg.setWrapText(true);
        textBox.getChildren().addAll(errTitle, errMsg);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button retryBtn = new Button("Retry");
        retryBtn.getStyleClass().addAll("btn", "btn-primary");
        retryBtn.setOnAction(e -> {
            messagesBox.getChildren().remove(card);
            if (retryAction != null) retryAction.run();
        });

        card.getChildren().addAll(textBox, retryBtn);
        return card;
    }

    private void renderMessages() {
        messagesBox.getChildren().clear();

        if (currentMessages.isEmpty()) {
            showEmptyState();
            return;
        }

        for (Message msg : currentMessages) {
            messagesBox.getChildren().add(buildMessageBubble(msg));
        }

        // Scroll to bottom
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private Node buildMessageBubble(Message msg) {
        boolean isUser = msg.getRole() == Message.Role.USER;

        VBox bubble = new VBox(4);
        bubble.getStyleClass().addAll("message-bubble",
            isUser ? "message-bubble-user" : "message-bubble-assistant");
        bubble.setMaxWidth(700);

        // Role & provenance header
        HBox roleBar = new HBox(6);
        roleBar.setAlignment(Pos.CENTER_LEFT);

        Label roleLabel = new Label(isUser ? "You" : "Chatbox");
        roleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " +
            (isUser ? "#c4beff" : "#8891a8") + ";");
        roleBar.getChildren().add(roleLabel);

        // Response provenance badge for assistant
        if (!isUser) {
            String contentStr = msg.getContent() != null ? msg.getContent() : "";
            Label badge = new Label();
            badge.setStyle("-fx-font-size: 9px; -fx-padding: 1 5; -fx-background-radius: 3; -fx-font-weight: bold; ");
            if (contentStr.startsWith("[Live AI")) {
                badge.setText("Live AI");
                badge.setStyle(badge.getStyle() + "-fx-background-color: #1f2538; -fx-text-fill: #9d8df1;");
                roleBar.getChildren().add(badge);
            } else if (contentStr.startsWith("[Local Demo") || contentStr.contains("Demo Mode")) {
                badge.setText("Offline Demo");
                badge.setStyle(badge.getStyle() + "-fx-background-color: #2b2318; -fx-text-fill: #f0a23c;");
                roleBar.getChildren().add(badge);
            } else if (contentStr.startsWith("[Second Brain")) {
                badge.setText("Second Brain");
                badge.setStyle(badge.getStyle() + "-fx-background-color: #17282b; -fx-text-fill: #3ecfc0;");
                roleBar.getChildren().add(badge);
            }
        }

        // Content
        Label content = new Label(msg.getContent());
        content.getStyleClass().add(isUser ? "message-text-user" : "message-text-assistant");
        content.setWrapText(true);
        content.setMaxWidth(660);

        // Time
        Label time = new Label(UIUtil.formatShort(msg.getCreatedAt()));
        time.getStyleClass().add("message-time");

        bubble.getChildren().addAll(roleBar, content, time);

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(0, 0, 2, 0));
        if (isUser) {
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }
        return row;
    }

    private void showEmptyState() {
        messagesBox.getChildren().clear();
        VBox empty = new VBox(12);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        VBox.setVgrow(empty, Priority.ALWAYS);

        Node icon = com.chatbox.util.IconUtil.createIcon("chat", 36, "#3a3566");
        Label text = new Label("Select or start a conversation");
        text.getStyleClass().add("empty-state-text");
        Label sub = new Label("Choose a mode and type your first message below");
        sub.getStyleClass().add("empty-state-sub");

        empty.getChildren().addAll(icon, text, sub);
        messagesBox.getChildren().add(empty);
    }

    private void refreshConvList() {
        List<Conversation> all = convRepo.findAll();
        Conversation.Mode filter = modeCombo != null ? modeCombo.getValue() : null;
        if (filter != null) {
            all.removeIf(c -> c.getMode() != filter);
        }
        convList.getItems().setAll(all);
    }
}
