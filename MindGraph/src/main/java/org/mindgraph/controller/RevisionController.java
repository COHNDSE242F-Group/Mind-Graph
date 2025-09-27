package org.mindgraph.controller;

import org.fxmisc.richtext.InlineCssTextArea;
import org.mindgraph.datastructure.Graph;
import org.mindgraph.datastructure.Queue;
import org.mindgraph.model.Note;
import org.mindgraph.util.NoteXmlUtil;

import java.io.*;
import java.util.Comparator;
import java.util.List;

/**
 * Handles the Revision Mode queue for notes.
 */
public class RevisionController implements Serializable {
    private static final long serialVersionUID = 1L;
    private Queue revisionQueue;
    private Graph graph; // Reference to graph-based study path
    private static final File PATH_FILE = new File(System.getProperty("user.home") + "/revisionQueue.dat");

    public RevisionController() {
        // Load saved queue or create new empty queue
        revisionQueue = PATH_FILE.exists() ? loadSavedQueue(PATH_FILE) : new Queue();
        if (revisionQueue == null) revisionQueue = new Queue();
    }

    // Returns true if there are notes in the revision queue.
    public boolean hasNotes() {
        return revisionQueue != null && !revisionQueue.isEmpty();
    }

    // Peek at the next note without dequeuing.
    public Note peekNextNote() {
        if (!hasNotes()) return null;
        return (Note) revisionQueue.peek();
    }

    // Dequeue the next note from the revision queue and save queue to disk.
    public Note dequeueNextNote() {
        if (!hasNotes()) return null;
        Note next = (Note) revisionQueue.dequeue();
        saveQueue(revisionQueue, PATH_FILE);
        return next;
    }

    // Add a note to the revision queue and save to disk.
    public void enqueueNoteForRevision(Note note) {
        if (note == null || note.getFilePath() == null) return;

        if (revisionQueue == null) revisionQueue = new Queue();
        revisionQueue.enqueue(note);
        saveQueue(revisionQueue, PATH_FILE);
    }

    // Clear the revision queue and remove saved file.
    public void clearQueue() {
        if (revisionQueue != null) revisionQueue.clear();
        if (PATH_FILE.exists()) PATH_FILE.delete();
    }

    // Serialization
    private void saveQueue(Queue queue, File file) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(queue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Queue loadSavedQueue(File file) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (Queue) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new Queue();
        }
    }
}