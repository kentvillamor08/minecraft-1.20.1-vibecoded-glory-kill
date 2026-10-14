package com.example.glorykill.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * The single place where a model is pushed through GeckoLib's {@link GeoObjectRenderer}.
 * {@code GeoObjectRenderer} is GeckoLib's renderer for "anything that is not an entity / block / item / armor":
 * it runs the animation controllers, applies the bone transforms and draws the baked model into the
 * {@link PoseStack} that we hand it - which is exactly what the first-person hand slot and the replaced
 * player / mob renderers need.
 *
 * <p>If a GeckoLib build ever changes this signature, this is the only line to adapt.
 */
@OnlyIn(Dist.CLIENT)
public final class GeoRenderBridge {

    private GeoRenderBridge() {
    }

    public static <T extends GeoAnimatable> void render(GeoObjectRenderer<T> renderer, PoseStack poseStack,
                                                        T animatable, MultiBufferSource bufferSource, int packedLight) {
        renderer.render(poseStack, animatable, bufferSource, null, null, packedLight);
    }
}
