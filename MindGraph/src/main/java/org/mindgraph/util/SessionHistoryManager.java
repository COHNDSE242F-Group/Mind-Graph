package org.mindgraph.util;

import org.mindgraph.datastructure.LinkedList;
import org.mindgraph.model.Note;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the session history of opened notes using a LinkedList.
 * Supports sequential navigation (Prev/Next) and history display.
 */
public class SessionHistoryManager {

    /** Represents a single visit to a note */
    public static final class SessionEntry {
        private final Note note;
        private final LocalDateTime visitedAt;

        public SessionEntry(Note note, LocalDateTime visitedAt) {
            this.note = note;
            this.visitedAt = visitedAt;
        }

        public Note getNote() { return note; }
        public LocalDateTime getVisitedAt() { return visitedAt; }

        @Override
        public String toString() {
            return visitedAt + " — " + note.getTitle();
        }
    }

    private final LinkedList<SessionEntry> history = new LinkedList<>();
    private LinkedList<SessionEntry>.Cursor cursor = null;

    /** Record a note opening into the session history */
    public void record(Note note) {
        if (note == null) return;
        SessionEntry entry = new SessionEntry(note, LocalDateTime.now());
        history.addLast(entry);
        cursor = history.cursorFromEnd(); // point to newest
    }

    /** Navigation through session history */
    public SessionEntry prevInSession() { return cursor == null ? null : cursor.movePrev(); }
    public SessionEntry nextInSession() { return cursor == null ? null : cursor.moveNext(); }
    public boolean canPrev() { return cursor != null && cursor.canPrev(); }
    public boolean canNext() { return cursor != null && cursor.canNext(); }
    public SessionEntry current() { return cursor == null ? null : cursor.current(); }

    /** Get all visited notes (for revision/history view) */
    public List<SessionEntry> all() { return history.toList(); }

    /** Utility */
    public int size() { return history.size(); }
    public void clear() { history.clear(); cursor = null; }
}
