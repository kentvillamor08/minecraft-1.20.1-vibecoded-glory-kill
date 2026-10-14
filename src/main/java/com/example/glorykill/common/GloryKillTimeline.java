package com.example.glorykill.common;

/**
 * All tick values of an execution, counted from the moment the server starts it (tick 0).
 *
 * <pre>
 *  START phase   [0, executeStartTick)        player_glory_kill_start   / zombie_glory_kill_start
 *  EXECUTE phase [executeStartTick, finish)   player_glory_kill_execute / zombie_glory_kill_execute
 *  FINISH phase  [finishPhaseTick, endTick]   player_glory_kill_finish  / zombie_glory_kill_death
 * </pre>
 *
 * The server is authoritative: it applies the lethal damage on {@code lethalTick} no matter what the client
 * animation does. If you retime an animation in Blockbench, adjust the numbers here to match.
 *
 * @param executeStartTick  first tick of the EXECUTE phase
 * @param impactTick        the first strike lands (hook: {@link GloryKillDefinition#onImpact})
 * @param victimReactTick   the victim visibly reacts (used for effects)
 * @param lethalTick        the server deals the lethal damage
 * @param finishPhaseTick   first tick of the FINISH phase
 * @param endTick           last tick of the execution; control is returned afterwards
 */
public record GloryKillTimeline(int executeStartTick, int impactTick, int victimReactTick, int lethalTick,
                                int finishPhaseTick, int endTick) {

    public GloryKillTimeline {
        if (executeStartTick < 0 || executeStartTick > finishPhaseTick || finishPhaseTick > endTick) {
            throw new IllegalArgumentException("Phase boundaries must satisfy 0 <= executeStart <= finishPhase <= end");
        }
        if (impactTick < 0 || victimReactTick < 0 || lethalTick < 0
                || impactTick > endTick || victimReactTick > endTick || lethalTick > endTick) {
            throw new IllegalArgumentException("Event ticks must lie inside [0, endTick]");
        }
    }

    public GloryKillPhase phaseAt(int tick) {
        if (tick < executeStartTick) {
            return GloryKillPhase.START;
        }
        if (tick < finishPhaseTick) {
            return GloryKillPhase.EXECUTE;
        }
        return GloryKillPhase.FINISH;
    }
}
