package org.mindgraph.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.InlineCssTextArea;
import org.mindgraph.db.NoteDao;
import org.mindgraph.model.Note;
import org.mindgraph.util.KeywordExtractor;
import org.mindgraph.model.NoteEntry;
import org.mindgraph.util.NoteXmlUtil;
import org.mindgraph.datastructure.Stack;
import org.mindgraph.model.Note;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotepadController {

    private Stack history = new Stack();  // stack of opened notes
    private final GraphController graphController = new GraphController("graph.dat");

    // --- FXML Fields ---
    @FXML private StackPane editorHost;
    @FXML private ToggleButton toggleEdit;
    @FXML private Button btnNew, btnOpen, btnSave;
    @FXML private Button btnBold, btnItalic, btnUnderline;
    @FXML private Button btnAlignLeft, btnAlignCenter, btnAlignRight;
    @FXML private ColorPicker cpTextColor, cpHighlight;
    @FXML private ComboBox<String> cmbFontFamily;
    @FXML private Spinner<Integer> spinnerFont;
    @FXML private Label lblSaved, lblTitle, lblCursor, lblWords, lblChars;
    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> cmbDifficulty;
    @FXML private ComboBox<String> cmbMode;
    @FXML private Button btnAddImage, btnFind, btnRep;

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

    @FXML
    public void initialize() {
        editor = new InlineCssTextArea();
        editor.setWrapText(true);
        editorHost.getChildren().add(editor);
        editor.setEditable(true);

        cmbFontFamily.setItems(FXCollections.observableArrayList(
                "System","Arial","Verdana","Tahoma","Times New Roman","Courier New","Georgia"
        ));
        cmbFontFamily.getSelectionModel().select("System");

        cmbDifficulty.setItems(FXCollections.observableArrayList("1","2","3","4","5"));
        cmbDifficulty.getSelectionModel().select("1");

        cmbMode.setItems(FXCollections.observableArrayList(
                "Concept Map",        //Graph
                "Backtrack Mode",         //tack
                "Revision",               //Queue
                "Session History"         //Linked List
        ));
        cmbMode.getSelectionModel().selectFirst(); // default selection

        // Font size
        spinnerFont.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8,72,14));

        editor.textProperty().addListener((obs, ov, nv) -> markDirty());
        editor.caretPositionProperty().addListener((obs, ov, nv) -> updateCaret());

        editor.sceneProperty().addListener((obs, old, scene) -> {
            if(scene == null) return;
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSave);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::onNew);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::onOpen);
        });

        spinnerFont.valueProperty().addListener((obs, ov, nv) -> applyFontSize(nv));
        cmbFontFamily.valueProperty().addListener((obs, ov, nv) -> applyFontFamily(nv));
        if(cpTextColor != null) cpTextColor.setOnAction(e -> applyTextColor(cpTextColor.getValue()));
        if(cpHighlight != null) cpHighlight.setOnAction(e -> applyHighlightColor(cpHighlight.getValue()));

        btnBold.setOnAction(e -> toggleStyle(BOLD));
        btnItalic.setOnAction(e -> toggleStyle(ITALIC));
        btnUnderline.setOnAction(e -> toggleStyle(UNDERLINE));

        lblTitle.setText(currentNote.getTitle());
        txtTitle.setText(currentNote.getTitle());
        updateCounts();
        updateCaret();

        // Handle clicks on keyword ranges
        editor.setOnMouseClicked(event -> {
            int pos = editor.getCaretPosition();
            for (KeywordRange kr : keywordRanges) {
                if (pos >= kr.start && pos < kr.end) {
                    String[] parts = kr.keyword.split("\\|\\|");
                    String keyword = parts[0];
                    int linkedNoteId = Integer.parseInt(parts[1]);

                    // get the linked note from the graph
                    Note linkedNote = graphController.getGraph().getGraphNodes()
                            .keySet()
                            .stream()
                            .filter(n -> n.getId() == linkedNoteId)
                            .findFirst()
                            .orElse(null);

                    if (linkedNote != null) {
                        // directly load note into editor
                        loadNoteInEditor(linkedNote, true);
                    }
                    break;
                }
            }
        });

        // Mouse move handler for changing cursor over links
        editor.setOnMouseMoved(event -> {
            int pos = editor.hit(event.getX(), event.getY()).getInsertionIndex(); // char under mouse
            boolean overLink = false;

            for (KeywordRange kr : keywordRanges) {
                if (pos >= kr.start && pos < kr.end) {
                    overLink = true;
                    break;
                }
            }

            if (overLink) {
                editor.setCursor(javafx.scene.Cursor.HAND);
            } else {
                editor.setCursor(javafx.scene.Cursor.TEXT);
            }
        });

        Platform.runLater(() -> {
            try {
                graphController.buildGraphFromDb(false); // build graph without resetting
                System.out.println("Graph built with " + graphController.getGraph().getGraphNodes().size() + " nodes.");
            } catch (SQLException e) {
                showError("Graph Initialization Failed", e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void showKeywordAlert(String noteTitle, String keyword){
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Note Title: " + noteTitle + "\nKeyword: " + keyword,
                ButtonType.OK);
        alert.setHeaderText("Keyword Clicked");
        alert.showAndWait();
    }

    @FXML
    public void onNew() {
        if(!confirmLoseChanges()) return;
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
            history.push(currentNote);  // just the Note object now
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
                showError("Open failed", ex.getMessage());
                ex.printStackTrace();
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
                f = fc.showSaveDialog(editor.getScene().getWindow());
                if (f == null) return;
                currentFile = f;
            }

            currentNote.setTitle(txtTitle.getText());
            currentNote.setDifficulty(parseDifficulty());
            if (currentNote.getCreatedAt() == null) currentNote.setCreatedAt(LocalDateTime.now());
            currentNote.setUpdatedAt(LocalDateTime.now());

            // --- Keyword extraction & selection ---
            List<String> extractedKeywords = KeywordExtractor.extractKeywords(editor.getText());
            List<String> selectedKeywords = showKeywordSelectionDialog(extractedKeywords);
            if (selectedKeywords == null) selectedKeywords = List.of();
            currentNote.setKeywords(selectedKeywords);

            // --- Save note ---
            NoteXmlUtil.save(currentNote, editor, f);
            noteDao.upsert(currentNote, f.getAbsolutePath());

            // --- Refresh editor ---
            NoteXmlUtil.load(currentNote, editor, f);
            keywordRanges.clear();
            markKeywords();

            lblTitle.setText(txtTitle.getText());
            clearDirty();

            // --- Update the graph ---
            try {
                graphController.buildGraphFromDb(true); // rebuild entire graph
            } catch (SQLException e) {
                showError("Graph Update Failed", e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception ex) {
            showError("Save failed", ex.getMessage());
            ex.printStackTrace();
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
            if(index >= 0){
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

    private void loadFile(File f) {
        try {
            Note note = new Note();
            NoteXmlUtil.load(note, editor, f);

            currentNote = note;
            currentFile = f;

            lblTitle.setText(note.getTitle());
            txtTitle.setText(note.getTitle());

            // Push note to history for backtracking
            history.push(note);

            clearDirty();
        } catch (Exception ex) {
            showError("Load failed", ex.getMessage());
            ex.printStackTrace();
        }
    }


    // --- Styling helpers ---
    private void toggleStyle(String css){ appendStyle(css); }
    private void applyFontSize(int size){ appendStyle("-fx-font-size:" + size + "px;"); }
    private void applyFontFamily(String family){ appendStyle("-fx-font-family:'" + family + "';"); }
    private void applyTextColor(Color color){ appendStyle("-fx-fill:" + toRgbString(color) + ";"); }
    private void applyHighlightColor(Color color){ appendStyle("-fx-background-color:" + toRgbString(color) + ";"); }

    private void appendStyle(String css){
        int start = editor.getSelection().getStart();
        int end = editor.getSelection().getEnd();
        if(start == end) return;
        for(int i = start; i < end; i++){
            String current = editor.getStyleOfChar(i);
            editor.setStyle(i, i+1, mergeStyle(current, css));
        }
        markDirty();
    }

    private String mergeStyle(String current, String newCss){
        if(current == null) current = "";
        if(newCss.contains("-fx-font-weight:")) current = current.replaceAll("-fx-font-weight:[^;]+;", "");
        if(newCss.contains("-fx-font-style:")) current = current.replaceAll("-fx-font-style:[^;]+;", "");
        if(newCss.contains("-fx-underline:")) current = current.replaceAll("-fx-underline:[^;]+;", "");
        if(newCss.contains("-fx-font-size:")) current = current.replaceAll("-fx-font-size:[^;]+;", "");
        if(newCss.contains("-fx-font-family:")) current = current.replaceAll("-fx-font-family:[^;]+;", "");
        if(newCss.contains("-fx-fill:")) current = current.replaceAll("-fx-fill:[^;]+;", "");
        if(newCss.contains("-fx-background-color:")) current = current.replaceAll("-fx-background-color:[^;]+;", "");
        return current + newCss;
    }

    private void applyAlignment(String align){
        int pIndex = editor.getCurrentParagraph();
        editor.setParagraphStyle(pIndex, "-fx-text-alignment:" + align + ";");
        markDirty();
    }

    private List<String> showKeywordSelectionDialog(List<String> keywords){
        if(keywords == null || keywords.isEmpty()) return List.of();
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
        for(int i = 0; i < keywords.size(); i++){
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
            if(button == okButtonType){
                List<String> selected = new ArrayList<>();
                for(CheckBox cb : checkBoxes){
                    if(cb.isSelected()) selected.add(cb.getText());
                }
                return selected;
            }
            return null;
        });

        return dialog.showAndWait().orElse(List.of());
    }

    // --- Misc helpers ---
    @FXML private void onBold() { toggleStyle(BOLD); }
    @FXML private void onItalic() { toggleStyle(ITALIC); }
    @FXML private void onUnderline() { toggleStyle(UNDERLINE); }

    @FXML private void onAlignLeft() { applyAlignment("left"); }
    @FXML private void onAlignCenter() { applyAlignment("center"); }
    @FXML private void onAlignRight() { applyAlignment("right"); }

    @FXML private void onToggleEdit() { editor.setEditable(toggleEdit.isSelected()); }

    private void markDirty(){ dirty = true; lblSaved.setText("● Unsaved"); updateCounts(); }
    private void clearDirty(){ dirty = false; lblSaved.setText("Saved"); updateCounts(); }

    private boolean confirmLoseChanges(){
        if(!dirty) return true;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,"Discard unsaved changes?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Unsaved changes");
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void updateCounts(){
        String text = editor.getText();
        lblChars.setText("Chars: " + text.length());
        lblWords.setText("Words: " + (text.isBlank() ? 0 : text.trim().split("\\s+").length));
    }

    private void updateCaret(){
        int caret = editor.getCaretPosition();
        String upToCaret = editor.getText().substring(0, Math.min(caret, editor.getLength()));
        int line = upToCaret.split("\n",-1).length;
        int col = upToCaret.length() - upToCaret.lastIndexOf('\n');
        lblCursor.setText("Ln " + line + ", Col " + col);
    }

    private void showError(String title,String msg){
        Alert a = new Alert(Alert.AlertType.ERROR,msg,ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    private String toRgbString(Color c){
        return "rgb(" + (int)(c.getRed()*255) + "," + (int)(c.getGreen()*255) + "," + (int)(c.getBlue()*255) + ")";
    }

    private int parseDifficulty(){
        try { return Integer.parseInt(cmbDifficulty.getValue()); }
        catch (Exception e) { return 1; }
    }

    public void onPrev(ActionEvent actionEvent) {
        if (history.isEmpty()) {
            showError("No history", "No previous notes available.");
            return;
        }

        // Pop previous note and load it
        Note prevNote = history.pop();

        if (prevNote != null) {
            loadNoteInEditor(prevNote, false); // false = don’t push to history again
        }
    }


    public void onNext(ActionEvent actionEvent) {
        if(currentFile!=null){
            loadFile(currentFile);  // later replace with stack.peek()/push
        }
    }

    private void loadNoteInEditor(Note note, boolean pushToHistory) {
        if (note == null) return;

        if (pushToHistory && currentNote != null) {
            history.push(currentNote); // only push when explicitly loading new note
        }

        currentNote = note;
        currentFile = (note.getFilePath() != null && !note.getFilePath().isBlank())
                ? new File(note.getFilePath())
                : null;

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

