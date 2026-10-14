package com.example.glorykill.client.animation;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * One GeckoLib {@link GeoModel} for all three roles: the animatable itself says which {@code .geo.json},
 * {@code .animation.json} and texture to use. That is what lets a Glory Kill definition swap in completely different
 * Blockbench assets without touching any Java.
 */
@OnlyIn(Dist.CLIENT)
public final class GloryKillGeoModel<T extends SessionAnimatable> extends GeoModel<T> {

    public ResourceLocation getModelResource(T animatable) {
        return animatable.modelResource();
    }

    public ResourceLocation getTextureResource(T animatable) {
        return animatable.textureResource();
    }

    public ResourceLocation getAnimationResource(T animatable) {
        return animatable.animationResource();
    }

    // GeckoLib 4.7+ additionally hands the renderer to the getters. Delegating keeps every 4.x build working
    // (deliberately no @Override, so this also compiles against builds that do not have these overloads).
    public ResourceLocation getModelResource(T animatable, GeoRenderer<T> renderer) {
        return getModelResource(animatable);
    }

    public ResourceLocation getTextureResource(T animatable, GeoRenderer<T> renderer) {
        return getTextureResource(animatable);
    }

    public ResourceLocation getAnimationResource(T animatable, GeoRenderer<T> renderer) {
        return getAnimationResource(animatable);
    }
}
