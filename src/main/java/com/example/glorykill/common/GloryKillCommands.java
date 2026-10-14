package com.example.glorykill.common;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.config.GloryKillConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Operator test commands:
 * <ul>
 *   <li>{@code /glorykill stagger}       - weakens and staggers the mob you are looking at</li>
 *   <li>{@code /glorykill force}         - executes the mob you are looking at (random move), ignoring health / look checks</li>
 *   <li>{@code /glorykill force <move>}  - same, with a specific move, e.g. {@code /glorykill force slam}</li>
 *   <li>{@code /glorykill moves}         - lists every registered move</li>
 * </ul>
 * Players can never be targeted.
 */
@Mod.EventBusSubscriber(modid = GloryKillMod.MOD_ID)
public final class GloryKillCommands {

    private GloryKillCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("glorykill")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("force")
                        .executes(ctx -> force(ctx.getSource(), null))
                        .then(Commands.argument("move", StringArgumentType.greedyString()) // greedy: word() would reject the ":" of "glorykill:slam"
                                .executes(ctx -> force(ctx.getSource(), StringArgumentType.getString(ctx, "move")))))
                .then(Commands.literal("stagger").executes(ctx -> stagger(ctx.getSource())))
                .then(Commands.literal("moves").executes(ctx -> listMoves(ctx.getSource()))));
    }

    private static int force(CommandSourceStack source, @Nullable String moveName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GloryKillDefinition move = null;
        if (moveName != null) {
            ResourceLocation id = ResourceLocation.tryParse(moveName.contains(":") ? moveName : GloryKillMod.MOD_ID + ":" + moveName);
            move = id == null ? null : GloryKillRegistry.byId(id);
            if (move == null) {
                source.sendFailure(Component.literal("Unknown move '" + moveName + "' - see /glorykill moves"));
                return 0;
            }
        }
        LivingEntity target = lookTarget(player, 6.0D);
        if (target == null) {
            source.sendFailure(Component.translatable("commands.glorykill.no_target"));
            return 0;
        }
        GloryKillManager.StartResult result = GloryKillManager.tryStart(player, target, true, move);
        if (result != GloryKillManager.StartResult.OK) {
            source.sendFailure(Component.translatable(result.translationKey()));
            return 0;
        }
        return 1;
    }

    private static int stagger(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LivingEntity target = lookTarget(player, 8.0D);
        if (target == null) {
            source.sendFailure(Component.translatable("commands.glorykill.no_target"));
            return 0;
        }
        if (!GloryKillRegistry.isExecutable(target)) {
            source.sendFailure(Component.translatable("commands.glorykill.not_executable"));
            return 0;
        }
        float weakHealth = Math.max(1.0F, target.getMaxHealth() * (float) GloryKillConfig.healthThreshold() * 0.75F);
        target.setHealth(Math.min(target.getHealth(), weakHealth));
        StaggerManager.stagger(target);
        source.sendSuccess(() -> Component.translatable("commands.glorykill.staggered"), false);
        return 1;
    }

    private static int listMoves(CommandSourceStack source) {
        StringBuilder text = new StringBuilder("Glory Kill moves:");
        for (GloryKillDefinition move : GloryKillRegistry.all()) {
            text.append("\n  ").append(move.id()).append("  weight ").append(move.weight());
            if (move.victimType() == null) {
                text.append(move.isUniversal() ? "  - every hostile mob, modded included" : "  - every hostile mob (modded: only if moddedMobsUseAllMoves)");
            } else {
                ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(move.victimType());
                text.append("  - ").append(type).append(" only");
            }
            if (GloryKillConfig.isMoveDisabled(move.id())) {
                text.append("  [DISABLED in config]");
            }
        }
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return GloryKillRegistry.all().size();
    }

    /** The living entity closest to the crosshair (within 15 degrees) inside {@code range} blocks. Never a player. */
    @Nullable
    private static LivingEntity lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double bestDot = Math.cos(Math.toRadians(15.0D));
        LivingEntity best = null;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), e -> e != player && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player))) {
            if (GloryKillMath.distanceSqToBox(eye, candidate.getBoundingBox()) > range * range) {
                continue;
            }
            Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eye);
            double length = toCandidate.length();
            if (length < 1.0E-4D) {
                continue;
            }
            double dot = look.dot(toCandidate) / length;
            if (dot >= bestDot) {
                bestDot = dot;
                best = candidate;
            }
        }
        return best;
    }
}
