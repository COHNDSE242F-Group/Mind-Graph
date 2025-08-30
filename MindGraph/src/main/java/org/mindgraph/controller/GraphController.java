package org.mindgraph.controller;

import org.mindgraph.datastructure.Graph;
import org.mindgraph.db.NoteDao;
import org.mindgraph.model.Note;

import java.io.*;
import java.sql.SQLException;
import java.util.List;

public class GraphController {
    private Graph graph;
    private final String saveFilePath;
    private final NoteDao noteDao;

    public GraphController(String saveFilePath) {
        this.saveFilePath = saveFilePath;
        this.noteDao = new NoteDao("mindgraph.db");
        this.graph = loadGraph();
    }

    public Graph getGraph() {
        return graph;
    }

    // Add a note
    public void addNote(Note note) {
        graph.addNote(note);
        saveGraph();
    }

    // Remove a note
    public void removeNote(Note note) {
        graph.removeNode(note);
        saveGraph();
    }

    // Add a directed edge
    public void addEdge(Note from, Note to) {
        graph.createEdge(from, to);
        saveGraph();
    }

    // Remove a directed edge
    public void removeEdge(Note from, Note to) {
        graph.removeEdge(from, to);
        saveGraph();
    }

    // Get neighbours (returns empty list if note not in graph)
    public List<Note> getNeighbours(Note note) {
        List<Note> neighbours = graph.getNeighbours(note);
        return neighbours != null ? neighbours : List.of();
    }

    // -------------------- Persistence --------------------

    private void saveGraph() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFilePath))) {
            oos.writeObject(graph);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Graph loadGraph() {
        File file = new File(saveFilePath);
        if (!file.exists()) {
            return new Graph(); // empty graph if nothing saved yet
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Graph) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new Graph(); // fallback to empty graph
        }
    }

    /** Build or update the graph based on keywords → title matches
     * @param reset true to clear the graph before rebuilding
     */
    public void buildGraphFromDb(boolean reset) throws SQLException {
        if (reset) {
            graph = new Graph(); // clear existing edges/nodes
        }

        List<Note> allNotes = noteDao.findAll();

        // Case-insensitive matching: keyword → title
        for (Note note : allNotes) {
            for (String keyword : note.getKeywords()) {
                for (Note candidate : allNotes) {
                    // Skip if the candidate is the same note (title matches its own title)
                    if (!candidate.equals(note) && candidate.getTitle().equalsIgnoreCase(keyword)) {
                        graph.createEdge(note, candidate);
                    }
                }
            }
        }

        saveGraph(); // persist updated graph
    }

    public void printGraph() {
        for (Note note : graph.getGraphNodes().keySet()) {
            List<Note> neighbours = graph.getNeighbours(note);
            String neighbourTitles = neighbours != null
                    ? neighbours.stream().map(Note::getTitle).toList().toString()
                    : "[]";
            System.out.println(note.getTitle() + " -> " + neighbourTitles);
        }
    }
}