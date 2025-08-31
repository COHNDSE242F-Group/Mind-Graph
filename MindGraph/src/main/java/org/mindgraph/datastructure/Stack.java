package org.mindgraph.datastructure;

import org.mindgraph.model.Note;

public class Stack {

    private Note[] data;
    private int top;

    public Stack() {
        data = new Note[300]; // max 300 notes in history
        top = -1;
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public boolean isFull() {
        return top == data.length - 1;
    }

    public void push(Note note) {
        if (!isFull()) {
            data[++top] = note;
        } else {
            System.out.println("Stack is full, cannot push: " + note.getTitle());
        }
    }

    public Note pop() {
        if (!isEmpty()) {
            return data[top--];
        } else {
            System.out.println("Stack is empty. No notes to return");
            return null;
        }
    }

    public Note peek() {
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
                System.out.println("Prev. viewed: " + data[c].getTitle());
            }
        }
    }

    public void clear() {
        while (!isEmpty()) pop();
    }

}