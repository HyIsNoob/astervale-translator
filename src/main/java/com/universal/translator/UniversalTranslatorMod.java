package com.universal.translator;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.CustomLexicon;
import com.universal.translator.engine.TranslationCache;
import com.universal.translator.engine.TranslationEngine;
import com.universal.translator.gui.TranslatorConfigScreen;
import com.universal.translator.listener.ChatEventListener;
import com.universal.translator.listener.KeybindHandler;
import com.universal.translator.listener.TooltipEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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

        // 5. Register Key Mapping on Mod Event Bus
        modEventBus.addListener(RegisterKeyMappingsEvent.class, KeybindHandler::onRegisterKeyMappings);

        // 6. Register NeoForge Config Screen Factory (Pause Menu -> Mods -> Config)
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (minecraft, screen) -> new TranslatorConfigScreen(screen));

        // 7. Register Game Events on NeoForge Event Bus
        NeoForge.EVENT_BUS.register(new ChatEventListener(engine, config));
        NeoForge.EVENT_BUS.register(new TooltipEventListener(engine, config));
        NeoForge.EVENT_BUS.register(new KeybindHandler());

        // 8. Register Client Commands (/translate & /translator)
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> {
            // /translator -> Opens GUI
            event.getDispatcher().register(Commands.literal("translator").executes(context -> {
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new TranslatorConfigScreen(null)));
                return 1;
            }));

            // /translate <text> -> Tests translation in chat directly
            event.getDispatcher().register(Commands.literal("translate")
                    .then(Commands.argument("text", StringArgumentType.greedyString()).executes(context -> {
                        String input = StringArgumentType.getString(context, "text");
                        engine.translateAsync(input, translated -> {
                            Minecraft client = Minecraft.getInstance();
                            if (client != null && client.gui != null && client.gui.getChat() != null) {
                                client.execute(() -> {
                                    client.gui.getChat().addMessage(Component.literal("§7[Gốc]: §f" + input));
                                    client.gui.getChat().addMessage(Component.literal(config.chatPrefix + translated));
                                });
                            }
                        });
                        return 1;
                    })));
        });

        // 9. Shutdown Hook
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
