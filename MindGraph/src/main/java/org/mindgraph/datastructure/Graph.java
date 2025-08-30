package org.mindgraph.datastructure;

import org.mindgraph.model.Note;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Graph implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Map<Note, List<Note>> notesGraph;

    public Graph() {
        this.notesGraph = new HashMap<>();
    }

    public void addNote(Note note) {
        notesGraph.computeIfAbsent(note, k -> new ArrayList<>());
    }

    public void removeNode(Note note) {
        for (List<Note> neighbours : notesGraph.values()) {
            neighbours.remove(note);
        }

        notesGraph.remove(note);
    }

    public void createEdge(Note baseNote, Note linkNote) {
        addNote(baseNote);
        addNote(linkNote);
        List<Note> notes = notesGraph.get(baseNote);
        if (!notes.contains(linkNote)) {
            notes.add(linkNote);
        }
    }

    public void removeEdge(Note baseNote, Note linkNote) {
        if (notesGraph.containsKey(baseNote)) {
            List<Note> notes = notesGraph.get(baseNote);
            notes.remove(linkNote);
        }
    }

    public Map<Note, List<Note>> getGraphNodes() {
        return notesGraph;
    }

    public List<Note> getNeighbours(Note note) {
        if (notesGraph.containsKey(note)) {
            return notesGraph.get(note);
        }
        return null;
    }

    public boolean containsNote(Note note) {
        return notesGraph.containsKey(note);
    }

    public boolean containsEdge(Note from, Note to) {
        return notesGraph.containsKey(from) && notesGraph.get(from).contains(to);
    }
}
