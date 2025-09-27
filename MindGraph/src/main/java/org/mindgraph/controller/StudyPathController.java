package org.mindgraph.controller;

import org.mindgraph.datastructure.Graph;
import org.mindgraph.datastructure.Queue;
import org.mindgraph.model.Note;

import java.io.*;
import java.util.*;

/**
 * Serializable controller that maintains a study path (using custom Queue of Note objects),
 * provides simple CRUD on the plan and can automatically generate an "optimal" study
 * path from a Graph instance. Supports queue-style navigation.
 */
public class StudyPathController implements Serializable {
    private static final long serialVersionUID = 1L;

    // The in-memory queue that holds the study plan
    private final Queue plan = new Queue();

    // Default location to persist the serialized plan.
    private final String saveFilePath;

    public StudyPathController() {
        this("study_path.ser");
    }

    public StudyPathController(String saveFilePath) {
        this.saveFilePath = saveFilePath;
        loadFromDisk();
    }

    /* ---------------- Basic plan operations ---------------- */

    public List<Note> getPlan() {
        // Convert internal queue to list (by dequeuing and re-enqueueing)
        List<Note> list = new ArrayList<>();
        int size = plan.size();
        for (int i = 0; i < size; i++) {
            Note n = (Note) plan.dequeue();
            list.add(n);
            plan.enqueue(n); // reinsert to maintain order
        }
        return list;
    }

    public Note peekNextNote() {
        return (Note) plan.peek();
    }

    public Note pollNextNote() {
        Note n = (Note) plan.dequeue();
        saveToDisk();
        return n;
    }

    public void addNote(Note note) {
        if (note == null) return;
        // prevent duplicates
        if (!getPlan().contains(note)) {
            plan.enqueue(note);
            saveToDisk();
        }
    }

    public void removeNote(Note note) {
        if (note == null) return;
        List<Note> items = getPlan();
        plan.clear();
        for (Note n : items) {
            if (!n.equals(note)) plan.enqueue(n);
        }
        saveToDisk();
    }

    public void clearPlan() {
        plan.clear();
        saveToDisk();
    }

    // Dequeue the next note in queue (like revision dequeue)
    public Note dequeueNextNote() {
        if (plan.isEmpty()) return null;
        Note n = (Note) plan.dequeue();
        saveToDisk();
        return n;
    }

    // Returns true if there are notes in the queue
    public boolean hasNotes() {
        return !plan.isEmpty();
    }

    private void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFilePath))) {
            oos.writeObject(getPlan());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        File f = new File(saveFilePath);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                plan.clear();
                for (Note n : (List<Note>) obj) plan.enqueue(n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Automatic plan generation

    public void generateFromGraph(Graph graph) {
        if (graph == null) return;

        plan.clear();

        Collection<Note> nodes = graph.getGraphNodes().keySet();

        // Build adjacency list and in-degree map
        Map<Note, List<Note>> adj = new HashMap<>();
        Map<Note, Integer> inDegree = new HashMap<>();
        for (Note n : nodes) {
            adj.putIfAbsent(n, new ArrayList<>());
            List<Note> deps = Optional.ofNullable(graph.getNeighbours(n)).orElse(new ArrayList<>());
            inDegree.putIfAbsent(n, 0);

            for (Note dep : deps) {
                adj.putIfAbsent(dep, new ArrayList<>());
                adj.get(dep).add(n); // dep -> n
                inDegree.put(n, inDegree.getOrDefault(n, 0) + 1);
            }
        }

        // Build keyword frequency map
        Map<String, Integer> keywordFreq = new HashMap<>();
        for (Note note : nodes) {
            List<String> kws = Optional.ofNullable(note.getKeywords()).orElse(Collections.emptyList());
            for (String k : kws) keywordFreq.put(k, keywordFreq.getOrDefault(k, 0) + 1);
        }

        // Priority queue for nodes with in-degree 0
        PriorityQueue<Note> readyNotes = new PriorityQueue<>(
                Comparator.comparingDouble(n -> computeNoteScore(n, adj, keywordFreq))
        );

        for (Map.Entry<Note, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) readyNotes.add(entry.getKey());
        }

        // Track added nodes
        Set<Note> added = new HashSet<>();

        while (!readyNotes.isEmpty()) {
            Note n = readyNotes.poll();
            plan.enqueue(n);
            added.add(n);

            for (Note neighbor : adj.getOrDefault(n, Collections.emptyList())) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0 && !added.contains(neighbor)) {
                    readyNotes.add(neighbor);
                }
            }
        }

        // Append any missing nodes (fallback for cycles or isolated nodes)
        for (Note n : nodes) {
            if (!added.contains(n)) {
                plan.enqueue(n);
            }
        }

        // Print the generated queue
        System.out.println("Generated Study Path Queue:");
        for (Note note : getPlan()) {
            System.out.println(note.getTitle());
        }

        saveToDisk();
    }

    private double computeNoteScore(Note n, Map<Note, List<Note>> adj, Map<String, Integer> keywordFreq) {
        List<String> kws = Optional.ofNullable(n.getKeywords()).orElse(Collections.emptyList());
        int uniqueKw = 0;
        for (String k : kws) if (keywordFreq.getOrDefault(k, 0) == 1) uniqueKw++;

        int outDegree = adj.getOrDefault(n, Collections.emptyList()).size();
        double difficulty = n.getDifficulty();

        return outDegree - uniqueKw * 0.5 + difficulty * 0.2;
    }
}