package com.universal.translator;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.CustomLexicon;
import com.universal.translator.engine.TranslationCache;
import com.universal.translator.engine.TranslationEngine;
import com.universal.translator.listener.ChatEventListener;
import com.universal.translator.listener.TooltipEventListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@Mod(value = UniversalTranslatorMod.MODID, dist = Dist.CLIENT)
public class UniversalTranslatorMod {

    public static final String MODID = "universal_translator";
    public static final String NAME = "Universal Auto Translator";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static TranslationCache cache;
    private static CustomLexicon lexicon;
    private static TranslationEngine engine;
    private static TranslatorConfig config;

    public UniversalTranslatorMod(IEventBus modEventBus, Dist dist) {
        if (!dist.isClient()) {
            LOGGER.info("{} is a client-side only mod. Skipping server initialization.", NAME);
            return;
        }

        LOGGER.info("Initializing {} v{} (Client-Side Stealth Edition)...", NAME, VERSION);

        Path configDir = FMLPaths.CONFIGDIR.get();

        // 1. Initialize Configuration
        config = new TranslatorConfig();
        config.init(configDir);

        // 2. Initialize Custom Lexicon Dictionary
        lexicon = new CustomLexicon();
        lexicon.init(configDir);

        // 3. Initialize Two-Tier Cache (L1 RAM + L2 Disk)
        cache = new TranslationCache();
        cache.init(configDir);

        // 4. Initialize Universal Translation Engine
        engine = new TranslationEngine(cache, lexicon, config);

        // 5. Register Event Listeners on NeoForge Game Event Bus
        NeoForge.EVENT_BUS.register(new ChatEventListener(engine, config));
        NeoForge.EVENT_BUS.register(new TooltipEventListener(engine, config));

        // 6. Register Shutdown Hook for clean cache flushing
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Flushing {} cache before exit...", NAME);
            engine.shutdown();
        }, "UniversalTranslator-Shutdown"));

        LOGGER.info("{} initialized successfully! Real-time translation active (Target: {}).", NAME, config.targetLanguage);
    }

    public static TranslationCache getCache() {
        return cache;
    }

    public static CustomLexicon getLexicon() {
        return lexicon;
    }

    public static TranslationEngine getEngine() {
        return engine;
    }

    public static TranslatorConfig getConfig() {
        return config;
    }
}
