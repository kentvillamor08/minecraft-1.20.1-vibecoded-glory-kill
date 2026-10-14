package com.example.glorykill.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Everything a server side hook / effect needs to know about the running execution. */
public record GloryKillContext(ServerLevel level, ServerPlayer executor, LivingEntity victim, ActiveGloryKill session) {

    /** Point at roughly the victim's chest. */
    public Vec3 focus() {
        return victimPoint(0.6D);
    }

    /** A point on the victim's vertical axis, {@code heightFraction} of the way from its feet to its head. */
    public Vec3 victimPoint(double heightFraction) {
        return victim.position().add(0.0D, victim.getBbHeight() * heightFraction, 0.0D);
    }
}
