package org.mindgraph.controller;

import org.mindgraph.datastructure.LinkedList;
import org.mindgraph.model.Note;
import java.io.*;
import java.util.List;

public class StudyPlanController{
    private LinkedList<Note> studyPlan = new LinkedList<>();
    private final String FILE_PATH = "studyplan.dat"; // saved in project folder

    public StudyPlanController() {
        loadPlan();
    }

    public void addNote(Note note) {
        if (!studyPlan.toList().contains(note)) {
            studyPlan.addLast(note);
            savePlan();
        }
    }

    public void removeNote(Note note) {
        studyPlan.remove(note);
        savePlan();
    }

    public List<Note> getPlan() {
        return studyPlan.toList();
    }

    private void savePlan() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(studyPlan.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlan() {
        File f = new File(FILE_PATH);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            List<Note> list = (List<Note>) ois.readObject();
            studyPlan.clear();
            for (Note n : list) studyPlan.addLast(n);
        } catch (Exception e) {
            System.out.println("No previous study plan found or failed to load.");
        }
    }
}
