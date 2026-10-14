package com.example.glorykill.common;

/** High level state of an entity with respect to the Glory Kill system (see {@link GloryKillManager#stateOf}). */
public enum GloryKillState {
    /** Normal gameplay. */
    NONE,
    /** A weakened mob inside its stagger window - it can be executed. */
    STAGGERED,
    /** The player who is currently performing an execution. */
    EXECUTING,
    /** The mob that is currently being executed. */
    BEING_EXECUTED
}
