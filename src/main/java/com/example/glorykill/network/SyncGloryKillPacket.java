package com.example.glorykill.network;

import com.example.glorykill.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: periodic resynchronisation of the execution clock (keeps player and victim animations aligned). */
public final class SyncGloryKillPacket {

    public final int executorId;
    public final int elapsedTicks;

    public SyncGloryKillPacket(int executorId, int elapsedTicks) {
        this.executorId = executorId;
        this.elapsedTicks = elapsedTicks;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(executorId);
        buf.writeVarInt(elapsedTicks);
    }

    public static SyncGloryKillPacket decode(FriendlyByteBuf buf) {
        return new SyncGloryKillPacket(buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSync(this)));
        context.setPacketHandled(true);
    }
}
