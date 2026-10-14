package com.example.glorykill.client;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.common.GloryKillDefinition;
import com.example.glorykill.config.GloryKillConfig;
import com.example.glorykill.network.NetworkHandler;
import com.example.glorykill.network.RequestGloryKillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-bus client events. Everything here only changes behaviour while an execution is running; outside of one every
 * handler returns immediately and the vanilla renderers / controls are untouched.
 */
@Mod.EventBusSubscriber(modid = GloryKillMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    // ------------------------------------------------------------------ ticking / input
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            ClientGloryKillManager.enforceLock(mc); // before the player ticks, so no rotation packet leaks the mouse
            return;
        }
        if (mc.player == null || mc.level == null) {
            ClientGloryKillManager.clearAll();
            return;
        }
        ClientGloryKillManager.tick(mc);
        GloryKillCamera.tick();
        GloryKillTargeting.update(mc);
        handleExecuteKey(mc);
    }

    private static void handleExecuteKey(Minecraft mc) {
        while (GloryKillKeyMappings.EXECUTE.consumeClick()) {
            if (mc.screen != null || mc.player == null || ClientGloryKillManager.local() != null) {
                continue;
            }
            LivingEntity target = GloryKillTargeting.current();
            if (target == null) {
                mc.player.displayClientMessage(Component.translatable("message.glorykill.no_target"), true);
            } else {
                // a REQUEST only - the server decides whether the execution happens
                NetworkHandler.CHANNEL.sendToServer(new RequestGloryKillPacket(target.getId()));
            }
        }
    }

    /** Frame start/end: freeze the mouse look and apply / restore the temporary camera position. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientGloryKillManager.enforceLock(Minecraft.getInstance());
            GloryKillCamera.beginFrame(event.renderTickTime);
        } else {
            GloryKillCamera.endFrame();
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (ClientGloryKillManager.local() == null) {
            return;
        }
        Input input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /** Normal attacks, item use and pick block are disabled while executing. */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (ClientGloryKillManager.local() != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    // ------------------------------------------------------------------ first person
    /** Frame in which the GeckoLib arms were last drawn (the hand event fires once per rendered hand). */
    private static long lastArmsFrame = -1L;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ClientGloryKillSession session = ClientGloryKillManager.local();
        if (session == null || !GloryKillConfig.firstPersonArms()
                || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        event.setCanceled(true); // no vanilla hand
        // The event fires per hand; the GeckoLib arms contain both, so draw them once per frame
        // (on whichever hand event comes first - vanilla skips the main-hand event in some item-use states).
        long frame = GloryKillCamera.frame();
        if (frame != lastArmsFrame) {
            lastArmsFrame = frame;
            FirstPersonGloryKillRenderer.render(session, event.getPoseStack(), event.getMultiBufferSource(),
                    event.getPackedLight());
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        GloryKillCamera.computeAngles(event);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        GloryKillCamera.computeFov(event);
    }

    // ------------------------------------------------------------------ third person (everybody who can see it)
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        ClientGloryKillSession session = ClientGloryKillManager.byExecutor(player.getId());
        if (session == null) {
            return;
        }
        event.setCanceled(true);
        ThirdPersonGloryKillRenderer.renderExecutor(player, session, event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), event.getPartialTick());
    }

    /** MODEL victims (the zombie): the mob's own renderer is cancelled and the GeckoLib model is drawn instead. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        ClientGloryKillSession session = ClientGloryKillManager.byVictim(entity.getId());
        if (session == null || session.definition().victimMode() != GloryKillDefinition.VictimMode.MODEL) {
            return;
        }
        event.setCanceled(true);
        ThirdPersonGloryKillRenderer.renderVictim(entity, session, event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), event.getPartialTick());
    }

    /**
     * BODY victims (every other hostile mob): not cancelled - the mob draws itself, with the animated root bone of the
     * GeckoLib rig applied on top. LOWEST so that nobody cancels the event after we pushed a PoseStack level.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPreBody(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        ClientGloryKillSession session = ClientGloryKillManager.byVictim(entity.getId());
        if (session == null || session.definition().victimMode() != GloryKillDefinition.VictimMode.BODY) {
            return;
        }
        ThirdPersonGloryKillRenderer.beginVictimBody(entity, session, event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        ThirdPersonGloryKillRenderer.endVictimBody(event.getEntity(), event.getPoseStack());
    }

    // ------------------------------------------------------------------ cleanup
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientGloryKillManager.clearAll();
        GloryKillCamera.reset();
    }
}
