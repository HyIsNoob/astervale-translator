package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

public class TooltipEventListener {

    private final TranslationEngine engine;
    private final TranslatorConfig config;

    public TooltipEventListener(TranslationEngine engine, TranslatorConfig config) {
        this.engine = engine;
        this.config = config;
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!config.tooltipTranslationEnabled) return;

        List<Component> tooltip = event.getToolTip();
        if (tooltip == null || tooltip.isEmpty()) return;

        boolean shiftDown = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        boolean holdShiftMode = config.tooltipHoldShift;

        // If in Hold Shift mode and shift is NOT held, check if item has foreign text to show hint
        if (holdShiftMode && !shiftDown) {
            boolean hasForeign = false;
            for (Component c : tooltip) {
                if (engine.needsTranslation(c.getString())) {
                    hasForeign = true;
                    break;
                }
            }
            if (hasForeign) {
                tooltip.add(Component.literal("§8[Hold §eShift§8 for translation]"));
            }
            return;
        }

        List<Component> newTooltip = new ArrayList<>();
        String currentTag = "[" + config.targetLanguage.toUpperCase() + "]";

        for (int i = 0; i < tooltip.size(); i++) {
            Component originalComponent = tooltip.get(i);
            newTooltip.add(originalComponent);

            String rawText = originalComponent.getString();
            if (rawText.isBlank() || rawText.contains(currentTag) || rawText.contains("[VI]") || rawText.contains("[TRANS]")) continue;

            // Check if this line contains foreign characters needing translation
            if (engine.needsTranslation(rawText)) {
                String cachedTranslation = engine.getCache().get(rawText.trim());

                if (cachedTranslation != null && !cachedTranslation.isBlank()) {
                    boolean alreadyInserted = false;
                    if (i + 1 < tooltip.size()) {
                        String nextLine = tooltip.get(i + 1).getString();
                        if (nextLine.contains(currentTag) || nextLine.contains("[VI]") || nextLine.contains("[TRANS]")) {
                            alreadyInserted = true;
                        }
                    }

                    if (!alreadyInserted) {
                        String prefix = config.tooltipPrefix != null ? config.tooltipPrefix : "§b[" + config.targetLanguage.toUpperCase() + "] §7";
                        Component translatedComponent = Component.literal(prefix + cachedTranslation);
                        newTooltip.add(translatedComponent);
                    }
                } else {
                    // Trigger async fetch into cache for subsequent renders
                    engine.translateAsync(rawText, res -> {
                        // Translation is saved to cache automatically
                    });
                }
            }
        }

        tooltip.clear();
        tooltip.addAll(newTooltip);
    }
}
