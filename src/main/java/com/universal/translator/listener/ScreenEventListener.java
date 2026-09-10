package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Handles translation for screens and menus:
 * 1. Container / Inventory GUI Titles (Chest Menus, NPC Shops, Dialogue Choice screens)
 * 2. General Screen Titles (Merchant screens, Anvil, Sign, Book screens)
 */
public class ScreenEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenEventListener.class);

    private static Field SCREEN_TITLE_FIELD;

    static {
        try {
            for (Field f : Screen.class.getDeclaredFields()) {
                if (Component.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    SCREEN_TITLE_FIELD = f;
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not reflect Screen title field: {}", e.getMessage());
        }
    }

    private final TranslationEngine engine;
    private final TranslatorConfig config;

    public ScreenEventListener(TranslationEngine engine, TranslatorConfig config) {
        this.engine = engine;
        this.config = config;
    }

    private void tryUpdateScreenTitle(Screen screen, Component newTitle) {
        if (SCREEN_TITLE_FIELD != null && screen != null && newTitle != null) {
            try {
                SCREEN_TITLE_FIELD.set(screen, newTitle);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Translates Container / Inventory GUI titles (NPC dialogue menus, shops, quest rewards).
     */
    @SubscribeEvent
    public void onContainerRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!config.masterEnabled) return;

        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (screen == null) return;

        Component title = screen.getTitle();
        if (title == null) return;

        String raw = title.getString();
        if (raw.isBlank() || !engine.needsTranslation(raw)) return;

        String cached = engine.getCache().get(raw.trim());
        if (cached != null && !cached.isBlank()) {
            tryUpdateScreenTitle(screen, Component.literal(cached));
        } else {
            engine.translateAsync(raw.trim(), res -> {
                // Next render will pick up cached translation
            });
        }
    }

    /**
     * Translates titles when any new Screen is initialized (e.g. MerchantScreen, BookViewScreen).
     */
    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!config.masterEnabled) return;

        Screen screen = event.getScreen();
        if (screen == null) return;

        Component title = screen.getTitle();
        if (title == null) return;

        String raw = title.getString();
        if (raw.isBlank() || !engine.needsTranslation(raw)) return;

        String cached = engine.getCache().get(raw.trim());
        if (cached != null && !cached.isBlank()) {
            tryUpdateScreenTitle(screen, Component.literal(cached));
        } else {
            engine.translateAsync(raw.trim(), res -> {
                Minecraft.getInstance().execute(() -> {
                    if (res != null && !res.isBlank()) {
                        tryUpdateScreenTitle(screen, Component.literal(res));
                    }
                });
            });
        }
    }
}
