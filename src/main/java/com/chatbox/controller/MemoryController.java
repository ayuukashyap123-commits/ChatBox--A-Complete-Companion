package com.chatbox.controller;

import com.chatbox.model.Memory;
import com.chatbox.service.MemoryService;
import com.chatbox.util.UIUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Memory Manager: view, add, edit, approve and delete user-controlled memories.
 */
public class MemoryController {

    private final MemoryService service = new MemoryService();
    private TableView<Memory> table;
    private Label countLabel;

    public Node build() {
        VBox root = new VBox();
        root.getStyleClass().add("content-area");

        // Header
        VBox header = new VBox(4);
        header.getStyleClass().add("page-header");
        Label title = new Label("Memory Manager");
        title.getStyleClass().add("page-title");
        Label sub = new Label("You have full control over what the AI remembers about you. " +
            "Approve, edit or delete any memory at any time.");
        sub.getStyleClass().add("page-subtitle");
        sub.setWrapText(true);
        header.getChildren().addAll(title, sub);

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(12, 28, 8, 28));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnAdd = new Button("+ Add Memory");
        btnAdd.getStyleClass().addAll("btn", "btn-primary");

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("btn");

        Button btnApprove = new Button("Toggle Approval");
        btnApprove.getStyleClass().add("btn");

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().addAll("btn", "btn-danger");

        countLabel = new Label("");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        HBox.setHgrow(countLabel, Priority.ALWAYS);
        HBox countSpacer = new HBox();
        HBox.setHgrow(countSpacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(btnAdd, btnEdit, btnApprove, btnDelete, countSpacer, countLabel);

        // Table
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPadding(new Insets(0, 28, 0, 28));

        TableColumn<Memory, String> approvedCol = new TableColumn<>("Status");
        approvedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isApproved() ? "✓ Active" : "⊘ Pending"));
        approvedCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-text-fill: " + (item.startsWith("✓") ? "#3ecf5e" : "#f0a23c") + ";");
            }
        });
        approvedCol.setMaxWidth(90);

        TableColumn<Memory, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().name()));
        categoryCol.setMaxWidth(100);

        TableColumn<Memory, String> contentCol = new TableColumn<>("Memory");
        contentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContent()));

        TableColumn<Memory, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSource()));
        sourceCol.setMaxWidth(90);

        TableColumn<Memory, String> dateCol = new TableColumn<>("Updated");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(UIUtil.formatDate(c.getValue().getUpdatedAt())));
        dateCol.setMaxWidth(90);

        table.getColumns().addAll(approvedCol, categoryCol, contentCol, sourceCol, dateCol);
        table.setPlaceholder(buildEmptyState());

        // Privacy notice footer
        HBox footer = new HBox();
        footer.setPadding(new Insets(8, 28, 12, 28));
        Label privacy = new Label("Privacy: Memories are stored locally. Only approved memories are included in AI context " +
            "when you explicitly ask. Your journal and full conversation history are never sent automatically.");
        privacy.setStyle("-fx-font-size: 10px; -fx-text-fill: #4d5468;");
        privacy.setWrapText(true);
        footer.getChildren().add(privacy);

        root.getChildren().addAll(header, toolbar, table, footer);

        // Actions
        btnAdd.setOnAction(e -> showMemoryDialog(null));
        btnEdit.setOnAction(e -> {
            Memory sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) showMemoryDialog(sel);
        });
        btnApprove.setOnAction(e -> {
            Memory sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                service.toggleApproval(sel.getId());
                refresh();
            }
        });
        btnDelete.setOnAction(e -> {
            Memory sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (UIUtil.showConfirm("Delete Memory", "Permanently delete this memory?")) {
                service.deleteMemory(sel.getId());
                refresh();
            }
        });

        table.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Memory sel = table.getSelectionModel().getSelectedItem();
                if (sel != null) showMemoryDialog(sel);
            }
        });

        refresh();
        return root;
    }

    private void showMemoryDialog(Memory existing) {
        Dialog<Memory> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Memory" : "Edit Memory");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
        form.setPrefWidth(460);

        TextArea contentField = new TextArea(existing != null ? existing.getContent() : "");
        contentField.setPromptText("What should the AI remember about you?");
        contentField.setPrefHeight(100);
        contentField.setWrapText(true);

        ComboBox<Memory.Category> catCombo = new ComboBox<>();
        catCombo.getItems().addAll(Memory.Category.values());
        catCombo.setValue(existing != null ? existing.getCategory() : Memory.Category.GENERAL);

        CheckBox approvedCheck = new CheckBox("Approved — include in AI context");
        approvedCheck.setSelected(existing == null || existing.isApproved());

        Label exampleLbl = new Label("Examples: \"I prefer concise answers\", \"My native language is Hindi\", \"I'm working on a React project\"");
        exampleLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #4d5468;");
        exampleLbl.setWrapText(true);

        form.getChildren().addAll(
            labeled("Memory Content", contentField),
            labeled("Category", catCombo),
            approvedCheck,
            exampleLbl
        );

        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Memory m = existing != null ? existing : new Memory();
            m.setContent(contentField.getText().trim());
            m.setCategory(catCombo.getValue());
            m.setApproved(approvedCheck.isSelected());
            return m;
        });

        dialog.showAndWait().ifPresent(m -> {
            if (m.getContent().isEmpty()) return;
            if (m.getId() == 0) service.createMemory(m.getContent(), m.getCategory(), m.isApproved());
            else service.updateMemory(m);
            refresh();
        });
    }

    private Node buildEmptyState() {
        VBox empty = new VBox(8);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        Label icon = new Label("◷");
        icon.getStyleClass().add("empty-state-icon");
        Label text = new Label("No memories yet");
        text.getStyleClass().add("empty-state-text");
        Label sub = new Label("Add memories to help the AI understand your context, preferences and goals");
        sub.getStyleClass().add("empty-state-sub");
        sub.setWrapText(true);
        empty.getChildren().addAll(icon, text, sub);
        return empty;
    }

    private VBox labeled(String label, Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");
        box.getChildren().addAll(lbl, control);
        return box;
    }

    private void refresh() {
        List<Memory> all = service.getAllMemories();
        table.getItems().setAll(all);
        long approved = all.stream().filter(Memory::isApproved).count();
        countLabel.setText(all.size() + " memories · " + approved + " active");
    }
}
