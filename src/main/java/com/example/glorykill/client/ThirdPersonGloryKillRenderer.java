package com.example.glorykill.client;

import com.example.glorykill.client.animation.GeoRenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Third-person rendering of an execution for everybody who can see it: the executing player (GeckoLib body with the
 * player's skin) and the victim (GeckoLib mob model).
 *
 * <p>Both are drawn from Forge's {@code RenderPlayerEvent.Pre} / {@code RenderLivingEvent.Pre}, which are cancelled
 * only while the entity is part of a running execution - so the normal renderers are never touched otherwise. The
 * PoseStack handed to those events is already translated to the entity's render position; we shift it to the
 * server-decided execution spot (so both participants always agree on where they stand) and rotate it like vanilla.
 *
 * <p>The victim is presented in one of two ways ({@code GloryKillDefinition.VictimMode}):
 * <ul>
 *   <li><b>MODEL</b> - the mob's renderer is cancelled and a GeckoLib model of the mob is drawn instead
 *       ({@link #renderVictim}); needs a rig that matches the mob (the zombie).</li>
 *   <li><b>BODY</b> - the mob keeps drawing itself with its own renderer (so armour, held items, modded models all
 *       survive) while the animated {@code root} bone of a GeckoLib rig transforms the whole body
 *       ({@link #beginVictimBody} / {@link #endVictimBody}). Works for every mob.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class ThirdPersonGloryKillRenderer {

    /** Vanilla renders the player model at 15/16 scale. */
    private static final float PLAYER_SCALE = 0.9375F;

    private ThirdPersonGloryKillRenderer() {
    }

    public static void renderExecutor(AbstractClientPlayer player, ClientGloryKillSession session, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, float partialTick) {
        poseStack.pushPose();
        shiftToward(poseStack, player, session.executorRenderPos(partialTick), partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - session.executorYaw()));
        poseStack.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        GeoRenderBridge.render(session.executorRenderer(), poseStack, session.executor(), bufferSource, packedLight);
        poseStack.popPose();
    }

    public static void renderVictim(LivingEntity victim, ClientGloryKillSession session, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, float partialTick) {
        poseStack.pushPose();
        shiftToward(poseStack, victim, session.victimRenderPos(partialTick), partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - session.victimYaw()));
        if (victim.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        GeoRenderBridge.render(session.victimRenderer(), poseStack, session.victim(), bufferSource, packedLight);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------ BODY mode (any mob)
    /** The one bone of the generic victim rig; its position / rotation / scale move the whole mob. */
    private static final String ROOT_BONE = "root";

    /** Entities whose PoseStack level was pushed by {@link #beginVictimBody} and must be popped in {@link #endVictimBody}. */
    private static final Set<Integer> BODY_PUSHED = new HashSet<>();

    /**
     * Called from {@code RenderLivingEvent.Pre} (not cancelled): runs the GeckoLib rig and leaves the animated root-bone
     * transform on the PoseStack, so the mob's own renderer draws the mob lifted / squashed / spinning.
     * Must be paired with {@link #endVictimBody} from {@code RenderLivingEvent.Post}.
     */
    public static void beginVictimBody(LivingEntity victim, ClientGloryKillSession session, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, float partialTick) {
        poseStack.pushPose();
        BODY_PUSHED.add(victim.getId());
        shiftToward(poseStack, victim, session.victimRenderPos(partialTick), partialTick);

        // the rig is expressed in the mob's own frame: the one its renderer uses (interpolated body rotation)
        float bodyYaw = Mth.rotLerp(partialTick, victim.yBodyRotO, victim.yBodyRot);

        // 1) advance the animation (the rig's single bone has no visible geometry)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        GeoRenderBridge.render(session.victimRenderer(), poseStack, session.victim(), bufferSource, packedLight);
        poseStack.popPose();

        // 2) apply the animated root bone to the real mob (same helper sequence GeckoLib uses for a bone)
        var root = session.victimModel().getAnimationProcessor().getBone(ROOT_BONE);
        if (root != null) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            RenderUtils.translateMatrixToBone(poseStack, root);
            RenderUtils.translateToPivotPoint(poseStack, root);
            RenderUtils.rotateMatrixAroundBone(poseStack, root);
            RenderUtils.scaleMatrixForBone(poseStack, root);
            RenderUtils.translateAwayFromPivotPoint(poseStack, root);
            poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - 180.0F));
        }
    }

    /** Restores the PoseStack level pushed by {@link #beginVictimBody} (no-op for entities we did not touch). */
    public static void endVictimBody(LivingEntity victim, PoseStack poseStack) {
        if (BODY_PUSHED.remove(victim.getId())) {
            poseStack.popPose();
        }
    }

    /** Moves the stack from the entity's interpolated render position to {@code target}. */
    private static void shiftToward(PoseStack poseStack, Entity entity, Vec3 target, float partialTick) {
        double x = Mth.lerp((double) partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp((double) partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp((double) partialTick, entity.zOld, entity.getZ());
        poseStack.translate(target.x - x, target.y - y, target.z - z);
    }
}
