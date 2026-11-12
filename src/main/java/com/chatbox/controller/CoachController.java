package com.chatbox.controller;

import com.chatbox.model.Goal;
import com.chatbox.model.Habit;
import com.chatbox.model.Task;
import com.chatbox.service.CoachService;
import com.chatbox.util.UIUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI Life Coach controller:
 * - Goals with categories, milestones, and auto-calculated progress.
 * - Tasks linked to goals with priority, status, and due dates.
 * - Daily Plan tab providing unified today's execution view.
 * - Habits tracking with daily completion check-offs.
 * - Real validation and intentional empty states.
 */
public class CoachController {

    private final CoachService service = new CoachService();

    public Node build() {
        VBox root = new VBox();
        root.getStyleClass().add("content-area");

        // Header
        VBox header = new VBox(4);
        header.getStyleClass().add("page-header");

        Label title = new Label("AI Life Coach");
        title.getStyleClass().add("page-title");

        Label sub = new Label("Structure your ambitions into actionable goals, linked tasks, and non-negotiable daily habits.");
        sub.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(title, sub);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        tabs.getTabs().addAll(
            buildDailyPlanTab(),
            buildGoalsTab(),
            buildTasksTab(),
            buildHabitsTab()
        );

        root.getChildren().addAll(header, tabs);
        return root;
    }

    // ─── 1. Daily Plan Tab ────────────────────────────────────────────────────

    private Tab buildDailyPlanTab() {
        Tab tab = new Tab("Daily Plan");
        VBox content = new VBox(20);
        content.getStyleClass().add("page-body");
        content.setMaxWidth(960);

        // Header Banner
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color: #1a1e28; -fx-padding: 14 16; -fx-background-radius: 6; -fx-border-color: #1e2334; -fx-border-radius: 6;");

        VBox bannerText = new VBox(3);
        String todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        Label bTitle = new Label("Today's Execution Blueprint — " + todayFormatted);
        bTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        Label bSub = new Label("Complete today's pending tasks and maintain habit consistency to make measurable progress toward your goals.");
        bSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        bannerText.getChildren().addAll(bTitle, bSub);
        HBox.setHgrow(bannerText, Priority.ALWAYS);
        banner.getChildren().add(bannerText);

        // Focus Tasks Section
        VBox tasksSection = new VBox(10);
        HBox tasksHead = new HBox(8);
        tasksHead.setAlignment(Pos.CENTER_LEFT);
        Label tasksTitle = new Label("PRIORITY ACTION ITEMS");
        tasksTitle.getStyleClass().add("section-title");
        HBox.setHgrow(tasksTitle, Priority.ALWAYS);

        Button btnAddTask = new Button("+ New Task");
        btnAddTask.getStyleClass().add("btn");
        btnAddTask.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        btnAddTask.setOnAction(e -> showTaskDialog(null, () -> tab.setContent(buildDailyPlanContent(tab))));
        tasksHead.getChildren().addAll(tasksTitle, btnAddTask);
        tasksSection.getChildren().add(tasksHead);

        List<Task> allTasks = service.getAllTasks();
        List<Task> todoTasks = allTasks.stream().filter(t -> t.getStatus() != Task.Status.DONE).toList();

        if (todoTasks.isEmpty()) {
            VBox empty = new VBox(6);
            empty.setStyle("-fx-background-color: #161922; -fx-padding: 14; -fx-background-radius: 6;");
            Label e1 = new Label("All tasks completed!");
            e1.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3ecf5e;");
            Label e2 = new Label("You have no pending tasks remaining. Add a new task or review your long-term goals.");
            e2.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
            empty.getChildren().addAll(e1, e2);
            tasksSection.getChildren().add(empty);
        } else {
            VBox taskList = new VBox(6);
            for (Task t : todoTasks.subList(0, Math.min(6, todoTasks.size()))) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #161922; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #1e2334; -fx-border-radius: 6;");

                CheckBox cb = new CheckBox();
                cb.setOnAction(e -> {
                    service.setTaskCompletion(t.getId(), cb.isSelected());
                });

                VBox tInfo = new VBox(2);
                Label tTitle = new Label(t.getTitle());
                tTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
                String desc = (t.getDescription() != null && !t.getDescription().isBlank()) ?
                    t.getDescription() : "Due: " + (t.getDueDate() != null ? t.getDueDate() : "Today");
                Label tSub = new Label(desc);
                tSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #8891a8;");
                tInfo.getChildren().addAll(tTitle, tSub);
                HBox.setHgrow(tInfo, Priority.ALWAYS);

                Label pBadge = new Label(t.getPriority().name());
                String pColor = t.getPriority() == Task.Priority.HIGH ? "#e05555" :
                                t.getPriority() == Task.Priority.MEDIUM ? "#f0a23c" : "#8891a8";
                pBadge.setStyle("-fx-font-size: 9px; -fx-text-fill: " + pColor + "; -fx-padding: 2 6; -fx-background-color: #1a1e28; -fx-background-radius: 3;");

                row.getChildren().addAll(cb, tInfo, pBadge);
                taskList.getChildren().add(row);
            }
            tasksSection.getChildren().add(taskList);
        }

        // Daily Habits Section
        VBox habitSection = new VBox(10);
        Label habitTitle = new Label("DAILY HABIT EXECUTION");
        habitTitle.getStyleClass().add("section-title");
        habitSection.getChildren().add(habitTitle);

        List<Habit> habits = service.getAllHabits();
        if (habits.isEmpty()) {
            VBox empty = new VBox(4);
            empty.setStyle("-fx-background-color: #161922; -fx-padding: 12; -fx-background-radius: 6;");
            Label e1 = new Label("No habits tracked yet. Visit the Habits tab to establish daily routines.");
            e1.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
            empty.getChildren().add(e1);
            habitSection.getChildren().add(empty);
        } else {
            VBox habitList = new VBox(6);
            for (Habit h : habits) {
                HBox hRow = new HBox(10);
                hRow.setAlignment(Pos.CENTER_LEFT);
                hRow.setStyle("-fx-background-color: #161922; -fx-padding: 8 12; -fx-background-radius: 6;");

                CheckBox cb = new CheckBox();
                cb.setSelected(h.isCompletedToday());
                cb.setOnAction(e -> {
                    service.toggleHabitCompletion(h.getId(), cb.isSelected());
                    h.setCompletedToday(cb.isSelected());
                });

                Label hLabel = new Label(h.getTitle());
                hLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (h.isCompletedToday() ? "#3ecf5e" : "#e8eaf0") + ";");
                HBox.setHgrow(hLabel, Priority.ALWAYS);

                Label status = new Label(h.isCompletedToday() ? "Completed Today" : "Pending");
                status.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (h.isCompletedToday() ? "#3ecf5e" : "#8891a8") + ";");

                hRow.getChildren().addAll(cb, hLabel, status);
                habitList.getChildren().add(hRow);
            }
            habitSection.getChildren().add(habitList);
        }

        content.getChildren().addAll(banner, tasksSection, habitSection);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        tab.setContent(scroll);
        return tab;
    }

    private Node buildDailyPlanContent(Tab tab) {
        return buildDailyPlanTab().getContent();
    }

    // ─── 2. Goals Tab ─────────────────────────────────────────────────────────

    private Tab buildGoalsTab() {
        Tab tab = new Tab("Goals");
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.42);

        // Left List Pane
        VBox listPane = new VBox();
        listPane.setStyle("-fx-background-color: #13161d;");

        HBox listHeader = new HBox(8);
        listHeader.setPadding(new Insets(12));
        listHeader.setAlignment(Pos.CENTER_LEFT);
        Label listLbl = new Label("Declared Goals");
        listLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #e8eaf0;");
        HBox.setHgrow(listLbl, Priority.ALWAYS);

        Button btnNewGoal = new Button("+ New");
        btnNewGoal.getStyleClass().addAll("btn", "btn-primary");
        btnNewGoal.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        listHeader.getChildren().addAll(listLbl, btnNewGoal);

        ListView<Goal> goalList = new ListView<>();
        goalList.setPlaceholder(buildEmptyGoalsState());
        VBox.setVgrow(goalList, Priority.ALWAYS);

        goalList.setCellFactory(lv -> new ListCell<>() {
            protected void updateItem(Goal g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) { setGraphic(null); setText(null); return; }
                VBox box = new VBox(4);
                box.setPadding(new Insets(6, 4, 6, 4));

                HBox top = new HBox(6);
                top.setAlignment(Pos.CENTER_LEFT);

                Label t = new Label(g.getTitle());
                t.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
                HBox.setHgrow(t, Priority.ALWAYS);

                Label pct = new Label(g.getProgress() + "%");
                pct.setStyle("-fx-font-size: 10px; -fx-text-fill: #8891a8;");
                top.getChildren().addAll(t, pct);

                ProgressBar pb = new ProgressBar(g.getProgress() / 100.0);
                pb.setMaxWidth(Double.MAX_VALUE);
                pb.setStyle("-fx-accent: #7c6ff7;");

                box.getChildren().addAll(top, pb);
                setGraphic(box);
            }
        });

        listPane.getChildren().addAll(listHeader, goalList);

        // Right Editor Pane
        VBox editor = new VBox(14);
        editor.setPadding(new Insets(20));
        editor.setStyle("-fx-background-color: #0d0f14;");

        Label edTitle = new Label("Goal Details");
        edTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        TextField titleField = new TextField();
        titleField.setPromptText("Goal title…");

        TextArea descField = new TextArea();
        descField.setPromptText("Description and desired outcomes…");
        descField.setPrefHeight(80);

        ComboBox<Goal.Category> catCombo = new ComboBox<>();
        catCombo.getItems().addAll(Goal.Category.values());
        catCombo.setValue(Goal.Category.PERSONAL);

        ComboBox<Goal.Status> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Goal.Status.values());
        statusCombo.setValue(Goal.Status.ACTIVE);

        TextField targetField = new TextField();
        targetField.setPromptText("Target completion date (YYYY-MM-DD)");

        Slider progSlider = new Slider(0, 100, 0);
        progSlider.setShowTickLabels(true);
        progSlider.setShowTickMarks(true);
        progSlider.setMajorTickUnit(25);

        Label progVal = new Label("Progress: 0%");
        progVal.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        progSlider.valueProperty().addListener((o, ov, nv) ->
            progVal.setText("Progress: " + nv.intValue() + "%")
        );

        HBox actions = new HBox(10);
        Button btnSave = new Button("Save Goal");
        btnSave.getStyleClass().addAll("btn", "btn-primary");
        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().addAll("btn", "btn-danger");

        actions.getChildren().addAll(btnSave, btnDelete);

        editor.getChildren().addAll(
            edTitle,
            vl("Goal Title", titleField),
            vl("Category", catCombo),
            vl("Status", statusCombo),
            vl("Target Date", targetField),
            vl("Description", descField),
            progVal, progSlider,
            actions
        );

        final Goal[] current = {null};

        Runnable refresh = () -> {
            goalList.getItems().setAll(service.getAllGoals());
        };

        btnNewGoal.setOnAction(e -> {
            current[0] = new Goal();
            titleField.clear();
            descField.clear();
            catCombo.setValue(Goal.Category.PERSONAL);
            statusCombo.setValue(Goal.Status.ACTIVE);
            targetField.setText(LocalDate.now().plusMonths(1).toString());
            progSlider.setValue(0);
        });

        goalList.getSelectionModel().selectedItemProperty().addListener((o, ov, sel) -> {
            if (sel == null) return;
            current[0] = sel;
            titleField.setText(sel.getTitle());
            descField.setText(sel.getDescription());
            catCombo.setValue(sel.getCategory());
            statusCombo.setValue(sel.getStatus());
            targetField.setText(sel.getTargetDate());
            progSlider.setValue(sel.getProgress());
        });

        btnSave.setOnAction(e -> {
            String t = titleField.getText().trim();
            if (t.isEmpty()) {
                UIUtil.showError("Validation Error", "Goal title cannot be empty.");
                return;
            }
            Goal g = current[0] != null ? current[0] : new Goal();
            g.setTitle(t);
            g.setDescription(descField.getText().trim());
            g.setCategory(catCombo.getValue());
            g.setStatus(statusCombo.getValue());
            g.setTargetDate(targetField.getText().trim());
            g.setProgress((int) progSlider.getValue());

            if (g.getId() == 0) service.createGoal(g.getTitle(), g.getDescription(), g.getCategory(), g.getStatus(), g.getTargetDate());
            else service.updateGoal(g);

            refresh.run();
        });

        btnDelete.setOnAction(e -> {
            if (current[0] == null || current[0].getId() == 0) return;
            if (!UIUtil.showConfirm("Delete Goal", "Are you sure you want to delete goal '" + current[0].getTitle() + "'?")) return;
            service.deleteGoal(current[0].getId());
            current[0] = null;
            titleField.clear();
            descField.clear();
            refresh.run();
        });

        refresh.run();
        split.getItems().addAll(listPane, editor);
        tab.setContent(split);
        return tab;
    }

    // ─── 3. Tasks Tab ─────────────────────────────────────────────────────────

    private Tab buildTasksTab() {
        Tab tab = new Tab("Tasks");
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #0d0f14;");

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(14, 16, 10, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Tasks & Action Items");
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add Task");
        btnAdd.getStyleClass().addAll("btn", "btn-primary");

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("btn");

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().addAll("btn", "btn-danger");

        toolbar.getChildren().addAll(lbl, btnAdd, btnEdit, btnDelete);

        // Table
        TableView<Task> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Task, String> titleCol = new TableColumn<>("Task");
        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));

        TableColumn<Task, String> prioCol = new TableColumn<>("Priority");
        prioCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority().name()));
        prioCol.setMaxWidth(80);

        TableColumn<Task, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        statusCol.setMaxWidth(90);

        TableColumn<Task, String> dueCol = new TableColumn<>("Due Date");
        dueCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDueDate() != null ? c.getValue().getDueDate() : ""));
        dueCol.setMaxWidth(100);

        table.getColumns().addAll(titleCol, prioCol, statusCol, dueCol);
        table.setPlaceholder(buildEmptyTasksState());

        Runnable refresh = () -> table.getItems().setAll(service.getAllTasks());

        btnAdd.setOnAction(e -> showTaskDialog(null, refresh));
        btnEdit.setOnAction(e -> {
            Task sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showTaskDialog(sel, refresh);
            } else {
                UIUtil.showError("No Selection", "Please select a task from the table to edit.");
            }
        });
        btnDelete.setOnAction(e -> {
            Task sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                UIUtil.showError("No Selection", "Please select a task from the table to delete.");
                return;
            }
            if (UIUtil.showConfirm("Delete Task", "Delete task '" + sel.getTitle() + "'?")) {
                service.deleteTask(sel.getId());
                refresh.run();
            }
        });

        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Task sel = table.getSelectionModel().getSelectedItem();
                if (sel != null) showTaskDialog(sel, refresh);
            }
        });

        refresh.run();
        root.getChildren().addAll(toolbar, table);
        tab.setContent(root);
        return tab;
    }

    private void showTaskDialog(Task existing, Runnable refresh) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New Task" : "Edit Task");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox form = new VBox(10);
        form.setPadding(new Insets(16));
        form.setPrefWidth(420);

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        titleField.setPromptText("Task title");

        TextArea descField = new TextArea(existing != null && existing.getDescription() != null ? existing.getDescription() : "");
        descField.setPromptText("Description (optional)");
        descField.setPrefHeight(60);

        ComboBox<Task.Priority> prioCombo = new ComboBox<>();
        prioCombo.getItems().addAll(Task.Priority.values());
        prioCombo.setValue(existing != null ? existing.getPriority() : Task.Priority.MEDIUM);

        ComboBox<Task.Status> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Task.Status.values());
        statusCombo.setValue(existing != null ? existing.getStatus() : Task.Status.TODO);

        TextField dueField = new TextField(existing != null && existing.getDueDate() != null ? existing.getDueDate() : "");
        dueField.setPromptText("Due date (YYYY-MM-DD)");

        // Goal Link Selection
        List<Goal> goals = service.getAllGoals();
        ComboBox<Goal> goalCombo = new ComboBox<>();
        goalCombo.getItems().add(null);
        goalCombo.getItems().addAll(goals);

        if (existing != null && existing.getGoalId() != null) {
            for (Goal g : goals) {
                if (g.getId() == existing.getGoalId()) {
                    goalCombo.setValue(g);
                    break;
                }
            }
        }

        form.getChildren().addAll(
            vl("Title *", titleField),
            vl("Description", descField),
            vl("Linked Goal", goalCombo),
            vl("Priority", prioCombo),
            vl("Status", statusCombo),
            vl("Due Date", dueField)
        );

        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String t = titleField.getText().trim();
            if (t.isEmpty()) return null;

            Task task = existing != null ? existing : new Task();
            task.setTitle(t);
            task.setDescription(descField.getText().trim());
            task.setPriority(prioCombo.getValue());
            task.setStatus(statusCombo.getValue());
            task.setDueDate(dueField.getText().trim());
            Goal linked = goalCombo.getValue();
            task.setGoalId(linked != null ? linked.getId() : null);
            return task;
        });

        dialog.showAndWait().ifPresent(task -> {
            if (task.getId() == 0) service.createTask(task.getGoalId(), task.getTitle(), task.getDescription(), task.getPriority(), task.getStatus(), task.getDueDate());
            else service.updateTask(task);
            refresh.run();
        });
    }

    // ─── 4. Habits Tab ────────────────────────────────────────────────────────

    private Tab buildHabitsTab() {
        Tab tab = new Tab("Habits");
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #0d0f14;");

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(14, 16, 10, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label today = new Label("Daily Habits — " + LocalDate.now());
        today.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        HBox.setHgrow(today, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add Habit");
        btnAdd.getStyleClass().addAll("btn", "btn-primary");
        toolbar.getChildren().addAll(today, btnAdd);

        VBox habitsList = new VBox(8);
        habitsList.setPadding(new Insets(16));
        ScrollPane sp = new ScrollPane(habitsList);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        VBox.setVgrow(sp, Priority.ALWAYS);

        Runnable refresh = () -> {
            habitsList.getChildren().clear();
            List<Habit> habits = service.getAllHabits();
            if (habits.isEmpty()) {
                VBox empty = new VBox(6);
                empty.setStyle("-fx-background-color: #1a1e28; -fx-padding: 16; -fx-background-radius: 6;");
                Label e1 = new Label("No habits tracked yet.");
                e1.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
                Label e2 = new Label("Create daily habits to track recurring consistency.");
                e2.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
                empty.getChildren().addAll(e1, e2);
                habitsList.getChildren().add(empty);
            } else {
                for (Habit h : habits) {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: #161922; -fx-padding: 10 14; -fx-background-radius: 6; -fx-border-color: #1e2334; -fx-border-radius: 6;");

                    CheckBox check = new CheckBox();
                    check.setSelected(h.isCompletedToday());
                    check.setOnAction(ev -> {
                        service.toggleHabitCompletion(h.getId(), check.isSelected());
                        h.setCompletedToday(check.isSelected());
                    });

                    VBox info = new VBox(2);
                    Label titleLbl = new Label(h.getTitle());
                    titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " +
                        (h.isCompletedToday() ? "#3ecf5e" : "#e8eaf0") + ";");

                    Label descLbl = new Label(h.getDescription() != null ? h.getDescription() : "");
                    descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
                    info.getChildren().addAll(titleLbl, descLbl);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    Label streakLbl = new Label(h.isCompletedToday() ? "Done Today" : "Pending");
                    streakLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                        (h.isCompletedToday() ? "#3ecf5e" : "#8891a8") + ";");

                    Button delBtn = new Button("Delete");
                    delBtn.getStyleClass().addAll("btn", "btn-danger");
                    delBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
                    delBtn.setOnAction(ev -> {
                        if (UIUtil.showConfirm("Delete Habit", "Delete habit '" + h.getTitle() + "'?")) {
                            service.deleteHabit(h.getId());
                            habitsList.getChildren().remove(row);
                        }
                    });

                    row.getChildren().addAll(check, info, streakLbl, delBtn);
                    habitsList.getChildren().add(row);
                }
            }
        };

        btnAdd.setOnAction(e -> showHabitDialog(null, refresh));
        refresh.run();

        root.getChildren().addAll(toolbar, sp);
        tab.setContent(root);
        return tab;
    }

    private void showHabitDialog(Habit existing, Runnable refresh) {
        Dialog<Habit> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New Habit" : "Edit Habit");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox form = new VBox(10);
        form.setPadding(new Insets(16));

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        titleField.setPromptText("Habit name (e.g. Deep Work 45m)");

        TextArea descField = new TextArea(existing != null && existing.getDescription() != null ? existing.getDescription() : "");
        descField.setPromptText("Description / reminder criteria…");
        descField.setPrefHeight(60);

        ComboBox<Habit.Frequency> freqCombo = new ComboBox<>();
        freqCombo.getItems().addAll(Habit.Frequency.values());
        freqCombo.setValue(existing != null ? existing.getFrequency() : Habit.Frequency.DAILY);

        form.getChildren().addAll(vl("Habit Name *", titleField), vl("Description", descField), vl("Frequency", freqCombo));
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String t = titleField.getText().trim();
            if (t.isEmpty()) return null;

            Habit h = existing != null ? existing : new Habit();
            h.setTitle(t);
            h.setDescription(descField.getText().trim());
            h.setFrequency(freqCombo.getValue());
            return h;
        });

        dialog.showAndWait().ifPresent(h -> {
            if (h.getId() == 0) service.createHabit(h.getTitle(), h.getDescription(), h.getFrequency());
            refresh.run();
        });
    }



    private VBox vl(String label, Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        box.getChildren().addAll(lbl, control);
        return box;
    }

    private Node buildEmptyGoalsState() {
        VBox empty = new VBox(8);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        Label icon = new Label("🎯");
        icon.getStyleClass().add("empty-state-icon");
        Label text = new Label("No goals declared yet");
        text.getStyleClass().add("empty-state-text");
        Label sub = new Label("Click '+ New' to establish milestone-driven personal or academic goals.");
        sub.getStyleClass().add("empty-state-sub");
        sub.setWrapText(true);
        empty.getChildren().addAll(icon, text, sub);
        return empty;
    }

    private Node buildEmptyTasksState() {
        VBox empty = new VBox(8);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        Label icon = new Label("✓");
        icon.getStyleClass().add("empty-state-icon");
        Label text = new Label("No tasks found");
        text.getStyleClass().add("empty-state-text");
        Label sub = new Label("Click '+ Add Task' to track action items and decompose your goals.");
        sub.getStyleClass().add("empty-state-sub");
        sub.setWrapText(true);
        empty.getChildren().addAll(icon, text, sub);
        return empty;
    }
}
