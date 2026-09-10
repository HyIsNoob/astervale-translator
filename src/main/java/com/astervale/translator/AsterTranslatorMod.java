package com.astervale.translator;

import com.astervale.translator.config.TranslatorConfig;
import com.astervale.translator.engine.TranslationCache;
import com.astervale.translator.engine.TranslationEngine;
import com.astervale.translator.listener.ChatEventListener;
import com.astervale.translator.listener.TooltipEventListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@Mod(value = AsterTranslatorMod.MODID, dist = Dist.CLIENT)
public class AsterTranslatorMod {

    public static final String MODID = "astervale_translator";
    public static final String NAME = "Aster Vale Auto Translator";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static TranslationCache cache;
    private static TranslationEngine engine;
    private static TranslatorConfig config;

    public AsterTranslatorMod(IEventBus modEventBus, Dist dist) {
        if (!dist.isClient()) {
            LOGGER.info("{} is a client-side only mod. Skipping server initialization.", NAME);
            return;
        }

        LOGGER.info("Initializing {} v{} (Client-Side Stealth Edition)...", NAME, VERSION);

        Path configDir = FMLPaths.CONFIGDIR.get();

        // 1. Initialize Configuration
        config = new TranslatorConfig();
        config.init(configDir);

        // 2. Initialize Two-Tier Cache (L1 RAM + L2 Disk)
        cache = new TranslationCache();
        cache.init(configDir);

        // 3. Initialize Async Translation Engine
        engine = new TranslationEngine(cache);

        // 4. Register Event Listeners on NeoForge Game Event Bus
        NeoForge.EVENT_BUS.register(new ChatEventListener(engine, config));
        NeoForge.EVENT_BUS.register(new TooltipEventListener(engine, config));

        // 5. Register Shutdown Hook for clean cache flushing
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Flushing {} cache before exit...", NAME);
            engine.shutdown();
        }, "AsterTranslator-Shutdown"));

        LOGGER.info("{} initialized successfully! Real-time Korean->Vietnamese translation active.", NAME);
    }

    public static TranslationCache getCache() {
        return cache;
    }

    public static TranslationEngine getEngine() {
        return engine;
    }

    public static TranslatorConfig getConfig() {
        return config;
    }
}
