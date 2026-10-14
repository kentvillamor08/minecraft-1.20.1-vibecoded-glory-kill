package com.example.glorykill.network;

import com.example.glorykill.client.ClientPacketHandlers;
import com.example.glorykill.common.EndReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the execution is over, normal rendering and controls resume. */
public final class EndGloryKillPacket {

    public final int executorId;
    public final int victimId;
    public final EndReason reason;

    public EndGloryKillPacket(int executorId, int victimId, EndReason reason) {
        this.executorId = executorId;
        this.victimId = victimId;
        this.reason = reason;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(executorId);
        buf.writeVarInt(victimId);
        buf.writeByte(reason.id());
    }

    public static EndGloryKillPacket decode(FriendlyByteBuf buf) {
        return new EndGloryKillPacket(buf.readVarInt(), buf.readVarInt(), EndReason.byId(buf.readByte()));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleEnd(this)));
        context.setPacketHandled(true);
    }
}
