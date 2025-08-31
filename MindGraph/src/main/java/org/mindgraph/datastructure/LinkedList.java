package org.mindgraph.datastructure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Generic doubly-linked list with a movable Cursor for sequential navigation.
 * Big-O:
 *  - addFirst/addLast/removeFirst/removeLast: O(1)
 *  - get(index), remove(value by equals): O(n)
 *  - iteration: O(n)
 */
public class LinkedList<T> implements Iterable<T> {

    public static final class Node<T> {
        T data;
        Node<T> next;
        Node<T> prev;

        Node(T data) { this.data = data; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size = 0;

    public void addFirst(T item) {
        Node<T> n = new Node<>(item);
        if (head == null) head = tail = n;
        else {
            n.next = head;
            head.prev = n;
            head = n;
        }
        size++;
    }

    public void addLast(T item) {
        Node<T> n = new Node<>(item);
        if (tail == null) head = tail = n;
        else {
            tail.next = n;
            n.prev = tail;
            tail = n;
        }
        size++;
    }

    public T removeFirst() {
        if (head == null) throw new NoSuchElementException("List is empty");
        T val = head.data;
        head = head.next;
        if (head == null) tail = null;
        else head.prev = null;
        size--;
        return val;
    }

    public T removeLast() {
        if (tail == null) throw new NoSuchElementException("List is empty");
        T val = tail.data;
        tail = tail.prev;
        if (tail == null) head = null;
        else tail.next = null;
        size--;
        return val;
    }

    public boolean remove(T item) {
        Node<T> cur = head;
        while (cur != null) {
            if ((item == null && cur.data == null) || (item != null && item.equals(cur.data))) {
                unlink(cur);
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    private void unlink(Node<T> n) {
        Node<T> p = n.prev, nx = n.next;
        if (p == null) head = nx; else p.next = nx;
        if (nx == null) tail = p; else nx.prev = p;
        size--;
    }

    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        Node<T> cur = head;
        for (int i = 0; i < index; i++) cur = cur.next;
        return cur.data;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        head = tail = null;
        size = 0;
    }

    public List<T> toList() {
        List<T> out = new ArrayList<>(size);
        for (T t : this) out.add(t);
        return out;
    }

    public Cursor cursorFromStart() {
        return new Cursor(head);
    }

    public Cursor cursorFromEnd() {
        return new Cursor(tail);
    }

    public final class Cursor {
        private Node<T> current;

        private Cursor(Node<T> start) { this.current = start; }

        public T current() { return current == null ? null : current.data; }
        public boolean canPrev() { return current != null && current.prev != null; }
        public boolean canNext() { return current != null && current.next != null; }

        public T movePrev() {
            if (!canPrev()) return null;
            current = current.prev;
            return current.data;
        }

        public T moveNext() {
            if (!canNext()) return null;
            current = current.next;
            return current.data;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Node<T> cur = head;
            @Override public boolean hasNext() { return cur != null; }
            @Override public T next() {
                if (cur == null) throw new NoSuchElementException();
                T val = cur.data; cur = cur.next; return val;
            }
        };
    }
}
