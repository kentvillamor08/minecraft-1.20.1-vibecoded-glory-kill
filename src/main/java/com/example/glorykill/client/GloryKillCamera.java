package com.example.glorykill.client;

import com.example.glorykill.common.GloryKillMath;
import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;

import javax.annotation.Nullable;

/**
 * First-person camera control while the local player executes.
 *
 * <ul>
 *   <li><b>Locked look:</b> the player's real yaw/pitch are frozen (see {@link ClientGloryKillManager#enforceLock}); the
 *       camera is blended to the execution view (facing the victim) and back, so afterwards the player looks exactly
 *       where they were looking before.</li>
 *   <li><b>Animated camera:</b> the {@code camera} bone of the first-person GeckoLib model (rotation + position) is
 *       sampled every frame by {@link FirstPersonGloryKillRenderer} and applied to the view.</li>
 *   <li><b>Glide:</b> the camera follows the executor's approach from the start position to the execution spot.</li>
 *   <li><b>Shake / FOV punch:</b> triggered by {@code camera_shake} / {@code fov_punch} keyframes of the animation.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class GloryKillCamera {

    private static boolean active;
    @Nullable
    private static ClientGloryKillSession session;

    /** 0 = real camera, 1 = full execution camera. */
    private static float blend;
    private static float prevBlend;
    private static float targetYaw;
    private static float targetPitch;

    // sampled 'camera' bone: rotation in degrees (GeckoLib values), position in model pixels
    private static final float[] BONE_ROT = new float[3];
    private static final float[] BONE_POS = new float[3];
    private static long boneSampleFrame = -1L;

    private static float shakeStrength;
    private static float shakeDuration = 1.0F;
    private static float shakeRemaining;
    private static float fovPunch;
    private static float prevFovPunch;

    private static long frame;
    private static final double[] SAVED = new double[9];
    private static boolean positionApplied;

    private GloryKillCamera() {
    }

    /** Index of the frame currently being rendered (incremented at the start of every frame). */
    public static long frame() {
        return frame;
    }

    // ------------------------------------------------------------------ lifecycle
    public static void onLocalStart(ClientGloryKillSession newSession) {
        session = newSession;
        active = true;
        targetYaw = newSession.executorYaw();
        targetPitch = Mth.clamp(autoAimPitch(newSession) + newSession.definition().camera().viewPitch(), -40.0F, 50.0F);
        if (GloryKillConfig.blendInTicks() <= 0) {
            blend = 1.0F;
            prevBlend = 1.0F;
        }
    }

    /**
     * Pitch (degrees, positive = down) that looks from the executor's eyes at the victim's head, so tall mobs are
     * looked up at and small ones are looked down at. The definition's {@code viewPitch} is added on top.
     */
    private static float autoAimPitch(ClientGloryKillSession s) {
        LivingEntity victim = s.victimEntity();
        if (victim == null) {
            return 0.0F;
        }
        Vec3 from = s.executorTarget();
        Vec3 to = s.victimTarget();
        double eyeY = from.y + 1.62D;
        double headY = to.y + victim.getBbHeight() * 0.8D;
        double distance = Math.max(0.3D, Math.hypot(to.x - from.x, to.z - from.z));
        return (float) Math.toDegrees(Math.atan2(eyeY - headY, distance));
    }

    /** The execution is over - the view now blends back to the player's real look direction. */
    public static void onLocalEnd() {
        active = false;
    }

    /** Disconnect / world change: forget everything. */
    public static void reset() {
        active = false;
        session = null;
        blend = 0.0F;
        prevBlend = 0.0F;
        shakeRemaining = 0.0F;
        fovPunch = 0.0F;
        prevFovPunch = 0.0F;
        positionApplied = false;
        clearBoneSample();
    }

    public static void tick() {
        prevBlend = blend;
        if (active) {
            int in = GloryKillConfig.blendInTicks();
            blend = in <= 0 ? 1.0F : Math.min(1.0F, blend + 1.0F / in);
        } else if (blend > 0.0F) {
            int out = GloryKillConfig.blendOutTicks();
            blend = out <= 0 ? 0.0F : Math.max(0.0F, blend - 1.0F / out);
            if (blend <= 0.0F) {
                prevBlend = 0.0F;
                session = null;
                clearBoneSample();
            }
        }
        if (shakeRemaining > 0.0F) {
            shakeRemaining -= 1.0F;
        }
        prevFovPunch = fovPunch;
        fovPunch *= 0.7F;
        if (Math.abs(fovPunch) < 0.01F) {
            fovPunch = 0.0F;
        }
    }

    // ------------------------------------------------------------------ effects
    public static void shake(float intensity, int ticks, float definitionScale) {
        if (!GloryKillConfig.cameraShake()) {
            return;
        }
        shakeStrength = intensity * definitionScale * GloryKillConfig.cameraShakeIntensity();
        shakeDuration = Math.max(1, ticks);
        shakeRemaining = shakeDuration;
    }

    public static void punchFov(float degrees) {
        if (GloryKillConfig.fovPunch() && Math.abs(degrees) > Math.abs(fovPunch)) {
            fovPunch = degrees;
        }
    }

    // ------------------------------------------------------------------ camera bone sample (written by the renderer)
    public static void setBoneSample(float rotX, float rotY, float rotZ, float posX, float posY, float posZ) {
        BONE_ROT[0] = rotX;
        BONE_ROT[1] = rotY;
        BONE_ROT[2] = rotZ;
        BONE_POS[0] = posX;
        BONE_POS[1] = posY;
        BONE_POS[2] = posZ;
        boneSampleFrame = frame;
    }

    public static void clearBoneSample() {
        for (int i = 0; i < 3; i++) {
            BONE_ROT[i] = 0.0F;
            BONE_POS[i] = 0.0F;
        }
        boneSampleFrame = -1L;
    }

    /** The bone is only sampled while the arms are drawn (not with F1 / third person) - ignore stale samples. */
    private static boolean boneFresh() {
        return boneSampleFrame >= 0L && frame - boneSampleFrame <= 3L;
    }

    private static float sign(boolean invert) {
        return invert ? -1.0F : 1.0F;
    }

    // ------------------------------------------------------------------ view angles / FOV
    public static void computeAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active && blend <= 0.0F && prevBlend <= 0.0F && shakeRemaining <= 0.0F) {
            return;
        }
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float b = GloryKillMath.smoothstep(Mth.lerp(partial, prevBlend, blend));

        float yaw = GloryKillMath.lerpAngle(event.getYaw(), targetYaw, b);
        float pitch = Mth.lerp(b, event.getPitch(), targetPitch);
        float roll = event.getRoll();

        if (GloryKillConfig.cameraAnimation() && boneFresh()) {
            // GeckoLib stores X/Y rotations with the opposite sign of Blockbench; Blockbench +X = look down
            pitch -= BONE_ROT[0] * sign(GloryKillConfig.invertCameraPitch()) * b;
            yaw -= BONE_ROT[1] * sign(GloryKillConfig.invertCameraYaw()) * b;
            roll -= BONE_ROT[2] * sign(GloryKillConfig.invertCameraRoll()) * b;
        }

        if (shakeRemaining > 0.0F) {
            float age = (shakeDuration - shakeRemaining) + partial;
            float falloff = Mth.clamp((shakeRemaining - partial) / shakeDuration, 0.0F, 1.0F);
            float amplitude = shakeStrength * falloff * falloff;
            yaw += Mth.sin(age * 2.3F) * 1.2F * amplitude;
            pitch += Mth.sin(age * 3.1F + 1.7F) * 1.6F * amplitude;
            roll += Mth.sin(age * 2.7F + 0.6F) * 2.0F * amplitude;
        }

        event.setYaw(yaw);
        event.setPitch(Mth.clamp(pitch, -89.5F, 89.5F));
        event.setRoll(roll);
    }

    public static void computeFov(ViewportEvent.ComputeFov event) {
        if ((fovPunch == 0.0F && prevFovPunch == 0.0F) || !GloryKillConfig.fovPunch()) {
            return;
        }
        float punch = Mth.lerp((float) event.getPartialTick(), prevFovPunch, fovPunch);
        event.setFOV(event.getFOV() + punch);
    }

    // ------------------------------------------------------------------ view position
    /**
     * Called at the start of every rendered frame. While executing in first person the local player's position is
     * temporarily replaced by the glide position + animated camera offset, so the camera moves smoothly; everything is
     * restored in {@link #endFrame()} so game logic never sees the offset.
     */
    public static void beginFrame(float partialTick) {
        frame++;
        positionApplied = false;
        ClientGloryKillSession current = session;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (current == null || !active || player == null || !GloryKillConfig.cameraPositionAnimation()
                || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        Vec3 base = current.executorRenderPos(partialTick);
        Vec3 offset = boneOffset(current.executorYaw());
        double x = base.x + offset.x;
        double y = base.y + offset.y;
        double z = base.z + offset.z;

        SAVED[0] = player.getX();
        SAVED[1] = player.getY();
        SAVED[2] = player.getZ();
        SAVED[3] = player.xo;
        SAVED[4] = player.yo;
        SAVED[5] = player.zo;
        SAVED[6] = player.xOld;
        SAVED[7] = player.yOld;
        SAVED[8] = player.zOld;

        player.xo = x;
        player.yo = y;
        player.zo = z;
        player.xOld = x;
        player.yOld = y;
        player.zOld = z;
        player.setPosRaw(x, y, z);
        positionApplied = true;
    }

    public static void endFrame() {
        if (!positionApplied) {
            return;
        }
        positionApplied = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.setPosRaw(SAVED[0], SAVED[1], SAVED[2]);
        player.xo = SAVED[3];
        player.yo = SAVED[4];
        player.zo = SAVED[5];
        player.xOld = SAVED[6];
        player.yOld = SAVED[7];
        player.zOld = SAVED[8];
    }

    /**
     * Converts the camera bone's position (Blockbench pixels: forward = -Z, right = -X, up = +Y) into a world space
     * offset for an execution that faces {@code yaw}.
     */
    private static Vec3 boneOffset(float yaw) {
        if (!boneFresh()) {
            return Vec3.ZERO;
        }
        float right = -BONE_POS[0] / 16.0F * sign(GloryKillConfig.invertCameraOffsetX());
        float up = BONE_POS[1] / 16.0F * sign(GloryKillConfig.invertCameraOffsetY());
        float forward = -BONE_POS[2] / 16.0F * sign(GloryKillConfig.invertCameraOffsetZ());
        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        // forward = (-sin, 0, cos), right = (-cos, 0, -sin)  (same frame as ExecutionPosition)
        return new Vec3(-cos * right - sin * forward, up, -sin * right + cos * forward);
    }
}
