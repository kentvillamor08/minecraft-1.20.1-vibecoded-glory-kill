package com.example.glorykill.client;

import com.example.glorykill.common.GloryKillMath;
import com.example.glorykill.common.GloryKillRegistry;
import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Client side suggestion of what the execute key would hit (drives the HUD prompt and the request packet).
 * This is only a hint: the server re-validates everything before anything happens.
 */
@OnlyIn(Dist.CLIENT)
public final class GloryKillTargeting {

    /** Aim assist: the target must be inside this cone (degrees) around the view direction. */
    private static final double AIM_CONE_DEGREES = 30.0D;

    @Nullable
    private static LivingEntity current;

    private GloryKillTargeting() {
    }

    @Nullable
    public static LivingEntity current() {
        return current;
    }

    public static void update(Minecraft mc) {
        current = null;
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.screen != null || !GloryKillConfig.enabled()
                || ClientGloryKillManager.local() != null || player.isSpectator() || !player.isAlive()) {
            return;
        }
        double maxDistance = GloryKillConfig.maxDistance();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double cosLimit = Math.cos(Math.toRadians(Math.min(AIM_CONE_DEGREES, GloryKillConfig.maxLookAngle())));
        double bestDot = cosLimit;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(maxDistance + 1.5D), e -> e != player)) {
            if (!isCandidate(candidate)) {
                continue;
            }
            if (GloryKillMath.distanceSqToBox(eye, candidate.getBoundingBox()) > maxDistance * maxDistance) {
                continue;
            }
            Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eye);
            double length = toCandidate.length();
            if (length < 1.0E-4D) {
                continue;
            }
            double dot = look.dot(toCandidate) / length;
            if (dot >= bestDot) {
                bestDot = dot;
                current = candidate;
            }
        }
    }

    private static boolean isCandidate(LivingEntity entity) {
        // hostile mobs only, never players, not too large - the exact rules the server applies
        if (!entity.isAlive() || !GloryKillRegistry.isExecutable(entity)) {
            return false;
        }
        if (ClientGloryKillManager.byVictim(entity.getId()) != null) {
            return false;
        }
        return entity.getHealth() <= entity.getMaxHealth() * GloryKillConfig.healthThreshold() + 1.0E-3D;
    }
}
