package com.example.glorykill.common;

/** Names of the GeckoLib animations that play in each {@link GloryKillPhase}. */
public record AnimationSet(String start, String execute, String finish) {

    public String forPhase(GloryKillPhase phase) {
        return switch (phase) {
            case START -> start;
            case EXECUTE -> execute;
            case FINISH -> finish;
        };
    }
}
