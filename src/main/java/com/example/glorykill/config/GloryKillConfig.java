package com.example.glorykill.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Forge configuration.
 *
 * <ul>
 *   <li><b>SERVER</b> ({@code glorykill-server.toml}, per world, synchronised to every client): all gameplay values.</li>
 *   <li><b>CLIENT</b> ({@code glorykill-client.toml}): camera / rendering preferences.</li>
 * </ul>
 * Every accessor is safe to call before the config has loaded (it returns the default value).
 */
public final class GloryKillConfig {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Server, ForgeConfigSpec> server = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();
        Pair<Client, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private GloryKillConfig() {
    }

    // ------------------------------------------------------------------ server spec
    public static final class Server {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.DoubleValue healthThreshold;
        public final ForgeConfigSpec.DoubleValue maxDistance;
        public final ForgeConfigSpec.DoubleValue maxLookAngle;
        public final ForgeConfigSpec.BooleanValue requireLineOfSight;
        public final ForgeConfigSpec.DoubleValue staggerDurationSeconds;
        public final ForgeConfigSpec.DoubleValue staggerCooldownSeconds;
        public final ForgeConfigSpec.BooleanValue protectStaggered;
        public final ForgeConfigSpec.BooleanValue glowWhenStaggered;
        public final ForgeConfigSpec.DoubleValue gloryKillHeal;
        public final ForgeConfigSpec.BooleanValue bloodParticles;
        public final ForgeConfigSpec.BooleanValue gloryKillSounds;
        public final ForgeConfigSpec.BooleanValue invulnerableDuringExecution;
        public final ForgeConfigSpec.DoubleValue cancelDriftBlocks;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledEntityTypes;
        public final ForgeConfigSpec.BooleanValue genericMoves;
        public final ForgeConfigSpec.BooleanValue moddedMobsUseAllMoves;
        public final ForgeConfigSpec.DoubleValue maxVictimWidth;
        public final ForgeConfigSpec.DoubleValue maxVictimHeight;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disabledMoves;

        Server(ForgeConfigSpec.Builder b) {
            b.comment("Glory Kill gameplay settings (synchronised from the server to every client)").push("gloryKill");

            enabled = b.comment("Master switch for the whole Glory Kill system.")
                    .define("enabled", true);
            healthThreshold = b.comment("A mob becomes executable once its health is at or below this fraction of its max health.")
                    .defineInRange("healthThreshold", 0.20D, 0.01D, 1.0D);
            maxDistance = b.comment("Maximum distance in blocks between the player's eyes and the target's hitbox.")
                    .defineInRange("maxDistance", 2.5D, 1.0D, 8.0D);
            maxLookAngle = b.comment("Maximum angle in degrees between the player's view direction and the target (server side check).")
                    .defineInRange("maxLookAngle", 35.0D, 5.0D, 90.0D);
            requireLineOfSight = b.comment("The player must have line of sight to the target.")
                    .define("requireLineOfSight", true);
            staggerDurationSeconds = b.comment("How long a weakened mob stays staggered (and executable), in seconds.")
                    .defineInRange("staggerDuration", 5.0D, 0.5D, 60.0D);
            staggerCooldownSeconds = b.comment("After a stagger runs out the mob cannot be staggered again for this many seconds.")
                    .defineInRange("staggerCooldown", 5.0D, 0.0D, 120.0D);
            protectStaggered = b.comment("Staggered mobs cannot be killed by normal damage (they stay at 1 HP) so the execution stays available.")
                    .define("protectStaggered", true);
            glowWhenStaggered = b.comment("Staggered mobs glow so they are easy to spot.")
                    .define("glowWhenStaggered", true);
            gloryKillHeal = b.comment("Hearts restored to the player on a successful Glory Kill (1 heart = 2 HP). 2.0 = two hearts.")
                    .defineInRange("gloryKillHeal", 2.0D, 0.0D, 20.0D);
            bloodParticles = b.comment("Spawn blood particles during executions.")
                    .define("bloodParticles", true);
            gloryKillSounds = b.comment("Play the execution sound effects.")
                    .define("gloryKillSounds", true);
            invulnerableDuringExecution = b.comment("The executing player takes no damage while the execution plays.")
                    .define("invulnerableDuringExecution", true);
            cancelDriftBlocks = b.comment("Cancel the execution if the player is displaced further than this from the execution spot (anti-cheat / pistons).")
                    .defineInRange("cancelDriftBlocks", 3.0D, 0.5D, 16.0D);
            disabledEntityTypes = b.comment("Entity type ids for which Glory Kills are disabled, e.g. [\"minecraft:zombie\"].")
                    .defineListAllowEmpty("disabledEntityTypes", List.of(), o -> o instanceof String);
            genericMoves = b.comment("Every hostile mob (net.minecraft.world.entity.monster.Enemy) can be executed with the generic moves,",
                            "where the mob keeps its own model and its whole body is animated. Players are never executable.",
                            "If false, only mobs with their own definition (the zombie) can be executed.")
                    .define("genericMoves", true);
            moddedMobsUseAllMoves = b.comment("Modded hostile mobs only get the single universal move (stab) by default, because modded models",
                            "can be anything. Set to true to let them use every generic move as well.")
                    .define("moddedMobsUseAllMoves", false);
            maxVictimWidth = b.comment("Mobs wider than this (blocks) cannot be executed (excludes the Ghast, Ender Dragon, ...).")
                    .defineInRange("maxVictimWidth", 2.2D, 0.2D, 16.0D);
            maxVictimHeight = b.comment("Mobs taller than this (blocks) cannot be executed.")
                    .defineInRange("maxVictimHeight", 4.0D, 0.2D, 32.0D);
            disabledMoves = b.comment("Move ids that are never used, e.g. [\"glorykill:slam\", \"glorykill:rip\"]. /glorykill moves lists them.")
                    .defineListAllowEmpty("disabledMoves", List.of(), o -> o instanceof String);

            b.pop();
        }
    }

    // ------------------------------------------------------------------ client spec
    public static final class Client {
        public final ForgeConfigSpec.BooleanValue cameraShake;
        public final ForgeConfigSpec.DoubleValue cameraShakeIntensity;
        public final ForgeConfigSpec.BooleanValue fovPunch;
        public final ForgeConfigSpec.BooleanValue cameraAnimation;
        public final ForgeConfigSpec.BooleanValue cameraPositionAnimation;
        public final ForgeConfigSpec.BooleanValue firstPersonArms;
        public final ForgeConfigSpec.BooleanValue forceFirstPerson;
        public final ForgeConfigSpec.BooleanValue showPrompt;
        public final ForgeConfigSpec.BooleanValue useBundledVictimTexture;
        public final ForgeConfigSpec.IntValue blendInTicks;
        public final ForgeConfigSpec.IntValue blendOutTicks;
        public final ForgeConfigSpec.BooleanValue invertCameraPitch;
        public final ForgeConfigSpec.BooleanValue invertCameraYaw;
        public final ForgeConfigSpec.BooleanValue invertCameraRoll;
        public final ForgeConfigSpec.BooleanValue invertCameraOffsetX;
        public final ForgeConfigSpec.BooleanValue invertCameraOffsetY;
        public final ForgeConfigSpec.BooleanValue invertCameraOffsetZ;
        public final ForgeConfigSpec.BooleanValue debugLogging;

        Client(ForgeConfigSpec.Builder b) {
            b.comment("Glory Kill client settings").push("client");

            cameraShake = b.comment("Camera shake on impact / kill keyframes.")
                    .define("cameraShake", true);
            cameraShakeIntensity = b.comment("Multiplier for the camera shake strength.")
                    .defineInRange("cameraShakeIntensity", 1.0D, 0.0D, 3.0D);
            fovPunch = b.comment("Short FOV kick on impact / kill keyframes.")
                    .define("fovPunch", true);
            cameraAnimation = b.comment("Apply the rotation of the 'camera' bone of the first-person model to the player's view.")
                    .define("cameraAnimation", true);
            cameraPositionAnimation = b.comment("Also apply the position of the 'camera' bone and the approach glide (temporarily offsets the local player's render position).")
                    .define("cameraPositionAnimation", true);
            firstPersonArms = b.comment("Replace the vanilla first-person hands with the GeckoLib arms while executing.")
                    .define("firstPersonArms", true);
            forceFirstPerson = b.comment("Switch to the first-person camera while executing (restored afterwards).")
                    .define("forceFirstPerson", false);
            showPrompt = b.comment("Show the '[G] GLORY KILL' prompt when an executable target is in reach.")
                    .define("showPrompt", true);
            useBundledVictimTexture = b.comment("Use assets/glorykill/textures/glory_kill/<mob>.png instead of the vanilla mob texture.")
                    .define("useBundledVictimTexture", false);
            blendInTicks = b.comment("Ticks used to blend the view into the execution camera.")
                    .defineInRange("blendInTicks", 4, 0, 40);
            blendOutTicks = b.comment("Ticks used to blend the view back to the player's real look direction.")
                    .defineInRange("blendOutTicks", 6, 0, 40);

            b.comment("Calibration toggles: flip an axis if the camera bone moves the view the wrong way compared to Blockbench.").push("calibration");
            invertCameraPitch = b.define("invertCameraPitch", false);
            invertCameraYaw = b.define("invertCameraYaw", false);
            invertCameraRoll = b.define("invertCameraRoll", false);
            invertCameraOffsetX = b.define("invertCameraOffsetX", false);
            invertCameraOffsetY = b.define("invertCameraOffsetY", false);
            invertCameraOffsetZ = b.define("invertCameraOffsetZ", false);
            b.pop();

            debugLogging = b.comment("Log keyframe events and session lifecycle on the client.")
                    .define("debugLogging", false);
            b.pop();
        }
    }

    // ------------------------------------------------------------------ safe server accessors
    private static boolean serverLoaded() {
        return SERVER_SPEC.isLoaded();
    }

    private static boolean clientLoaded() {
        return CLIENT_SPEC.isLoaded();
    }

    public static boolean enabled() {
        return !serverLoaded() || SERVER.enabled.get();
    }

    public static double healthThreshold() {
        return serverLoaded() ? SERVER.healthThreshold.get() : 0.20D;
    }

    public static double maxDistance() {
        return serverLoaded() ? SERVER.maxDistance.get() : 2.5D;
    }

    public static double maxLookAngle() {
        return serverLoaded() ? SERVER.maxLookAngle.get() : 35.0D;
    }

    public static boolean requireLineOfSight() {
        return !serverLoaded() || SERVER.requireLineOfSight.get();
    }

    public static int staggerTicks() {
        return (int) Math.round((serverLoaded() ? SERVER.staggerDurationSeconds.get() : 5.0D) * 20.0D);
    }

    public static int staggerCooldownTicks() {
        return (int) Math.round((serverLoaded() ? SERVER.staggerCooldownSeconds.get() : 5.0D) * 20.0D);
    }

    public static boolean protectStaggered() {
        return !serverLoaded() || SERVER.protectStaggered.get();
    }

    public static boolean glowWhenStaggered() {
        return !serverLoaded() || SERVER.glowWhenStaggered.get();
    }

    /** Hearts healed per Glory Kill. */
    public static double healHearts() {
        return serverLoaded() ? SERVER.gloryKillHeal.get() : 2.0D;
    }

    public static boolean bloodParticles() {
        return !serverLoaded() || SERVER.bloodParticles.get();
    }

    public static boolean gloryKillSounds() {
        return !serverLoaded() || SERVER.gloryKillSounds.get();
    }

    public static boolean invulnerableDuringExecution() {
        return !serverLoaded() || SERVER.invulnerableDuringExecution.get();
    }

    public static double cancelDriftBlocks() {
        return serverLoaded() ? SERVER.cancelDriftBlocks.get() : 3.0D;
    }

    public static boolean genericMoves() {
        return !serverLoaded() || SERVER.genericMoves.get();
    }

    public static boolean moddedMobsUseAllMoves() {
        return serverLoaded() && SERVER.moddedMobsUseAllMoves.get();
    }

    public static double maxVictimWidth() {
        return serverLoaded() ? SERVER.maxVictimWidth.get() : 2.2D;
    }

    public static double maxVictimHeight() {
        return serverLoaded() ? SERVER.maxVictimHeight.get() : 4.0D;
    }

    public static boolean isMoveDisabled(ResourceLocation id) {
        return serverLoaded() && SERVER.disabledMoves.get().contains(id.toString());
    }

    public static boolean isTypeDisabled(EntityType<?> type) {
        if (!serverLoaded()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return key != null && SERVER.disabledEntityTypes.get().contains(key.toString());
    }

    // ------------------------------------------------------------------ safe client accessors
    public static boolean cameraShake() {
        return !clientLoaded() || CLIENT.cameraShake.get();
    }

    public static float cameraShakeIntensity() {
        return clientLoaded() ? CLIENT.cameraShakeIntensity.get().floatValue() : 1.0F;
    }

    public static boolean fovPunch() {
        return !clientLoaded() || CLIENT.fovPunch.get();
    }

    public static boolean cameraAnimation() {
        return !clientLoaded() || CLIENT.cameraAnimation.get();
    }

    public static boolean cameraPositionAnimation() {
        return !clientLoaded() || CLIENT.cameraPositionAnimation.get();
    }

    public static boolean firstPersonArms() {
        return !clientLoaded() || CLIENT.firstPersonArms.get();
    }

    public static boolean forceFirstPerson() {
        return clientLoaded() && CLIENT.forceFirstPerson.get();
    }

    public static boolean showPrompt() {
        return !clientLoaded() || CLIENT.showPrompt.get();
    }

    public static boolean useBundledVictimTexture() {
        return clientLoaded() && CLIENT.useBundledVictimTexture.get();
    }

    public static int blendInTicks() {
        return clientLoaded() ? CLIENT.blendInTicks.get() : 4;
    }

    public static int blendOutTicks() {
        return clientLoaded() ? CLIENT.blendOutTicks.get() : 6;
    }

    public static boolean invertCameraPitch() {
        return clientLoaded() && CLIENT.invertCameraPitch.get();
    }

    public static boolean invertCameraYaw() {
        return clientLoaded() && CLIENT.invertCameraYaw.get();
    }

    public static boolean invertCameraRoll() {
        return clientLoaded() && CLIENT.invertCameraRoll.get();
    }

    public static boolean invertCameraOffsetX() {
        return clientLoaded() && CLIENT.invertCameraOffsetX.get();
    }

    public static boolean invertCameraOffsetY() {
        return clientLoaded() && CLIENT.invertCameraOffsetY.get();
    }

    public static boolean invertCameraOffsetZ() {
        return clientLoaded() && CLIENT.invertCameraOffsetZ.get();
    }

    public static boolean debugLogging() {
        return clientLoaded() && CLIENT.debugLogging.get();
    }
}
