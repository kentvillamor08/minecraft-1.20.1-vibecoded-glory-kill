package com.example.glorykill.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.RenderUtils;

/**
 * Draws the player's held item on an animated bone of a GeckoLib model, so the weapon follows the arm during the
 * execution ("weapon / item interaction").
 *
 * <p>It composes the transforms of a bone chain exactly the way GeckoLib's own {@code renderRecursively} does
 * (translate, pivot, rotate, scale, un-pivot) and stops at the pivot of the last bone, where the item is drawn
 * with the vanilla display transform. For the first-person rig the chain is {@code right_arm -> item_anchor};
 * {@code item_anchor} carries a counter-rotation so that, at rest, the item looks exactly like a vanilla held item.
 */
@OnlyIn(Dist.CLIENT)
public final class HeldItemRenderer {

    private HeldItemRenderer() {
    }

    public static <T extends GeoAnimatable> void render(GeoModel<T> model, String[] boneChain, ItemStack stack,
                                                        ItemDisplayContext context, LivingEntity holder,
                                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                                        int packedLight) {
        if (stack.isEmpty() || boneChain.length == 0) {
            return;
        }
        var processor = model.getAnimationProcessor();
        for (String name : boneChain) {
            if (processor.getBone(name) == null) {
                return; // model has no such bone (yet) - nothing to attach to
            }
        }

        poseStack.pushPose();
        for (int i = 0; i < boneChain.length; i++) {
            var bone = processor.getBone(boneChain[i]);
            RenderUtils.translateMatrixToBone(poseStack, bone);
            RenderUtils.translateToPivotPoint(poseStack, bone);
            RenderUtils.rotateMatrixAroundBone(poseStack, bone);
            RenderUtils.scaleMatrixForBone(poseStack, bone);
            if (i < boneChain.length - 1) {
                RenderUtils.translateAwayFromPivotPoint(poseStack, bone);
            }
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(holder, stack, context, false, poseStack, bufferSource,
                holder.level(), packedLight, OverlayTexture.NO_OVERLAY, holder.getId());
        poseStack.popPose();
    }
}
