package com.example.glorykill.common.definitions;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.common.AnimationSet;
import com.example.glorykill.common.ExecutionPosition;
import com.example.glorykill.common.GloryKillAssets;
import com.example.glorykill.common.GloryKillCameraSettings;
import com.example.glorykill.common.GloryKillDefinition;
import com.example.glorykill.common.GloryKillEffect;
import com.example.glorykill.common.GloryKillRegistry;
import com.example.glorykill.common.GloryKillTimeline;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * The generic moves: they work on EVERY hostile mob. The mob keeps its own model (armour, held items and all) and the
 * animated {@code root} bone of {@code geo/glory_kill_generic.geo.json} transforms its whole body (lean, lift, squash,
 * spin ...). Players use the same first/third-person GeckoLib rigs as always.
 *
 * <pre>
 *  move         player                          victim                              weight  modded mobs
 *  stab         thrust and rip (classic)        lurches, recoils                    1       YES (the universal move)
 *  slam         both fists overhead, hammer     squashed flat, crushed twice        2       no
 *  neck_snap    grabs the head and twists       lifted, wrung round, goes limp      2       no
 *  uppercut     crouch, launching punch         thrown up, spinning, comes down     2       no
 *  rip          grabs both sides, pulls apart   stretched thin, then bursts          2       no
 * </pre>
 *
 * Every move ends with the mob standing neutral and dies at {@code lethalTick}; the vanilla death animation (the mob
 * keels over) then plays by itself, so every mob falls the way it normally does. {@code endTick = lethalTick + 20}
 * matches the time a corpse needs to disappear.
 *
 * <p>Animation files (see the README for the Blockbench guide):
 * <pre>
 *  animations/{move}_player_first_person.animation.json   player_glory_kill_{move}_start | _execute | _finish
 *  animations/{move}_player.animation.json                player_glory_kill_{move}_start | _execute | _finish
 *  animations/{move}_victim.animation.json                victim_glory_kill_{move}_start | _execute | _death
 * </pre>
 * ({@code stab} reuses the zombie's player animations.)
 */
public final class GenericGloryKills {

    private GenericGloryKills() {
    }

    public static void registerAll() {
        GloryKillRegistry.registerGeneric(stab());
        GloryKillRegistry.registerGeneric(slam());
        GloryKillRegistry.registerGeneric(neckSnap());
        GloryKillRegistry.registerGeneric(uppercut());
        GloryKillRegistry.registerGeneric(rip());
    }

    private static ResourceLocation id(String move) {
        return new ResourceLocation(GloryKillMod.MOD_ID, move);
    }

    private static AnimationSet playerSet(String move) {
        return new AnimationSet("player_glory_kill_" + move + "_start", "player_glory_kill_" + move + "_execute",
                "player_glory_kill_" + move + "_finish");
    }

    private static AnimationSet victimSet(String move) {
        return new AnimationSet("victim_glory_kill_" + move + "_start", "victim_glory_kill_" + move + "_execute",
                "victim_glory_kill_" + move + "_death");
    }

    // ------------------------------------------------------------------ stab (classic) - the universal move
    /** Right-hand thrust and rip. The only move modded mobs get by default (gentle, works on any shape). */
    static GloryKillDefinition stab() {
        return GloryKillDefinition.genericBuilder(id("stab"))
                .universal(true)
                .weight(1)
                .position(ExecutionPosition.facingEachOther(1.15D, 40))
                .timeline(new GloryKillTimeline(10, 10, 15, 20, 30, 40))
                // same player choreography as the zombie's
                .playerAnimations(new AnimationSet("player_glory_kill_start", "player_glory_kill_execute", "player_glory_kill_finish"))
                .victimAnimations(victimSet("stab"))
                .assets(GloryKillAssets.generic("zombie", "stab"))
                .camera(new GloryKillCameraSettings(0.0F, 1.0F))
                .effect(0, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 0.7F))
                .effect(10, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.8F))
                .effect(10, GloryKillEffect.particles(ParticleTypes.CRIT, 12, 0.25D, 0.25D, 0.65D, false))
                .effect(10, GloryKillEffect.bloodMist(12, 0.25D, 0.02D, 0.65D))
                .effect(15, GloryKillEffect.bloodChunks(14, 0.3D, 0.12D, 0.8D))
                .effect(20, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F))
                .effect(20, GloryKillEffect.bloodChunks(40, 0.45D, 0.2D, 0.7D))
                .effect(20, GloryKillEffect.bloodMist(24, 0.4D, 0.04D, 0.7D))
                .effect(20, GloryKillEffect.particles(ParticleTypes.CRIT, 18, 0.4D, 0.5D, 0.7D, false))
                .effect(32, GloryKillEffect.sound(SoundEvents.GENERIC_BIG_FALL, 0.7F, 0.9F))
                .build();
    }

    // ------------------------------------------------------------------ slam
    /** Both fists overhead, brought down like a hammer. The victim is squashed twice. */
    static GloryKillDefinition slam() {
        return GloryKillDefinition.genericBuilder(id("slam"))
                .weight(2)
                .position(ExecutionPosition.facingEachOther(1.2D, 38))
                .timeline(new GloryKillTimeline(14, 14, 16, 18, 22, 38))
                .playerAnimations(playerSet("slam"))
                .victimAnimations(victimSet("slam"))
                .assets(GloryKillAssets.generic("slam", "slam"))
                .camera(new GloryKillCameraSettings(4.0F, 1.1F))
                .effect(0, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7F, 0.6F))
                .effect(12, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.6F))
                .effect(14, GloryKillEffect.sound(SoundEvents.ANVIL_LAND, 0.8F, 0.9F))
                .effect(14, GloryKillEffect.particles(ParticleTypes.POOF, 10, 0.4D, 0.05D, 0.05D, false))
                .effect(14, GloryKillEffect.bloodMist(10, 0.3D, 0.02D, 0.5D))
                .effect(16, GloryKillEffect.bloodChunks(12, 0.3D, 0.1D, 0.5D))
                .effect(18, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.5F))
                .effect(18, GloryKillEffect.sound(SoundEvents.GENERIC_EXPLODE, 0.35F, 1.2F))
                .effect(18, GloryKillEffect.bloodChunks(45, 0.45D, 0.2D, 0.4D))
                .effect(18, GloryKillEffect.particles(ParticleTypes.POOF, 12, 0.5D, 0.08D, 0.1D, false))
                .effect(36, GloryKillEffect.sound(SoundEvents.GENERIC_BIG_FALL, 0.7F, 1.0F))
                .build();
    }

    // ------------------------------------------------------------------ neck snap
    /** The player grabs the head with both hands and wrings it round. */
    static GloryKillDefinition neckSnap() {
        return GloryKillDefinition.genericBuilder(id("neck_snap"))
                .weight(2)
                .position(ExecutionPosition.facingEachOther(1.0D, 42))
                .timeline(new GloryKillTimeline(12, 12, 16, 22, 26, 42))
                .playerAnimations(playerSet("neck_snap"))
                .victimAnimations(victimSet("neck_snap"))
                .assets(GloryKillAssets.generic("neck_snap", "neck_snap"))
                .camera(new GloryKillCameraSettings(2.0F, 0.9F))
                .effect(4, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7F, 0.9F))
                .effect(12, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8F, 0.6F))
                .effect(16, GloryKillEffect.sound(SoundEvents.TURTLE_EGG_CRACK, 0.9F, 0.7F))
                .effect(22, GloryKillEffect.sound(SoundEvents.TURTLE_EGG_CRACK, 1.2F, 0.5F))
                .effect(22, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.5F))
                .effect(22, GloryKillEffect.bloodMist(10, 0.25D, 0.02D, 0.9D))
                .effect(22, GloryKillEffect.particles(ParticleTypes.CRIT, 14, 0.3D, 0.3D, 0.9D, false))
                .effect(40, GloryKillEffect.sound(SoundEvents.GENERIC_BIG_FALL, 0.6F, 1.1F))
                .build();
    }

    // ------------------------------------------------------------------ uppercut
    /** The player crouches and launches the mob into the air with one punch. It spins up, then drops. */
    static GloryKillDefinition uppercut() {
        return GloryKillDefinition.genericBuilder(id("uppercut"))
                .weight(2)
                .position(ExecutionPosition.facingEachOther(1.1D, 44))
                .timeline(new GloryKillTimeline(8, 8, 10, 24, 26, 44))
                .playerAnimations(playerSet("uppercut"))
                .victimAnimations(victimSet("uppercut"))
                .assets(GloryKillAssets.generic("uppercut", "uppercut"))
                .camera(new GloryKillCameraSettings(-8.0F, 1.0F)) // looks up to follow the launch
                .effect(6, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.5F))
                .effect(8, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F))
                .effect(8, GloryKillEffect.sound(SoundEvents.GENERIC_EXPLODE, 0.3F, 1.4F))
                .effect(8, GloryKillEffect.particles(ParticleTypes.POOF, 10, 0.3D, 0.06D, 0.3D, false))
                .effect(8, GloryKillEffect.bloodChunks(20, 0.3D, 0.15D, 0.3D))
                .effect(10, GloryKillEffect.particles(ParticleTypes.CRIT, 14, 0.4D, 0.4D, 0.8D, false))
                .effect(24, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 0.6F))
                .effect(24, GloryKillEffect.bloodChunks(30, 0.4D, 0.15D, 0.5D))
                .effect(42, GloryKillEffect.sound(SoundEvents.GENERIC_BIG_FALL, 0.8F, 0.8F))
                .build();
    }

    // ------------------------------------------------------------------ rip
    /** The player grabs the mob on both sides and pulls it apart. */
    static GloryKillDefinition rip() {
        return GloryKillDefinition.genericBuilder(id("rip"))
                .weight(2)
                .position(ExecutionPosition.facingEachOther(1.05D, 46))
                .timeline(new GloryKillTimeline(10, 10, 18, 26, 30, 46))
                .playerAnimations(playerSet("rip"))
                .victimAnimations(victimSet("rip"))
                .assets(GloryKillAssets.generic("rip", "rip"))
                .camera(new GloryKillCameraSettings(0.0F, 1.2F))
                .effect(6, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8F, 0.8F))
                .effect(10, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 0.7F))
                .effect(18, GloryKillEffect.sound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.7F, 0.5F))
                .effect(18, GloryKillEffect.bloodMist(16, 0.3D, 0.02D, 0.6D))
                .effect(22, GloryKillEffect.sound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.8F, 0.4F))
                .effect(22, GloryKillEffect.bloodChunks(20, 0.35D, 0.12D, 0.6D))
                .effect(26, GloryKillEffect.sound(SoundEvents.GENERIC_EXPLODE, 0.5F, 1.0F))
                .effect(26, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.5F))
                .effect(26, GloryKillEffect.bloodChunks(60, 0.6D, 0.25D, 0.5D))
                .effect(26, GloryKillEffect.bloodMist(40, 0.5D, 0.05D, 0.5D))
                .effect(26, GloryKillEffect.particles(ParticleTypes.CRIT, 25, 0.5D, 0.5D, 0.5D, false))
                .effect(44, GloryKillEffect.sound(SoundEvents.GENERIC_BIG_FALL, 0.7F, 0.9F))
                .build();
    }
}
