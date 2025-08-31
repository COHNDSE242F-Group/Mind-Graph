package org.mindgraph.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.StringConverter;
import org.fxmisc.richtext.InlineCssTextArea;
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
    private final Stack keywordHistory = new Stack();
    private static final File REVISION_PATH_FILE = new File("revisionPath.dat");
    private final Stack revisionBackStack = new Stack(); // tracks notes opened from links in revision
    private boolean inRevisionMode = false;
    private Note currentQueueNote = null;

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


    private ObservableList<Note> sessionHistoryList = FXCollections.observableArrayList();
    private ListView<Note> suggestionListView = new ListView<>();
    private Popup suggestionPopup = new Popup(); //
    private final ObservableList<Note> studyPlanList = FXCollections.observableArrayList();
    ;
    @FXML private ComboBox<String> cmbSessionSort;
    // --- Add ComboBox in your FXML and Controller ---
    @FXML
    private ComboBox<Note> cmbStudyPlan;



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
                "Backtrack Mode",
                "Revision",
                "Session History"
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
                        if (inRevisionMode) {
                            if (currentQueueNote != null && currentQueueNote != linkedNote) {
                                revisionBackStack.push(currentQueueNote);
                            }
                            loadNoteInEditor(linkedNote, false);
                            btnNext.setDisable(true);
                            btnPrev.setDisable(false);
                        } else {
                            loadNoteInEditor(linkedNote, true);
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
            if ("Revision".equals(newMode)) {
                inRevisionMode = true;
                revisionBackStack.clear();

                if (revisionController == null) {
                    revisionController = new RevisionController();
                }

                if (graphController.getGraph() != null) {
                    revisionController.setGraph(graphController.getGraph());
                }

                loadSessionHistoryFromDB();
                cmbSessionHistory.setEditable(true);
                cmbSessionHistory.setItems(sessionHistoryList);

                cmbSessionHistory.setCellFactory(lv -> new ListCell<Note>() {
                    @Override
                    protected void updateItem(Note item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getTitle());
                    }
                });

                cmbSessionHistory.setButtonCell(new ListCell<Note>() {
                    @Override
                    protected void updateItem(Note item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "" : item.getTitle());
                    }
                });

                setupSuggestionPopup();

                cmbSessionHistory.getEditor().textProperty().addListener((obs2, oldVal, newVal) -> {
                    filterAndShowSuggestions(newVal);
                    Note match = sessionHistoryList.stream()
                            .filter(n -> n.getTitle().equalsIgnoreCase(newVal))
                            .findFirst()
                            .orElse(null);
                    cmbSessionHistory.setValue(match);
                });

                cmbSessionHistory.focusedProperty().addListener((obs3, oldVal, newVal) -> {
                    if (!newVal) suggestionPopup.hide();
                });

                cmbSessionHistory.getEditor().setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        onLoadHistory();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        suggestionPopup.hide();
                        event.consume();
                    }
                });

                cmbSessionHistory.getSelectionModel().selectedItemProperty().addListener((obs4, oldVal, newVal) -> {
                    if (newVal != null) {
                        Platform.runLater(() -> cmbSessionHistory.getEditor().setText(newVal.getTitle()));
                    } else {
                        Platform.runLater(() -> cmbSessionHistory.getEditor().clear());
                    }
                });

                cmbSessionHistory.setConverter(new StringConverter<Note>() {
                    @Override
                    public String toString(Note note) {
                        return note == null ? "" : note.getTitle();
                    }

                    @Override
                    public Note fromString(String string) {
                        if (string == null || string.isBlank()) return null;
                        return sessionHistoryList.stream()
                                .filter(n -> n.getTitle().equalsIgnoreCase(string))
                                .findFirst()
                                .orElse(null);
                    }
                });

                cmbSessionSort.setItems(FXCollections.observableArrayList("Newest","Oldest","MostUsed"));
                cmbSessionSort.getSelectionModel().select("Newest");
                cmbSessionSort.valueProperty().addListener((obs5, oldVal, newVal) -> loadSessionHistoryFromDB());

                studyPlanList.setAll(studyPlanManager.getPlan()); // load initial items
                cmbStudyPlan.setItems(studyPlanList); // bind ComboBox to observable list
                cmbStudyPlan.setCellFactory(lv -> new ListCell<Note>() {
                    @Override
                    protected void updateItem(Note item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getTitle());
                    }
                });
                cmbStudyPlan.setButtonCell(new ListCell<Note>() {
                    @Override
                    protected void updateItem(Note item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getTitle());
                    }
                });

                revisionController.prepareNextNote();
                currentQueueNote = revisionController.dequeueNextNote();
                if (currentQueueNote != null) {
                    loadNoteInEditor(currentQueueNote, false);
                    btnPrev.setDisable(true);
                    btnNext.setDisable(!revisionController.hasNotes());
                } else {
                    showError("Revision Empty", "No notes available for revision.");
                    btnPrev.setDisable(true);
                    btnNext.setDisable(true);
                }

            } else {
                inRevisionMode = false;
                btnPrev.setDisable(false);
                btnNext.setDisable(false);
            }

            studyPlanList.setAll(studyPlanManager.getPlan());
            cmbStudyPlan.setItems(studyPlanList);

            cmbStudyPlan.setCellFactory(lv -> new ListCell<Note>() {
                @Override
                protected void updateItem(Note item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitle());
                }
            });
            cmbStudyPlan.setButtonCell(new ListCell<Note>() {
                @Override
                protected void updateItem(Note item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTitle());
                }
            });
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
            if(currentNote == null) currentNote = new Note();
            File f = currentFile;
            if(f == null){
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MindGraph XML","*.xml"));
                f = fc.showSaveDialog(editor.getScene().getWindow());
                if(f == null) return;
                currentFile = f;
            }
            currentNote.setTitle(txtTitle.getText());
            currentNote.setDifficulty(parseDifficulty());
            if(currentNote.getCreatedAt() == null) currentNote.setCreatedAt(LocalDateTime.now());
            currentNote.setUpdatedAt(LocalDateTime.now());

            List<String> extractedKeywords = KeywordExtractor.extractKeywords(editor.getText());
            List<String> selectedKeywords = showKeywordSelectionDialog(extractedKeywords);
            if(selectedKeywords == null) selectedKeywords = List.of();
            currentNote.setKeywords(selectedKeywords);

            NoteXmlUtil.save(currentNote, editor, f);
            noteDao.upsert(currentNote, f.getAbsolutePath());

            // Refresh editor
            NoteXmlUtil.load(currentNote, editor, f);
            keywordRanges.clear();
            markKeywords();

            lblTitle.setText(txtTitle.getText());
            clearDirty();

            currentNote.setFilePath(f.getAbsolutePath()); // ensure path is saved
            noteDao.upsert(currentNote, f.getAbsolutePath());

            updateSession(currentNote);

        } catch(Exception ex){

            // --- Update the graph ---
            try {
                graphController.buildGraphFromDb(true); // rebuild entire graph
            } catch (SQLException e) {
                showError("Graph Update Failed", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void markKeywords() {
        keywordRanges.clear();
        if (currentNote == null) return;

        String content = editor.getText();

        // Get neighbors of the current note
        List<Note> neighbors = graphController.getGraph().getNeighbours(currentNote);
        if (neighbors == null || neighbors.isEmpty()) return;

        for (Note neighbor : neighbors) {
            String neighborTitle = neighbor.getTitle();
            if (neighborTitle == null || neighborTitle.isBlank()) continue;

            // Regex for case-insensitive + optional plural ('s' or 'es')
            String regex = "\\b" + neighborTitle + "(?:s|es)?\\b";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();

                // Add to keywordRanges with noteId
                keywordRanges.add(new KeywordRange(start, end, neighborTitle + "||" + neighbor.getId()));

                // Apply link style
                editor.setStyle(start, end, mergeStyle(
                        editor.getStyleOfChar(start),
                        "-fx-fill: blue; -fx-underline: true;"
                ));
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

        if (newCss.contains("-fx-font-weight:")) current = current.replaceAll("-fx-font-weight:[^;]+;", "");
        if (newCss.contains("-fx-font-style:")) current = current.replaceAll("-fx-font-style:[^;]+;", "");
        if (newCss.contains("-fx-underline:")) current = current.replaceAll("-fx-underline:[^;]+;", "");
        if (newCss.contains("-fx-font-size:")) current = current.replaceAll("-fx-font-size:[^;]+;", "");
        if (newCss.contains("-fx-font-family:")) current = current.replaceAll("-fx-font-family:[^;]+;", "");
        if (newCss.contains("-fx-fill:")) current = current.replaceAll("-fx-fill:[^;]+;", "");
        if (newCss.contains("-fx-background-color:")) current = current.replaceAll("-fx-background-color:[^;]+;", "");

        return current + newCss;
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
        if (!inRevisionMode) {
            // Normal backtracking from history
            if (history.isEmpty()) {
                showError("No history", "No previous notes available.");
                return;
            }
            Note prevNote = history.pop();
            if (prevNote != null) loadNoteInEditor(prevNote, false);
            return;
        }

        if (revisionBackStack.isEmpty()) {
            // Already at original queue note
            btnPrev.setDisable(true);
            btnNext.setDisable(revisionController.peekNextNote() != null);
            return;
        }

        // Pop last opened note (via keyword link)
        Note prev = revisionBackStack.pop();
        loadNoteInEditor(prev, false);

        // Update buttons
        btnPrev.setDisable(revisionBackStack.isEmpty());
        btnNext.setDisable(false);
    }

    public void onNext(ActionEvent actionEvent) {
        if (inRevisionMode) {
            Note nextNote = revisionController.dequeueNextNote();
            if (nextNote != null) {
                currentQueueNote = nextNote;
                loadNoteInEditor(nextNote, false);
            } else {
                showError("Revision Complete", "No more notes left in the revision path!");
            }
        } else {
            showError("Not Available", "Next is only available in Revision Mode.");
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

        // Increment usage count safely
        try {
            noteDao.incrementUsageCount(selected.getId());
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Database error", "Could not update usage count.");
        }

        // Clear the editor text after successful load in session history combo
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

    @FXML
    private void onRemoveFromStudyPlan() {
        Note selected = cmbStudyPlan.getSelectionModel().getSelectedItem();
        if (selected != null) {
            studyPlanManager.removeNote(selected);
            refreshStudyPlanCombo(); // update ComboBox
        }
    }

    private void refreshStudyPlanCombo() {
        cmbStudyPlan.setItems(FXCollections.observableArrayList(studyPlanManager.getPlan()));
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
    }
}