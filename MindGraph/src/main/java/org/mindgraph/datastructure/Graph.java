package org.mindgraph.datastructure;

import org.mindgraph.model.Note;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Graph implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Map<Note, List<Note>> notesGraph;

    public Graph() {
        this.notesGraph = new HashMap<>();
    }

    public void addNote(Note note) {
        notesGraph.computeIfAbsent(note, k -> new ArrayList<>());
    }

    public void removeNode(Note note) {
        notesGraph.values().forEach(neighbours -> neighbours.remove(note));
        notesGraph.remove(note);
    }

    public void createEdge(Note baseNote, Note linkNote) {
        addNote(baseNote);
        addNote(linkNote);
        List<Note> neighbours = notesGraph.get(baseNote);
        if (!neighbours.contains(linkNote)) neighbours.add(linkNote);
    }

    public void removeEdge(Note baseNote, Note linkNote) {
        notesGraph.getOrDefault(baseNote, Collections.emptyList()).remove(linkNote);
    }

    public Map<Note, List<Note>> getGraphNodes() {

        return notesGraph;
    }

    public List<Note> getNeighbours(Note note) {

        return notesGraph.getOrDefault(note, List.of());
    }
}