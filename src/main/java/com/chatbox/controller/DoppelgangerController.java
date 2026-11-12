package com.chatbox.controller;

import com.chatbox.model.UserProfile;
import com.chatbox.service.ProfileService;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Digital Doppelgänger controller:
 * - Manages persona preferences, interests, communication patterns, and goals.
 * - Injects user identity directly into AI system instructions.
 * - Supports explicit Save and Cancel/Reset controls.
 */
public class DoppelgangerController {

    private final ProfileService service = new ProfileService();
    private final Runnable onProfileSaved;
    private UserProfile profile;

    // Form fields for easy reset
    private TextField tfName;
    private TextField tfPreferred;
    private TextField tfAge;
    private TextField tfOccupation;
    private TextArea taBio;
    private TextArea taInterests;
    private TextArea taPersonality;
    private TextArea taStyle;
    private TextArea taGoals;
    private Label saveStatus;

    public DoppelgangerController() {
        this(null);
    }

    public DoppelgangerController(Runnable onProfileSaved) {
        this.onProfileSaved = onProfileSaved;
        this.profile = service.getProfile();
    }

    public UserProfile getProfile() {
        return profile;
    }

    public Node build() {
        this.profile = service.getProfile(); // Fresh load from database

        VBox root = new VBox();
        root.getStyleClass().add("content-area");

        // Header
        VBox header = new VBox(4);
        header.getStyleClass().add("page-header");

        Label title = new Label("Digital Doppelgänger");
        title.getStyleClass().add("page-title");

        Label sub = new Label("Declare your identity, communication style, and worldview. Your companion models its responses after this blueprint.");
        sub.getStyleClass().add("page-subtitle");
        sub.setWrapText(true);

        header.getChildren().addAll(title, sub);

        // Body inside ScrollPane
        ScrollPane scroll = new ScrollPane(buildForm());
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll);
        return root;
    }

    private VBox buildForm() {
        VBox form = new VBox(20);
        form.getStyleClass().add("page-body");
        form.setMaxWidth(960);

        // Personalization Explanatory Banner
        VBox explanationBox = new VBox(6);
        explanationBox.getStyleClass().add("card");
        explanationBox.setStyle("-fx-background-color: #161924; -fx-border-color: #272d42; -fx-border-radius: 6; -fx-padding: 14;");

        Label expTitle = new Label("HOW PROFILE INFORMATION IS USED FOR PERSONALIZATION");
        expTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c4beff;");

        Label expDesc = new Label("When interacting in Digital Doppelgänger mode, the AI synthesizes your declared interests, tone, and goals as grounded system guidance. It mirrors your intellectual curiosity and communication preferences while ethically identifying itself as a simulation. All data remains stored exclusively on your local machine.");
        expDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #8891a8; -fx-line-spacing: 2px;");
        expDesc.setWrapText(true);

        explanationBox.getChildren().addAll(expTitle, expDesc);
        form.getChildren().add(explanationBox);

        // 1. Identity & Core Demographics
        form.getChildren().add(section("1. Core Identity"));

        GridPane idGrid = new GridPane();
        idGrid.setHgap(16);
        idGrid.setVgap(12);
        idGrid.getColumnConstraints().addAll(colConstraint(50), colConstraint(50));

        tfName = field("e.g. Ayush Kashyap", profile.getName());
        tfPreferred = field("e.g. Ayush", profile.getPreferredName());
        tfAge = field("e.g. 21", profile.getAge() > 0 ? String.valueOf(profile.getAge()) : "");
        tfOccupation = field("e.g. CS Undergraduate & Software Engineer", profile.getOccupation());

        idGrid.add(labeled("Full Legal Name", tfName), 0, 0);
        idGrid.add(labeled("Preferred Name / Nickname", tfPreferred), 1, 0);
        idGrid.add(labeled("Age", tfAge), 0, 1);
        idGrid.add(labeled("Occupation or Academic Role", tfOccupation), 1, 1);
        form.getChildren().add(idGrid);

        // 2. Personal Narrative & Bio
        form.getChildren().add(section("2. Narrative & Background"));
        taBio = textArea("A concise summary of where you are in life, your values, and overarching focus…", profile.getBio(), 70);
        form.getChildren().add(labeled("Personal Bio & Academic Journey", taBio));

        // 3. Interests & Intellectual Curiosities
        form.getChildren().add(section("3. Interests & Technical Pursuits"));
        taInterests = textArea("e.g. Artificial Intelligence, Distributed Computing, System Architecture, Philosophy, Cognitive Science…", profile.getInterests(), 70);
        form.getChildren().add(labeled("Declared Interests & Specializations", taInterests));

        // 4. Communication Style & Tone
        form.getChildren().add(section("4. Communication Style & Persona"));
        GridPane styleGrid = new GridPane();
        styleGrid.setHgap(16);
        styleGrid.setVgap(12);
        styleGrid.getColumnConstraints().addAll(colConstraint(50), colConstraint(50));

        taStyle = textArea("e.g. Structured, concise, analytical, courteous, and detail-oriented…", profile.getCommunicationStyle(), 70);
        taPersonality = textArea("e.g. Inquisitive, introspective, systematic, calm, driven by excellence…", profile.getPersonality(), 70);

        styleGrid.add(labeled("Preferred Communication Style", taStyle), 0, 0);
        styleGrid.add(labeled("Personality Characteristics", taPersonality), 1, 0);
        form.getChildren().add(styleGrid);

        // 5. Personal Goals & Long-Term Vision
        form.getChildren().add(section("5. Personal Goals & Core Aspirations"));
        taGoals = textArea("e.g. Graduate with distinction, build high-reliability native systems, foster personal wellness habits…", profile.getGoals(), 70);
        form.getChildren().add(labeled("Core Life Aspirations", taGoals));

        // Action Toolbar (Save and Cancel/Reset)
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(12, 0, 16, 0));

        Button btnSave = new Button("Save Profile Changes");
        btnSave.getStyleClass().addAll("btn", "btn-primary");

        Button btnReset = new Button("Cancel / Revert Changes");
        btnReset.getStyleClass().add("btn");

        saveStatus = new Label("");
        saveStatus.setStyle("-fx-font-size: 12px;");

        btnSave.setOnAction(e -> saveProfile());
        btnReset.setOnAction(e -> resetForm());

        actions.getChildren().addAll(btnSave, btnReset, saveStatus);
        form.getChildren().add(actions);

        return form;
    }

    private void saveProfile() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            saveStatus.setText("⚠ Full Legal Name cannot be empty.");
            saveStatus.setStyle("-fx-text-fill: #e05555;");
            return;
        }
        profile.setName(name);
        profile.setPreferredName(tfPreferred.getText().trim());
        int ageVal = 0;
        try {
            if (!tfAge.getText().trim().isEmpty()) {
                ageVal = Integer.parseInt(tfAge.getText().trim());
                if (ageVal < 0 || ageVal > 130) {
                    saveStatus.setText("⚠ Please enter a valid age between 1 and 120.");
                    saveStatus.setStyle("-fx-text-fill: #e05555;");
                    return;
                }
            }
        } catch (NumberFormatException ex) {
            saveStatus.setText("⚠ Age must be a valid integer number.");
            saveStatus.setStyle("-fx-text-fill: #e05555;");
            return;
        }
        profile.setAge(ageVal);
        profile.setOccupation(tfOccupation.getText().trim());
        profile.setBio(taBio.getText().trim());
        profile.setInterests(taInterests.getText().trim());
        profile.setPersonality(taPersonality.getText().trim());
        profile.setCommunicationStyle(taStyle.getText().trim());
        profile.setGoals(taGoals.getText().trim());

        service.saveProfile(profile);

        saveStatus.setText("✓ Profile saved successfully.");
        saveStatus.setStyle("-fx-text-fill: #3ecf5e;");

        if (onProfileSaved != null) {
            onProfileSaved.run();
        }
    }

    private void resetForm() {
        this.profile = service.getProfile(); // Reload saved
        tfName.setText(profile.getName());
        tfPreferred.setText(profile.getPreferredName());
        tfAge.setText(profile.getAge() > 0 ? String.valueOf(profile.getAge()) : "");
        tfOccupation.setText(profile.getOccupation());
        taBio.setText(profile.getBio());
        taInterests.setText(profile.getInterests());
        taPersonality.setText(profile.getPersonality());
        taStyle.setText(profile.getCommunicationStyle());
        taGoals.setText(profile.getGoals());

        saveStatus.setText("Reverted unsaved edits.");
        saveStatus.setStyle("-fx-text-fill: #8891a8;");
    }

    private Label section(String text) {
        Label lbl = new Label(text.toUpperCase());
        lbl.getStyleClass().add("section-title");
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c4beff; -fx-padding: 8 0 2 0;");
        return lbl;
    }

    private TextField field(String prompt, String value) {
        TextField tf = new TextField(value != null ? value : "");
        tf.setPromptText(prompt);
        return tf;
    }

    private TextArea textArea(String prompt, String value, int prefHeight) {
        TextArea ta = new TextArea(value != null ? value : "");
        ta.setPromptText(prompt);
        ta.setPrefHeight(prefHeight);
        ta.setWrapText(true);
        return ta;
    }

    private VBox labeled(String label, Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        box.getChildren().addAll(lbl, control);
        VBox.setVgrow(control, Priority.ALWAYS);
        HBox.setHgrow(box, Priority.ALWAYS);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private ColumnConstraints colConstraint(double percent) {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(percent);
        cc.setHgrow(Priority.ALWAYS);
        return cc;
    }
}
