package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

public class ChatEventListener {

    private final TranslationEngine engine;
    private final TranslatorConfig config;

    public ChatEventListener(TranslationEngine engine, TranslatorConfig config) {
        this.engine = engine;
        this.config = config;
    }

    @SubscribeEvent
    public void onPlayerChat(ClientChatReceivedEvent.Player event) {
        handleChat(event.getMessage());
    }

    @SubscribeEvent
    public void onSystemChat(ClientChatReceivedEvent.System event) {
        handleChat(event.getMessage());
    }

    private void handleChat(Component message) {
        if (!config.chatTranslationEnabled || message == null) return;

        String rawText = message.getString();
        if (rawText.isBlank()) return;

        // Skip messages that already contain translation tag
        if (rawText.contains("[VI]") || rawText.contains("[TRANS]")) return;

        // Check if text contains foreign characters needing translation
        if (!engine.needsTranslation(rawText)) return;

        // Asynchronously translate the message
        engine.translateAsync(rawText, translated -> {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.gui == null || client.gui.getChat() == null) return;

            client.execute(() -> {
                String prefix = config.chatPrefix != null ? config.chatPrefix : "  §b[VI] §f";
                Component translatedComponent = Component.literal(prefix + translated);
                client.gui.getChat().addMessage(translatedComponent);
            });
        });
    }
}
