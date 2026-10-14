package com.example.glorykill.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Everything that makes one mob's Glory Kill unique. Register an instance with
 * {@link GloryKillRegistry#register(EntityType, GloryKillDefinition)}.
 *
 * <p>Define a new execution either with the {@link Builder} directly, or by subclassing (see
 * {@code ZombieGloryKill}) when you also want to override the server side hooks.
 *
 * <p>A definition describes: player animation, victim animation, duration, damage tick, positioning, rotation,
 * sounds, particles, rewards and camera effects.
 *
 * <p>A definition is one <b>move</b> (one choreography). A mob can have several moves; one is picked per execution.
 * <ul>
 *   <li><b>specific</b> moves are bound to an entity type ({@link #builder}) - e.g. the zombie's own GeckoLib model;</li>
 *   <li><b>generic</b> moves ({@link #genericBuilder}) work on every hostile mob: the mob keeps its own model and the
 *       GeckoLib rig animates its whole body ({@link VictimMode#BODY}).</li>
 * </ul>
 */
public class GloryKillDefinition {

    private final ResourceLocation id;
    private final EntityType<?> victimType;
    private final ExecutionPosition position;
    private final GloryKillTimeline timeline;
    private final AnimationSet playerAnimations;
    private final AnimationSet victimAnimations;
    private final GloryKillAssets assets;
    private final List<GloryKillEffect.Timed> effects;
    private final Reward reward;
    private final GloryKillCameraSettings camera;
    private final int approachTicks;
    private final VictimMode victimMode;
    private final int weight;
    private final boolean universal;

    protected GloryKillDefinition(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.victimType = b.victimType; // null for generic moves
        this.position = Objects.requireNonNull(b.position, "position");
        this.timeline = Objects.requireNonNull(b.timeline, "timeline");
        this.playerAnimations = Objects.requireNonNull(b.playerAnimations, "playerAnimations");
        this.victimAnimations = Objects.requireNonNull(b.victimAnimations, "victimAnimations");
        this.assets = Objects.requireNonNull(b.assets, "assets");
        this.effects = Collections.unmodifiableList(new ArrayList<>(b.effects));
        this.reward = Objects.requireNonNull(b.reward, "reward");
        this.camera = Objects.requireNonNull(b.camera, "camera");
        this.approachTicks = Math.max(0, b.approachTicks);
        this.victimMode = Objects.requireNonNull(b.victimMode, "victimMode");
        this.weight = Math.max(1, b.weight);
        this.universal = b.universal;
    }

    /** A move bound to one entity type. */
    public static Builder builder(ResourceLocation id, EntityType<?> victimType) {
        return new Builder(id, Objects.requireNonNull(victimType, "victimType"))
                .victimMode(VictimMode.MODEL);
    }

    /** A move that works on any hostile mob (the mob keeps its own model, the whole body is animated). */
    public static Builder genericBuilder(ResourceLocation id) {
        return new Builder(id, null).victimMode(VictimMode.BODY);
    }

    public ResourceLocation id() {
        return id;
    }

    /** The entity type this move is bound to, or {@code null} for a generic move. */
    @Nullable
    public EntityType<?> victimType() {
        return victimType;
    }

    /** How the victim is presented while executed. */
    public VictimMode victimMode() {
        return victimMode;
    }

    /** Relative probability of this move when a mob has several (at least 1). */
    public int weight() {
        return weight;
    }

    /**
     * Generic moves only: whether this move may be used on <b>modded</b> mobs by default. Modded models can be
     * anything, so only gentle, safe choreographies should be universal.
     */
    public boolean isUniversal() {
        return universal;
    }

    public ExecutionPosition position() {
        return position;
    }

    public GloryKillTimeline timeline() {
        return timeline;
    }

    public AnimationSet playerAnimations() {
        return playerAnimations;
    }

    public AnimationSet victimAnimations() {
        return victimAnimations;
    }

    public GloryKillAssets assets() {
        return assets;
    }

    public List<GloryKillEffect.Timed> effects() {
        return effects;
    }

    public Reward reward() {
        return reward;
    }

    public GloryKillCameraSettings camera() {
        return camera;
    }

    /** Ticks the entities glide from their start position to the execution spot (rendering only). */
    public int approachTicks() {
        return approachTicks;
    }

    /** Total number of ticks both entities stay locked. */
    public int durationTicks() {
        return Math.max(timeline.endTick(), position.durationTicks());
    }

    // ------------------------------------------------------------------ server side hooks (default: nothing)

    /** Called once when the execution starts, after both entities have been placed. */
    public void onStart(GloryKillContext context) {
    }

    /** Called on {@link GloryKillTimeline#impactTick()}. */
    public void onImpact(GloryKillContext context) {
    }

    /** Called right after the lethal damage and the reward have been applied. */
    public void onLethal(GloryKillContext context) {
    }

    /** Called when the execution ends (completed or cancelled). */
    public void onEnd(GloryKillContext context, EndReason reason) {
    }

    /** How the victim is drawn during an execution. */
    public enum VictimMode {
        /** The mob's model is replaced by the GeckoLib victim model (needs a rig that matches the mob, e.g. the zombie). */
        MODEL,
        /** The mob keeps its own model; the animated {@code root} bone of a GeckoLib rig transforms the whole body. */
        BODY
    }

    // ------------------------------------------------------------------ builder
    public static final class Builder {
        private final ResourceLocation id;
        @Nullable
        private final EntityType<?> victimType;
        private ExecutionPosition position = ExecutionPosition.facingEachOther(1.15D, 40);
        private GloryKillTimeline timeline = new GloryKillTimeline(10, 10, 15, 20, 30, 40);
        private AnimationSet playerAnimations;
        private AnimationSet victimAnimations;
        private GloryKillAssets assets;
        private final List<GloryKillEffect.Timed> effects = new ArrayList<>();
        private Reward reward = Reward.CONFIG_DEFAULT;
        private GloryKillCameraSettings camera = new GloryKillCameraSettings(0.0F, 1.0F);
        private int approachTicks = 4;
        private VictimMode victimMode = VictimMode.MODEL;
        private int weight = 1;
        private boolean universal;

        private Builder(ResourceLocation id, @Nullable EntityType<?> victimType) {
            this.id = id;
            this.victimType = victimType;
        }

        public Builder position(ExecutionPosition position) {
            this.position = position;
            return this;
        }

        public Builder timeline(GloryKillTimeline timeline) {
            this.timeline = timeline;
            return this;
        }

        public Builder playerAnimations(AnimationSet animations) {
            this.playerAnimations = animations;
            return this;
        }

        public Builder victimAnimations(AnimationSet animations) {
            this.victimAnimations = animations;
            return this;
        }

        public Builder assets(GloryKillAssets assets) {
            this.assets = assets;
            return this;
        }

        public Builder effect(int tick, GloryKillEffect effect) {
            this.effects.add(new GloryKillEffect.Timed(tick, effect));
            return this;
        }

        public Builder reward(Reward reward) {
            this.reward = reward;
            return this;
        }

        public Builder camera(GloryKillCameraSettings camera) {
            this.camera = camera;
            return this;
        }

        public Builder approachTicks(int ticks) {
            this.approachTicks = ticks;
            return this;
        }

        public Builder victimMode(VictimMode mode) {
            this.victimMode = mode;
            return this;
        }

        /** Relative pick probability (default 1). */
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        /** Allow this generic move on modded mobs too (default false). */
        public Builder universal(boolean universal) {
            this.universal = universal;
            return this;
        }

        /** Creates the move (use this for plain moves; subclasses pass the builder to {@code super(...)}). */
        public GloryKillDefinition build() {
            return new GloryKillDefinition(this);
        }
    }
}
