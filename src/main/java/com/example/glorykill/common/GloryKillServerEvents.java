package com.example.glorykill.common;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.config.GloryKillConfig;
import com.example.glorykill.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server side game events: stagger, protection, execution locking, cleanup and late-join synchronisation. */
@Mod.EventBusSubscriber(modid = GloryKillMod.MOD_ID)
public final class GloryKillServerEvents {

    private GloryKillServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        GloryKillManager.tick();
        StaggerManager.tick();
    }

    // ------------------------------------------------------------------ damage rules
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // staggered mobs and mobs being executed cannot hurt anybody
        if (attacker != null && (StaggerManager.isStaggered(attacker) || GloryKillManager.isVictim(attacker))) {
            event.setCanceled(true);
            return;
        }
        // the victim only takes the scripted lethal strike (no duplicate damage, no other killers)
        if (GloryKillManager.isVictim(target)) {
            if (!GloryKillManager.isLethalStrike(target) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
            }
            return;
        }
        // the executing player is protected while the animation plays
        if (target instanceof ServerPlayer player && GloryKillManager.isExecuting(player)
                && GloryKillConfig.invulnerableDuringExecution()
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** LOWEST: the clamp must see the final damage amount after every other mod has had its say. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        StaggerManager.onDamage(event);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getNewTarget() != null && (StaggerManager.isStaggered(entity) || GloryKillManager.isVictim(entity))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            StaggerManager.clear(event.getEntity());
        }
    }

    // ------------------------------------------------------------------ player locking
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelable() && event.getEntity() instanceof ServerPlayer player && GloryKillManager.isExecuting(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GloryKillManager.isExecuting(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GloryKillManager.isExecuting(player)) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
    }

    // ------------------------------------------------------------------ cleanup
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GloryKillManager.cancelFor(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GloryKillManager.cancelFor(player);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            GloryKillManager.onEntityLeave(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GloryKillManager.clearAll();
        StaggerManager.clearAll();
    }

    // ------------------------------------------------------------------ multiplayer: late joiners
    /** A player that comes into range of a running execution gets the state it missed. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) {
            return;
        }
        ActiveGloryKill kill = GloryKillManager.involving(event.getTarget());
        if (kill != null) {
            NetworkHandler.sendStartTo(viewer, kill);
        }
    }
}
