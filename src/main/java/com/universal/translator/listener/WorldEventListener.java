package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Handles translation for in-world elements:
 * 1. NPC Nametags floating above entities
 * 2. Big Center-Screen Titles & Subtitles (Quest dialogues, cutscene text)
 * 3. Boss Bar Announcements
 * 4. Scoreboard Sidebar Objectives
 */
public class WorldEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldEventListener.class);

    private static Field TITLE_FIELD;
    private static Field SUBTITLE_FIELD;

    static {
        try {
            for (Field f : Gui.class.getDeclaredFields()) {
                if (Component.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    if ("title".equals(f.getName())) {
                        TITLE_FIELD = f;
                    } else if ("subtitle".equals(f.getName())) {
                        SUBTITLE_FIELD = f;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not reflect Gui title fields: {}", e.getMessage());
        }
    }

    private final TranslationEngine engine;
    private final TranslatorConfig config;

    public WorldEventListener(TranslationEngine engine, TranslatorConfig config) {
        this.engine = engine;
        this.config = config;
    }

    /**
     * Translates NPC nametags rendered above entities in the world (e.g. "[상인] 대장장이", "Lv.50 사막 도적").
     */
    @SubscribeEvent
    public void onRenderNameTag(RenderNameTagEvent.DoRender event) {
        if (!config.masterEnabled) return;

        EntityRenderState state = event.getEntityRenderState();
        if (state == null || state.nameTag == null) return;

        String rawName = state.nameTag.getString();
        if (rawName.isBlank() || !engine.needsTranslation(rawName)) return;

        String cached = engine.getCache().get(rawName.trim());
        if (cached != null && !cached.isBlank()) {
            state.nameTag = Component.literal(cached);
        } else {
            // Trigger async fetch into cache for subsequent frames
            engine.translateAsync(rawName.trim(), translated -> {
                // Cached result will be picked up on next render frame
            });
        }
    }

    /**
     * Translates GUI overlay layers: Center-Screen Titles/Subtitles and Scoreboard Sidebar.
     */
    @SubscribeEvent
    public void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!config.masterEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null) return;

        // 1. Center-Screen Title & Subtitle Translation (NPC Quest Prompts & Cutscenes)
        if (VanillaGuiLayers.TITLE.equals(event.getName())) {
            handleCenterTitles(mc.gui);
            return;
        }

        // 2. Scoreboard Sidebar Objective Translation
        if (VanillaGuiLayers.SCOREBOARD_SIDEBAR.equals(event.getName())) {
            handleScoreboardSidebar(mc);
        }
    }

    private void handleCenterTitles(Gui gui) {
        try {
            // Check Title
            if (TITLE_FIELD != null) {
                Component currentTitle = (Component) TITLE_FIELD.get(gui);
                if (currentTitle != null) {
                    String rawTitle = currentTitle.getString();
                    if (!rawTitle.isBlank() && engine.needsTranslation(rawTitle)) {
                        String cached = engine.getCache().get(rawTitle.trim());
                        if (cached != null && !cached.isBlank()) {
                            gui.setTitle(Component.literal(cached));
                        } else {
                            engine.translateAsync(rawTitle.trim(), translated -> {
                                Minecraft.getInstance().execute(() -> {
                                    if (translated != null && !translated.isBlank()) {
                                        gui.setTitle(Component.literal(translated));
                                    }
                                });
                            });
                        }
                    }
                }
            }

            // Check Subtitle
            if (SUBTITLE_FIELD != null) {
                Component currentSubtitle = (Component) SUBTITLE_FIELD.get(gui);
                if (currentSubtitle != null) {
                    String rawSub = currentSubtitle.getString();
                    if (!rawSub.isBlank() && engine.needsTranslation(rawSub)) {
                        String cached = engine.getCache().get(rawSub.trim());
                        if (cached != null && !cached.isBlank()) {
                            gui.setSubtitle(Component.literal(cached));
                        } else {
                            engine.translateAsync(rawSub.trim(), translated -> {
                                Minecraft.getInstance().execute(() -> {
                                    if (translated != null && !translated.isBlank()) {
                                        gui.setSubtitle(Component.literal(translated));
                                    }
                                });
                            });
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void handleScoreboardSidebar(Minecraft mc) {
        if (mc.level == null) return;
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective sidebarObj = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebarObj != null && sidebarObj.getDisplayName() != null) {
            String rawName = sidebarObj.getDisplayName().getString();
            if (!rawName.isBlank() && engine.needsTranslation(rawName)) {
                String cached = engine.getCache().get(rawName.trim());
                if (cached != null && !cached.isBlank()) {
                    sidebarObj.setDisplayName(Component.literal(cached));
                } else {
                    engine.translateAsync(rawName.trim(), translated -> {
                        if (translated != null && !translated.isBlank()) {
                            sidebarObj.setDisplayName(Component.literal(translated));
                        }
                    });
                }
            }
        }
    }

    /**
     * Translates Boss Bar alerts and raid announcements (e.g. "[이벤트] 월드 보스 출현!").
     */
    @SubscribeEvent
    public void onBossBarProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!config.masterEnabled) return;

        LerpingBossEvent bossEvent = event.getBossEvent();
        if (bossEvent == null || bossEvent.getName() == null) return;

        String rawName = bossEvent.getName().getString();
        if (rawName.isBlank() || !engine.needsTranslation(rawName)) return;

        String cached = engine.getCache().get(rawName.trim());
        if (cached != null && !cached.isBlank()) {
            bossEvent.setName(Component.literal(cached));
        } else {
            engine.translateAsync(rawName.trim(), translated -> {
                if (translated != null && !translated.isBlank()) {
                    bossEvent.setName(Component.literal(translated));
                }
            });
        }
    }
}
