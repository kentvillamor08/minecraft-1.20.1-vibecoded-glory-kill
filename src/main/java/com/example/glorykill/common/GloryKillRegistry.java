package com.example.glorykill.common;

import com.example.glorykill.common.definitions.GenericGloryKills;
import com.example.glorykill.common.definitions.ZombieGloryKill;
import com.example.glorykill.config.GloryKillConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of Glory Kill <b>moves</b> (choreographies). Both logical sides populate it identically.
 *
 * <ul>
 *   <li><b>Specific</b> moves are bound to an entity type (the zombie's own GeckoLib model):
 *       {@code GloryKillRegistry.register(EntityType.ZOMBIE, new ZombieGloryKill())}. A type can have several.</li>
 *   <li><b>Generic</b> moves work on every hostile mob: {@link #registerGeneric}. Vanilla mobs may use all of them;
 *       modded mobs get the {@linkplain GloryKillDefinition#isUniversal() universal} ones unless the
 *       {@code moddedMobsUseAllMoves} config is on.</li>
 * </ul>
 *
 * Which entities can be executed at all is decided in one place: {@link #check(LivingEntity)}. Players are never
 * executable (no PvP executions).
 */
public final class GloryKillRegistry {

    /** Why an entity can / cannot be executed. */
    public enum Eligibility {
        OK,
        /** Players are never valid victims. */
        PLAYER,
        NOT_HOSTILE,
        DISABLED_TYPE,
        TOO_LARGE,
        NO_MOVES
    }

    private static final Map<EntityType<?>, List<GloryKillDefinition>> SPECIFIC = new LinkedHashMap<>();
    private static final List<GloryKillDefinition> GENERIC = new ArrayList<>();
    private static final Map<ResourceLocation, GloryKillDefinition> BY_ID = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private GloryKillRegistry() {
    }

    /** Registers the built-in moves. Idempotent. */
    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        register(EntityType.ZOMBIE, new ZombieGloryKill());
        GenericGloryKills.registerAll();
    }

    // ------------------------------------------------------------------ registration
    /** Registers a move that is bound to {@code type}. A type may have several moves. */
    public static synchronized void register(EntityType<?> type, GloryKillDefinition definition) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(definition, "definition");
        if (definition.victimType() == null || !definition.victimType().equals(type)) {
            throw new IllegalArgumentException("Definition " + definition.id() + " was built for " + definition.victimType()
                    + " but is being registered for " + type);
        }
        addById(definition);
        SPECIFIC.computeIfAbsent(type, t -> new ArrayList<>()).add(definition);
    }

    /** Registers a move for every hostile mob (build it with {@link GloryKillDefinition#genericBuilder}). */
    public static synchronized void registerGeneric(GloryKillDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definition.victimType() != null) {
            throw new IllegalArgumentException("Generic move " + definition.id() + " must not be bound to an entity type");
        }
        addById(definition);
        GENERIC.add(definition);
    }

    private static void addById(GloryKillDefinition definition) {
        if (BY_ID.containsKey(definition.id())) {
            throw new IllegalStateException("Duplicate Glory Kill id " + definition.id());
        }
        BY_ID.put(definition.id(), definition);
    }

    // ------------------------------------------------------------------ eligibility
    /**
     * The single rule set that decides whether an entity can be executed:
     * not a player, hostile ({@link Enemy}), not disabled in the config, small enough, and at least one move available.
     */
    public static Eligibility check(LivingEntity victim) {
        if (victim instanceof Player) {
            return Eligibility.PLAYER; // never player vs player
        }
        if (!(victim instanceof Enemy)) {
            return Eligibility.NOT_HOSTILE;
        }
        if (GloryKillConfig.isTypeDisabled(victim.getType())) {
            return Eligibility.DISABLED_TYPE;
        }
        if (victim.getBbWidth() > GloryKillConfig.maxVictimWidth() || victim.getBbHeight() > GloryKillConfig.maxVictimHeight()) {
            return Eligibility.TOO_LARGE;
        }
        if (!hasAnyMove(victim.getType())) {
            return Eligibility.NO_MOVES;
        }
        return Eligibility.OK;
    }

    public static boolean isExecutable(LivingEntity victim) {
        return check(victim) == Eligibility.OK;
    }

    private static boolean hasAnyMove(EntityType<?> type) {
        return SPECIFIC.containsKey(type) || (GloryKillConfig.genericMoves() && !GENERIC.isEmpty());
    }

    // ------------------------------------------------------------------ move selection
    /** All enabled moves that can be used on the given type, in registration order. */
    public static List<GloryKillDefinition> movesFor(EntityType<?> type) {
        List<GloryKillDefinition> pool = new ArrayList<>();
        List<GloryKillDefinition> specific = SPECIFIC.get(type);
        if (specific != null) {
            for (GloryKillDefinition definition : specific) {
                if (isEnabled(definition)) {
                    pool.add(definition);
                }
            }
        }
        if (GloryKillConfig.genericMoves()) {
            boolean everything = isVanilla(type) || GloryKillConfig.moddedMobsUseAllMoves();
            for (GloryKillDefinition definition : GENERIC) {
                if ((everything || definition.isUniversal()) && isEnabled(definition)) {
                    pool.add(definition);
                }
            }
        }
        return pool;
    }

    /**
     * Picks a move for this victim (weighted random).
     *
     * @param avoid a move to skip if there is any alternative (the player's previous move, for variety)
     * @return the move, or {@code null} if every move is disabled
     */
    @Nullable
    public static GloryKillDefinition pick(LivingEntity victim, RandomSource random, @Nullable ResourceLocation avoid) {
        List<GloryKillDefinition> pool = movesFor(victim.getType());
        if (pool.isEmpty()) {
            return null;
        }
        if (avoid != null && pool.size() > 1) {
            pool.removeIf(definition -> definition.id().equals(avoid));
        }
        return GloryKillMath.pickWeighted(pool, GloryKillDefinition::weight, random::nextInt);
    }

    private static boolean isEnabled(GloryKillDefinition definition) {
        return !GloryKillConfig.isMoveDisabled(definition.id());
    }

    private static boolean isVanilla(EntityType<?> type) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return key != null && "minecraft".equals(key.getNamespace());
    }

    // ------------------------------------------------------------------ lookup
    @Nullable
    public static GloryKillDefinition byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Collection<GloryKillDefinition> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
