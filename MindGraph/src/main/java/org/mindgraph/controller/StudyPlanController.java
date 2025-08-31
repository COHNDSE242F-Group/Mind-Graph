package org.mindgraph.controller;

import org.mindgraph.model.Note;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StudyPlanController {
    private List<Note> studyPlan = new ArrayList<>();
    private final String FILE_PATH = System.getProperty("user.home") + "/mindgraph_studyplan.dat";

    public StudyPlanController() {
        loadPlan();
    }

    public void addNote(Note note) {
        if (note == null) return;

        // Check if note already exists (by ID if available, or by title)
        boolean exists = studyPlan.stream().anyMatch(n ->
                (n.getId() != 0 && n.getId() == note.getId()) ||
                        n.getTitle().equals(note.getTitle())
        );

        if (!exists) {
            studyPlan.add(note);
            savePlan();
        }
    }

    public void removeNote(Note note) {
        if (note == null) return;

        studyPlan.removeIf(n ->
                (n.getId() != 0 && n.getId() == note.getId()) ||
                        n.getTitle().equals(note.getTitle())
        );
        savePlan();
    }

    public List<Note> getPlan() {
        return new ArrayList<>(studyPlan);
    }

    public void clearPlan() {
        studyPlan.clear();
        savePlan();
    }

    private void savePlan() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(studyPlan);
        } catch (IOException e) {
            System.err.println("Failed to save study plan: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlan() {
        File f = new File(FILE_PATH);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            List<Note> loadedPlan = (List<Note>) ois.readObject();
            studyPlan.clear();
            studyPlan.addAll(loadedPlan);
        } catch (Exception e) {
            System.out.println("No previous study plan found or failed to load: " + e.getMessage());
            studyPlan = new ArrayList<>();
        }
    }
}