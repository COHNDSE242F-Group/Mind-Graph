package org.mindgraph.controller;

import org.fxmisc.richtext.InlineCssTextArea;
import org.mindgraph.datastructure.Graph;
import org.mindgraph.datastructure.Queue;
import org.mindgraph.model.Note;
import org.mindgraph.util.NoteXmlUtil;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * Handles the Revision Mode queue for notes.
 */
public class RevisionController implements Serializable {
    private static final long serialVersionUID = 1L;
    private Queue revisionQueue;
    private Graph graph; // Reference to graph-based study path
    private static final File PATH_FILE = new File("revisionPath.dat");

    public RevisionController() {
        // Load saved queue or create new
        revisionQueue = PATH_FILE.exists() ? loadSavedQueue(PATH_FILE) : new Queue();
        if (revisionQueue == null) revisionQueue = new Queue();
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Returns true if there are notes in the revision queue.
     */
    public boolean hasNotes() {
        return revisionQueue != null && !revisionQueue.isEmpty();
    }

    /**
     * Peek at the next note without dequeuing.
     */
    public Note peekNextNote() {
        if (!hasNotes()) return null;
        return (Note) revisionQueue.peek();
    }

    /**
     * Dequeue the next note from the revision queue.
     */
    public Note dequeueNextNote() {
        if (!hasNotes()) return null;
        Note next = (Note) revisionQueue.dequeue();
        saveQueue(revisionQueue, PATH_FILE);
        return next;
    }

    /**
     * Add a note to the revision queue.
     */
    public void enqueueNoteForRevision(Note note) {
        if (note == null || note.getFilePath() == null) return;
        if (revisionQueue == null) revisionQueue = new Queue();
        revisionQueue.enqueue(note);
        saveQueue(revisionQueue, PATH_FILE);
    }

    /**
     * Prepare the revision queue, either from the graph or fallback folder scan.
     */
    public void prepareNextNote() {
        if (hasNotes()) return;
        buildRevisionQueue();
    }

    // ---------------- Core Queue Builder ----------------
    private void buildRevisionQueue() {
        revisionQueue.clear();

        // Use graph if available
        if (graph != null && !graph.getGraphNodes().isEmpty()) {
            Note startNote = graph.getGraphNodes().keySet().stream()
                    .min(Comparator.comparingInt(Note::getDifficulty))
                    .orElse(null);

            if (startNote != null) {
                List<Note> path = graph.getStudyPath(startNote);
                for (Note note : path) revisionQueue.enqueue(note);
            }
        }

        // Fallback: load from notes folder if empty
        if (revisionQueue.isEmpty()) {
            loadNotesFromFolder();
        }

        saveQueue(revisionQueue, PATH_FILE);
    }

    private void loadNotesFromFolder() {
        File folder = new File("notes");
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".rnote"));
        if (files == null) return;

        InlineCssTextArea tempArea = new InlineCssTextArea();
        for (File f : files) {
            try {
                Note note = new Note();
                NoteXmlUtil.load(note, tempArea, f);
                if (note.getDifficulty() >= 2 && note.getFilePath() != null) {
                    revisionQueue.enqueue(note);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ---------------- Serialization ----------------
    private void saveQueue(Queue queue, File file) {
        try (var out = new java.io.ObjectOutputStream(new java.io.FileOutputStream(file))) {
            out.writeObject(queue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Queue loadSavedQueue(File file) {
        try (var in = new java.io.ObjectInputStream(new java.io.FileInputStream(file))) {
            return (Queue) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new Queue();
        }
    }
}