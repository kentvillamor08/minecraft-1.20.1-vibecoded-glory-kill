package com.example.glorykill.common;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

/** Small geometry helpers shared by client and server. */
public final class GloryKillMath {

    private GloryKillMath() {
    }

    /** Squared distance from a point to the closest point of a box (0 when inside). */
    public static double distanceSqToBox(Vec3 p, AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, p.x - box.maxX), 0.0D);
        double dy = Math.max(Math.max(box.minY - p.y, p.y - box.maxY), 0.0D);
        double dz = Math.max(Math.max(box.minZ - p.z, p.z - box.maxZ), 0.0D);
        return dx * dx + dy * dy + dz * dz;
    }

    /** Minecraft yaw (degrees) of a horizontal direction; forward is (-sin(yaw), cos(yaw)). */
    public static float yawOf(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Interpolates angles in degrees along the shortest arc. */
    public static float lerpAngle(float from, float to, float t) {
        return from + Mth.wrapDegrees(to - from) * t;
    }

    /**
     * Weighted random choice. Every item counts at least 1.
     *
     * @param random returns a value in {@code [0, bound)} for the given bound
     */
    public static <T> T pickWeighted(List<T> items, ToIntFunction<T> weight, IntUnaryOperator random) {
        int total = 0;
        for (T item : items) {
            total += Math.max(1, weight.applyAsInt(item));
        }
        int roll = random.applyAsInt(total);
        for (T item : items) {
            roll -= Math.max(1, weight.applyAsInt(item));
            if (roll < 0) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    /** Smooth 0..1 easing. */
    public static float smoothstep(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }
}
