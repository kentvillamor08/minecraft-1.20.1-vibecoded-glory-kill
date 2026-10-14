package com.example.glorykill.common;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

/**
 * Picks the "blood" particles that suit a victim: red for flesh, bone dust for skeletons, green goo for slimes, embers
 * for fire mobs, violet for the End and so on. The style is derived from the entity's registry id, so it also works
 * for modded mobs ({@code "mymod:bone_knight"} -> bone).
 */
public final class BloodStyle {

    private enum Style {
        FLESH(Blocks.REDSTONE_BLOCK, 0.55F, 0.02F, 0.02F),
        BONE(Blocks.BONE_BLOCK, 0.92F, 0.90F, 0.80F),
        SLIME(Blocks.SLIME_BLOCK, 0.35F, 0.80F, 0.30F),
        MAGMA(Blocks.MAGMA_BLOCK, 0.95F, 0.45F, 0.05F),
        ENDER(Blocks.PURPUR_BLOCK, 0.60F, 0.25F, 0.85F),
        MOSS(Blocks.MOSS_BLOCK, 0.30F, 0.65F, 0.25F);

        final BlockParticleOption chunks;
        final DustParticleOptions mist;

        Style(Block block, float red, float green, float blue) {
            this.chunks = new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
            this.mist = new DustParticleOptions(new Vector3f(red, green, blue), 1.4F);
        }
    }

    private BloodStyle() {
    }

    /** Chunky particles (flying pieces). */
    public static ParticleOptions chunks(LivingEntity victim) {
        return styleOf(victim).chunks;
    }

    /** A fine coloured mist. */
    public static ParticleOptions mist(LivingEntity victim) {
        return styleOf(victim).mist;
    }

    private static Style styleOf(LivingEntity victim) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        String path = key == null ? "" : key.getPath();
        if (path.contains("skeleton") || path.contains("bone") || path.contains("stray") || path.equals("wither")) {
            return Style.BONE;
        }
        if (path.contains("magma") || path.contains("blaze") || path.contains("fire") || path.contains("lava")) {
            return Style.MAGMA;
        }
        if (path.contains("slime")) {
            return Style.SLIME;
        }
        if (path.contains("ender") || path.contains("shulker") || path.contains("void")) {
            return Style.ENDER;
        }
        if (path.contains("creeper") || path.contains("moss")) {
            return Style.MOSS;
        }
        return Style.FLESH;
    }
}
