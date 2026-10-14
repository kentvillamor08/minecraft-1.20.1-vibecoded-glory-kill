package com.example.glorykill.common;

import com.example.glorykill.config.GloryKillConfig;
import com.example.glorykill.network.NetworkHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/**
 * Server side state of ONE running execution. All gameplay decisions (positions, timing, lethal damage, reward,
 * cancellation) are made here - the clients only play the matching animations.
 */
public final class ActiveGloryKill {

    private static final int SYNC_INTERVAL_TICKS = 10;

    private final GloryKillDefinition definition;
    private final ServerPlayer executor;
    private final LivingEntity victim;
    private final ExecutionPosition.Resolved layout;
    private final Vec3 executorFrom;
    private final Vec3 victimFrom;

    private int elapsed;
    private boolean lethalDone;
    private boolean ended;

    ActiveGloryKill(GloryKillDefinition definition, ServerPlayer executor, LivingEntity victim,
                    ExecutionPosition.Resolved layout) {
        this.definition = definition;
        this.executor = executor;
        this.victim = victim;
        this.layout = layout;
        this.executorFrom = executor.position();
        this.victimFrom = victim.position();
    }

    // ------------------------------------------------------------------ accessors
    public GloryKillDefinition definition() {
        return definition;
    }

    public ServerPlayer executor() {
        return executor;
    }

    public LivingEntity victim() {
        return victim;
    }

    public ExecutionPosition.Resolved layout() {
        return layout;
    }

    public Vec3 executorFrom() {
        return executorFrom;
    }

    public Vec3 victimFrom() {
        return victimFrom;
    }

    /** Ticks since the execution started (0 on the first tick). */
    public int elapsed() {
        return elapsed;
    }

    public boolean lethalDone() {
        return lethalDone;
    }

    GloryKillContext context() {
        return new GloryKillContext(executor.serverLevel(), executor, victim, this);
    }

    // ------------------------------------------------------------------ lifecycle
    void begin() {
        if (victim instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (victim instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
        }
        victim.removeEffect(MobEffects.GLOWING);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, definition.durationTicks() + 40, 6, false, false, false));
        placeVictim();

        Vec3 p = layout.playerPos();
        // The player's own look direction is NOT touched: the client blends its camera to the execution view and back.
        executor.connection.teleport(p.x, p.y, p.z, executor.getYRot(), executor.getXRot());
        executor.setDeltaMovement(Vec3.ZERO);
        executor.hurtMarked = true;
        executor.fallDistance = 0.0F;

        NetworkHandler.sendStart(this);
        definition.onStart(context());
    }

    void tick() {
        if (!isValid()) {
            GloryKillManager.end(this, EndReason.CANCELLED);
            return;
        }
        keepInPlace();
        runTimedEvents();

        if (elapsed >= definition.durationTicks()) {
            GloryKillManager.end(this, EndReason.COMPLETED);
            return;
        }
        if (elapsed > 0 && elapsed % SYNC_INTERVAL_TICKS == 0) {
            NetworkHandler.sendSync(this);
        }
        elapsed++;
    }

    void finish(EndReason reason) {
        if (ended) {
            return;
        }
        ended = true;
        if (!lethalDone && victim.isAlive()) {
            victim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        NetworkHandler.sendEnd(this, reason);
        definition.onEnd(context(), reason);
    }

    // ------------------------------------------------------------------ internals
    private boolean isValid() {
        if (executor.isRemoved() || !executor.isAlive() || executor.hasDisconnected()) {
            return false;
        }
        if (executor.level() != victim.level()) {
            return false;
        }
        if (!lethalDone && (victim.isRemoved() || !victim.isAlive())) {
            return false;
        }
        double drift = GloryKillConfig.cancelDriftBlocks();
        return executor.position().distanceToSqr(layout.playerPos()) <= drift * drift;
    }

    private void keepInPlace() {
        Vec3 p = layout.playerPos();
        if (executor.position().distanceToSqr(p) > 0.0025D) {
            executor.connection.teleport(p.x, p.y, p.z, executor.getYRot(), executor.getXRot());
        }
        executor.setDeltaMovement(Vec3.ZERO);
        executor.fallDistance = 0.0F;
        placeVictim();
    }

    private void placeVictim() {
        Vec3 v = layout.victimPos();
        float yaw = layout.victimYaw();
        victim.moveTo(v.x, v.y, v.z, yaw, 0.0F);
        victim.setYHeadRot(yaw);
        victim.setYBodyRot(yaw);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.fallDistance = 0.0F;
        if (victim instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    private void runTimedEvents() {
        GloryKillTimeline t = definition.timeline();
        GloryKillContext ctx = context();
        for (GloryKillEffect.Timed timed : definition.effects()) {
            if (timed.tick() == elapsed) {
                timed.effect().play(ctx);
            }
        }
        if (elapsed == t.impactTick()) {
            definition.onImpact(ctx);
        }
        if (elapsed == t.lethalTick()) {
            lethal(ctx);
        }
    }

    /** The server decides when the victim dies. */
    private void lethal(GloryKillContext ctx) {
        lethalDone = true;
        GloryKillManager.setLethal(victim.getUUID(), true);
        try {
            victim.invulnerableTime = 0;
            DamageSource source = executor.damageSources().playerAttack(executor);
            victim.hurt(source, Math.max(victim.getMaxHealth() * 4.0F, 1000.0F));
            if (victim.isAlive()) {
                victim.kill();
            }
        } finally {
            GloryKillManager.setLethal(victim.getUUID(), false);
        }
        applyReward(ctx);
        definition.onLethal(ctx);
    }

    private void applyReward(GloryKillContext ctx) {
        Reward reward = definition.reward();
        double hearts = reward.healHearts() >= 0.0D ? reward.healHearts() : GloryKillConfig.healHearts();
        ServerLevel level = ctx.level();
        if (hearts > 0.0D && executor.isAlive()) {
            executor.heal((float) (hearts * 2.0D));
            level.sendParticles(ParticleTypes.HEART, executor.getX(), executor.getY() + executor.getBbHeight() * 0.8D,
                    executor.getZ(), 5, 0.4D, 0.3D, 0.4D, 0.02D);
            if (GloryKillConfig.gloryKillSounds()) {
                level.playSound(null, executor.getX(), executor.getY(), executor.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.7F);
            }
        }
        if (reward.experience() > 0) {
            ExperienceOrb.award(level, victim.position(), reward.experience());
        }
    }
}
