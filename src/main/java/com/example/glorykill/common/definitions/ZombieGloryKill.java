package com.example.glorykill.common.definitions;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.common.AnimationSet;
import com.example.glorykill.common.ExecutionPosition;
import com.example.glorykill.common.GloryKillAssets;
import com.example.glorykill.common.GloryKillCameraSettings;
import com.example.glorykill.common.GloryKillDefinition;
import com.example.glorykill.common.GloryKillEffect;
import com.example.glorykill.common.GloryKillTimeline;
import com.example.glorykill.common.Reward;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/**
 * Reference implementation of a <b>specific</b> move: the zombie's own GeckoLib model replaces the vanilla model
 * ({@code VictimMode.MODEL}). The zombie can also be executed with every generic move ({@link GenericGloryKills}).
 * Copy this class to give another mob its own model-based move - only the numbers, the animation names and the
 * effects change, the core system stays untouched.
 *
 * <pre>
 *  tick  0   START   player + zombie start their *_start animations        (grab sound)
 *  tick 10   EXECUTE impact: the strike lands                              (crit + blood, camera shake keyframe)
 *  tick 15           the zombie reacts                                     (hurt sound, blood)
 *  tick 20           lethal damage is applied by the SERVER, reward given  (death sound, big blood burst)
 *  tick 30   FINISH  player_glory_kill_finish / zombie_glory_kill_death
 *  tick 40           execution ends, controls return
 * </pre>
 */
public class ZombieGloryKill extends GloryKillDefinition {

    public static final ResourceLocation ID = new ResourceLocation(GloryKillMod.MOD_ID, "zombie");

    private static final ResourceLocation VANILLA_ZOMBIE_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");

    private static final DustParticleOptions BLOOD_MIST = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.02F), 1.4F);
    private static final BlockParticleOption BLOOD_CHUNKS =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());

    public ZombieGloryKill() {
        super(GloryKillDefinition.builder(ID, EntityType.ZOMBIE)
                .position(ExecutionPosition.facingEachOther(1.15D, 40))
                .timeline(new GloryKillTimeline(10, 10, 15, 20, 30, 40))
                .playerAnimations(new AnimationSet("player_glory_kill_start", "player_glory_kill_execute", "player_glory_kill_finish"))
                .victimAnimations(new AnimationSet("zombie_glory_kill_start", "zombie_glory_kill_execute", "zombie_glory_kill_death"))
                .assets(GloryKillAssets.standard("zombie", VANILLA_ZOMBIE_TEXTURE))
                .reward(Reward.CONFIG_DEFAULT)
                .camera(new GloryKillCameraSettings(1.0F, 1.0F))
                .approachTicks(4)
                .weight(3) // the zombie's own GeckoLib model is picked more often than each generic move
                // tick 0 - the grab
                .effect(0, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 0.7F))
                // tick 10 - impact
                .effect(10, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.8F))
                .effect(10, GloryKillEffect.sound(SoundEvents.ZOMBIE_HURT, 1.0F, 0.6F))
                .effect(10, GloryKillEffect.particles(ParticleTypes.CRIT, 12, 0.25D, 0.25D, 0.65D, false))
                .effect(10, GloryKillEffect.particles(BLOOD_MIST, 14, 0.25D, 0.02D, 0.65D, true))
                // tick 15 - the zombie reacts
                .effect(15, GloryKillEffect.sound(SoundEvents.ZOMBIE_HURT, 0.9F, 0.5F))
                .effect(15, GloryKillEffect.particles(BLOOD_CHUNKS, 18, 0.3D, 0.12D, 0.8D, true))
                // tick 20 - lethal damage
                .effect(20, GloryKillEffect.sound(SoundEvents.ZOMBIE_DEATH, 1.0F, 0.7F))
                .effect(20, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F))
                .effect(20, GloryKillEffect.particles(BLOOD_CHUNKS, 45, 0.45D, 0.2D, 0.7D, true))
                .effect(20, GloryKillEffect.particles(BLOOD_MIST, 30, 0.4D, 0.04D, 0.7D, true))
                .effect(20, GloryKillEffect.particles(ParticleTypes.CRIT, 20, 0.4D, 0.5D, 0.7D, false))
                // tick 32 - the body hits the ground
                .effect(32, GloryKillEffect.sound(SoundEvents.GENERIC_BIG_FALL, 0.8F, 0.9F)));
    }
}
