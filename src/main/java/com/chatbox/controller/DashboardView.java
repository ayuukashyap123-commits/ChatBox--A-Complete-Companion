package com.chatbox.controller;

import com.chatbox.model.*;
import com.chatbox.repository.*;
import com.chatbox.service.AiService;
import com.chatbox.service.CoachService;
import com.chatbox.service.WellnessService;
import com.chatbox.util.UIUtil;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Overview dashboard providing:
 * - Dynamic greeting and date header.
 * - Today's mood and real longitudinal trend visualization.
 * - Active goals and task list with completion toggles.
 * - Recent conversations and imported Second Brain documents.
 * - Quick action launchpads and intentional empty states.
 */
public class DashboardView {

    private final UserProfile profile;
    private final AiService aiService;
    private final Consumer<String> navigate;

    private final WellnessService wellnessService = new WellnessService();
    private final CoachService coachService = new CoachService();
    private final KnowledgeRepository knowledgeRepo = new KnowledgeRepository();
    private final MemoryRepository memoryRepo = new MemoryRepository();
    private final ConversationRepository convRepo = new ConversationRepository();

    public DashboardView(UserProfile profile, AiService aiService, Consumer<String> navigate) {
        this.profile = profile;
        this.aiService = aiService;
        this.navigate = navigate;
    }

    public Node build() {
        VBox root = new VBox();
        root.getStyleClass().add("content-area");

        // 1. Header
        VBox header = new VBox(4);
        header.getStyleClass().add("page-header");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String greeting = getGreeting();
        Label greetLabel = new Label(greeting + ", " + profile.getDisplayName() + ".");
        greetLabel.getStyleClass().add("page-title");
        HBox.setHgrow(greetLabel, Priority.ALWAYS);

        boolean isLive = aiService.isLive();
        Label statusPill = new Label(aiService.getStatusLabel());
        statusPill.setStyle("-fx-padding: 4 10; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-background-color: " + (isLive ? "#1a3322" : "#33261a") + "; " +
            "-fx-text-fill: " + (isLive ? "#3ecf5e" : "#f0a23c") + ";");

        topRow.getChildren().addAll(greetLabel, statusPill);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("page-subtitle");

        header.getChildren().addAll(topRow, dateLabel);

        // 2. Body inside ScrollPane for adaptable layout
        VBox body = new VBox(20);
        body.getStyleClass().add("page-body");

        // Top Metric Counters
        body.getChildren().add(buildMetricRow());

        // Two-Column Grid
        GridPane mainGrid = new GridPane();
        mainGrid.setHgap(20);
        mainGrid.setVgap(20);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(58);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(42);
        col2.setHgrow(Priority.ALWAYS);

        mainGrid.getColumnConstraints().addAll(col1, col2);

        // Left Column (Wellness & Goals)
        VBox leftCol = new VBox(20);
        leftCol.getChildren().addAll(
            buildTodayMoodCard(),
            buildActiveGoalsCard()
        );

        // Right Column (Recent Conversations, Documents, Quick Nav)
        VBox rightCol = new VBox(20);
        rightCol.getChildren().addAll(
            buildRecentChatsCard(),
            buildRecentDocsCard(),
            buildQuickActionsCard()
        );

        mainGrid.add(leftCol, 0, 0);
        mainGrid.add(rightCol, 1, 0);

        body.getChildren().add(mainGrid);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll);
        return root;
    }

    // ─── Metric Row ──────────────────────────────────────────────────────────

    private HBox buildMetricRow() {
        HBox row = new HBox(12);

        List<Goal> goals = coachService.getAllGoals();
        long activeGoals = goals.stream().filter(g -> g.getStatus() == Goal.Status.ACTIVE).count();

        List<com.chatbox.model.Task> tasks = coachService.getAllTasks();
        long doneTasks = tasks.stream().filter(t -> t.getStatus() == com.chatbox.model.Task.Status.DONE).count();

        List<KnowledgeDocument> docs = knowledgeRepo.findAll();
        List<Memory> memories = memoryRepo.findAll();
        List<Conversation> convs = convRepo.findAll();

        row.getChildren().addAll(
            statBox(String.valueOf(activeGoals), "Active Goals", "coach"),
            statBox(doneTasks + "/" + tasks.size(), "Tasks Completed", "coach"),
            statBox(String.valueOf(docs.size()), "Brain Docs", "knowledge"),
            statBox(String.valueOf(memories.size()), "Memories", "memory"),
            statBox(String.valueOf(convs.size()), "Conversations", "chat")
        );

        return row;
    }

    private VBox statBox(String number, String label, String section) {
        VBox box = new VBox(3);
        box.getStyleClass().add("dashboard-stat-box");
        box.setCursor(javafx.scene.Cursor.HAND);
        box.setOnMouseClicked(e -> navigate.accept(section));
        HBox.setHgrow(box, Priority.ALWAYS);

        Label num = new Label(number);
        num.getStyleClass().add("stat-number");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");

        box.getChildren().addAll(num, lbl);
        return box;
    }

    // ─── Left Column: Mood & Wellness ────────────────────────────────────────

    private VBox buildTodayMoodCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("TODAY'S EMOTIONAL STATE");
        title.getStyleClass().add("section-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnWellness = new Button("Open Wellness");
        btnWellness.getStyleClass().add("btn");
        btnWellness.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        btnWellness.setOnAction(e -> navigate.accept("wellness"));
        cardHeader.getChildren().addAll(title, btnWellness);

        card.getChildren().add(cardHeader);

        List<MoodEntry> moods = wellnessService.getRecentMoods(7);
        String todayStr = LocalDate.now().toString();

        MoodEntry todayMood = null;
        for (MoodEntry m : moods) {
            if (m.getCreatedAt() != null && m.getCreatedAt().toLocalDate().toString().equals(todayStr)) {
                todayMood = m;
                break;
            }
        }

        if (todayMood != null) {
            HBox stateRow = new HBox(12);
            stateRow.setAlignment(Pos.CENTER_LEFT);
            stateRow.setStyle("-fx-background-color: #1a1e28; -fx-padding: 10 14; -fx-background-radius: 6;");

            Label scoreBadge = new Label(todayMood.getMoodScore() + " / 5");
            scoreBadge.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3ecf5e; " +
                "-fx-background-color: #1a3322; -fx-padding: 4 8; -fx-background-radius: 4;");

            VBox moodDetails = new VBox(2);
            Label labelText = new Label(todayMood.getMoodLabel() != null ? todayMood.getMoodLabel() : "Recorded Check-In");
            labelText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

            String note = todayMood.getNote() != null && !todayMood.getNote().isBlank() ?
                "\"" + todayMood.getNote() + "\"" : "No additional reflection note added.";
            Label noteText = new Label(note);
            noteText.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
            noteText.setWrapText(true);

            moodDetails.getChildren().addAll(labelText, noteText);
            stateRow.getChildren().addAll(scoreBadge, moodDetails);
            card.getChildren().add(stateRow);
        } else {
            HBox promptRow = new HBox(12);
            promptRow.setAlignment(Pos.CENTER_LEFT);
            promptRow.setStyle("-fx-background-color: #1a1e28; -fx-padding: 10 14; -fx-background-radius: 6;");

            VBox promptText = new VBox(2);
            Label pTitle = new Label("No check-in logged yet today");
            pTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
            Label pSub = new Label("Record your emotional state to track longitudinal patterns.");
            pSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
            promptText.getChildren().addAll(pTitle, pSub);
            HBox.setHgrow(promptText, Priority.ALWAYS);

            Button btnCheckIn = new Button("+ Log Mood");
            btnCheckIn.getStyleClass().addAll("btn", "btn-primary");
            btnCheckIn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
            btnCheckIn.setOnAction(e -> navigate.accept("wellness"));

            promptRow.getChildren().addAll(promptText, btnCheckIn);
            card.getChildren().add(promptRow);
        }

        // Compact real mood visualization
        if (!moods.isEmpty()) {
            VBox trendBox = new VBox(6);
            trendBox.setPadding(new Insets(4, 0, 0, 0));

            Label trendLabel = new Label("Recent Mood Trajectory (Last " + Math.min(moods.size(), 7) + " Logs):");
            trendLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8; -fx-font-weight: bold;");

            HBox barsRow = new HBox(8);
            barsRow.setAlignment(Pos.BOTTOM_LEFT);
            barsRow.setPrefHeight(60);

            for (int i = Math.min(moods.size(), 7) - 1; i >= 0; i--) {
                MoodEntry m = moods.get(i);
                VBox barCol = new VBox(3);
                barCol.setAlignment(Pos.BOTTOM_CENTER);

                double h = Math.max(12, m.getMoodScore() * 11);
                Region bar = new Region();
                bar.setPrefWidth(22);
                bar.setPrefHeight(h);
                bar.setMinHeight(h);
                bar.setMaxHeight(h);

                String color = m.getMoodScore() <= 2 ? "#e05555" : m.getMoodScore() == 3 ? "#f0a23c" : "#3ecf5e";
                bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");

                Label scoreLbl = new Label(String.valueOf(m.getMoodScore()));
                scoreLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #8891a8;");

                Tooltip.install(bar, new Tooltip(m.getMoodLabel() + " (" + m.getMoodScore() + "/5)\n" +
                    UIUtil.formatDate(m.getCreatedAt()) + (m.getNote() != null ? "\n" + m.getNote() : "")));

                barCol.getChildren().addAll(scoreLbl, bar);
                barsRow.getChildren().add(barCol);
            }

            trendBox.getChildren().addAll(trendLabel, barsRow);
            card.getChildren().add(trendBox);
        }

        return card;
    }

    // ─── Left Column: Active Goals & Tasks ───────────────────────────────────

    private VBox buildActiveGoalsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("ACTIVE GOALS & PRIORITIES");
        title.getStyleClass().add("section-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnCoach = new Button("Open Coach");
        btnCoach.getStyleClass().add("btn");
        btnCoach.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        btnCoach.setOnAction(e -> navigate.accept("coach"));
        cardHeader.getChildren().addAll(title, btnCoach);

        card.getChildren().add(cardHeader);

        List<Goal> goals = coachService.getAllGoals();
        List<Goal> activeGoals = goals.stream().filter(g -> g.getStatus() == Goal.Status.ACTIVE).toList();

        if (activeGoals.isEmpty()) {
            VBox empty = buildEmptyState("No active goals found", "Establish your first goal to unlock structured milestone tracking.", "coach");
            card.getChildren().add(empty);
        } else {
            VBox goalsList = new VBox(10);
            for (Goal g : activeGoals.subList(0, Math.min(2, activeGoals.size()))) {
                VBox goalItem = new VBox(6);
                goalItem.setStyle("-fx-background-color: #1a1e28; -fx-padding: 10 12; -fx-background-radius: 6;");

                HBox header = new HBox(8);
                header.setAlignment(Pos.CENTER_LEFT);

                Label gTitle = new Label(g.getTitle());
                gTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
                HBox.setHgrow(gTitle, Priority.ALWAYS);

                Label catBadge = new Label(g.getCategory().name());
                catBadge.setStyle("-fx-font-size: 9px; -fx-padding: 2 6; -fx-background-color: #272d42; -fx-text-fill: #c4beff; -fx-background-radius: 3;");

                header.getChildren().addAll(gTitle, catBadge);

                // Progress Bar
                HBox progRow = new HBox(8);
                progRow.setAlignment(Pos.CENTER_LEFT);

                ProgressBar pb = new ProgressBar(g.getProgress() / 100.0);
                pb.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(pb, Priority.ALWAYS);
                pb.setStyle("-fx-accent: #7c6ff7;");

                Label pct = new Label(g.getProgress() + "%");
                pct.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");

                progRow.getChildren().addAll(pb, pct);
                goalItem.getChildren().addAll(header, progRow);
                goalsList.getChildren().add(goalItem);
            }
            card.getChildren().add(goalsList);
        }

        // Top Priority Tasks
        List<com.chatbox.model.Task> tasks = coachService.getAllTasks();
        List<com.chatbox.model.Task> todoTasks = tasks.stream()
            .filter(t -> t.getStatus() != com.chatbox.model.Task.Status.DONE)
            .toList();

        if (!todoTasks.isEmpty()) {
            VBox taskBox = new VBox(6);
            taskBox.setPadding(new Insets(4, 0, 0, 0));

            Label taskHeading = new Label("Pending Action Items (" + todoTasks.size() + "):");
            taskHeading.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8891a8;");
            taskBox.getChildren().add(taskHeading);

            for (com.chatbox.model.Task t : todoTasks.subList(0, Math.min(3, todoTasks.size()))) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 4 6; -fx-background-color: #151821; -fx-background-radius: 4;");

                CheckBox cb = new CheckBox();
                cb.setOnAction(e -> {
                    coachService.setTaskCompletion(t.getId(), cb.isSelected());
                    navigate.accept("overview"); // refresh dashboard
                });

                Label tTitle = new Label(t.getTitle());
                tTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
                HBox.setHgrow(tTitle, Priority.ALWAYS);

                Label pBadge = new Label(t.getPriority().name());
                String pColor = t.getPriority() == com.chatbox.model.Task.Priority.HIGH ? "#e05555" :
                                t.getPriority() == com.chatbox.model.Task.Priority.MEDIUM ? "#f0a23c" : "#8891a8";
                pBadge.setStyle("-fx-font-size: 9px; -fx-text-fill: " + pColor + ";");

                row.getChildren().addAll(cb, tTitle, pBadge);
                taskBox.getChildren().add(row);
            }
            card.getChildren().add(taskBox);
        }

        return card;
    }

    // ─── Right Column: Recent Conversations ──────────────────────────────────

    private VBox buildRecentChatsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("RECENT CONVERSATIONS");
        title.getStyleClass().add("section-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnNew = new Button("+ New");
        btnNew.getStyleClass().add("btn");
        btnNew.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        btnNew.setOnAction(e -> navigate.accept("chat"));
        cardHeader.getChildren().addAll(title, btnNew);

        card.getChildren().add(cardHeader);

        List<Conversation> convs = convRepo.findAll();
        if (convs.isEmpty()) {
            card.getChildren().add(buildEmptyState("No conversations yet", "Start an AI conversation in General, Doppelgänger, or Coach mode.", "chat"));
        } else {
            VBox list = new VBox(6);
            for (Conversation c : convs.subList(0, Math.min(3, convs.size()))) {
                HBox item = new HBox(8);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setStyle("-fx-padding: 8 10; -fx-background-color: #1a1e28; -fx-background-radius: 4; -fx-cursor: hand;");
                item.setOnMouseClicked(e -> navigate.accept("chat"));

                Label cTitle = new Label(UIUtil.truncate(c.getTitle(), 24));
                cTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
                HBox.setHgrow(cTitle, Priority.ALWAYS);

                Label modeBadge = new Label(c.getMode().getLabel());
                modeBadge.setStyle("-fx-font-size: 9px; -fx-padding: 2 6; -fx-background-color: #272d42; -fx-text-fill: #c4beff; -fx-background-radius: 3;");

                item.getChildren().addAll(cTitle, modeBadge);
                list.getChildren().add(item);
            }
            card.getChildren().add(list);
        }

        return card;
    }

    // ─── Right Column: Recent Imported Documents ─────────────────────────────

    private VBox buildRecentDocsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("RECENT KNOWLEDGE DOCUMENTS");
        title.getStyleClass().add("section-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnImport = new Button("Import");
        btnImport.getStyleClass().add("btn");
        btnImport.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        btnImport.setOnAction(e -> navigate.accept("knowledge"));
        cardHeader.getChildren().addAll(title, btnImport);

        card.getChildren().add(cardHeader);

        List<KnowledgeDocument> docs = knowledgeRepo.findAll();
        if (docs.isEmpty()) {
            card.getChildren().add(buildEmptyState("No documents imported", "Import PDF, TXT, or Markdown notes into your Second Brain.", "knowledge"));
        } else {
            VBox list = new VBox(6);
            for (KnowledgeDocument d : docs.subList(0, Math.min(3, docs.size()))) {
                HBox item = new HBox(8);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setStyle("-fx-padding: 8 10; -fx-background-color: #1a1e28; -fx-background-radius: 4; -fx-cursor: hand;");
                item.setOnMouseClicked(e -> navigate.accept("knowledge"));

                Label dTitle = new Label(UIUtil.truncate(d.getTitle(), 22));
                dTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
                HBox.setHgrow(dTitle, Priority.ALWAYS);

                Label dType = new Label(d.getFileType() != null ? d.getFileType().name() : "DOC");
                dType.setStyle("-fx-font-size: 9px; -fx-padding: 2 6; -fx-background-color: #202b20; -fx-text-fill: #3ecf5e; -fx-background-radius: 3;");

                Label dWords = new Label(d.getWordCount() + "w");
                dWords.setStyle("-fx-font-size: 10px; -fx-text-fill: #8891a8;");

                item.getChildren().addAll(dTitle, dType, dWords);
                list.getChildren().add(item);
            }
            card.getChildren().add(list);
        }

        return card;
    }

    // ─── Right Column: Quick Navigation Launchpad ────────────────────────────

    private VBox buildQuickActionsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label title = new Label("QUICK ACCESS");
        title.getStyleClass().add("section-title");
        card.getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        String[][] actions = {
            {"AI Chat", "Conversational assistant", "chat"},
            {"Mood Check-In", "Log emotional state", "wellness"},
            {"Life Coach", "Manage goals & habits", "coach"},
            {"Second Brain", "Search knowledge docs", "knowledge"},
            {"Memory Manager", "Audit permanent memory", "memory"},
            {"Settings", "Configure AI provider", "settings"}
        };

        for (int i = 0; i < actions.length; i++) {
            String[] act = actions[i];
            Button btn = new Button(act[0]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("btn");
            btn.setStyle("-fx-font-size: 11px; -fx-alignment: CENTER_LEFT; -fx-padding: 6 10;");
            btn.setOnAction(e -> navigate.accept(act[2]));
            Tooltip.install(btn, new Tooltip(act[1]));

            int col = i % 2;
            int row = i / 2;
            grid.add(btn, col, row);
            GridPane.setHgrow(btn, Priority.ALWAYS);
        }

        card.getChildren().add(grid);
        return card;
    }

    private VBox buildEmptyState(String title, String subtitle, String targetSection) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #1a1e28; -fx-padding: 12; -fx-background-radius: 6;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        s.setWrapText(true);

        Button btn = new Button("Open " + targetSection.substring(0, 1).toUpperCase() + targetSection.substring(1));
        btn.getStyleClass().add("btn");
        btn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
        btn.setOnAction(e -> navigate.accept(targetSection));

        box.getChildren().addAll(t, s, btn);
        return box;
    }

    private String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }
}
