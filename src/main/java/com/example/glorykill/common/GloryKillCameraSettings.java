package com.example.glorykill.common;

/**
 * Camera effects of a Glory Kill (first-person only).
 *
 * @param viewPitch  extra pitch in degrees (positive = down) added to the automatic aim at the victim's head -
 *                   the camera always frames the victim, whatever its height; 0 = look straight at the head
 * @param shakeScale multiplier applied to every {@code camera_shake} keyframe of the animation
 */
public record GloryKillCameraSettings(float viewPitch, float shakeScale) {
}
