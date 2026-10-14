package com.example.glorykill.common;

import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * A server side effect played on a specific tick of an execution (sound, particles, ...).
 * Sounds and particles are driven by the server so every nearby player sees/hears them exactly once.
 */
@FunctionalInterface
public interface GloryKillEffect {

    void play(GloryKillContext context);

    /** A sound at the victim's chest. Honours the {@code gloryKillSounds} config. */
    static GloryKillEffect sound(SoundEvent sound, float volume, float pitch) {
        return ctx -> {
            if (!GloryKillConfig.gloryKillSounds()) {
                return;
            }
            Vec3 p = ctx.focus();
            ctx.level().playSound(null, p.x, p.y, p.z, sound, SoundSource.PLAYERS, volume, pitch);
        };
    }

    /**
     * Particles on the victim's vertical axis.
     *
     * @param heightFraction 0 = feet, 1 = top of the head
     * @param blood          if true the effect is skipped when the {@code bloodParticles} config is off
     */
    static GloryKillEffect particles(ParticleOptions options, int count, double spread, double speed,
                                     double heightFraction, boolean blood) {
        return ctx -> {
            if (blood && !GloryKillConfig.bloodParticles()) {
                return;
            }
            Vec3 p = ctx.victimPoint(heightFraction);
            ctx.level().sendParticles(options, p.x, p.y, p.z, count, spread, spread * 0.6D, spread, speed);
        };
    }

    /**
     * Chunky "blood" (flying pieces) coloured to suit the victim: red for flesh, bone dust for skeletons, green goo for
     * slimes, ... see {@link BloodStyle}. Honours the {@code bloodParticles} config.
     */
    static GloryKillEffect bloodChunks(int count, double spread, double speed, double heightFraction) {
        return ctx -> {
            if (!GloryKillConfig.bloodParticles()) {
                return;
            }
            Vec3 p = ctx.victimPoint(heightFraction);
            ctx.level().sendParticles(BloodStyle.chunks(ctx.victim()), p.x, p.y, p.z, count, spread, spread * 0.6D, spread, speed);
        };
    }

    /** A fine coloured mist, matching {@link #bloodChunks}. Honours the {@code bloodParticles} config. */
    static GloryKillEffect bloodMist(int count, double spread, double speed, double heightFraction) {
        return ctx -> {
            if (!GloryKillConfig.bloodParticles()) {
                return;
            }
            Vec3 p = ctx.victimPoint(heightFraction);
            ctx.level().sendParticles(BloodStyle.mist(ctx.victim()), p.x, p.y, p.z, count, spread, spread * 0.6D, spread, speed);
        };
    }

    /** An effect bound to a tick of the execution timeline. */
    record Timed(int tick, GloryKillEffect effect) {
    }
}
