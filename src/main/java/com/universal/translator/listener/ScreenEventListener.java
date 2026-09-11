package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Handles comprehensive translation for screens, menus, and custom UIs:
 * 1. Screen Titles (Vanilla & Custom screens)
 * 2. Interactive Widgets & Buttons (screen.children() and renderables)
 * 3. Dialogue and Screen text fields via reflection
 * 4. Container / Inventory GUI Titles
 */
public class ScreenEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenEventListener.class);

    private static Field SCREEN_TITLE_FIELD;
    private static Field RENDERABLES_FIELD;

    static {
        try {
            for (Field f : Screen.class.getDeclaredFields()) {
                if (Component.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    SCREEN_TITLE_FIELD = f;
                } else if (List.class.isAssignableFrom(f.getType()) && "renderables".equals(f.getName())) {
                    f.setAccessible(true);
                    RENDERABLES_FIELD = f;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not reflect Screen fields: {}", e.getMessage());
        }
    }

    private final TranslationEngine engine;
    private final TranslatorConfig config;
    private long lastScanTime = 0;

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

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!config.masterEnabled) return;
        Screen screen = event.getScreen();
        if (screen == null) return;

        translateScreen(screen);
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!config.masterEnabled) return;
        Screen screen = event.getScreen();
        if (screen == null) return;

        // Throttle full scan to every 250ms to ensure 0% FPS impact
        long now = System.currentTimeMillis();
        if (now - lastScanTime < 250) {
            return;
        }
        lastScanTime = now;

        translateScreen(screen);
    }

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
            engine.translateAsync(raw.trim(), res -> {});
        }
    }

    /**
     * Translates everything inside the given screen:
     * 1. Screen Title
     * 2. All widgets & buttons from screen.children() and renderables
     * 3. Dialogue/label fields declared on custom Screen classes
     */
    public void translateScreen(Screen screen) {
        if (screen == null) return;

        // 1. Screen Title
        Component title = screen.getTitle();
        if (title != null) {
            String raw = title.getString();
            if (!raw.isBlank() && engine.needsTranslation(raw)) {
                String cached = engine.getCache().get(raw.trim());
                if (cached != null && !cached.isBlank()) {
                    tryUpdateScreenTitle(screen, Component.literal(cached));
                } else {
                    engine.translateAsync(raw.trim(), res -> {
                        if (res != null && !res.isBlank()) {
                            Minecraft.getInstance().execute(() -> tryUpdateScreenTitle(screen, Component.literal(res)));
                        }
                    });
                }
            }
        }

        // 2. Translate widgets in screen.children()
        try {
            for (GuiEventListener child : screen.children()) {
                processWidgetOrContainer(child);
            }
        } catch (Exception ignored) {
        }

        // 3. Translate renderables
        if (RENDERABLES_FIELD != null) {
            try {
                Object rendObj = RENDERABLES_FIELD.get(screen);
                if (rendObj instanceof List<?> list) {
                    for (Object r : list) {
                        if (r instanceof AbstractWidget widget) {
                            translateWidget(widget);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 4. Translate dialogue/label fields on the custom Screen class
        translateScreenFields(screen);
    }

    private void processWidgetOrContainer(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            translateWidget(widget);
        } else if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener sub : container.children()) {
                processWidgetOrContainer(sub);
            }
        }
    }

    private void translateWidget(AbstractWidget widget) {
        if (widget == null) return;

        Component msg = widget.getMessage();
        if (msg != null) {
            String raw = msg.getString();
            if (!raw.isBlank() && engine.needsTranslation(raw)) {
                String cached = engine.getCache().get(raw.trim());
                if (cached != null && !cached.isBlank()) {
                    widget.setMessage(Component.literal(cached));
                } else {
                    engine.translateAsync(raw.trim(), translated -> {
                        if (translated != null && !translated.isBlank()) {
                            Minecraft.getInstance().execute(() -> widget.setMessage(Component.literal(translated)));
                        }
                    });
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void translateScreenFields(Screen screen) {
        Class<?> clazz = screen.getClass();
        while (clazz != null && clazz != Screen.class && clazz != Object.class) {
            String pkg = clazz.getPackageName();
            if (pkg.startsWith("java.") || pkg.startsWith("javax.")) {
                break;
            }

            for (Field f : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
                    continue;
                }

                try {
                    f.setAccessible(true);
                    Object val = f.get(screen);
                    if (val == null) continue;

                    if (val instanceof Component comp) {
                        String raw = comp.getString();
                        if (!raw.isBlank() && engine.needsTranslation(raw)) {
                            String cached = engine.getCache().get(raw.trim());
                            if (cached != null && !cached.isBlank()) {
                                f.set(screen, Component.literal(cached));
                            } else {
                                engine.translateAsync(raw.trim(), res -> {
                                    if (res != null && !res.isBlank()) {
                                        Minecraft.getInstance().execute(() -> {
                                            try { f.set(screen, Component.literal(res)); } catch (Exception ignored) {}
                                        });
                                    }
                                });
                            }
                        }
                    } else if (val instanceof String str) {
                        if (!str.isBlank() && engine.needsTranslation(str)) {
                            String cached = engine.getCache().get(str.trim());
                            if (cached != null && !cached.isBlank()) {
                                f.set(screen, cached);
                            } else {
                                engine.translateAsync(str.trim(), res -> {
                                    if (res != null && !res.isBlank()) {
                                        Minecraft.getInstance().execute(() -> {
                                            try { f.set(screen, res); } catch (Exception ignored) {}
                                        });
                                    }
                                });
                            }
                        }
                    } else if (val instanceof List<?> list) {
                        for (int i = 0; i < list.size(); i++) {
                            Object item = list.get(i);
                            if (item instanceof Component itemComp) {
                                String raw = itemComp.getString();
                                if (!raw.isBlank() && engine.needsTranslation(raw)) {
                                    String cached = engine.getCache().get(raw.trim());
                                    if (cached != null && !cached.isBlank()) {
                                        try { ((List<Component>) list).set(i, Component.literal(cached)); } catch (Exception ignored) {}
                                    } else {
                                        final int idx = i;
                                        engine.translateAsync(raw.trim(), res -> {
                                            if (res != null && !res.isBlank()) {
                                                Minecraft.getInstance().execute(() -> {
                                                    try { ((List<Component>) list).set(idx, Component.literal(res)); } catch (Exception ignored) {}
                                                });
                                            }
                                        });
                                    }
                                }
                            } else if (item instanceof String itemStr) {
                                if (!itemStr.isBlank() && engine.needsTranslation(itemStr)) {
                                    String cached = engine.getCache().get(itemStr.trim());
                                    if (cached != null && !cached.isBlank()) {
                                        try { ((List<String>) list).set(i, cached); } catch (Exception ignored) {}
                                    } else {
                                        final int idx = i;
                                        engine.translateAsync(itemStr.trim(), res -> {
                                            if (res != null && !res.isBlank()) {
                                                Minecraft.getInstance().execute(() -> {
                                                    try { ((List<String>) list).set(idx, res); } catch (Exception ignored) {}
                                                });
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
