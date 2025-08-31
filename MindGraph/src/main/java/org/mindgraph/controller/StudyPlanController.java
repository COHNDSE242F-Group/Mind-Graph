package org.mindgraph.controller;

import org.mindgraph.model.Note;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StudyPlanController implements Serializable {

    private static class Node implements Serializable {
        Note note;
        Node next;

        Node(Note note) {
            this.note = note;
        }
    }

    private Node head = null;
    private final String FILE_PATH = System.getProperty("user.home") + "/mindgraph_studyplan.dat";

    public StudyPlanController() {
        loadPlan();
    }

    // Add note at the end, avoid duplicates
    public void addNote(Note note) {
        if (note == null) return;

        if (contains(note)) return; // skip duplicates

        Node newNode = new Node(note);
        if (head == null) {
            head = newNode;
        } else {
            Node current = head;
            while (current.next != null) current = current.next;
            current.next = newNode;
        }
        savePlan();
    }

    // Remove note by ID or title
    public void removeNote(Note note) {
        if (note == null || head == null) return;

        if (matches(head.note, note)) {
            head = head.next;
            savePlan();
            return;
        }

        Node prev = head;
        Node current = head.next;

        while (current != null) {
            if (matches(current.note, note)) {
                prev.next = current.next;
                savePlan();
                return;
            }
            prev = current;
            current = current.next;
        }
    }

    // Convert linked list to a List<Note>
    public List<Note> getPlan() {
        List<Note> list = new ArrayList<>();
        Node current = head;
        while (current != null) {
            list.add(current.note);
            current = current.next;
        }
        return list;
    }

    // Clear entire plan
    public void clearPlan() {
        head = null;
        savePlan();
    }

    // Check if a note already exists
    private boolean contains(Note note) {
        Node current = head;
        while (current != null) {
            if (matches(current.note, note)) return true;
            current = current.next;
        }
        return false;
    }

    // Check equality by ID (if present) or title
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
            head = null;
            for (Note n : loadedList) {
                addNote(n); // rebuild linked list
            }
        } catch (Exception e) {
            System.out.println("No previous study plan found or failed to load: " + e.getMessage());
        }
    }
}