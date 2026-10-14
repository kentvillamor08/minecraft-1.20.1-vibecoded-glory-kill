package com.example.glorykill.common;

import com.example.glorykill.GloryKillMod;
import net.minecraft.resources.ResourceLocation;

/**
 * Resource locations of the GeckoLib assets used by one Glory Kill. All paths are complete GeckoLib resource paths
 * ({@code geo/...geo.json}, {@code animations/...animation.json}).
 */
public record GloryKillAssets(
        ResourceLocation playerFirstPersonModel,
        ResourceLocation playerFirstPersonModelSlim,
        ResourceLocation playerFirstPersonAnimations,
        ResourceLocation playerThirdPersonModel,
        ResourceLocation playerThirdPersonModelSlim,
        ResourceLocation playerThirdPersonAnimations,
        ResourceLocation victimModel,
        ResourceLocation victimAnimations,
        ResourceLocation victimTexture,
        ResourceLocation victimBundledTexture) {

    /**
     * Default asset layout for a mob called {@code name}:
     * <pre>
     *  geo/glory_kill_player(_slim).geo.json                 shared third-person player rig
     *  geo/glory_kill_player_first_person(_slim).geo.json    shared first-person arms rig
     *  geo/glory_kill_{name}.geo.json                         victim rig
     *  animations/{name}_player.animation.json                third-person executor animations
     *  animations/{name}_player_first_person.animation.json   first-person animations
     *  animations/{name}_victim.animation.json                victim animations
     *  textures/glory_kill/{name}.png                         optional bundled victim texture
     * </pre>
     *
     * @param victimTexture texture used for the victim (normally the vanilla mob texture)
     */
    public static GloryKillAssets standard(String name, ResourceLocation victimTexture) {
        return new GloryKillAssets(
                asset("geo/glory_kill_player_first_person.geo.json"),
                asset("geo/glory_kill_player_first_person_slim.geo.json"),
                asset("animations/" + name + "_player_first_person.animation.json"),
                asset("geo/glory_kill_player.geo.json"),
                asset("geo/glory_kill_player_slim.geo.json"),
                asset("animations/" + name + "_player.animation.json"),
                asset("geo/glory_kill_" + name + ".geo.json"),
                asset("animations/" + name + "_victim.animation.json"),
                victimTexture,
                asset("textures/glory_kill/" + name + ".png"));
    }

    /**
     * Assets of a generic (whole-body) move.
     *
     * <pre>
     *  geo/glory_kill_generic.geo.json                          victim rig: a single 'root' bone
     *  animations/{victimFiles}_victim.animation.json           victim animations (root bone: position / rotation / scale)
     *  animations/{playerFiles}_player(_first_person).animation.json   player animations (may be shared between moves)
     * </pre>
     */
    public static GloryKillAssets generic(String playerFiles, String victimFiles) {
        // nothing of the generic rig is visible; the texture only has to exist
        ResourceLocation placeholder = asset("textures/glory_kill/zombie.png");
        return new GloryKillAssets(
                asset("geo/glory_kill_player_first_person.geo.json"),
                asset("geo/glory_kill_player_first_person_slim.geo.json"),
                asset("animations/" + playerFiles + "_player_first_person.animation.json"),
                asset("geo/glory_kill_player.geo.json"),
                asset("geo/glory_kill_player_slim.geo.json"),
                asset("animations/" + playerFiles + "_player.animation.json"),
                asset("geo/glory_kill_generic.geo.json"),
                asset("animations/" + victimFiles + "_victim.animation.json"),
                placeholder,
                placeholder);
    }

    private static ResourceLocation asset(String path) {
        return new ResourceLocation(GloryKillMod.MOD_ID, path);
    }
}
