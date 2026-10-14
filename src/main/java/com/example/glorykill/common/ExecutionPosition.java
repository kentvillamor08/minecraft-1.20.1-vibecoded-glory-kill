package com.example.glorykill.common;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Reusable description of where the player and the victim stand during an execution.
 *
 * <p>Offsets are expressed in a local frame that is anchored on the victim's position at the moment the execution
 * starts, and whose forward axis is the horizontal direction from the player towards the victim:
 * <ul>
 *   <li>{@code x} = right (as seen by the player)</li>
 *   <li>{@code y} = up</li>
 *   <li>{@code z} = forward (towards the victim)</li>
 * </ul>
 * Yaw offsets are added to the frame's yaw (the direction the player looks at the victim).
 *
 * <pre>
 *  Zombie    player (0,0,-1.15)  yaw +0     victim (0,0,0)    yaw +180   - face to face
 *  Creeper   player (0.9,0,-0.4) yaw -35    victim (0,0,0)    yaw +90    - grabbed from the side
 *  Skeleton  player (0,0,-0.9)   yaw +0     victim (0,0,0.3)  yaw +180   - knocked back
 * </pre>
 *
 * @param durationTicks how long both entities are held in place (at least the timeline's end tick is used)
 */
public record ExecutionPosition(Vec3 playerOffset, Vec3 victimOffset, float playerYawOffset, float victimYawOffset,
                                int durationTicks) {

    /** Width of the mob the offsets were authored for (a zombie). Wider mobs push the player back by the difference. */
    private static final double REFERENCE_WIDTH = 0.6D;

    /** The player stands {@code distance} blocks in front of the victim and both look at each other. */
    public static ExecutionPosition facingEachOther(double distance, int durationTicks) {
        return new ExecutionPosition(new Vec3(0.0D, 0.0D, -distance), Vec3.ZERO, 0.0F, 180.0F, durationTicks);
    }

    /**
     * Adapts the stand-off distance to the victim: the player offset grows (along its own horizontal direction) by half
     * of how much wider than a zombie the victim is, so the player always stands the same distance from its surface.
     */
    public ExecutionPosition forVictim(double victimWidth) {
        double extra = Math.max(0.0D, victimWidth - REFERENCE_WIDTH) * 0.5D;
        double length = Math.hypot(playerOffset.x, playerOffset.z);
        if (extra <= 0.0D || length < 1.0E-4D) {
            return this;
        }
        double factor = 1.0D + extra / length;
        return new ExecutionPosition(new Vec3(playerOffset.x * factor, playerOffset.y, playerOffset.z * factor),
                victimOffset, playerYawOffset, victimYawOffset, durationTicks);
    }

    /**
     * @param victimStart the victim's feet position when the execution starts
     * @param baseYaw     yaw (Minecraft convention) of the direction player -> victim
     */
    public Resolved resolve(Vec3 victimStart, float baseYaw) {
        double rad = Math.toRadians(baseYaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        Vec3 player = victimStart.add(toWorld(playerOffset, sin, cos));
        Vec3 victim = victimStart.add(toWorld(victimOffset, sin, cos));
        return new Resolved(player, Mth.wrapDegrees(baseYaw + playerYawOffset),
                victim, Mth.wrapDegrees(baseYaw + victimYawOffset));
    }

    /** forward = (-sin, 0, cos), right = (-cos, 0, -sin) in Minecraft's yaw convention. */
    private static Vec3 toWorld(Vec3 local, double sin, double cos) {
        double x = -cos * local.x - sin * local.z;
        double z = -sin * local.x + cos * local.z;
        return new Vec3(x, local.y, z);
    }

    /** World space result of {@link #resolve}. */
    public record Resolved(Vec3 playerPos, float playerYaw, Vec3 victimPos, float victimYaw) {

        /** Same layout with the player at another height (used when the victim is airborne, so nobody is lifted). */
        public Resolved withPlayerY(double y) {
            return new Resolved(new Vec3(playerPos.x, y, playerPos.z), playerYaw, victimPos, victimYaw);
        }
    }
}
