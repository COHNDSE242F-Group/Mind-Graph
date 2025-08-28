package org.mindgraph.datastructure;

import org.mindgraph.model.Note;


public class Queue {
    private final int MAX_SIZE = 100;
    private Note[] queue;
    private int front;
    private int rear;
    private int size;

    // Constructor initialize the queue
    public Queue() {
        queue = new Note[MAX_SIZE];
        front = 0;
        rear = -1;
        size = 0;
    }


     //Adds a note to the end of the queue

    public void enqueue(Note note) {
        if (size == MAX_SIZE) {
            System.out.println("Queue is full");
            return;
        }
        rear = (rear + 1) % MAX_SIZE;
        queue[rear] = note;
        size++;
    }

    //Removes and returns the note at the front of the queue
    public Note dequeue() {
        if (size == 0) {
            System.out.println("Queue is empty");
            return null;
        }
        Note temp = queue[front];
        front = (front + 1) % MAX_SIZE;
        size--;
        return temp;
    }

    //Returns the note at the front without removing it
    public Note peek() {
        if (size == 0) return null;
        return queue[front];
    }

    //Checks if the queue is empty.
    public boolean isEmpty() {
        return size == 0;
    }


     // Returns the current number of notes in the queue
    public int getSize() {
        return size;
    }


     //Clears the queue by resetting pointers and size
    public void clear() {
        front = 0;
        rear = -1;
        size = 0;
    }
}