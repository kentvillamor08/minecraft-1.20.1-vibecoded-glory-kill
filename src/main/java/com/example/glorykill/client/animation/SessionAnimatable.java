package com.example.glorykill.client.animation;

import com.example.glorykill.client.ClientGloryKillSession;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base class of the three GeckoLib animatables of one execution (first-person arms, third-person executor, victim).
 *
 * <p>The animation clock is the execution clock of the {@link ClientGloryKillSession} (server synchronised ticks +
 * partial tick), NOT wall-clock time. That is what keeps player and victim phase-locked: every frame the single
 * controller asks the session which {@code GloryKillPhase} we are in and plays the matching animation
 * ({@code *_start}, {@code *_execute}, {@code *_finish/death}).
 *
 * <p>Custom instruction keyframes in the animation JSON ({@code "timeline"}) are forwarded to the session, which is
 * how Blockbench-authored keyframes ({@code impact;}, {@code kill;}, {@code camera_shake:0.7:8;}, ...) reach the game.
 */
@OnlyIn(Dist.CLIENT)
public abstract class SessionAnimatable implements GeoAnimatable {

    protected final ClientGloryKillSession session;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected SessionAnimatable(ClientGloryKillSession session) {
        this.session = session;
    }

    /** Complete GeckoLib path of the {@code .geo.json}. */
    public abstract ResourceLocation modelResource();

    /** Texture path (may be a dynamic texture such as the player's skin). */
    public abstract ResourceLocation textureResource();

    /** Complete GeckoLib path of the {@code .animation.json}. */
    public abstract ResourceLocation animationResource();

    /** True for the victim rig (uses the victim's {@code AnimationSet}); false for both player rigs. */
    public abstract boolean isVictim();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<SessionAnimatable> controller = new AnimationController<>(this, "glory_kill", 0,
                state -> state.setAndContinue(session.rawAnimation(this)));
        controller.setCustomInstructionKeyframeHandler(
                event -> session.handleInstructions(this, event.getKeyframeData().getInstructions()));
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object relatedObject) {
        return session.animationClock();
    }
}
