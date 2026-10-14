package com.example.glorykill.network;

import com.example.glorykill.common.GloryKillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: "I would like to execute the entity with this id".
 * The client never decides that an execution succeeded - the server re-validates everything.
 */
public final class RequestGloryKillPacket {

    private final int targetId;

    public RequestGloryKillPacket(int targetId) {
        this.targetId = targetId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetId);
    }

    public static RequestGloryKillPacket decode(FriendlyByteBuf buf) {
        return new RequestGloryKillPacket(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                GloryKillManager.requestStart(sender, targetId);
            }
        });
        context.setPacketHandled(true);
    }
}
