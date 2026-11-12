package com.chatbox.controller;

import com.chatbox.model.*;
import com.chatbox.service.WellnessService;
import com.chatbox.util.UIUtil;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;

/**
 * Mental Wellness controller: mood check-in, journal, mood history,
 * guided breathing, and grounding exercises.
 */
public class WellnessController {

    private final WellnessService service = new WellnessService();

    public Node build() {
        VBox root = new VBox();
        root.getStyleClass().add("content-area");

        VBox header = new VBox(4);
        header.getStyleClass().add("page-header");
        Label title = new Label("Mental Wellness");
        title.getStyleClass().add("page-title");
        Label sub = new Label("A private space for self-reflection. These tools are not medical advice.");
        sub.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(title, sub);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        tabs.getTabs().addAll(
            buildMoodTab(),
            buildJournalTab(),
            buildHistoryTab(),
            buildBreathingTab(),
            buildGroundingTab()
        );

        root.getChildren().addAll(header, tabs);
        return root;
    }

    // ─── Mood Check-In ────────────────────────────────────────────────────

    private Tab buildMoodTab() {
        Tab tab = new Tab("Mood Check-In");

        VBox content = new VBox(20);
        content.getStyleClass().add("page-body");
        content.setAlignment(Pos.TOP_CENTER);
        content.setMaxWidth(600);

        Label intro = new Label("How are you feeling right now?");
        intro.setStyle("-fx-font-size: 16px; -fx-text-fill: #e8eaf0;");

        // Slider
        Slider slider = new Slider(1, 10, 5);
        slider.getStyleClass().add("mood-slider");
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setMaxWidth(500);

        Label moodDisplay = new Label("5 — Neutral");
        moodDisplay.setStyle("-fx-font-size: 18px; -fx-text-fill: #7c6ff7; -fx-font-weight: bold;");

        slider.valueProperty().addListener((obs, old, val) -> {
            int score = val.intValue();
            moodDisplay.setText(score + " — " + MoodEntry.labelForScore(score));
        });

        // Note
        Label noteLabel = new Label("Optional note (private, stored locally):");
        noteLabel.getStyleClass().add("meta-text");
        TextArea noteArea = new TextArea();
        noteArea.setPromptText("What's on your mind? Any context for this mood?");
        noteArea.setPrefHeight(80);
        noteArea.setWrapText(true);
        noteArea.setMaxWidth(500);

        Button saveBtn = new Button("Save Mood");
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        Label savedLbl = new Label("");
        savedLbl.getStyleClass().add("meta-text");

        saveBtn.setOnAction(e -> {
            int score = (int) slider.getValue();
            service.logMood(score, noteArea.getText().trim());
            noteArea.clear();
            savedLbl.setText("✓ Mood saved — " + MoodEntry.labelForScore(score));
        });

        HBox btnRow = new HBox(10, saveBtn, savedLbl);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(intro, slider, moodDisplay, noteLabel, noteArea, btnRow);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        tab.setContent(sp);
        return tab;
    }

    // ─── Journal ──────────────────────────────────────────────────────────

    private Tab buildJournalTab() {
        Tab tab = new Tab("Journal");

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.35);

        // Journal list
        VBox listPane = new VBox();
        listPane.setStyle("-fx-background-color: #13161d;");

        HBox listHeader = new HBox(8);
        listHeader.setPadding(new Insets(12));
        listHeader.setAlignment(Pos.CENTER_LEFT);
        Label listTitle = new Label("Entries");
        listTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        HBox.setHgrow(listTitle, Priority.ALWAYS);
        listHeader.getChildren().add(listTitle);

        ListView<JournalEntry> journalList = new ListView<>();
        journalList.getStyleClass().add("list-view");
        VBox.setVgrow(journalList, Priority.ALWAYS);

        journalList.setCellFactory(lv -> new ListCell<>() {
            protected void updateItem(JournalEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                VBox cell = new VBox(2);
                String t = item.getTitle() != null && !item.getTitle().isBlank() ? item.getTitle() : "Untitled";
                Label tl = new Label(UIUtil.truncate(t, 26));
                tl.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
                Label dl = new Label(UIUtil.formatDate(item.getCreatedAt()));
                dl.setStyle("-fx-font-size: 10px; -fx-text-fill: #4d5468;");
                cell.getChildren().addAll(tl, dl);
                setGraphic(cell);
                setText(null);
            }
        });

        listPane.getChildren().addAll(listHeader, journalList);

        // Journal editor
        VBox editorPane = new VBox(12);
        editorPane.setPadding(new Insets(16));
        editorPane.setStyle("-fx-background-color: #0d0f14;");

        TextField titleField = new TextField();
        titleField.setPromptText("Entry title (optional)");

        TextArea bodyField = new TextArea();
        bodyField.setPromptText("Write freely. This is your private space…");
        bodyField.setWrapText(true);
        VBox.setVgrow(bodyField, Priority.ALWAYS);

        HBox toolbar = new HBox(8);
        Button btnNew = new Button("New Entry");
        btnNew.getStyleClass().addAll("btn", "btn-primary");
        Button btnSave = new Button("Save");
        btnSave.getStyleClass().addAll("btn");
        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().addAll("btn", "btn-danger");
        Label saveStatus = new Label("");
        saveStatus.getStyleClass().add("meta-text");

        toolbar.getChildren().addAll(btnNew, btnSave, btnDelete, saveStatus);

        editorPane.getChildren().addAll(toolbar, titleField, bodyField);

        // State
        final JournalEntry[] current = {null};

        Runnable refreshList = () -> {
            journalList.getItems().setAll(service.getAllJournals());
        };

        journalList.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                current[0] = entry;
                titleField.setText(entry.getTitle() != null ? entry.getTitle() : "");
                bodyField.setText(entry.getContent() != null ? entry.getContent() : "");
            }
        });

        btnNew.setOnAction(e -> {
            current[0] = null;
            titleField.clear();
            bodyField.clear();
            journalList.getSelectionModel().clearSelection();
            saveStatus.setText("");
            titleField.requestFocus();
        });

        btnSave.setOnAction(e -> {
            String body = bodyField.getText().trim();
            if (body.isEmpty()) { saveStatus.setText("Nothing to save."); return; }
            if (current[0] == null) {
                JournalEntry entry = service.saveJournal(titleField.getText().trim(), body, null, null);
                current[0] = entry;
            } else {
                current[0].setTitle(titleField.getText().trim());
                current[0].setContent(body);
                service.updateJournal(current[0]);
            }
            refreshList.run();
            saveStatus.setText("✓ Saved");
        });

        btnDelete.setOnAction(e -> {
            if (current[0] == null) return;
            if (!UIUtil.showConfirm("Delete Entry", "Delete this journal entry?")) return;
            service.deleteJournal(current[0].getId());
            current[0] = null;
            titleField.clear();
            bodyField.clear();
            refreshList.run();
            saveStatus.setText("");
        });

        refreshList.run();
        split.getItems().addAll(listPane, editorPane);
        tab.setContent(split);
        return tab;
    }

    // ─── Mood History ──────────────────────────────────────────────────────

    private Tab buildHistoryTab() {
        Tab tab = new Tab("Mood History");

        VBox content = new VBox(16);
        content.getStyleClass().add("page-body");

        Label heading = new Label("Recent Mood Entries");
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        TableView<MoodEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<MoodEntry, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            UIUtil.formatDateTime(c.getValue().getCreatedAt())));

        TableColumn<MoodEntry, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(c.getValue().getMoodScore())));
        scoreCol.setMaxWidth(60);

        TableColumn<MoodEntry, String> labelCol = new TableColumn<>("Mood");
        labelCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            c.getValue().getMoodLabel()));
        labelCol.setMaxWidth(100);

        TableColumn<MoodEntry, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            UIUtil.truncate(c.getValue().getNote(), 60)));

        table.getColumns().addAll(dateCol, scoreCol, labelCol, noteCol);
        table.getItems().setAll(service.getAllMoods());

        // Mood summary card
        HBox summaryRow = buildMoodSummaryRow();

        // Simple bar chart
        VBox chart = buildMoodChart();

        content.getChildren().addAll(heading, summaryRow, chart, table);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        tab.setContent(sp);
        return tab;
    }

    private HBox buildMoodSummaryRow() {
        WellnessService.MoodSummary summary = service.calculateMoodSummary();
        HBox row = new HBox(12);

        row.getChildren().addAll(
            summaryBox(String.valueOf(summary.totalCheckIns()), "Total Check-Ins"),
            summaryBox(summary.totalCheckIns() > 0 ? String.valueOf(summary.averageScore()) : "—", "Average Score"),
            summaryBox(summary.dominantMood(), "Dominant Mood"),
            summaryBox(summary.latestEntry() != null ? summary.latestEntry().getMoodLabel() : "—", "Latest State")
        );
        return row;
    }

    private VBox summaryBox(String val, String lbl) {
        VBox box = new VBox(2);
        box.setStyle("-fx-background-color: #161922; -fx-padding: 10 14; -fx-background-radius: 6; -fx-border-color: #1e2334; -fx-border-radius: 6;");
        HBox.setHgrow(box, Priority.ALWAYS);

        Label v = new Label(val);
        v.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #c4beff;");
        Label l = new Label(lbl);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #8891a8;");

        box.getChildren().addAll(v, l);
        return box;
    }

    private VBox buildMoodChart() {
        List<MoodEntry> recent = service.getRecentMoods(14);
        VBox chart = new VBox(6);
        chart.setPadding(new Insets(12));
        chart.setStyle("-fx-background-color: #13161d; -fx-border-color: #1e2334; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label lbl = new Label("Last 14 Check-ins");
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");

        HBox bars = new HBox(4);
        bars.setAlignment(Pos.BOTTOM_LEFT);

        if (recent.isEmpty()) {
            Label empty = new Label("No mood data yet. Do your first check-in.");
            empty.setStyle("-fx-text-fill: #4d5468; -fx-font-size: 12px;");
            chart.getChildren().addAll(lbl, empty);
        } else {
            for (int i = recent.size() - 1; i >= 0; i--) {
                MoodEntry m = recent.get(i);
                double h = m.getMoodScore() * 12;
                VBox bar = new VBox();
                bar.setPrefWidth(18);
                bar.setPrefHeight(h);
                bar.setMaxHeight(h);
                bar.setMinHeight(h);
                String color = m.getMoodScore() <= 3 ? "#e05555" :
                               m.getMoodScore() <= 5 ? "#f0a23c" :
                               m.getMoodScore() <= 7 ? "#a8c93a" : "#3ecf5e";
                bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");
                Tooltip tip = new Tooltip(m.getMoodLabel() + " (" + m.getMoodScore() + ") — " +
                    UIUtil.formatDate(m.getCreatedAt()));
                Tooltip.install(bar, tip);
                bars.getChildren().add(bar);
            }
            chart.getChildren().addAll(lbl, bars);
        }
        return chart;
    }

    // ─── Breathing ────────────────────────────────────────────────────────

    private Tab buildBreathingTab() {
        Tab tab = new Tab("Breathing");

        VBox content = new VBox(24);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(40));

        Label title = new Label("4-7-8 Breathing");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        Label desc = new Label("Inhale for 4, hold for 7, exhale for 8. Helps activate the parasympathetic nervous system.");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #8891a8;");
        desc.setWrapText(true);
        desc.setMaxWidth(480);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Circle
        Circle circle = new Circle(80);
        circle.setFill(Color.web("#3a3566"));
        circle.setStroke(Color.web("#7c6ff7"));
        circle.setStrokeWidth(2);

        Label phaseLabel = new Label("Ready");
        phaseLabel.getStyleClass().add("breathing-label");

        Label countLabel = new Label("");
        countLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #c4beff;");

        StackPane circleStack = new StackPane(circle, phaseLabel);

        // Scale animation
        ScaleTransition expand = new ScaleTransition(Duration.seconds(4), circle);
        expand.setFromX(1.0);
        expand.setFromY(1.0);
        expand.setToX(1.6);
        expand.setToY(1.6);

        ScaleTransition hold = new ScaleTransition(Duration.seconds(7), circle);
        hold.setFromX(1.6);
        hold.setFromY(1.6);
        hold.setToX(1.6);
        hold.setToY(1.6);

        ScaleTransition shrink = new ScaleTransition(Duration.seconds(8), circle);
        shrink.setFromX(1.6);
        shrink.setFromY(1.6);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        SequentialTransition sequence = new SequentialTransition(expand, hold, shrink);
        sequence.setCycleCount(Animation.INDEFINITE);

        final Timeline[] countTimer = {null};
        final int[] remaining = {0};
        final boolean[] running = {false};
        final boolean[] paused = {false};
        final int[][] phases = {{4, 7, 8}};
        final String[] names = {"Inhale", "Hold", "Exhale"};
        final int[] phaseIdx = {0};

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);

        Button startBtn = new Button("Start");
        startBtn.getStyleClass().addAll("btn", "btn-primary");
        startBtn.setPrefWidth(100);

        Button pauseBtn = new Button("Pause");
        pauseBtn.getStyleClass().add("btn");
        pauseBtn.setPrefWidth(90);
        pauseBtn.setDisable(true);

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("btn");
        resetBtn.setPrefWidth(90);
        resetBtn.setDisable(true);

        startBtn.setOnAction(e -> {
            if (!running[0]) {
                // Fresh start
                running[0] = true;
                paused[0] = false;
                startBtn.setDisable(true);
                pauseBtn.setDisable(false);
                resetBtn.setDisable(false);

                phaseIdx[0] = 0;
                remaining[0] = phases[0][0];
                phaseLabel.setText(names[0]);
                countLabel.setText(String.valueOf(remaining[0]));

                sequence.playFromStart();

                countTimer[0] = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                    remaining[0]--;
                    if (remaining[0] <= 0) {
                        phaseIdx[0] = (phaseIdx[0] + 1) % 3;
                        remaining[0] = phases[0][phaseIdx[0]];
                        phaseLabel.setText(names[phaseIdx[0]]);
                    }
                    countLabel.setText(String.valueOf(remaining[0]));
                }));
                countTimer[0].setCycleCount(Animation.INDEFINITE);
                countTimer[0].play();
            } else if (paused[0]) {
                // Resume from pause
                paused[0] = false;
                startBtn.setDisable(true);
                pauseBtn.setDisable(false);
                sequence.play();
                if (countTimer[0] != null) countTimer[0].play();
            }
        });

        pauseBtn.setOnAction(e -> {
            if (running[0] && !paused[0]) {
                paused[0] = true;
                sequence.pause();
                if (countTimer[0] != null) countTimer[0].pause();
                startBtn.setText("Resume");
                startBtn.setDisable(false);
                pauseBtn.setDisable(true);
            }
        });

        resetBtn.setOnAction(e -> {
            running[0] = false;
            paused[0] = false;
            sequence.stop();
            if (countTimer[0] != null) countTimer[0].stop();

            startBtn.setText("Start");
            startBtn.setDisable(false);
            pauseBtn.setDisable(true);
            resetBtn.setDisable(true);

            phaseLabel.setText("Ready");
            countLabel.setText("");
            circle.setScaleX(1.0);
            circle.setScaleY(1.0);
        });

        controls.getChildren().addAll(startBtn, pauseBtn, resetBtn);

        Label note = new Label("Notice: This exercise activates the parasympathetic calming response. Stop immediately if you experience dizziness.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: #4d5468;");

        content.getChildren().addAll(title, desc, circleStack, countLabel, controls, note);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        tab.setContent(sp);
        return tab;
    }

    // ─── Grounding ────────────────────────────────────────────────────────

    private Tab buildGroundingTab() {
        Tab tab = new Tab("Grounding");

        VBox content = new VBox(20);
        content.getStyleClass().add("page-body");
        content.setMaxWidth(680);

        Label title = new Label("5-4-3-2-1 Grounding Technique");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        Label desc = new Label("This technique helps bring you back to the present moment. " +
            "Take your time with each sense. There are no right or wrong answers.");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #8891a8;");
        desc.setWrapText(true);

        String[][] prompts = {
            {"5", "Things you can SEE", "Look around slowly. Name 5 things you can see right now."},
            {"4", "Things you can TOUCH", "Feel the textures around you. Name 4 things you can touch."},
            {"3", "Things you can HEAR", "Listen quietly. Name 3 sounds you can hear."},
            {"2", "Things you can SMELL", "Notice the air. Name 2 things you can smell."},
            {"1", "Thing you can TASTE", "Notice your mouth. Name 1 thing you can taste."}
        };

        for (String[] p : prompts) {
            VBox item = new VBox(6);
            item.getStyleClass().add("grounding-item");

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label(p[0]);
            num.getStyleClass().add("grounding-num");
            VBox labels = new VBox(2);
            Label senseLabel = new Label(p[1]);
            senseLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
            Label senseDesc = new Label(p[2]);
            senseDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
            senseDesc.setWrapText(true);
            labels.getChildren().addAll(senseLabel, senseDesc);
            row.getChildren().addAll(num, labels);

            TextArea field = new TextArea();
            field.setPromptText("Write your observations here…");
            field.setPrefHeight(60);
            field.setWrapText(true);

            item.getChildren().addAll(row, field);
            content.getChildren().add(item);
        }

        Label footer = new Label("Take a deep breath when you're done. You're grounded in this moment.");
        footer.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c6ff7; -fx-font-style: italic;");
        footer.setWrapText(true);
        content.getChildren().addAll(title, desc);
        // Re-add because we added title/desc at end above - fix:
        content.getChildren().clear();
        content.getChildren().add(title);
        content.getChildren().add(desc);
        for (String[] p : prompts) {
            VBox item = new VBox(6);
            item.getStyleClass().add("grounding-item");
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label(p[0]);
            num.getStyleClass().add("grounding-num");
            VBox labels = new VBox(2);
            Label senseLabel = new Label(p[1]);
            senseLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
            Label senseDesc = new Label(p[2]);
            senseDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
            senseDesc.setWrapText(true);
            labels.getChildren().addAll(senseLabel, senseDesc);
            row.getChildren().addAll(num, labels);
            TextArea field = new TextArea();
            field.setPromptText("Write your observations here…");
            field.setPrefHeight(60);
            field.setWrapText(true);
            item.getChildren().addAll(row, field);
            content.getChildren().add(item);
        }
        content.getChildren().add(footer);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        tab.setContent(sp);
        return tab;
    }
}
