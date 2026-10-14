package com.example.glorykill.client;

import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** "[G] GLORY KILL" prompt under the crosshair while an executable target is in reach. */
@OnlyIn(Dist.CLIENT)
public final class GloryKillHud {

    public static final IGuiOverlay PROMPT = (gui, graphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !GloryKillConfig.showPrompt()) {
            return;
        }
        LivingEntity target = GloryKillTargeting.current();
        if (target == null) {
            return;
        }
        Component text = Component.translatable("hud.glorykill.prompt", GloryKillKeyMappings.EXECUTE.getTranslatedKeyMessage());
        graphics.drawCenteredString(mc.font, text, screenWidth / 2, screenHeight / 2 + 22, 0xFF3030);
    };

    private GloryKillHud() {
    }
}
