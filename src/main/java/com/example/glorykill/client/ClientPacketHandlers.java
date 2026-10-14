package com.example.glorykill.client;

import com.example.glorykill.network.EndGloryKillPacket;
import com.example.glorykill.network.StartGloryKillPacket;
import com.example.glorykill.network.SyncGloryKillPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client side entry points of the server -> client packets (kept in the client package, never loaded on a server). */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    public static void handleStart(StartGloryKillPacket packet) {
        ClientGloryKillManager.handleStart(packet);
    }

    public static void handleSync(SyncGloryKillPacket packet) {
        ClientGloryKillManager.handleSync(packet);
    }

    public static void handleEnd(EndGloryKillPacket packet) {
        ClientGloryKillManager.handleEnd(packet);
    }
}
