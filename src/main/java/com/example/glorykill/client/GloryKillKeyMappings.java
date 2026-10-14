package com.example.glorykill.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** The execute key. Default: G. It is a normal Minecraft key mapping, so it is rebindable in Options > Controls. */
@OnlyIn(Dist.CLIENT)
public final class GloryKillKeyMappings {

    public static final String CATEGORY = "key.categories.glorykill";

    public static final KeyMapping EXECUTE = new KeyMapping("key.glorykill.execute", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private GloryKillKeyMappings() {
    }
}
