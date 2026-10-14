package com.example.glorykill.network;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.common.ActiveGloryKill;
import com.example.glorykill.common.EndReason;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Network channel. Only {@link RequestGloryKillPacket} travels client -> server (a request, never a result);
 * the other three travel server -> client and carry the authoritative state.
 */
public final class NetworkHandler {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GloryKillMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    private NetworkHandler() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, RequestGloryKillPacket.class, RequestGloryKillPacket::encode,
                RequestGloryKillPacket::decode, RequestGloryKillPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, StartGloryKillPacket.class, StartGloryKillPacket::encode,
                StartGloryKillPacket::decode, StartGloryKillPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, SyncGloryKillPacket.class, SyncGloryKillPacket::encode,
                SyncGloryKillPacket::decode, SyncGloryKillPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, EndGloryKillPacket.class, EndGloryKillPacket::encode,
                EndGloryKillPacket::decode, EndGloryKillPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    // ------------------------------------------------------------------ server -> clients
    /** Sent to everybody who tracks the executor (and the executor itself) or the victim. Clients de-duplicate. */
    public static void sendStart(ActiveGloryKill kill) {
        StartGloryKillPacket packet = StartGloryKillPacket.of(kill);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(kill::executor), packet);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(kill::victim), packet);
    }

    /** For players that start tracking one of the participants while the execution is already running. */
    public static void sendStartTo(ServerPlayer viewer, ActiveGloryKill kill) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), StartGloryKillPacket.of(kill));
    }

    public static void sendSync(ActiveGloryKill kill) {
        SyncGloryKillPacket packet = new SyncGloryKillPacket(kill.executor().getId(), kill.elapsed());
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(kill::executor), packet);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(kill::victim), packet);
    }

    public static void sendEnd(ActiveGloryKill kill, EndReason reason) {
        EndGloryKillPacket packet = new EndGloryKillPacket(kill.executor().getId(), kill.victim().getId(), reason);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(kill::executor), packet);
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(kill::victim), packet);
    }
}
