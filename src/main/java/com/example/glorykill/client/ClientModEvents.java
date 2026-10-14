package com.example.glorykill.client;

import com.example.glorykill.GloryKillMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus client registrations: the key binding and the HUD prompt. */
@Mod.EventBusSubscriber(modid = GloryKillMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GloryKillKeyMappings.EXECUTE);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("prompt", GloryKillHud.PROMPT);
    }
}
