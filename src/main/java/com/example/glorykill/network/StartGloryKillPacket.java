package com.example.glorykill.network;

import com.example.glorykill.client.ClientPacketHandlers;
import com.example.glorykill.common.ActiveGloryKill;
import com.example.glorykill.common.ExecutionPosition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: an execution has started (or, for late joiners, is already running at {@code elapsedTicks}).
 * Carries everything a client needs to render it without asking again: which definition, the participants,
 * where they come from and where the server has placed them.
 */
public final class StartGloryKillPacket {

    public final ResourceLocation definitionId;
    public final int executorId;
    public final int victimId;
    public final Vec3 executorFrom;
    public final Vec3 executorTo;
    public final float executorYaw;
    public final Vec3 victimFrom;
    public final Vec3 victimTo;
    public final float victimYaw;
    public final int elapsedTicks;

    public StartGloryKillPacket(ResourceLocation definitionId, int executorId, int victimId,
                                Vec3 executorFrom, Vec3 executorTo, float executorYaw,
                                Vec3 victimFrom, Vec3 victimTo, float victimYaw, int elapsedTicks) {
        this.definitionId = definitionId;
        this.executorId = executorId;
        this.victimId = victimId;
        this.executorFrom = executorFrom;
        this.executorTo = executorTo;
        this.executorYaw = executorYaw;
        this.victimFrom = victimFrom;
        this.victimTo = victimTo;
        this.victimYaw = victimYaw;
        this.elapsedTicks = elapsedTicks;
    }

    public static StartGloryKillPacket of(ActiveGloryKill kill) {
        ExecutionPosition.Resolved layout = kill.layout();
        return new StartGloryKillPacket(kill.definition().id(), kill.executor().getId(), kill.victim().getId(),
                kill.executorFrom(), layout.playerPos(), layout.playerYaw(),
                kill.victimFrom(), layout.victimPos(), layout.victimYaw(), kill.elapsed());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(definitionId);
        buf.writeVarInt(executorId);
        buf.writeVarInt(victimId);
        writeVec(buf, executorFrom);
        writeVec(buf, executorTo);
        buf.writeFloat(executorYaw);
        writeVec(buf, victimFrom);
        writeVec(buf, victimTo);
        buf.writeFloat(victimYaw);
        buf.writeVarInt(elapsedTicks);
    }

    public static StartGloryKillPacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int executor = buf.readVarInt();
        int victim = buf.readVarInt();
        Vec3 exFrom = readVec(buf);
        Vec3 exTo = readVec(buf);
        float exYaw = buf.readFloat();
        Vec3 viFrom = readVec(buf);
        Vec3 viTo = readVec(buf);
        float viYaw = buf.readFloat();
        int elapsed = buf.readVarInt();
        return new StartGloryKillPacket(id, executor, victim, exFrom, exTo, exYaw, viFrom, viTo, viYaw, elapsed);
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleStart(this)));
        context.setPacketHandled(true);
    }
}
