package com.chatbox.controller;

import com.chatbox.model.UserProfile;
import com.chatbox.repository.UserProfileRepository;
import com.chatbox.service.AiService;
import com.chatbox.util.IconUtil;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.*;

/**
 * Root controller that coordinates the persistent dark-themed sidebar,
 * navigation dispatching, and sub-controllers for all six modules plus Settings.
 */
public class MainController {

    private final Stage stage;
    private final AiService aiService;
    private final UserProfileRepository profileRepo = new UserProfileRepository();

    // Sidebar navigation components
    private final List<Button> navButtons = new ArrayList<>();
    private final BorderPane root = new BorderPane();
    private Label aiStatusLabel;
    private Circle aiStatusDot;
    private Label profileNameLabel;
    private Label profileRoleLabel;
    private Label profileAvatarLabel;

    // Sub-controllers
    private DoppelgangerController doppelgangerCtrl;
    private ChatController chatCtrl;
    private WellnessController wellnessCtrl;
    private CoachController coachCtrl;
    private KnowledgeController knowledgeCtrl;
    private MemoryController memoryCtrl;
    private SettingsController settingsCtrl;

    // Current active destination
    private String activeSection = "overview";

    public MainController(Stage stage, AiService aiService) {
        this.stage = stage;
        this.aiService = aiService;

        // Listen for AI status updates from Settings
        this.aiService.addStatusListener(() -> Platform.runLater(this::updateStatusIndicator));
    }

    public Parent build() {
        String css = Objects.requireNonNull(
            getClass().getResource("/com/chatbox/css/styles.css")
        ).toExternalForm();
        root.getStylesheets().add(css);
        root.getStyleClass().add("content-area");

        root.setLeft(buildSidebar());
        showOverview();
        return root;
    }

    // ─── Sidebar ─────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // 1. Logo & Wordmark Area
        VBox logoArea = new VBox(2);
        logoArea.getStyleClass().add("sidebar-logo-area");

        Label appName = new Label("Chatbox");
        appName.getStyleClass().add("app-name");

        Label appSub = new Label("A COMPLETE COMPANION");
        appSub.getStyleClass().add("app-subtitle");

        logoArea.getChildren().addAll(appName, appSub);

        // 2. Navigation items
        VBox nav = new VBox(2);
        nav.setPadding(new Insets(12, 0, 12, 0));
        VBox.setVgrow(nav, Priority.ALWAYS);

        Button btnOverview = createNavButton("Overview", "overview");
        Button btnChat = createNavButton("AI Chat", "chat");
        Button btnDoppelganger = createNavButton("Digital Doppelgänger", "profile");
        Button btnWellness = createNavButton("Wellness", "wellness");
        Button btnCoach = createNavButton("Life Coach", "coach");
        Button btnKnowledge = createNavButton("Second Brain", "knowledge");
        Button btnMemory = createNavButton("Memory", "memory");
        Button btnSettings = createNavButton("Settings", "settings");

        nav.getChildren().addAll(
            btnOverview,
            btnChat,
            btnDoppelganger,
            btnWellness,
            btnCoach,
            btnKnowledge,
            btnMemory,
            btnSettings
        );

        // 3. User Profile and Truthful Connection Footer
        VBox footer = buildSidebarFooter();

        sidebar.getChildren().addAll(logoArea, nav, footer);
        return sidebar;
    }

    private Button createNavButton(String title, String section) {
        Button btn = new Button(title);
        btn.getStyleClass().add("sidebar-item");
        btn.setMaxWidth(Double.MAX_VALUE);

        // Clean vector icon from IconUtil
        Node icon = IconUtil.createIcon(section, 15, "#8891a8");
        btn.setGraphic(icon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(12);

        btn.setOnAction(e -> navigate(section));
        navButtons.add(btn);
        return btn;
    }

    private VBox buildSidebarFooter() {
        VBox footer = new VBox(10);
        footer.getStyleClass().add("sidebar-footer");

        UserProfile profile = profileRepo.getProfile();

        // Profile pill
        HBox profileCard = new HBox(10);
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setCursor(javafx.scene.Cursor.HAND);
        profileCard.setPadding(new Insets(6, 8, 6, 8));
        profileCard.setStyle("-fx-background-color: #1a1e28; -fx-background-radius: 6; -fx-border-color: #1e2334; -fx-border-radius: 6;");
        profileCard.setOnMouseClicked(e -> navigate("profile"));

        // Initials avatar
        String initials = getInitials(profile.getDisplayName());
        profileAvatarLabel = new Label(initials);
        profileAvatarLabel.setAlignment(Pos.CENTER);
        profileAvatarLabel.setPrefSize(28, 28);
        profileAvatarLabel.setMinSize(28, 28);
        profileAvatarLabel.setMaxSize(28, 28);
        profileAvatarLabel.setStyle("-fx-background-color: #3a3566; -fx-text-fill: #c4beff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 14;");

        VBox userDetails = new VBox(1);
        profileNameLabel = new Label(profile.getDisplayName());
        profileNameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        String role = profile.getOccupation() != null && !profile.getOccupation().isBlank() ?
            profile.getOccupation() : "User Profile";
        profileRoleLabel = new Label(truncate(role, 20));
        profileRoleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #8891a8;");

        userDetails.getChildren().addAll(profileNameLabel, profileRoleLabel);
        HBox.setHgrow(userDetails, Priority.ALWAYS);

        profileCard.getChildren().addAll(profileAvatarLabel, userDetails);

        // Connection Indicator
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setCursor(javafx.scene.Cursor.HAND);
        statusBox.setOnMouseClicked(e -> navigate("settings"));
        Tooltip.install(statusBox, new Tooltip("Click to configure AI providers and offline mode in Settings"));

        aiStatusDot = new Circle(4);
        aiStatusLabel = new Label(aiService.getStatusLabel());
        aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        updateStatusIndicator();

        statusBox.getChildren().addAll(aiStatusDot, aiStatusLabel);
        footer.getChildren().addAll(profileCard, statusBox);
        return footer;
    }

    private void updateStatusIndicator() {
        if (aiStatusDot != null && aiStatusLabel != null) {
            boolean isLive = aiService.isLive();
            aiStatusDot.setFill(Color.web(isLive ? "#3ecf5e" : "#f0a23c"));
            aiStatusLabel.setText(aiService.getStatusLabel());
            aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " +
                (isLive ? "#3ecf5e" : "#f0a23c") + ";");
        }
    }

    public void refreshUserProfile() {
        UserProfile p = profileRepo.getProfile();
        if (profileNameLabel != null) profileNameLabel.setText(p.getDisplayName());
        if (profileRoleLabel != null) {
            String role = p.getOccupation() != null && !p.getOccupation().isBlank() ? p.getOccupation() : "User Profile";
            profileRoleLabel.setText(truncate(role, 20));
        }
        if (profileAvatarLabel != null) profileAvatarLabel.setText(getInitials(p.getDisplayName()));
    }

    // ─── Navigation Dispatcher ────────────────────────────────────────────────

    public void navigate(String section) {
        activeSection = section;

        // Highlight active sidebar button
        for (Button b : navButtons) {
            boolean active = switch (section) {
                case "overview", "dashboard" -> b.getText().equals("Overview");
                case "chat" -> b.getText().equals("AI Chat");
                case "profile", "doppelganger" -> b.getText().equals("Digital Doppelgänger");
                case "wellness" -> b.getText().equals("Wellness");
                case "coach" -> b.getText().equals("Life Coach");
                case "knowledge", "brain" -> b.getText().equals("Second Brain");
                case "memory" -> b.getText().equals("Memory");
                case "settings" -> b.getText().equals("Settings");
                default -> false;
            };

            if (active) {
                if (!b.getStyleClass().contains("active")) b.getStyleClass().add("active");
            } else {
                b.getStyleClass().remove("active");
            }
        }

        switch (section) {
            case "overview", "dashboard" -> showOverview();
            case "chat" -> showChat();
            case "profile", "doppelganger" -> showProfile();
            case "wellness" -> showWellness();
            case "coach" -> showCoach();
            case "knowledge", "brain" -> showKnowledge();
            case "memory" -> showMemory();
            case "settings" -> showSettings();
        }
    }

    // ─── Module Presenters ───────────────────────────────────────────────────

    private void showOverview() {
        UserProfile profile = profileRepo.getProfile();
        Node view = new DashboardView(profile, aiService, this::navigate).build();
        root.setCenter(view);
    }

    private void showChat() {
        if (chatCtrl == null) chatCtrl = new ChatController(aiService);
        root.setCenter(chatCtrl.build());
    }

    private void showProfile() {
        if (doppelgangerCtrl == null) doppelgangerCtrl = new DoppelgangerController(this::refreshUserProfile);
        root.setCenter(doppelgangerCtrl.build());
    }

    private void showWellness() {
        if (wellnessCtrl == null) wellnessCtrl = new WellnessController();
        root.setCenter(wellnessCtrl.build());
    }

    private void showCoach() {
        if (coachCtrl == null) coachCtrl = new CoachController();
        root.setCenter(coachCtrl.build());
    }

    private void showKnowledge() {
        if (knowledgeCtrl == null) knowledgeCtrl = new KnowledgeController(aiService, stage);
        root.setCenter(knowledgeCtrl.build());
    }

    private void showMemory() {
        if (memoryCtrl == null) memoryCtrl = new MemoryController();
        root.setCenter(memoryCtrl.build());
    }

    private void showSettings() {
        if (settingsCtrl == null) settingsCtrl = new SettingsController(aiService, this::updateStatusIndicator);
        root.setCenter(settingsCtrl.build());
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
