package org.mindgraph.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.InlineCssTextArea;
import org.mindgraph.model.Note;
import org.mindgraph.util.NoteXmlUtil;

import java.io.File;

public class NotepadController {

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

    // Toolbar additional fields
    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> cmbDifficulty;
    @FXML private ComboBox<String> cmbMode;
    @FXML private Button btnAddImage, btnFind, btnRep;

    // --- Editor ---
    private InlineCssTextArea editor;
    private boolean dirty = false;
    private File currentFile = null;

    // --- Styles ---
    private static final String BOLD = "-fx-font-weight:bold;";
    private static final String ITALIC = "-fx-font-style:italic;";
    private static final String UNDERLINE = "-fx-underline:true;";

    @FXML
    public void initialize() {
        // Initialize editor
        editor = new InlineCssTextArea();
        editor.setWrapText(true);
        editorHost.getChildren().add(editor);
        editor.setEditable(true);

        // Font families
        cmbFontFamily.setItems(FXCollections.observableArrayList(
                "System","Arial","Verdana","Tahoma","Times New Roman","Courier New","Georgia"
        ));
        cmbFontFamily.getSelectionModel().select("System");

        cmbMode.setItems(FXCollections.observableArrayList(
                "Concept Map",        //Graph
                "Backtrack Mode",         //tack
                "Revision",               //Queue
                "Session History"         //Linked List
        ));
        cmbMode.getSelectionModel().selectFirst(); // default selection

        // Font size
        spinnerFont.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8,72,14));

        // Track changes
        editor.textProperty().addListener((obs, oldV, newV) -> markDirty());
        editor.caretPositionProperty().addListener((obs, oldV, newV) -> updateCaret());

        // Keyboard shortcuts
        editor.sceneProperty().addListener((obs, old, scene) -> {
            if(scene == null) return;
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::onSave);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::onNew);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::onOpen);
        });

        // Toolbar listeners
        spinnerFont.valueProperty().addListener((obs, oldV, newV) -> applyFontSize(newV));
        cmbFontFamily.valueProperty().addListener((obs, oldV, newV) -> applyFontFamily(newV));
        if(cpTextColor != null) cpTextColor.setOnAction(e -> applyTextColor(cpTextColor.getValue()));
        if(cpHighlight != null) cpHighlight.setOnAction(e -> applyHighlightColor(cpHighlight.getValue()));

        btnBold.setOnAction(e -> toggleStyle(BOLD));
        btnItalic.setOnAction(e -> toggleStyle(ITALIC));
        btnUnderline.setOnAction(e -> toggleStyle(UNDERLINE));

        // Default values
        lblTitle.setText("Untitled");
        txtTitle.setText("Untitled");
        updateCounts();
        updateCaret();



    }

    // --- File Operations ---
    @FXML public void onNew() {
        if(!confirmLoseChanges()) return;
        editor.clear();
        currentFile = null;
        lblTitle.setText("Untitled");
        txtTitle.setText("Untitled");
        clearDirty();
    }

    @FXML public void onOpen() {
        if(!confirmLoseChanges()) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("RichText Note","*.rnote"));
        File f = fc.showOpenDialog(editor.getScene().getWindow());
        if(f != null){
            loadFile(f);   // call helper
        }
    }


    @FXML public void onSave() {
        try {
            File f = currentFile;
            if(f == null){
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("RichText Note","*.rnote"));
                f = fc.showSaveDialog(editor.getScene().getWindow());
                if(f == null) return;
                currentFile = f;
            }

            Note note = new Note(txtTitle.getText(), "");
            NoteXmlUtil.save(note, editor, f);
            lblTitle.setText(txtTitle.getText());
            clearDirty();
        } catch(Exception ex) {
            showError("Save failed", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadFile(File f) {
        try {
            Note note = new Note();
            NoteXmlUtil.load(note, editor, f);
            currentFile = f;
            lblTitle.setText(f.getName());
            txtTitle.setText(f.getName());
            clearDirty();
        } catch (Exception ex) {
            showError("Load failed", ex.getMessage());
            ex.printStackTrace();
        }
    }


    // --- Styling helpers ---
    private void toggleStyle(String css) {
        int start = editor.getSelection().getStart();
        int end = editor.getSelection().getEnd();
        if(start == end) return;

        String current = editor.getStyleOfChar(start);
        if(current.contains(css)) editor.setStyle(start, end, current.replace(css,""));
        else editor.setStyle(start, end, current + css);

        markDirty();
    }

    private void applyFontSize(int size) {
        int start = editor.getSelection().getStart();
        int end = editor.getSelection().getEnd();
        if(start == end) return;
        editor.setStyle(start, end, "-fx-font-size:" + size + "px;");
        markDirty();
    }

    private void applyFontFamily(String family){
        int start = editor.getSelection().getStart();
        int end = editor.getSelection().getEnd();
        if(start == end) return;
        editor.setStyle(start, end, "-fx-font-family:'" + family + "';");
        markDirty();
    }

    private void applyTextColor(Color color){
        int start = editor.getSelection().getStart();
        int end = editor.getSelection().getEnd();
        if(start == end) return;
        editor.setStyle(start, end, "-fx-fill:" + toRgbString(color) + ";");
        markDirty();
    }

    private void applyHighlightColor(Color color){
        int start = editor.getSelection().getStart();
        int end = editor.getSelection().getEnd();
        if(start == end) return;
        editor.setStyle(start, end, "-fx-background-color:" + toRgbString(color) + ";");
        markDirty();
    }

    private void applyAlignment(String align){
        int pIndex = editor.getCurrentParagraph();
        editor.setParagraphStyle(pIndex, "-fx-text-alignment:" + align + ";");
        markDirty();
    }

    // --- FXML Event Handlers ---
    @FXML private void onBold() { toggleStyle(BOLD); }
    @FXML private void onItalic() { toggleStyle(ITALIC); }
    @FXML private void onUnderline() { toggleStyle(UNDERLINE); }

    @FXML private void onAlignLeft() { applyAlignment("left"); }
    @FXML private void onAlignCenter() { applyAlignment("center"); }
    @FXML private void onAlignRight() { applyAlignment("right"); }

    @FXML private void onAddImage() { /* TODO: implement image insertion */ }
    @FXML private void onToggleEdit() { editor.setEditable(toggleEdit.isSelected()); }
    @FXML private void onFind() { /* TODO: implement find */ }
    @FXML private void onReplace() { /* TODO: implement replace */ }

    // --- Helper methods ---
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

    public void onPrev(ActionEvent actionEvent) {
        if(currentFile != null){
            loadFile(currentFile);  // later replace with stack.pop()
        }
    }


    public void onNext(ActionEvent actionEvent) {
        if(currentFile!=null){
            loadFile(currentFile);  // later replace with stack.peek()/push
        }
    }

}

