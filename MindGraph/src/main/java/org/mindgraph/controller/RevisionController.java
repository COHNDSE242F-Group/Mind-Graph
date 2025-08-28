package org.mindgraph.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.mindgraph.datastructure.Queue;
import org.mindgraph.model.Note;
import org.mindgraph.util.NoteXmlUtil;

import java.io.File;

public class RevisionController {

    @FXML private StackPane revisionHost;
    @FXML private Label lblRevisionTitle;

    private InlineCssTextArea revisionArea;
    private Queue revisionQueue;

    @FXML
    public void initialize() {
        revisionArea = new InlineCssTextArea();
        revisionArea.setWrapText(true);
        revisionArea.setEditable(false);
        revisionHost.getChildren().add(revisionArea);

        revisionQueue = new Queue();
        loadNotesForRevision();
        showNextNote();
    }

    private void loadNotesForRevision() {
        File folder = new File("notes");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".rnote"));

        if (files != null) {
            for (File f : files) {
                try {
                    Note note = new Note();
                    NoteXmlUtil.load(note, revisionArea, f);

                    // Only queue medium+ difficulty notes
                    if (note.getDifficulty() >= 2) {
                        revisionQueue.enqueue(note);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    public void showNextNote() {
        Note next = revisionQueue.dequeue();
        if (next != null) {
            revisionArea.clear();
            lblRevisionTitle.setText(next.getTitle());
            revisionArea.replaceText(next.getContentMarkup()); // Show content directly
        } else {
            lblRevisionTitle.setText("No more notes to revise!");
            revisionArea.clear();
        }
    }
}
