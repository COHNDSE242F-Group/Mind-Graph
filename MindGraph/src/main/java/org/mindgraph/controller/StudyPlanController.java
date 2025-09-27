package org.mindgraph.controller;

import org.mindgraph.datastructure.LinkedList;
import org.mindgraph.model.Note;

import java.io.*;
import java.util.List;

/**
 * StudyPlanController manages a study plan using a custom LinkedList.
 * Supports adding, inserting, removing notes, clearing the plan,
 * cursor-based navigation, and saving/loading from disk.
 */
public class StudyPlanController implements Serializable {

    private final LinkedList<Note> plan = new LinkedList<>();
    private final String FILE_PATH = System.getProperty("user.home") + "/mindgraph_studyplan.dat";

    public StudyPlanController() {
        loadPlan();
    }

    /** Add note at the end, skip duplicates */
    public void addNote(Note note) {
        if (note == null) return;
        if (contains(note)) return;

        plan.addLast(note);
        savePlan();
    }

    /** Insert note at specific index, skips duplicates */
    public void insertNoteAt(Note note, int index) {
        if (note == null) return;
        if (contains(note)) return;

        plan.insertAt(note, index);
        savePlan();
    }

    /** Remove note by object (ID or title) */
    public void removeNote(Note note) {
        if (note == null) return;

        LinkedList<Note>.Cursor cursor = plan.cursorFromStart();
        int index = 0;
        while (cursor.current() != null) {
            Note n = cursor.current();
            if (matches(n, note)) {
                plan.removeAt(index);
                savePlan();
                return;
            }
            cursor.moveNext();
            index++;
        }
    }

    /** Get the entire plan as a List */
    public List<Note> getPlan() {
        return plan.toList();
    }

    /** Clear the plan */
    public void clearPlan() {
        plan.clear();
        savePlan();
    }

    /** Get a cursor to navigate the study plan */
    public LinkedList<Note>.Cursor getPlanCursor() {
        return plan.cursorFromStart();
    }

    /** Check if note exists in plan */
    private boolean contains(Note note) {
        for (Note n : plan) {
            if (matches(n, note)) return true;
        }
        return false;
    }

    /** Match notes by ID (if exists) or title */
    private boolean matches(Note n1, Note n2) {
        if (n1 == null || n2 == null) return false;
        return (n1.getId() != 0 && n1.getId() == n2.getId()) || n1.getTitle().equals(n2.getTitle());
    }

    // --- Save/Load ---
    private void savePlan() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(getPlan()); // serialize as List
        } catch (IOException e) {
            System.err.println("Failed to save study plan: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlan() {
        File f = new File(FILE_PATH);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            List<Note> loadedList = (List<Note>) ois.readObject();
            plan.clear();
            for (Note n : loadedList) {
                plan.addLast(n);
            }
        } catch (Exception e) {
            System.out.println("No previous study plan found or failed to load: " + e.getMessage());
        }
    }
}