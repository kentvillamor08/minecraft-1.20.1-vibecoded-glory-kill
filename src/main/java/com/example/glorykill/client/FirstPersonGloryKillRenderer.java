package com.example.glorykill.client;

import com.example.glorykill.client.animation.FirstPersonArmsAnimatable;
import com.example.glorykill.client.animation.GeoRenderBridge;
import com.example.glorykill.client.animation.HeldItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * Draws the GeckoLib first-person arms in place of the vanilla hands.
 *
 * <p>It runs inside Forge's {@code RenderHandEvent}: at that moment the PoseStack is in view space (the camera is the
 * origin, -Z is straight ahead) - exactly the space the first-person rig was authored in, so the model is drawn with
 * no extra transform. The vanilla hands are cancelled by {@link ClientEvents} for the duration of the execution.
 */
@OnlyIn(Dist.CLIENT)
public final class FirstPersonGloryKillRenderer {

    /** Bone chain the held item is attached to: right_arm -> item_anchor. */
    private static final String[] ITEM_CHAIN = {"right_arm", "item_anchor"};

    private FirstPersonGloryKillRenderer() {
    }

    public static void render(ClientGloryKillSession session, PoseStack poseStack, MultiBufferSource bufferSource,
                              int packedLight) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        FirstPersonArmsAnimatable animatable = session.firstPerson();
        GeoObjectRenderer<FirstPersonArmsAnimatable> renderer = session.firstPersonRenderer();

        poseStack.pushPose();
        GeoRenderBridge.render(renderer, poseStack, animatable, bufferSource, packedLight);
        HeldItemRenderer.render(session.firstPersonModel(), ITEM_CHAIN, player.getMainHandItem(),
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, player, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        sampleCameraBone(session);
    }

    /** Hands the animated 'camera' bone of this frame to {@link GloryKillCamera} (used by the next camera update). */
    private static void sampleCameraBone(ClientGloryKillSession session) {
        var bone = session.firstPersonModel().getAnimationProcessor().getBone("camera");
        if (bone == null) {
            GloryKillCamera.clearBoneSample();
            return;
        }
        GloryKillCamera.setBoneSample(
                (float) Math.toDegrees(bone.getRotX()),
                (float) Math.toDegrees(bone.getRotY()),
                (float) Math.toDegrees(bone.getRotZ()),
                bone.getPosX(), bone.getPosY(), bone.getPosZ());
    }
}
