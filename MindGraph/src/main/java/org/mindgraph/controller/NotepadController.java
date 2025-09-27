package org.mindgraph.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.fxmisc.richtext.InlineCssTextArea;
import org.mindgraph.datastructure.LinkedList;
import org.mindgraph.db.NoteDao;
import org.mindgraph.model.Note;
import org.mindgraph.util.KeywordExtractor;
import org.mindgraph.model.NoteEntry;
import org.mindgraph.util.NoteXmlUtil;
import org.mindgraph.datastructure.Stack;
import org.mindgraph.model.Note;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.input.MouseEvent;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotepadController {
    private Stack history = new Stack(); // stack of opened notes
    private final GraphController graphController = new GraphController("graph.dat");
    private RevisionController revisionController;
    private final Stack revisionBackStack = new Stack(); // tracks notes opened from links in revision
    private boolean inRevisionMode = false;
    private Note currentQueueNote = null;
    private final Stack backStack = new Stack();

    // --- FXML Fields ---
    @FXML
    private StackPane editorHost;
    @FXML
    private ToggleButton toggleEdit;
    @FXML
    private Button btnNew, btnOpen, btnSave;
    @FXML
    private Button btnBold, btnItalic, btnUnderline;
    @FXML
    private Button btnAlignLeft, btnAlignCenter, btnAlignRight;
    @FXML
    private ColorPicker cpTextColor, cpHighlight;
    @FXML
    private ComboBox<String> cmbFontFamily;
    @FXML
    private Spinner<Integer> spinnerFont;
    @FXML
    private Label lblSaved, lblTitle, lblCursor, lblWords, lblChars;
    @FXML
    private TextField txtTitle;
    @FXML
    private ComboBox<String> cmbDifficulty;
    @FXML
    private ComboBox<String> cmbMode;
    @FXML
    private Button btnAddImage, btnFind, btnRep;
    @FXML
    private Button btnPrev;
    @FXML
    private Button btnNext;
    @FXML private ComboBox<Note> cmbSessionHistory;
    @FXML private VBox vboxNotes;
    @FXML private Button btnAddNoteToPanel;


    private ObservableList<Note> sessionHistoryList = FXCollections.observableArrayList();
    private ListView<Note> suggestionListView = new ListView<>();
    private Popup suggestionPopup = new Popup(); //
    private final ObservableList<Note> studyPlanList = FXCollections.observableArrayList();
    private final ObservableList<Note> studyPathList = FXCollections.observableArrayList();
    ;
    @FXML private ComboBox<String> cmbSessionSort;
    // --- Add ComboBox in your FXML and Controller ---
//    @FXML
//    private ComboBox<Note> cmbStudyPlan;

    @FXML private Label lblLastStudied;
    @FXML private Button btnMarkStudied;

    @FXML private HBox toastContainer;



    private InlineCssTextArea editor;
    private boolean dirty = false;
    private File currentFile = null;
    private Note currentNote = new Note();

    private static final String BOLD = "-fx-font-weight:bold;";
    private static final String ITALIC = "-fx-font-style:italic;";
    private static final String UNDERLINE = "-fx-underline:true;";

    private final NoteDao noteDao = new NoteDao("mindgraph.db");

    private record KeywordRange(int start, int end, String keyword) {}
    private final List<KeywordRange> keywordRanges = new ArrayList<>();

    // --- Add this field ---
    private final StudyPlanController studyPlanManager = new StudyPlanController();

    private LinkedList<Note>.Cursor studyPlanCursor;
    private Note currentlySelectedNote = null;

    private final StudyPathController studyPathManager = new StudyPathController();


    @FXML
    public void initialize() {
        editor = new InlineCssTextArea();
        editor.setWrapText(true);
        editorHost.getChildren().add(editor);
        editor.setEditable(true);
        revisionController = new RevisionController();

        cmbFontFamily.setItems(FXCollections.observableArrayList(
                "System","Arial","Verdana","Tahoma","Times New Roman","Courier New","Georgia"
        ));
        cmbFontFamily.getSelectionModel().select("System");

        cmbDifficulty.setItems(FXCollections.observableArrayList("1","2","3","4","5"));
        cmbDifficulty.getSelectionModel().select("1");

        cmbMode.setItems(FXCollections.observableArrayList(
                "Concept Map",
                "Study Plan",
                "Revision",
                "Study Path"
        ));
        cmbMode.getSelectionModel().selectFirst();

        spinnerFont.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 72, 14));

        editor.textProperty().addListener((obs, ov, nv) -> markDirty());
        editor.caretPositionProperty().addListener((obs, ov, nv) -> updateCaret());

        editor.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) return;
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSave);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::onNew);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::onOpen);
        });

        spinnerFont.valueProperty().addListener((obs, ov, nv) -> applyFontSize(nv));
        cmbFontFamily.valueProperty().addListener((obs, ov, nv) -> applyFontFamily(nv));

        if (cpTextColor != null) cpTextColor.setOnAction(e -> applyTextColor(cpTextColor.getValue()));
        if (cpHighlight != null) cpHighlight.setOnAction(e -> applyHighlightColor(cpHighlight.getValue()));

        btnBold.setOnAction(e -> toggleStyle(BOLD));
        btnItalic.setOnAction(e -> toggleStyle(ITALIC));
        btnUnderline.setOnAction(e -> toggleStyle(UNDERLINE));

        lblTitle.setText(currentNote.getTitle());
        txtTitle.setText(currentNote.getTitle());

        updateCounts();
        updateCaret();

        editor.setOnMouseClicked(event -> {
            int pos = editor.getCaretPosition();
            for (KeywordRange kr : keywordRanges) {
                if (pos >= kr.start && pos < kr.end) {
                    String[] parts = kr.keyword.split("\\|\\|");
                    String keyword = parts[0];
                    int linkedNoteId = Integer.parseInt(parts[1]);

                    Note linkedNote = graphController.getGraph().getGraphNodes()
                            .keySet()
                            .stream()
                            .filter(n -> n.getId() == linkedNoteId)
                            .findFirst()
                            .orElse(null);

                    if (linkedNote != null) {
                        if (currentNote != null && currentNote != linkedNote) {
                            backStack.push(currentNote); // push current note to backtrack stack
                            btnPrev.setDisable(false);   // enable previous button
                            btnNext.setDisable(true);    // disable next until user navigates forward
                        }

                        loadNoteInEditor(linkedNote, false);

                        // Revision mode: next button depends on controller
                        if (inRevisionMode) {
                            btnNext.setDisable(!revisionController.hasNotes());
                        }
                    }
                    break;
                }
            }
        });

        editor.setOnMouseMoved(event -> {
            int pos = editor.hit(event.getX(), event.getY()).getInsertionIndex();
            boolean overLink = false;
            for (KeywordRange kr : keywordRanges) {
                if (pos >= kr.start && pos < kr.end) {
                    overLink = true;
                    break;
                }
            }
            editor.setCursor(overLink ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.TEXT);
        });

        Platform.runLater(() -> {
            try {
                graphController.buildGraphFromDb(false);
                System.out.println("Graph built with " + graphController.getGraph().getGraphNodes().size() + " nodes.");
            } catch (SQLException e) {
                showError("Graph Initialization Failed", e.getMessage());
                e.printStackTrace();
            }
        });

        cmbMode.valueProperty().addListener((obs, oldMode, newMode) -> {
            // Clear backtracking stack for Study Plan navigation
            backStack.clear();
            btnPrev.setDisable(true);
            btnNext.setDisable(false);

            if ("Revision".equals(newMode)) {
                inRevisionMode = true;
                revisionBackStack.clear();

                // Load or create the revision controller (it will automatically load the saved queue)
                if (revisionController == null) {
                    revisionController = new RevisionController();
                }

                // Get the first note in the queue
                currentQueueNote = revisionController.dequeueNextNote();

                if (currentQueueNote != null) {
                    // Display it in editor
                    loadNoteInEditor(currentQueueNote, false);

                    // Update last studied label
                    if (currentQueueNote.getLastStudied() != null) {
                        lblLastStudied.setText("Last Studied: " + currentQueueNote.getLastStudied().toString());
                    } else {
                        lblLastStudied.setText("Last Studied: Never");
                    }

                    // Navigation buttons
                    btnPrev.setDisable(true);  // first note, so Prev is disabled
                    btnNext.setDisable(!revisionController.hasNotes()); // enable Next only if more notes
                } else {
                    // Queue empty
                    showError("Revision Empty", "No notes available for revision.");
                    btnPrev.setDisable(true);
                    btnNext.setDisable(true);
                }

                clearAllHighlights();

            } else if ("Study Plan".equals(newMode)) {
                inRevisionMode = false;

                // Load saved study plan into memory
                List<Note> savedPlan = studyPlanManager.getPlan();
                studyPlanList.setAll(savedPlan);
//                cmbStudyPlan.setItems(studyPlanList);

                // Render side panel if you have one
                renderStudyPlan();

                // Set up the cursor for navigation
                studyPlanCursor = studyPlanManager.getPlanCursor();

                // Load the first note if available
                Note firstNote = (studyPlanCursor != null) ? studyPlanCursor.current() : null;
                if (firstNote != null) {
                    loadNoteInEditor(firstNote, false);
                    currentNote = firstNote;
                    btnPrev.setDisable(true);                  // first note, so Prev disabled
                    btnNext.setDisable(!studyPlanCursor.canNext()); // Next enabled only if more notes
                } else {
                    btnPrev.setDisable(true);
                    btnNext.setDisable(true);
                    showError("Study Plan Empty", "No notes available in the study plan.");
                }

            } else if ("Study Path".equals(newMode)) {
                inRevisionMode = false;

                // Generate or load the study plan from the graph
                studyPathManager.generateFromGraph(graphController.getGraph());

                // Load the first note if available
                Note firstNote = studyPathManager.peekNextNote();
                if (firstNote != null) {
                    loadNoteInEditor(firstNote, false);
                    currentNote = firstNote;
                    btnPrev.setDisable(true);                          // first note, so Prev disabled
                    btnNext.setDisable(studyPathManager.getPlan().size() <= 1); // Next enabled only if more notes
                } else {
                    btnPrev.setDisable(true);
                    btnNext.setDisable(true);
                    showError("Study Path Empty", "No notes available in the study path.");
                }

                clearAllHighlights();
            } else {
                // Default: disable Revision mode
                inRevisionMode = false;
                btnPrev.setDisable(false);
                btnNext.setDisable(false);
            }

            // Configure the study plan combo box for display
//            cmbStudyPlan.setCellFactory(lv -> new ListCell<Note>() {
//                @Override
//                protected void updateItem(Note item, boolean empty) {
//                    super.updateItem(item, empty);
//                    setText(empty || item == null ? null : item.getTitle());
//                }
//            });

//            cmbStudyPlan.setButtonCell(new ListCell<Note>() {
//                @Override
//                protected void updateItem(Note item, boolean empty) {
//                    super.updateItem(item, empty);
//                    setText(empty || item == null ? null : item.getTitle());
//                }
//            });
        });

        loadSessionHistoryFromDB();
        // Remove all FilteredList related code and replace with:
        cmbSessionHistory.setEditable(true);
        cmbSessionHistory.setItems(sessionHistoryList);



// Make ComboBox display Note titles
        cmbSessionHistory.setCellFactory(lv -> new ListCell<Note>() {
            @Override
            protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });

// Set button cell so the selected item shows title
        cmbSessionHistory.setButtonCell(new ListCell<Note>() {
            @Override
            protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitle());
            }
        });


        // Setup suggestion popup
        setupSuggestionPopup();

        cmbSessionHistory.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            filterAndShowSuggestions(newVal);

            // Avoid setting raw string as value
            Note match = sessionHistoryList.stream()
                    .filter(n -> n.getTitle().equalsIgnoreCase(newVal))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                cmbSessionHistory.setValue(match); // safe, it's a Note
            } else {
                cmbSessionHistory.setValue(null); // safe
            }
        });
        // Hide popup when focus lost
        cmbSessionHistory.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                suggestionPopup.hide();
            }
        });
// Add keyboard support for the combobox editor
        cmbSessionHistory.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onLoadHistory();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                suggestionPopup.hide();
                event.consume();
            }});




        cmbSessionSort.setItems(FXCollections.observableArrayList(
                "Newest", "Oldest", "Most Used", "Least Used"
        ));
        cmbSessionSort.getSelectionModel().select("Newest");

        // Add listener for sort changes
        cmbSessionSort.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadSessionHistoryFromDB();
        });
    }

    private void showKeywordAlert(String noteTitle, String keyword) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Note Title: " + noteTitle + "\nKeyword: " + keyword, ButtonType.OK);
        alert.setHeaderText("Keyword Clicked");
        alert.showAndWait();
    }

    @FXML
    public void onNew() {
        if (!confirmLoseChanges()) return;
        editor.clear();
        currentFile = null;
        currentNote = new Note();
        keywordRanges.clear();
        lblTitle.setText("Untitled");
        txtTitle.setText("Untitled");
        cmbDifficulty.getSelectionModel().select("1");
        clearDirty();
    }

    @FXML
    public void onOpen() {
        if (!confirmLoseChanges()) return;

        // Push current note to history for backtracking
        if (currentNote != null) {
            history.push(currentNote); // just the Note object now
        }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MindGraph XML", "*.xml"));

        File defaultFolder = new File("C:\\\\Users\\\\milin\\\\OneDrive\\\\Desktop\\\\mind-graph-notes");
        if (defaultFolder.exists() && defaultFolder.isDirectory()) {
            fc.setInitialDirectory(defaultFolder);
        }
        File f = fc.showOpenDialog(editor.getScene().getWindow());

        if (f != null) {
            try {
                Note n = noteDao.findByFilePath(f.getAbsolutePath());
                if (n == null) n = new Note();
                NoteXmlUtil.load(n, editor, f);
                currentNote = n;
                currentFile = f;
                lblTitle.setText(n.getTitle());
                txtTitle.setText(n.getTitle());
                cmbDifficulty.getSelectionModel().select(String.valueOf(n.getDifficulty()));
                keywordRanges.clear();
                markKeywords();
                clearDirty();

                updateSession(currentNote);
                noteDao.incrementUsageCount(currentNote.getId());
                loadSessionHistoryFromDB();
            } catch (Exception ex) {
                updateSession(currentNote);

                noteDao.updateSession(currentNote);
                updateSession(currentNote); // optional, updates title/difficulty

                try {
                    noteDao.incrementUsageCount(currentNote.getId());
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Database Error", "Failed to increment usage count: " + e.getMessage());
                }
                loadSessionHistoryFromDB();


            }
        }
    }

    @FXML
    public void onSave() {
        try {
            if (currentNote == null) currentNote = new Note();

            File f = currentFile;
            if (f == null) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MindGraph XML", "*.xml"));
                File defaultFolder = new File("C:\\Users\\milin\\OneDrive\\Desktop\\mind-graph-notes");
                if (defaultFolder.exists() && defaultFolder.isDirectory()) {
                    fc.setInitialDirectory(defaultFolder);
                }
                f = fc.showSaveDialog(editor.getScene().getWindow());
                if (f == null) return;
                currentFile = f;
            }

            // --- Update note properties before saving ---
            currentNote.setTitle(txtTitle.getText());
            currentNote.setDifficulty(parseDifficulty());
            if (currentNote.getCreatedAt() == null) {
                currentNote.setCreatedAt(LocalDateTime.now());
            }
            currentNote.setUpdatedAt(LocalDateTime.now());
            currentNote.setFilePath(f.getAbsolutePath()); // make sure it's stored

            // Extract & set keywords
            List<String> extractedKeywords = KeywordExtractor.extractKeywords(editor.getText());
            List<String> selectedKeywords = showKeywordSelectionDialog(extractedKeywords);
            if (selectedKeywords == null) selectedKeywords = List.of();
            currentNote.setKeywords(selectedKeywords);

            // --- Save note to XML ---
            NoteXmlUtil.save(currentNote, editor, f);

            // --- Save to DB ---
            noteDao.upsert(currentNote, f.getAbsolutePath());

            // --- Refresh UI ---
            keywordRanges.clear();
            markKeywords();
            lblTitle.setText(txtTitle.getText());
            clearDirty();

            // --- Update session ---
            updateSession(currentNote);

            // --- Update graph ---
            try {
                graphController.buildGraphFromDb(true);
            } catch (SQLException e) {
                showError("Graph Update Failed", e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception ex) {
            showError("Save Failed", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void markKeywords() {
        keywordRanges.clear();
        if (currentNote == null) return;

        String content = editor.getText();
        List<Note> neighbors = graphController.getGraph().getNeighbours(currentNote);
        // First: remove any leftover "link" styling from the whole doc so non-matches don't look like links
        int len = editor.getLength();
        for (int i = 0; i < len; i++) {
            String cur = editor.getStyleOfChar(i);
            if (cur == null || cur.isEmpty()) continue;

            // Remove underline and any fill that was used for links (blue / rgb(...))
            String cleaned = cur
                    .replaceAll("-fx-underline\\s*:\\s*[^;]+;?", "")
                    .replaceAll("-fx-fill\\s*:\\s*blue;?", "")
                    .replaceAll("-fx-fill\\s*:\\s*rgb\\([^)]*\\);?", "");

            // If cleaned changed, write it back (preserve other style attributes)
            if (!cleaned.equals(cur)) {
                // ensure there's no stray trailing semicolon/space problems
                cleaned = cleaned.trim();
                if (!cleaned.endsWith(";") && !cleaned.isEmpty()) cleaned = cleaned + ";";
                editor.setStyle(i, i + 1, cleaned);
            }
        }

        if (neighbors == null || neighbors.isEmpty()) return;

        for (Note neighbor : neighbors) {
            String neighborTitle = neighbor.getTitle();
            if (neighborTitle == null || neighborTitle.isBlank()) continue;

            // Remove trailing 's' if present to get singular root
            String root = neighborTitle.replaceAll("(?i)s$", "");

            // Escape regex special chars
            String escapedRoot = java.util.regex.Pattern.quote(root);

            // Match singular or plural (optional 's' or 'es') case-insensitive
            String regex = "(?i)\\b" + escapedRoot + "(?:s|es)?\\b";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();

                // record range with note id so click handler can use it
                keywordRanges.add(new KeywordRange(start, end, neighborTitle + "||" + neighbor.getId()));

                // apply link style but preserve other existing char styles
                editor.setStyle(start, end, mergeStyle(editor.getStyleOfChar(start), "-fx-fill: blue; -fx-underline: true;"));
            }
        }
    }

    @FXML
    private void onAddImage() {
        // code to add an image
        System.out.println("Add image clicked!");
    }

    @FXML
    private void onFind() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Find Text");
        dialog.setHeaderText("Enter text to find:");
        dialog.setContentText("Text:");
        dialog.showAndWait().ifPresent(searchText -> {
            String content = editor.getText();
            int index = content.indexOf(searchText);
            if (index >= 0) {
                editor.selectRange(index, index + searchText.length());
                editor.requestFocus();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Text not found!", ButtonType.OK);
                alert.setHeaderText("Find");
                alert.showAndWait();
            }
        });
    }

    @FXML
    private void onReplace() {
        TextInputDialog findDialog = new TextInputDialog();
        findDialog.setTitle("Find & Replace");
        findDialog.setHeaderText("Enter the text to find:");
        findDialog.setContentText("Find:");
        findDialog.showAndWait().ifPresent(findText -> {
            TextInputDialog replaceDialog = new TextInputDialog();
            replaceDialog.setTitle("Replace Text");
            replaceDialog.setHeaderText("Replace with:");
            replaceDialog.setContentText("Replace:");

            replaceDialog.showAndWait().ifPresent(replaceText -> {
                String content = editor.getText();
                content = content.replace(findText, replaceText);
                editor.replaceText(content);
            });
        });
    }

    // --- Styling helpers ---
    private void toggleStyle(String css) {
        appendStyle(css);
    }

    private void applyFontSize(int size) {
        appendStyle("-fx-font-size:" + size + "px;");
    }

    private void applyFontFamily(String family) {
        appendStyle("-fx-font-family:'" + family + "';");
    }

    private void applyTextColor(Color color) {
        appendStyle("-fx-fill:" + toRgbString(color) + ";");
    }

    private void applyHighlightColor(Color color) {
        appendStyle("-fx-background-color:" + toRgbString(color) + ";");
    }

    private void appendStyle(String css) {
        int start = editor.getSelection().getStart();
        int end = editor.getSelection().getEnd();
        if (start == end) return;

        for (int i = start; i < end; i++) {
            String current = editor.getStyleOfChar(i);
            editor.setStyle(i, i + 1, mergeStyle(current, css));
        }
        markDirty();
    }

    private String mergeStyle(String current, String newCss) {
        if (current == null) current = "";
        if (newCss == null || newCss.isBlank()) return current.trim();

        current = current.trim();
        if (!current.endsWith(";") && !current.isEmpty()) current += ";";

        return current + newCss.trim() + ";";
    }

    private void applyAlignment(String align) {
        int pIndex = editor.getCurrentParagraph();
        editor.setParagraphStyle(pIndex, "-fx-text-alignment:" + align + ";");
        markDirty();
    }

    private List<String> showKeywordSelectionDialog(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return List.of();

        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Select Keywords");
        dialog.setHeaderText("Choose keywords to save with this note");

        ButtonType okButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        int columns = 3;
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (int i = 0; i < keywords.size(); i++) {
            CheckBox cb = new CheckBox(keywords.get(i));
            checkBoxes.add(cb);
            int col = i % columns;
            int row = i / columns;
            grid.add(cb, col, row);
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        dialog.getDialogPane().setContent(scroll);

        dialog.setResultConverter(button -> {
            if (button == okButtonType) {
                List<String> selected = new ArrayList<>();
                for (CheckBox cb : checkBoxes) {
                    if (cb.isSelected()) selected.add(cb.getText());
                }
                return selected;
            }
            return null;
        });

        return dialog.showAndWait().orElse(List.of());
    }

    // --- Misc helpers ---
    @FXML
    private void onBold() {
        toggleStyle(BOLD);
    }

    @FXML
    private void onItalic() {
        toggleStyle(ITALIC);
    }

    @FXML
    private void onUnderline() {
        toggleStyle(UNDERLINE);
    }

    @FXML
    private void onAlignLeft() {
        applyAlignment("left");
    }

    @FXML
    private void onAlignCenter() {
        applyAlignment("center");
    }

    @FXML
    private void onAlignRight() {
        applyAlignment("right");
    }

    @FXML
    private void onToggleEdit() {
        editor.setEditable(toggleEdit.isSelected());
    }

    private void markDirty() {
        dirty = true;
        lblSaved.setText("● Unsaved");
        updateCounts();
    }

    private void clearDirty() {
        dirty = false;
        lblSaved.setText("Saved");
        updateCounts();
    }

    private boolean confirmLoseChanges() {
        if (!dirty) return true;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Discard unsaved changes?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Unsaved changes");
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void updateCounts() {
        String text = editor.getText();
        lblChars.setText("Chars: " + text.length());
        lblWords.setText("Words: " + (text.isBlank() ? 0 : text.trim().split("\\s+").length));
    }

    private void updateCaret() {
        int caret = editor.getCaretPosition();
        String upToCaret = editor.getText().substring(0, Math.min(caret, editor.getLength()));
        int line = upToCaret.split("\n", -1).length;
        int col = upToCaret.length() - upToCaret.lastIndexOf('\n');
        lblCursor.setText("Ln " + line + ", Col " + col);
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    private String toRgbString(Color c) {
        return "rgb(" + (int) (c.getRed() * 255) + "," + (int) (c.getGreen() * 255) + "," + (int) (c.getBlue() * 255) + ")";
    }

    private int parseDifficulty() {
        try {
            return Integer.parseInt(cmbDifficulty.getValue());
        } catch (Exception e) {
            return 1;
        }
    }

    public void onPrev(ActionEvent actionEvent) {
        if (!backStack.isEmpty()) {
            // Move back in the history stack
            Note prevNote = backStack.pop();
            loadNoteInEditor(prevNote, false);
            currentNote = prevNote;

            // Button logic:
            // If backStack is still not empty, keep Prev enabled and Next disabled
            // If backStack is empty now, enable both Prev and Next to navigate linked list
            btnPrev.setDisable(backStack.isEmpty() && (studyPlanCursor == null || !studyPlanCursor.canPrev()));
            btnNext.setDisable(!backStack.isEmpty());

            // Update highlight
            renderStudyPlan();
        } else if ("Study Plan".equals(cmbMode.getValue()) && studyPlanCursor != null) {
            // Navigate linked list normally
            if (studyPlanCursor.canPrev()) {
                currentNote = studyPlanCursor.movePrev();
                loadNoteInEditor(currentNote, false);
                renderStudyPlan();

                btnPrev.setDisable(!studyPlanCursor.canPrev());
                btnNext.setDisable(!studyPlanCursor.canNext());
            }
        }
    }

    public void onNext(ActionEvent actionEvent) {
        if (inRevisionMode) {
            Note nextNote = revisionController.dequeueNextNote();
            if (nextNote != null) {
                loadNoteInEditor(nextNote, false);
                currentNote = nextNote;

                btnPrev.setDisable(true);
                btnNext.setDisable(!revisionController.hasNotes());
            }
        } else if ("Study Path".equals(cmbMode.getValue())) {
            Note nextNote = studyPathManager.dequeueNextNote();
            if (nextNote != null) {
                loadNoteInEditor(nextNote, false);
                currentNote = nextNote;

                btnPrev.setDisable(true);
                btnNext.setDisable(!studyPathManager.hasNotes());
            }
        } else if ("Study Plan".equals(cmbMode.getValue()) && studyPlanCursor != null) {
            if (!backStack.isEmpty()) {
                // Forward from backStack: disabled because user is still moving back in history
                Note nextNote = backStack.peek(); // peek but don't pop yet
                loadNoteInEditor(nextNote, false);
                currentNote = nextNote;

                btnPrev.setDisable(false);
                btnNext.setDisable(true); // keep next disabled until backStack is empty
                renderStudyPlan();
            } else if (studyPlanCursor.canNext()) {
                // Move forward in linked list
                currentNote = studyPlanCursor.moveNext();
                loadNoteInEditor(currentNote, false);
                renderStudyPlan();

                btnPrev.setDisable(!studyPlanCursor.canPrev());
                btnNext.setDisable(!studyPlanCursor.canNext());
            }
        }
    }

    private void loadSessionHistoryFromDB() {
        String sortMode = cmbSessionSort.getValue();
        if (sortMode == null) sortMode = "Newest";

        try {
            List<Note> historyNotes = noteDao.getSessionHistory(sortMode);
            sessionHistoryList.setAll(historyNotes);

            // Always reset to show all items
            cmbSessionHistory.setItems(sessionHistoryList);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Load Error", "Could not load session history: " + e.getMessage());
        }
    }




    @FXML
    private void onLoadHistory() {
        Object selected = cmbSessionHistory.getValue();

        Note note = null;

        if (selected instanceof Note) {
            note = (Note) selected;
        } else if (selected instanceof String) {
            String searchText = (String) selected;
            note = sessionHistoryList.stream()
                    .filter(n -> n != null && n.getTitle().equalsIgnoreCase(searchText.trim()))
                    .findFirst()
                    .orElse(null);
        }

        if (note == null) {
            // Try from editor text
            String editorText = cmbSessionHistory.getEditor().getText();
            if (editorText != null && !editorText.trim().isEmpty()) {
                note = sessionHistoryList.stream()
                        .filter(n -> n != null && n.getTitle().equalsIgnoreCase(editorText.trim()))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (note == null) {
            showError("No Selection", "Please select a valid note from the history list.");
            return;
        }

        loadSelectedNote(note);
    }

    private void loadSelectedNote(Note selected) {
        if (selected == null) return;

        // Push current note to history for backtracking
        if (currentNote != null) {
            history.push(currentNote);
        }

        // Load the selected note directly
        loadNoteInEditor(selected, false);

        // Update current note reference
        currentNote = selected;

        // Increment usage count and update session
        try {
            noteDao.incrementUsageCount(selected.getId());
            updateSession(selected); // This will refresh the history list
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Database error", "Could not update usage count.");
        }

        // Clear the editor text after successful load
        Platform.runLater(() -> cmbSessionHistory.getEditor().clear());
    }
    private void saveSession(Note note) {
        try {
            noteDao.saveSession(note); // define in NoteDao
            loadSessionHistoryFromDB(); // refresh dropdown
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSession(Note note) {
        try {
            noteDao.updateSession(note);
            loadSessionHistoryFromDB();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupSuggestionPopup() {
        suggestionListView.setCellFactory(lv -> new ListCell<Note>() {
            @Override
            protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });

        suggestionListView.setOnMouseClicked(event -> {
            Note selected = suggestionListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                cmbSessionHistory.getSelectionModel().select(selected);
                suggestionPopup.hide();
            }
        });

        suggestionPopup.getContent().add(suggestionListView);
        suggestionPopup.setAutoHide(true);
    }
    private void filterAndShowSuggestions(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            suggestionPopup.hide();
            return;
        }

        String filter = filterText.toLowerCase();
        ObservableList<Note> filtered = sessionHistoryList.filtered(note ->
                note != null && note.getTitle().toLowerCase().contains(filter)
        );

        if (filtered.isEmpty()) {
            suggestionPopup.hide();
            return;
        }

        suggestionListView.setItems(filtered);
        suggestionListView.setPrefWidth(cmbSessionHistory.getWidth());

        // Position the popup below the combobox
        if (!suggestionPopup.isShowing()) {
            Bounds bounds = cmbSessionHistory.localToScreen(cmbSessionHistory.getBoundsInLocal());
            suggestionPopup.show(cmbSessionHistory.getScene().getWindow(),
                    bounds.getMinX(), bounds.getMaxY());
        }


    }

    @FXML
    private void onAddToStudyPlan() {
        if (currentNote != null && currentNote.getTitle() != null && !currentNote.getTitle().isBlank()) {
            studyPlanManager.addNote(currentNote);
            refreshStudyPlanCombo(); // update ComboBox
        }
    }

//    @FXML
//    private void onRemoveFromStudyPlan() {
//        Note selected = cmbStudyPlan.getSelectionModel().getSelectedItem();
//        if (selected != null) {
//            studyPlanManager.removeNote(selected);
//            refreshStudyPlanCombo(); // update ComboBox
//        }
//    }

    private void refreshStudyPlanCombo() {
//        cmbStudyPlan.setItems(FXCollections.observableArrayList(studyPlanManager.getPlan()));
    }
    private void loadNoteInEditor(Note note, boolean pushToHistory) {
        if (note == null) return;

        if (pushToHistory && currentNote != null) {
            history.push(currentNote); // only push when explicitly loading new note
        }

        currentNote = note;
        currentFile = (note.getFilePath() != null && !note.getFilePath().isBlank()) ? new File(note.getFilePath()) : null;

        if (currentFile != null && currentFile.exists()) {
            try {
                NoteXmlUtil.load(note, editor, currentFile);
            } catch (Exception e) {
                showError("Load Failed", e.getMessage());
            }
        } else {
            editor.clear();
        }

        lblTitle.setText(note.getTitle());
        txtTitle.setText(note.getTitle());
        cmbDifficulty.getSelectionModel().select(String.valueOf(note.getDifficulty()));
        keywordRanges.clear();
        markKeywords();
        clearDirty();

        // Update Last Studied label if available
        if (note.getLastStudied() != null) {
            lblLastStudied.setText("Last Studied: " + note.getLastStudied().toString());
        } else {
            lblLastStudied.setText("Last Studied: Never");
        }
    }

    private void renderStudyPlan() {
        vboxNotes.getChildren().clear(); // Clear previous content
        List<Note> planNotes = studyPlanManager.getPlan();

        // Top "+" button (optional)
        addPlusButton(0);

        for (int i = 0; i < planNotes.size(); i++) {
            Note note = planNotes.get(i);

            HBox noteBox = new HBox(10);
            noteBox.setAlignment(Pos.CENTER_LEFT);
            noteBox.setPadding(new Insets(5, 10, 5, 10));
            noteBox.setStyle("-fx-background-radius: 4;");

            Label lblNote = new Label(note.getTitle());
            lblNote.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lblNote, Priority.ALWAYS);
            lblNote.setMouseTransparent(true); // make label non-clickable

            // Highlight the currently loaded note
            if (currentNote != null && note.equals(currentNote)) {
                noteBox.setStyle("-fx-background-color: #cce5ff; -fx-background-radius: 4;");
            } else {
                noteBox.setStyle("-fx-background-radius: 4;"); // normal
            }

            // Optional remove button
            Button btnRemove = new Button("×");
            btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: red;");
            btnRemove.setOnAction(e -> {
                studyPlanManager.removeNote(note);
                // Update cursor after removal
                studyPlanCursor = studyPlanManager.getPlanCursor();
                if (!studyPlanManager.getPlan().isEmpty()) {
                    // keep current note valid
                    if (!studyPlanManager.getPlan().contains(currentNote)) {
                        currentNote = studyPlanCursor.current();
                        loadNoteInEditor(currentNote, false);
                    }
                } else {
                    currentNote = null;
                    editor.clear();
                    studyPlanCursor = null;
                }
                renderStudyPlan();
            });

            noteBox.getChildren().addAll(lblNote, btnRemove);
            vboxNotes.getChildren().add(noteBox);

            // "+" button below this note (optional)
            addPlusButton(i + 1);
        }
    }

    // Helper to add a "+" button at a given index
    private void addPlusButton(int index) {
        Button btnAdd = new Button("+");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAdd, Priority.ALWAYS);

        btnAdd.setOnAction(e -> {
            if (currentNote != null) {
                studyPlanManager.insertNoteAt(currentNote, index);
                renderStudyPlan();
            }
        });

        HBox btnBox = new HBox(btnAdd);
        btnBox.setAlignment(Pos.CENTER);
        vboxNotes.getChildren().add(btnBox);
    }

    @FXML
    private void onMarkStudied() {
        if (currentNote != null) {
            currentNote.setLastStudied(LocalDateTime.now());
            lblLastStudied.setText(currentNote.getLastStudied().toString());

            // Also enqueue for revision
            revisionController.enqueueNoteForRevision(currentNote);

            showTemporaryMessage("Note added to revision queue");

            if("Revision Mode".equals(cmbMode.getValue())) {
                if (revisionController.peekNextNote() != null) {
                    onNext(new ActionEvent());
                }
            } else if ("Study Plan".equals(cmbMode.getValue())) {
                if (studyPlanCursor != null) {
                    onNext(new ActionEvent());
                }
            } else if ("Study Path".equals(cmbMode.getValue())) {
                if (studyPathManager.peekNextNote() != null) {
                    onNext(new ActionEvent());
                }
            }
        }
    }

    // Optional: clear all highlights from note HBoxes
    private void clearAllHighlights() {
        for (Node node : vboxNotes.getChildren()) {
            if (node instanceof HBox hbox) {
                // Only clear HBox if it contains a Label (skip "+" buttons)
                boolean hasLabel = hbox.getChildren().stream().anyMatch(n -> n instanceof Label);
                if (hasLabel) {
                    hbox.setStyle("-fx-background-radius: 4;");
                }
            }
        }
    }

    private void showTemporaryMessage(String message) {
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: #333; -fx-text-fill: white; "
                + "-fx-padding: 6 12; -fx-background-radius: 6;");
        toast.setOpacity(0);

        toastContainer.getChildren().setAll(toast); // replace old toasts
        toastContainer.setVisible(true);
        toastContainer.setManaged(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            toastContainer.setVisible(false);
            toastContainer.setManaged(false);
            toastContainer.getChildren().clear();
        });

        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }
}