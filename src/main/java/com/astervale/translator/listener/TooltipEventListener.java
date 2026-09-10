package com.astervale.translator.listener;

import com.astervale.translator.config.TranslatorConfig;
import com.astervale.translator.engine.TranslationEngine;
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

        List<Component> newTooltip = new ArrayList<>();

        for (int i = 0; i < tooltip.size(); i++) {
            Component originalComponent = tooltip.get(i);
            newTooltip.add(originalComponent);

            String rawText = originalComponent.getString();
            if (rawText.isBlank() || rawText.contains("[VI]")) continue;

            // Check if this line has Korean Hangul
            if (engine.containsKorean(rawText)) {
                String cachedTranslation = engine.getCache().get(rawText.trim());

                if (cachedTranslation != null && !cachedTranslation.isBlank()) {
                    // Check if the next line is already our translation
                    boolean alreadyInserted = false;
                    if (i + 1 < tooltip.size()) {
                        String nextLine = tooltip.get(i + 1).getString();
                        if (nextLine.contains("[VI]")) {
                            alreadyInserted = true;
                        }
                    }

                    if (!alreadyInserted) {
                        Component translatedComponent = Component.literal(config.tooltipPrefix + cachedTranslation);
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

        // Replace the tooltip lines with our enhanced list
        tooltip.clear();
        tooltip.addAll(newTooltip);
    }
}
