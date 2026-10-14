package com.example.glorykill.common;

public enum EndReason {
    /** The execution ran to its end. */
    COMPLETED,
    /** The execution was aborted (disconnect, death, dimension change, ...). */
    CANCELLED;

    public static EndReason byId(int id) {
        return id == 0 ? COMPLETED : CANCELLED;
    }

    public int id() {
        return this == COMPLETED ? 0 : 1;
    }
}
