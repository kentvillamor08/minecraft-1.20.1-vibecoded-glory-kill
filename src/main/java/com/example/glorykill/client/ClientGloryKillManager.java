package com.example.glorykill.client;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.common.GloryKillDefinition;
import com.example.glorykill.common.GloryKillRegistry;
import com.example.glorykill.config.GloryKillConfig;
import com.example.glorykill.network.EndGloryKillPacket;
import com.example.glorykill.network.StartGloryKillPacket;
import com.example.glorykill.network.SyncGloryKillPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of the executions this client currently knows about, filled exclusively from server packets
 * (the client never decides that anything happened).
 */
@OnlyIn(Dist.CLIENT)
public final class ClientGloryKillManager {

    private static final Map<Integer, ClientGloryKillSession> BY_EXECUTOR = new HashMap<>();
    private static final Map<Integer, ClientGloryKillSession> BY_VICTIM = new HashMap<>();

    /** The execution the local player is performing, if any. */
    @Nullable
    private static ClientGloryKillSession local;

    // the real look direction / hotbar slot of the local player are frozen while executing and restored implicitly
    private static float lockedYaw;
    private static float lockedPitch;
    private static int lockedSlot;
    @Nullable
    private static CameraType previousCameraType;

    private ClientGloryKillManager() {
    }

    // ------------------------------------------------------------------ queries
    @Nullable
    public static ClientGloryKillSession local() {
        return local;
    }

    @Nullable
    public static ClientGloryKillSession byExecutor(int entityId) {
        return BY_EXECUTOR.get(entityId);
    }

    @Nullable
    public static ClientGloryKillSession byVictim(int entityId) {
        return BY_VICTIM.get(entityId);
    }

    // ------------------------------------------------------------------ packets
    static void handleStart(StartGloryKillPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || BY_EXECUTOR.containsKey(packet.executorId)) {
            return; // not in a world, or a duplicate of a start we already know
        }
        GloryKillDefinition definition = GloryKillRegistry.byId(packet.definitionId);
        if (definition == null) {
            GloryKillMod.LOGGER.warn("Server started unknown Glory Kill {} - are both sides running the same mod version?",
                    packet.definitionId);
            return;
        }
        boolean isLocal = mc.player.getId() == packet.executorId;
        ClientGloryKillSession session = new ClientGloryKillSession(definition, packet, isLocal);
        BY_EXECUTOR.put(packet.executorId, session);
        BY_VICTIM.put(packet.victimId, session);
        if (GloryKillConfig.debugLogging()) {
            GloryKillMod.LOGGER.info("[GloryKill] session start {} executor={} victim={} local={}",
                    definition.id(), packet.executorId, packet.victimId, isLocal);
        }
        if (isLocal) {
            beginLocal(mc, session);
        }
    }

    static void handleSync(SyncGloryKillPacket packet) {
        ClientGloryKillSession session = BY_EXECUTOR.get(packet.executorId);
        if (session != null) {
            session.syncElapsed(packet.elapsedTicks);
        }
    }

    static void handleEnd(EndGloryKillPacket packet) {
        ClientGloryKillSession session = BY_EXECUTOR.remove(packet.executorId);
        BY_VICTIM.remove(packet.victimId);
        if (session == null) {
            return;
        }
        session.markEnded();
        if (GloryKillConfig.debugLogging()) {
            GloryKillMod.LOGGER.info("[GloryKill] session end executor={} reason={}", packet.executorId, packet.reason);
        }
        if (session == local) {
            endLocal(Minecraft.getInstance());
        }
    }

    // ------------------------------------------------------------------ local player lock
    private static void beginLocal(Minecraft mc, ClientGloryKillSession session) {
        LocalPlayer player = mc.player;
        local = session;
        lockedYaw = player.getYRot();
        lockedPitch = player.getXRot();
        lockedSlot = player.getInventory().selected;
        if (GloryKillConfig.forceFirstPerson() && !mc.options.getCameraType().isFirstPerson()) {
            previousCameraType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        GloryKillCamera.onLocalStart(session);
    }

    private static void endLocal(Minecraft mc) {
        local = null;
        GloryKillCamera.onLocalEnd();
        if (previousCameraType != null) {
            mc.options.setCameraType(previousCameraType);
            previousCameraType = null;
        }
    }

    /**
     * Keeps the real look direction frozen. Called before the player ticks and before every frame; the visible camera
     * is produced separately by {@link GloryKillCamera}. Because the real rotation is never changed, the view returns
     * to exactly where the player was looking when the execution is over.
     */
    public static void enforceLock(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (local == null || player == null) {
            return;
        }
        player.setYRot(lockedYaw);
        player.setXRot(lockedPitch);
        player.yRotO = lockedYaw;
        player.xRotO = lockedPitch;
        player.getInventory().selected = lockedSlot;
    }

    // ------------------------------------------------------------------ ticking
    public static void tick(Minecraft mc) {
        if (BY_EXECUTOR.isEmpty()) {
            return;
        }
        for (ClientGloryKillSession session : new ArrayList<>(BY_EXECUTOR.values())) {
            session.tick();
            if (session.isEnded()) {
                BY_EXECUTOR.remove(session.executorId(), session);
                BY_VICTIM.remove(session.victimId(), session);
                if (session == local) {
                    endLocal(mc);
                }
            }
        }
    }

    /** Disconnect / world change. */
    public static void clearAll() {
        BY_EXECUTOR.clear();
        BY_VICTIM.clear();
        if (local != null) {
            endLocal(Minecraft.getInstance());
        }
    }
}
