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

    // ---------------- Basic Graph ----------------
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

    public boolean containsNote(Note note) {
        return notesGraph.containsKey(note);
    }

    public boolean containsEdge(Note from, Note to) {
        return notesGraph.containsKey(from) && notesGraph.get(from).contains(to);
    }

    // ---------------- Study Path Features ----------------

    // Edge class for MST
    public static class Edge {
        public final Note from;
        public final Note to;
        public final double weight;

        public Edge(Note from, Note to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }

    // Get weighted edges based on keyword similarity and difficulty
    public List<Edge> getWeightedEdges() {
        List<Edge> edges = new ArrayList<>();
        for (Map.Entry<Note, List<Note>> entry : notesGraph.entrySet()) {
            Note from = entry.getKey();
            for (Note to : entry.getValue()) {
                double weight = calculateWeight(from, to);
                edges.add(new Edge(from, to, weight));
            }
        }
        return edges;
    }

    // Weight: lower = better
    private double calculateWeight(Note n1, Note n2) {
        Set<String> commonKeywords = new HashSet<>(n1.getKeywords());
        commonKeywords.retainAll(n2.getKeywords());
        double similarity = commonKeywords.size();
        double difficultyDiff = Math.abs(n1.getDifficulty() - n2.getDifficulty());
        return difficultyDiff - similarity; // lower = more similar & closer difficulty
    }

    // ---------------- Kruskal MST ----------------
    public List<Edge> getMinimumSpanningTree() {
        List<Edge> edges = getWeightedEdges();
        edges.sort(Comparator.comparingDouble(e -> e.weight));

        UnionFind uf = new UnionFind(notesGraph.keySet());
        List<Edge> mst = new ArrayList<>();

        for (Edge e : edges) {
            if (uf.union(e.from, e.to)) {
                mst.add(e);
            }
        }
        return mst;
    }

    // ---------------- Generate Study Path ----------------
    public List<Note> getStudyPath(Note start) {
        List<Edge> mst = getMinimumSpanningTree();
        Map<Note, List<Note>> tree = new HashMap<>();

        // Build adjacency list from MST
        for (Edge e : mst) {
            tree.computeIfAbsent(e.from, k -> new ArrayList<>()).add(e.to);
            tree.computeIfAbsent(e.to, k -> new ArrayList<>()).add(e.from);
        }

        List<Note> path = new ArrayList<>();
        Set<Note> visited = new HashSet<>();
        dfs(start, tree, visited, path);
        return path;
    }

    private void dfs(Note current, Map<Note, List<Note>> tree, Set<Note> visited, List<Note> path) {
        visited.add(current);
        path.add(current);
        for (Note n : tree.getOrDefault(current, List.of())) {
            if (!visited.contains(n)) dfs(n, tree, visited, path);
        }
    }

    // ---------------- Union-Find Helper ----------------
    private static class UnionFind {
        private final Map<Note, Note> parent = new HashMap<>();

        public UnionFind(Set<Note> notes) {
            for (Note n : notes) parent.put(n, n);
        }

        public Note find(Note n) {
            if (parent.get(n) != n) {
                parent.put(n, find(parent.get(n))); // path compression
            }
            return parent.get(n);
        }

        public boolean union(Note a, Note b) {
            Note rootA = find(a);
            Note rootB = find(b);
            if (rootA == rootB) return false;
            parent.put(rootA, rootB);
            return true;
        }
    }
}