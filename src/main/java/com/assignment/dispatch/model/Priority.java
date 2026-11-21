package com.assignment.dispatch.model;

/**
 * Delivery priority. Lower rank = more urgent, so sorting by rank ascending
 * puts HIGH priority orders first.
 */
public enum Priority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
