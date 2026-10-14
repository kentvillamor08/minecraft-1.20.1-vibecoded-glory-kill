package com.example.glorykill.client.animation;

import com.example.glorykill.client.ClientGloryKillSession;
import com.example.glorykill.common.GloryKillAssets;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The executing player as seen by everybody else (and by yourself in third person). Uses the executor's skin. */
@OnlyIn(Dist.CLIENT)
public final class ExecutorAnimatable extends SessionAnimatable {

    public ExecutorAnimatable(ClientGloryKillSession session) {
        super(session);
    }

    @Override
    public ResourceLocation modelResource() {
        GloryKillAssets assets = session.definition().assets();
        AbstractClientPlayer player = session.executorEntity();
        boolean slim = player != null && "slim".equals(player.getModelName());
        return slim ? assets.playerThirdPersonModelSlim() : assets.playerThirdPersonModel();
    }

    @Override
    public ResourceLocation textureResource() {
        AbstractClientPlayer player = session.executorEntity();
        return player != null ? player.getSkinTextureLocation() : DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public ResourceLocation animationResource() {
        return session.definition().assets().playerThirdPersonAnimations();
    }

    @Override
    public boolean isVictim() {
        return false;
    }
}
