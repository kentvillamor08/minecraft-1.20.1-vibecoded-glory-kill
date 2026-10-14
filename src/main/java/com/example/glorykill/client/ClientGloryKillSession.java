package com.example.glorykill.client;

import com.example.glorykill.GloryKillMod;
import com.example.glorykill.client.animation.ExecutorAnimatable;
import com.example.glorykill.client.animation.FirstPersonArmsAnimatable;
import com.example.glorykill.client.animation.GloryKillGeoModel;
import com.example.glorykill.client.animation.SessionAnimatable;
import com.example.glorykill.client.animation.VictimAnimatable;
import com.example.glorykill.common.GloryKillDefinition;
import com.example.glorykill.common.GloryKillPhase;
import com.example.glorykill.config.GloryKillConfig;
import com.example.glorykill.network.StartGloryKillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The client's view of one running execution. Everything that is rendered (first-person arms, third-person executor,
 * victim) reads its clock, phase and positions from here, so all of them stay phase-locked to the server's timeline.
 *
 * <p>The GeckoLib objects (animatable + model + {@link GeoObjectRenderer}) are created lazily and are owned by the
 * session, so simultaneous executions never share animation state.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientGloryKillSession {

    private final GloryKillDefinition definition;
    private final int executorId;
    private final int victimId;
    private final Vec3 executorFrom;
    private final Vec3 executorTo;
    private final Vec3 victimFrom;
    private final Vec3 victimTo;
    private final float executorYaw;
    private final float victimYaw;
    private final boolean local;

    private int elapsed;
    private boolean ended;

    private final Map<GloryKillPhase, RawAnimation> playerAnimations = new EnumMap<>(GloryKillPhase.class);
    private final Map<GloryKillPhase, RawAnimation> victimAnimations = new EnumMap<>(GloryKillPhase.class);

    private FirstPersonArmsAnimatable firstPerson;
    private GloryKillGeoModel<FirstPersonArmsAnimatable> firstPersonModel;
    private GeoObjectRenderer<FirstPersonArmsAnimatable> firstPersonRenderer;

    private ExecutorAnimatable executor;
    private GloryKillGeoModel<ExecutorAnimatable> executorModel;
    private GeoObjectRenderer<ExecutorAnimatable> executorRenderer;

    private VictimAnimatable victim;
    private GloryKillGeoModel<VictimAnimatable> victimModel;
    private GeoObjectRenderer<VictimAnimatable> victimRenderer;

    ClientGloryKillSession(GloryKillDefinition definition, StartGloryKillPacket packet, boolean local) {
        this.definition = definition;
        this.executorId = packet.executorId;
        this.victimId = packet.victimId;
        this.executorFrom = packet.executorFrom;
        this.executorTo = packet.executorTo;
        this.executorYaw = packet.executorYaw;
        this.victimFrom = packet.victimFrom;
        this.victimTo = packet.victimTo;
        this.victimYaw = packet.victimYaw;
        this.local = local;
        this.elapsed = packet.elapsedTicks;
    }

    // ------------------------------------------------------------------ basic data
    public GloryKillDefinition definition() {
        return definition;
    }

    public int executorId() {
        return executorId;
    }

    public int victimId() {
        return victimId;
    }

    /** True if the executing player is the local player. */
    public boolean isLocal() {
        return local;
    }

    public float executorYaw() {
        return executorYaw;
    }

    /** Where the server placed the executor (feet). */
    public Vec3 executorTarget() {
        return executorTo;
    }

    /** Where the server placed the victim (feet). */
    public Vec3 victimTarget() {
        return victimTo;
    }

    public float victimYaw() {
        return victimYaw;
    }

    public int elapsedTicks() {
        return elapsed;
    }

    public GloryKillPhase phase() {
        return definition.timeline().phaseAt(elapsed);
    }

    /** Animation clock handed to GeckoLib: server synchronised ticks plus the partial tick of the current frame. */
    public double animationClock() {
        return elapsed + Minecraft.getInstance().getFrameTime();
    }

    @Nullable
    public AbstractClientPlayer executorEntity() {
        Level level = Minecraft.getInstance().level;
        return level != null && level.getEntity(executorId) instanceof AbstractClientPlayer player ? player : null;
    }

    @Nullable
    public LivingEntity victimEntity() {
        Level level = Minecraft.getInstance().level;
        return level != null && level.getEntity(victimId) instanceof LivingEntity living ? living : null;
    }

    // ------------------------------------------------------------------ clock
    void tick() {
        if (!Minecraft.getInstance().isPaused()) {
            elapsed++;
        }
        if (elapsed > definition.durationTicks() + 60) {
            ended = true; // failsafe: the End packet got lost
        }
    }

    /** Server resynchronisation; tiny differences are ignored so the animation never stutters. */
    void syncElapsed(int serverElapsed) {
        if (Math.abs(serverElapsed - elapsed) >= 2) {
            elapsed = serverElapsed;
        }
    }

    boolean isEnded() {
        return ended;
    }

    void markEnded() {
        ended = true;
    }

    // ------------------------------------------------------------------ positions (glide from start spot to execution spot)
    public Vec3 executorRenderPos(float partialTick) {
        return glide(executorFrom, executorTo, partialTick);
    }

    public Vec3 victimRenderPos(float partialTick) {
        return glide(victimFrom, victimTo, partialTick);
    }

    private Vec3 glide(Vec3 from, Vec3 to, float partialTick) {
        int ticks = definition.approachTicks();
        if (ticks <= 0) {
            return to;
        }
        float t = Mth.clamp((elapsed + partialTick) / ticks, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        return from.add(to.subtract(from).scale(eased));
    }

    // ------------------------------------------------------------------ animation
    /** The animation GeckoLib must play right now for this participant (phase driven, so the server timeline wins). */
    public RawAnimation rawAnimation(SessionAnimatable who) {
        GloryKillPhase phase = phase();
        Map<GloryKillPhase, RawAnimation> cache = who.isVictim() ? victimAnimations : playerAnimations;
        RawAnimation cached = cache.get(phase);
        if (cached == null) {
            String name = (who.isVictim() ? definition.victimAnimations() : definition.playerAnimations()).forPhase(phase);
            cached = RawAnimation.begin().thenPlayAndHold(name);
            cache.put(phase, cached);
        }
        return cached;
    }

    /**
     * Receives the custom instruction keyframes of the animation JSON ({@code "timeline"}). Instructions are separated
     * by ';' and take ':' separated arguments:
     * <pre>
     *  impact; react; kill; finish; start;     markers (logged when debugLogging is on)
     *  camera_shake:&lt;intensity&gt;:&lt;ticks&gt;;    first-person camera shake
     *  fov_punch:&lt;degrees&gt;;                  short FOV kick
     * </pre>
     */
    public void handleInstructions(SessionAnimatable who, @Nullable String instructions) {
        if (instructions == null) {
            return;
        }
        for (String raw : instructions.split(";")) {
            String instruction = raw.trim();
            if (instruction.isEmpty()) {
                continue;
            }
            if (GloryKillConfig.debugLogging()) {
                GloryKillMod.LOGGER.info("[GloryKill] {} keyframe '{}' at tick {}", who.getClass().getSimpleName(), instruction, elapsed);
            }
            // camera effects only come from the first-person animation of the local player
            if (!(who instanceof FirstPersonArmsAnimatable) || !local) {
                continue;
            }
            String[] parts = instruction.split(":");
            switch (parts[0].toLowerCase(Locale.ROOT)) {
                case "camera_shake" -> GloryKillCamera.shake(number(parts, 1, 1.0F), Math.round(number(parts, 2, 8.0F)),
                        definition.camera().shakeScale());
                case "fov_punch" -> GloryKillCamera.punchFov(number(parts, 1, 4.0F));
                default -> {
                }
            }
        }
    }

    private static float number(String[] parts, int index, float fallback) {
        if (index >= parts.length) {
            return fallback;
        }
        try {
            return Float.parseFloat(parts[index].trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------ lazily created GeckoLib objects
    public FirstPersonArmsAnimatable firstPerson() {
        if (firstPerson == null) {
            firstPerson = new FirstPersonArmsAnimatable(this);
        }
        return firstPerson;
    }

    public GeoObjectRenderer<FirstPersonArmsAnimatable> firstPersonRenderer() {
        if (firstPersonRenderer == null) {
            firstPersonModel = new GloryKillGeoModel<>();
            firstPersonRenderer = new GeoObjectRenderer<>(firstPersonModel);
        }
        return firstPersonRenderer;
    }

    public GloryKillGeoModel<FirstPersonArmsAnimatable> firstPersonModel() {
        firstPersonRenderer();
        return firstPersonModel;
    }

    public ExecutorAnimatable executor() {
        if (executor == null) {
            executor = new ExecutorAnimatable(this);
        }
        return executor;
    }

    public GeoObjectRenderer<ExecutorAnimatable> executorRenderer() {
        if (executorRenderer == null) {
            executorModel = new GloryKillGeoModel<>();
            executorRenderer = new GeoObjectRenderer<>(executorModel);
        }
        return executorRenderer;
    }

    public VictimAnimatable victim() {
        if (victim == null) {
            victim = new VictimAnimatable(this);
        }
        return victim;
    }

    public GeoObjectRenderer<VictimAnimatable> victimRenderer() {
        if (victimRenderer == null) {
            victimModel = new GloryKillGeoModel<>();
            victimRenderer = new GeoObjectRenderer<>(victimModel);
        }
        return victimRenderer;
    }

    public GloryKillGeoModel<VictimAnimatable> victimModel() {
        victimRenderer();
        return victimModel;
    }

    /** Convenience for {@code Entity} look-ups by the renderers. */
    @Nullable
    public Entity entity(int id) {
        Level level = Minecraft.getInstance().level;
        return level == null ? null : level.getEntity(id);
    }
}
