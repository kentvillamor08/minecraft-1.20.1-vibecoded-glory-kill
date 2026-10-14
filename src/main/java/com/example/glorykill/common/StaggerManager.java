package com.example.glorykill.common;

import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server side stagger state. A mob that is hit by a player down to the health threshold is put into a stagger window:
 * it freezes, glows, cannot attack and (optionally) cannot die from normal damage, so the player can walk up and
 * execute it. When the window runs out the mob recovers and gets a short cooldown before it can be staggered again.
 */
public final class StaggerManager {

    /** A protected / staggered mob is never reduced below this much health by normal damage. */
    private static final float MIN_HEALTH = 1.0F;

    private record Entry(ResourceKey<Level> dimension, long endTick) {
    }

    private static final Map<UUID, Entry> STAGGERED = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private StaggerManager() {
    }

    public static boolean isStaggered(Entity entity) {
        return STAGGERED.containsKey(entity.getUUID());
    }

    /** Hooked from {@link LivingDamageEvent} (final damage, after armour). */
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || !GloryKillConfig.enabled()) {
            return;
        }
        if (GloryKillManager.isLethalStrike(target) || GloryKillManager.isVictim(target)) {
            return;
        }

        float health = target.getHealth();
        float newHealth = health - event.getAmount();

        if (isStaggered(target)) {
            if (GloryKillConfig.protectStaggered() && newHealth < MIN_HEALTH) {
                event.setAmount(Math.max(0.0F, health - MIN_HEALTH));
            }
            return;
        }

        if (!(event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer) || !canBeStaggered(target)) {
            return;
        }
        float threshold = target.getMaxHealth() * (float) GloryKillConfig.healthThreshold();
        if (newHealth > threshold) {
            return;
        }
        if (newHealth < MIN_HEALTH) {
            // the hit would kill it - keep it alive at 1 HP (or its current health if that is already lower)
            event.setAmount(Math.max(0.0F, health - Math.min(MIN_HEALTH, health)));
        }
        stagger(target);
    }

    private static boolean canBeStaggered(LivingEntity target) {
        // same rule set as the execution itself: hostile only, never a player, not too large, a move exists
        return target.isAlive() && GloryKillRegistry.isExecutable(target) && !onCooldown(target);
    }

    private static boolean onCooldown(LivingEntity target) {
        Long until = COOLDOWNS.get(target.getUUID());
        if (until == null) {
            return false;
        }
        if (target.level().getGameTime() >= until) {
            COOLDOWNS.remove(target.getUUID());
            return false;
        }
        return true;
    }

    /** Puts the mob into the stagger state (also used by {@code /glorykill stagger}). */
    public static void stagger(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        int ticks = GloryKillConfig.staggerTicks();
        STAGGERED.put(target.getUUID(), new Entry(level.dimension(), level.getGameTime() + ticks));

        // Slowness VII freezes the mob completely.
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks + 10, 6, false, false, false));
        if (GloryKillConfig.glowWhenStaggered()) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks + 10, 0, false, false, false));
        }
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (target instanceof Creeper creeper) {
            creeper.setSwellDir(-1); // a staggered creeper must not blow up in the player's face
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK,
                SoundSource.HOSTILE, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                14, 0.3D, 0.4D, 0.3D, 0.3D);
    }

    /** Forgets the stagger of this entity (death, execution start, ...). */
    public static void clear(LivingEntity target) {
        if (STAGGERED.remove(target.getUUID()) != null) {
            release(target);
        }
    }

    private static void release(LivingEntity target) {
        target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        target.removeEffect(MobEffects.GLOWING);
    }

    public static void clearAll() {
        STAGGERED.clear();
        COOLDOWNS.clear();
    }

    /** Called once per server tick. */
    public static void tick() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        if (!STAGGERED.isEmpty()) {
            Iterator<Map.Entry<UUID, Entry>> it = STAGGERED.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Entry> mapEntry = it.next();
                Entry entry = mapEntry.getValue();
                ServerLevel level = server.getLevel(entry.dimension());
                Entity entity = level == null ? null : level.getEntity(mapEntry.getKey());
                if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                    it.remove();
                    continue;
                }
                if (GloryKillManager.isVictim(living)) {
                    // the execution has taken over; the stagger effects are handled by the session
                    it.remove();
                    continue;
                }
                long now = level.getGameTime();
                if (now >= entry.endTick()) {
                    it.remove();
                    release(living);
                    COOLDOWNS.put(living.getUUID(), now + GloryKillConfig.staggerCooldownTicks());
                    continue;
                }
                if ((now & 3L) == 0L) {
                    level.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY() + living.getBbHeight() + 0.2D, living.getZ(),
                            2, 0.2D, 0.05D, 0.2D, 0.05D);
                }
            }
        }
        if (!COOLDOWNS.isEmpty()) {
            long now = server.overworld().getGameTime();
            COOLDOWNS.values().removeIf(until -> until <= now);
        }
    }
}
