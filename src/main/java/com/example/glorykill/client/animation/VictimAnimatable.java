package com.example.glorykill.client.animation;

import com.example.glorykill.client.ClientGloryKillSession;
import com.example.glorykill.common.GloryKillAssets;
import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The mob being executed. Replaces the vanilla model for the duration of the execution. */
@OnlyIn(Dist.CLIENT)
public final class VictimAnimatable extends SessionAnimatable {

    public VictimAnimatable(ClientGloryKillSession session) {
        super(session);
    }

    @Override
    public ResourceLocation modelResource() {
        return session.definition().assets().victimModel();
    }

    @Override
    public ResourceLocation textureResource() {
        GloryKillAssets assets = session.definition().assets();
        return GloryKillConfig.useBundledVictimTexture() ? assets.victimBundledTexture() : assets.victimTexture();
    }

    @Override
    public ResourceLocation animationResource() {
        return session.definition().assets().victimAnimations();
    }

    @Override
    public boolean isVictim() {
        return true;
    }
}
