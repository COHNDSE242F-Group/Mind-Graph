package org.mindgraph.model;

import java.io.File;

public class NoteEntry {

    private Note note;
    private File file;

    public NoteEntry(Note note, File file) {
        this.note = note;
        this.file = file;
    }

    public Note getNote() {
        return note;
    }

    public File getFile() {
        return file;
    }


}
