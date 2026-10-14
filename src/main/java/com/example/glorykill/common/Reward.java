package com.example.glorykill.common;

/**
 * Reward for a completed Glory Kill.
 *
 * @param healHearts hearts to heal; a negative value means "use the config value {@code gloryKillHeal}"
 * @param experience bonus experience points dropped at the victim (on top of its normal drops)
 */
public record Reward(double healHearts, int experience) {

    public static final Reward CONFIG_DEFAULT = new Reward(-1.0D, 0);
}
