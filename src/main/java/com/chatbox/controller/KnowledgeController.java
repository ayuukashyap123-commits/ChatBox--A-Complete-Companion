package com.chatbox.controller;

import com.chatbox.model.*;
import com.chatbox.repository.KnowledgeRepository;
import com.chatbox.service.*;
import com.chatbox.util.UIUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;

/**
 * Personal AI Second Brain: import files, search content, get answers.
 */
public class KnowledgeController {

    private final AiService aiService;
    private final Stage stage;
    private final KnowledgeRepository repo = new KnowledgeRepository();
    private final DocumentService docService = new DocumentService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "doc-worker");
        t.setDaemon(true);
        return t;
    });

    private TableView<KnowledgeDocument> docTable;
    private TextArea searchResultArea;
    private TextField searchField;
    private Label statusLabel;

    public KnowledgeController(AiService aiService, Stage stage) {
        this.aiService = aiService;
        this.stage = stage;
    }

    public Node build() {
        VBox root = new VBox();
        root.getStyleClass().add("content-area");

        VBox header = new VBox(4);
        header.getStyleClass().add("page-header");
        Label title = new Label("Personal Second Brain");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Import documents, search your knowledge base, and get source-grounded answers.");
        sub.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(title, sub);

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.45);
        VBox.setVgrow(split, Priority.ALWAYS);

        split.getItems().addAll(buildDocPanel(), buildSearchPanel());
        root.getChildren().addAll(header, split);
        return root;
    }

    private VBox buildDocPanel() {
        VBox panel = new VBox();
        panel.setStyle("-fx-background-color: #13161d;");

        // Toolbar
        HBox toolbar = new HBox(8);
        toolbar.setPadding(new Insets(12, 16, 10, 16));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Documents");
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Button btnImport = new Button("Import File");
        btnImport.getStyleClass().addAll("btn", "btn-primary");
        Button btnDelete = new Button("Remove");
        btnDelete.getStyleClass().addAll("btn", "btn-danger");

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8891a8;");

        toolbar.getChildren().addAll(lbl, btnImport, btnDelete);

        // Table
        docTable = new TableView<>();
        docTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(docTable, Priority.ALWAYS);

        TableColumn<KnowledgeDocument, String> nameCol = new TableColumn<>("Title");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));

        TableColumn<KnowledgeDocument, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getFileType() != null ? c.getValue().getFileType().name() : ""));
        typeCol.setMaxWidth(50);

        TableColumn<KnowledgeDocument, String> wordsCol = new TableColumn<>("Words");
        wordsCol.setCellValueFactory(c -> new SimpleStringProperty(
            String.valueOf(c.getValue().getWordCount())));
        wordsCol.setMaxWidth(70);

        TableColumn<KnowledgeDocument, String> dateCol = new TableColumn<>("Imported");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
            UIUtil.formatDate(c.getValue().getCreatedAt())));
        dateCol.setMaxWidth(90);

        docTable.getColumns().addAll(nameCol, typeCol, wordsCol, dateCol);
        docTable.setPlaceholder(buildEmptyDocsState());

        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(6, 16, 6, 16));

        panel.getChildren().addAll(toolbar, docTable, statusBar);

        // Actions
        btnImport.setOnAction(e -> importFile());
        btnDelete.setOnAction(e -> {
            KnowledgeDocument sel = docTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                statusLabel.setText("Please select a document from the table to remove.");
                statusLabel.setStyle("-fx-text-fill: #f0a23c; -fx-font-size: 11px;");
                return;
            }
            if (UIUtil.showConfirm("Remove Document", "Remove '" + sel.getTitle() + "' from your knowledge base?")) {
                repo.delete(sel.getId());
                refreshDocs();
                statusLabel.setText("Removed document: " + sel.getTitle());
                statusLabel.setStyle("-fx-text-fill: #8891a8; -fx-font-size: 11px;");
            }
        });

        refreshDocs();
        return panel;
    }

    private VBox buildSearchPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color: #0d0f14;");

        Label lbl = new Label("Search & Ask");
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        HBox searchRow = new HBox(8);
        searchField = new TextField();
        searchField.setPromptText("Search your documents or ask a question…");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button btnSearch = new Button("Search");
        btnSearch.getStyleClass().addAll("btn");
        Button btnAsk = new Button("Ask AI");
        btnAsk.getStyleClass().addAll("btn", "btn-primary");

        searchRow.getChildren().addAll(searchField, btnSearch, btnAsk);

        searchResultArea = new TextArea();
        searchResultArea.setEditable(false);
        searchResultArea.setWrapText(true);
        searchResultArea.setPromptText("Search results and AI answers will appear here.");
        VBox.setVgrow(searchResultArea, Priority.ALWAYS);
        searchResultArea.setStyle("-fx-font-size: 12px;");

        Label disclaimer = new Label("AI answers are based on your documents only. Verify important information from original sources.");
        disclaimer.setStyle("-fx-font-size: 10px; -fx-text-fill: #4d5468;");
        disclaimer.setWrapText(true);

        panel.getChildren().addAll(lbl, searchRow, searchResultArea, disclaimer);

        searchField.setOnAction(e -> btnSearch.fire());

        btnSearch.setOnAction(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) return;
            List<KnowledgeDocument> results = repo.search(q);
            if (results.isEmpty()) {
                searchResultArea.setText("No matching documents found for: \"" + q + "\"");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(results.size()).append(" document(s) matching \"").append(q).append("\":\n\n");
            for (KnowledgeDocument doc : results) {
                sb.append("📄 ").append(doc.getTitle()).append(" (").append(doc.getFileType()).append(")\n");
                // Get full content for excerpt
                KnowledgeDocument full = repo.findById(doc.getId());
                if (full != null) {
                    String excerpt = docService.extractRelevantExcerpt(full, q);
                    if (!excerpt.isEmpty()) {
                        sb.append("   ").append(excerpt).append("\n");
                    }
                }
                sb.append("\n");
            }
            searchResultArea.setText(sb.toString());
        });

        btnAsk.setOnAction(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) return;

            searchResultArea.setText("Searching Second Brain documents and generating grounded answer…");
            btnAsk.setDisable(true);

            executor.submit(() -> {
                try {
                    String answer = aiService.chat(Conversation.Mode.KNOWLEDGE, q, List.of(), null);
                    Platform.runLater(() -> {
                        searchResultArea.setText(answer);
                        btnAsk.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        searchResultArea.setText("Error generating answer: " + ex.getMessage());
                        btnAsk.setDisable(false);
                    });
                }
            });
        });

        return panel;
    }

    private void importFile() {
        File file = UIUtil.chooseFile(stage, "Import Document",
            new FileChooser.ExtensionFilter("Documents", "*.txt", "*.md", "*.pdf"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("Markdown", "*.md"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        if (file == null) return;
        String name = file.getName();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!java.util.List.of("txt", "md", "pdf").contains(ext)) {
            statusLabel.setText("✗ Unsupported format: ." + ext + " (Only .pdf, .md, and .txt supported)");
            statusLabel.setStyle("-fx-text-fill: #e05555; -fx-font-size: 11px;");
            return;
        }

        String title = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        if (repo.existsByTitleOrPath(title, file.getAbsolutePath())) {
            statusLabel.setText("Notice: Document '" + title + "' is already imported in your Second Brain.");
            statusLabel.setStyle("-fx-text-fill: #f0a23c; -fx-font-size: 11px;");
            return;
        }

        statusLabel.setText("Processing & extracting " + file.getName() + "…");
        statusLabel.setStyle("-fx-text-fill: #f0a23c; -fx-font-size: 11px;");

        Task<KnowledgeDocument> task = new Task<>() {
            protected KnowledgeDocument call() throws Exception {
                return docService.processFile(file);
            }
        };

        task.setOnSucceeded(e -> {
            KnowledgeDocument doc = task.getValue();
            repo.save(doc);
            refreshDocs();
            statusLabel.setText("✓ Imported: " + doc.getTitle() + " (" + doc.getWordCount() + " words)");
            statusLabel.setStyle("-fx-text-fill: #3ecf5e; -fx-font-size: 11px;");
        });

        task.setOnFailed(e -> {
            statusLabel.setText("✗ Import failed: " + task.getException().getMessage());
            statusLabel.setStyle("-fx-text-fill: #e05555; -fx-font-size: 11px;");
        });

        executor.submit(task);
    }

    private void refreshDocs() {
        docTable.getItems().setAll(repo.findAll());
    }

    private Node buildEmptyDocsState() {
        VBox empty = new VBox(8);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        Label icon = new Label("📄");
        icon.getStyleClass().add("empty-state-icon");
        Label text = new Label("No documents imported yet");
        text.getStyleClass().add("empty-state-text");
        Label sub = new Label("Import PDF, Markdown (.md), or Text (.txt) files to populate your Second Brain.");
        sub.getStyleClass().add("empty-state-sub");
        sub.setWrapText(true);
        empty.getChildren().addAll(icon, text, sub);
        return empty;
    }
}
