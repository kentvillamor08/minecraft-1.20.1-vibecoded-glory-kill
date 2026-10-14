package com.example.glorykill.common;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server authoritative entry point. The client only ever sends a request; everything below is verified here:
 * player exists, target exists / alive / eligible (hostile, never a player, small enough) / in range / looked at,
 * nobody else is executing it, the player is not already executing, health is within the threshold and a move exists
 * for the entity type. The move itself (which choreography) is chosen here, on the server.
 */
public final class GloryKillManager {

    private static final Map<UUID, ActiveGloryKill> BY_EXECUTOR = new HashMap<>();
    private static final Map<UUID, ActiveGloryKill> BY_VICTIM = new HashMap<>();
    /** Victims that are currently receiving the scripted lethal hit (so our own damage handlers let it through). */
    private static final Set<UUID> LETHAL_IN_PROGRESS = new HashSet<>();

    /** The move each player used last, so the next execution is a different one whenever possible. */
    private static final Map<UUID, ResourceLocation> LAST_MOVE = new HashMap<>();

    /** Extra reach tolerance (blocks) so latency does not reject executions that were valid on the client. */
    private static final double DISTANCE_TOLERANCE = 0.75D;

    private GloryKillManager() {
    }

    public enum StartResult {
        OK,
        DISABLED,
        INVALID_EXECUTOR,
        NO_TARGET,
        NOT_ENEMY,
        DISABLED_TYPE,
        TOO_LARGE,
        NO_GLORY_KILL,
        NOT_WEAK,
        TOO_FAR,
        NOT_LOOKING,
        NO_LINE_OF_SIGHT,
        EXECUTOR_BUSY,
        TARGET_BUSY,
        NO_SPACE;

        public String translationKey() {
            return "message.glorykill.fail." + name().toLowerCase(Locale.ROOT);
        }
    }

    // ------------------------------------------------------------------ start
    /** Handles a {@code RequestGloryKillPacket}. */
    public static void requestStart(ServerPlayer player, int targetId) {
        Entity target = player.serverLevel().getEntity(targetId);
        StartResult result = tryStart(player, target, false, null);
        if (result != StartResult.OK) {
            player.displayClientMessage(Component.translatable(result.translationKey()), true);
        }
    }

    /**
     * Validates and starts an execution.
     *
     * @param force      skips the health, look and stagger requirements (used by {@code /glorykill force})
     * @param forcedMove use exactly this move instead of picking one (testing); must be usable on the target
     */
    public static StartResult tryStart(ServerPlayer player, @Nullable Entity targetEntity, boolean force,
                                       @Nullable GloryKillDefinition forcedMove) {
        if (!GloryKillConfig.enabled()) {
            return StartResult.DISABLED;
        }
        if (player.isRemoved() || !player.isAlive() || player.isSpectator() || player.isPassenger()
                || player.isSleeping() || player.isFallFlying() || player.hasDisconnected()) {
            return StartResult.INVALID_EXECUTOR;
        }
        if (BY_EXECUTOR.containsKey(player.getUUID())) {
            return StartResult.EXECUTOR_BUSY;
        }
        if (!(targetEntity instanceof LivingEntity victim) || victim.isRemoved() || !victim.isAlive()
                || victim.level() != player.level()) {
            return StartResult.NO_TARGET;
        }
        if (BY_VICTIM.containsKey(victim.getUUID())) {
            return StartResult.TARGET_BUSY;
        }
        switch (GloryKillRegistry.check(victim)) { // players are never executable - no player vs player
            case PLAYER, NOT_HOSTILE -> {
                return StartResult.NOT_ENEMY;
            }
            case DISABLED_TYPE -> {
                return StartResult.DISABLED_TYPE;
            }
            case TOO_LARGE -> {
                return StartResult.TOO_LARGE;
            }
            case NO_MOVES -> {
                return StartResult.NO_GLORY_KILL;
            }
            case OK -> {
            }
        }
        if (victim.isPassenger() || victim.isVehicle()) {
            return StartResult.NO_SPACE; // mounted mobs (spider jockeys) cannot be moved around
        }
        GloryKillDefinition definition = forcedMove != null
                ? forcedMove
                : GloryKillRegistry.pick(victim, player.getRandom(), LAST_MOVE.get(player.getUUID()));
        if (definition == null || (forcedMove != null && !GloryKillRegistry.movesFor(victim.getType()).contains(forcedMove))) {
            return StartResult.NO_GLORY_KILL;
        }
        if (!force && !isWeak(victim)) {
            return StartResult.NOT_WEAK;
        }

        // distance (eye -> closest point of the hitbox)
        Vec3 eye = player.getEyePosition();
        double maxDistance = force ? 6.0D : GloryKillConfig.maxDistance() + DISTANCE_TOLERANCE;
        if (GloryKillMath.distanceSqToBox(eye, victim.getBoundingBox()) > maxDistance * maxDistance) {
            return StartResult.TOO_FAR;
        }

        if (!force) {
            // the player must be looking at the target
            Vec3 toTarget = victim.getBoundingBox().getCenter().subtract(eye);
            double length = toTarget.length();
            if (length > 1.0E-4D) {
                double cos = Mth.clamp(player.getViewVector(1.0F).dot(toTarget) / length, -1.0D, 1.0D);
                if (Math.toDegrees(Math.acos(cos)) > GloryKillConfig.maxLookAngle()) {
                    return StartResult.NOT_LOOKING;
                }
            }
            if (GloryKillConfig.requireLineOfSight() && !player.hasLineOfSight(victim)) {
                return StartResult.NO_LINE_OF_SIGHT;
            }
        }

        // where will everybody stand?
        Vec3 playerPos = player.position();
        Vec3 victimPos = victim.position();
        double dx = victimPos.x - playerPos.x;
        double dz = victimPos.z - playerPos.z;
        float baseYaw = (dx * dx + dz * dz < 1.0E-6D) ? player.getYRot() : GloryKillMath.yawOf(dx, dz);
        ExecutionPosition.Resolved layout = definition.position().forVictim(victim.getBbWidth()).resolve(victimPos, baseYaw);
        if (victim.level().noCollision(victim, victim.getBoundingBox().move(0.0D, -0.35D, 0.0D))) {
            // nothing below the victim (flying / floating mob): do not lift the player into the air
            layout = layout.withPlayerY(playerPos.y);
        }

        AABB playerBox = player.getBoundingBox().move(layout.playerPos().subtract(playerPos));
        AABB victimBox = victim.getBoundingBox().move(layout.victimPos().subtract(victimPos));
        if (!player.level().noCollision(player, playerBox) || !player.level().noCollision(victim, victimBox)) {
            return StartResult.NO_SPACE;
        }

        StaggerManager.clear(victim);
        ActiveGloryKill kill = new ActiveGloryKill(definition, player, victim, layout);
        BY_EXECUTOR.put(player.getUUID(), kill);
        BY_VICTIM.put(victim.getUUID(), kill);
        LAST_MOVE.put(player.getUUID(), definition.id());
        kill.begin();
        GloryKillMod.LOGGER.debug("Glory Kill {} started: {} -> {}", definition.id(), player.getGameProfile().getName(), victim);
        return StartResult.OK;
    }

    /** Health at or below the configured fraction of max health. */
    public static boolean isWeak(LivingEntity entity) {
        return entity.getHealth() <= entity.getMaxHealth() * GloryKillConfig.healthThreshold() + 1.0E-3D;
    }

    // ------------------------------------------------------------------ tick / end
    public static void tick() {
        if (BY_EXECUTOR.isEmpty()) {
            return;
        }
        for (ActiveGloryKill kill : new ArrayList<>(BY_EXECUTOR.values())) {
            kill.tick();
        }
    }

    static void end(ActiveGloryKill kill, EndReason reason) {
        BY_EXECUTOR.remove(kill.executor().getUUID(), kill);
        BY_VICTIM.remove(kill.victim().getUUID(), kill);
        LETHAL_IN_PROGRESS.remove(kill.victim().getUUID());
        kill.finish(reason);
    }

    public static void cancelFor(ServerPlayer player) {
        LAST_MOVE.remove(player.getUUID());
        ActiveGloryKill kill = BY_EXECUTOR.get(player.getUUID());
        if (kill != null) {
            end(kill, EndReason.CANCELLED);
        }
    }

    /** An entity left the level: abort unless it is the victim's corpse being cleaned up after the lethal hit. */
    public static void onEntityLeave(Entity entity) {
        ActiveGloryKill kill = involving(entity);
        if (kill == null) {
            return;
        }
        if (entity == kill.executor() || !kill.lethalDone()) {
            end(kill, EndReason.CANCELLED);
        }
    }

    public static void clearAll() {
        BY_EXECUTOR.clear();
        BY_VICTIM.clear();
        LETHAL_IN_PROGRESS.clear();
        LAST_MOVE.clear();
    }

    // ------------------------------------------------------------------ queries
    public static boolean isExecuting(Entity entity) {
        return BY_EXECUTOR.containsKey(entity.getUUID());
    }

    public static boolean isVictim(Entity entity) {
        return BY_VICTIM.containsKey(entity.getUUID());
    }

    public static boolean isLethalStrike(Entity entity) {
        return LETHAL_IN_PROGRESS.contains(entity.getUUID());
    }

    @Nullable
    public static ActiveGloryKill involving(Entity entity) {
        ActiveGloryKill kill = BY_EXECUTOR.get(entity.getUUID());
        return kill != null ? kill : BY_VICTIM.get(entity.getUUID());
    }

    public static GloryKillState stateOf(Entity entity) {
        if (isExecuting(entity)) {
            return GloryKillState.EXECUTING;
        }
        if (isVictim(entity)) {
            return GloryKillState.BEING_EXECUTED;
        }
        if (StaggerManager.isStaggered(entity)) {
            return GloryKillState.STAGGERED;
        }
        return GloryKillState.NONE;
    }

    static void setLethal(UUID victim, boolean on) {
        if (on) {
            LETHAL_IN_PROGRESS.add(victim);
        } else {
            LETHAL_IN_PROGRESS.remove(victim);
        }
    }
}
