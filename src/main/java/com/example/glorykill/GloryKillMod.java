package com.example.glorykill;

import com.example.glorykill.common.GloryKillRegistry;
import com.example.glorykill.config.GloryKillConfig;
import com.example.glorykill.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Glory Kill - DOOM style executions for Minecraft 1.20.1 (Forge).
 *
 * <p>GeckoLib is a hard dependency: it is declared mandatory in {@code mods.toml} (Forge refuses to start without
 * it) and verified once more here. There is intentionally no GeckoLib-free fallback.
 */
@Mod(GloryKillMod.MOD_ID)
public class GloryKillMod {

    public static final String MOD_ID = "glorykill";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GloryKillMod() {
        if (!ModList.get().isLoaded("geckolib")) {
            throw new IllegalStateException("Glory Kill requires GeckoLib (geckolib-forge-1.20.1). Install it and restart.");
        }

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.SERVER, GloryKillConfig.SERVER_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, GloryKillConfig.CLIENT_SPEC);

        GloryKillRegistry.bootstrap();
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
        LOGGER.info("Glory Kill initialised with {} registered execution(s)", GloryKillRegistry.all().size());
    }
}
