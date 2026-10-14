package com.example.glorykill.client.animation;

import com.example.glorykill.client.ClientGloryKillSession;
import com.example.glorykill.common.GloryKillAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The player's own arms in first person. Textured with the local player's skin (wide or slim arms). */
@OnlyIn(Dist.CLIENT)
public final class FirstPersonArmsAnimatable extends SessionAnimatable {

    public FirstPersonArmsAnimatable(ClientGloryKillSession session) {
        super(session);
    }

    private static boolean slim() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && "slim".equals(player.getModelName());
    }

    @Override
    public ResourceLocation modelResource() {
        GloryKillAssets assets = session.definition().assets();
        return slim() ? assets.playerFirstPersonModelSlim() : assets.playerFirstPersonModel();
    }

    @Override
    public ResourceLocation textureResource() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? player.getSkinTextureLocation() : DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public ResourceLocation animationResource() {
        return session.definition().assets().playerFirstPersonAnimations();
    }

    @Override
    public boolean isVictim() {
        return false;
    }
}
