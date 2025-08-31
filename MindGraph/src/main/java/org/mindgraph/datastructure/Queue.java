package org.mindgraph.datastructure;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * Simple FIFO queue for storing notes.
 */
public class Queue implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final LinkedList<Object> list;

    public Queue() {
        list = new LinkedList<>();
    }

    public void enqueue(Object obj) {
        if (obj == null) return;
        list.addLast(obj);
    }

    public Object dequeue() {
        if (list.isEmpty()) return null;
        return list.removeFirst();
    }

    public Object peek() {
        if (list.isEmpty()) return null;
        return list.getFirst();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void clear() {
        list.clear();
    }

    public int size() {
        return list.size();
    }
}