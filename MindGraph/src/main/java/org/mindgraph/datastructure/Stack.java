package org.mindgraph.datastructure;

import org.mindgraph.model.NoteEntry;

public class Stack {

    private NoteEntry[] data;
    private int top;

    public Stack() {
        data = new NoteEntry[300];
        top = -1;
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public boolean isFull() {
        return top == data.length - 1;
    }

    public void push(NoteEntry entry) {
        if (!isFull()) {
            data[++top] = entry;
        } else {
            System.out.println("Stack is full, cannot push: " + entry.getNote().getTitle());
        }
    }

    public NoteEntry pop() {
        if (!isEmpty()) {
            return data[top--];
        } else {
            System.out.println("Stack is empty. No notes to return");
            return null;
        }
    }

    public NoteEntry peek() {
        if (!isEmpty()) {
            return data[top];
        } else {
            System.out.println("Stack is empty.");
            return null;
        }
    }

    public void display() {
        if (isEmpty()) {
            System.out.println("No previously viewed notes.");
        } else {
            for (int c = 0; c <= top; c++) {
                System.out.println("Prev. viewed: " + data[c].getNote().getTitle());
            }
        }
    }
}
