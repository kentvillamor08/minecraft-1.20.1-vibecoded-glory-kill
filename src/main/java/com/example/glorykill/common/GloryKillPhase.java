package com.example.glorykill.common;

/**
 * Animation phase of an execution. Both participants switch phase on the same tick; the animation names that play
 * in each phase come from the definition's {@link AnimationSet}s
 * ({@code player_glory_kill_start/execute/finish} and {@code zombie_glory_kill_start/execute/death}).
 */
public enum GloryKillPhase {
    START,
    EXECUTE,
    FINISH
}
