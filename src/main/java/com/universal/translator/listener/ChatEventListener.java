package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatEventListener {

    // Regex to match Minecraft player chat prefixes: <Player>, [Rank] <Player>, Player:
    private static final Pattern SENDER_PATTERN = Pattern.compile(
            "^(?<sender>(?:\\[[^\\]]+\\]\\s*)?<[a-zA-Z0-9_]{1,16}>\\s*:?\\s*|(?:\\[[^\\]]+\\]\\s*)?[a-zA-Z0-9_]{2,16}:\\s+)(?<content>.+)$"
    );

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

        // Extract sender prefix if present (e.g. "<HyIsNoob> ")
        String senderPrefix = "";
        String textToTranslate = rawText.trim();

        Matcher matcher = SENDER_PATTERN.matcher(textToTranslate);
        if (matcher.find()) {
            senderPrefix = matcher.group("sender");
            textToTranslate = matcher.group("content").trim();
        }

        // Check if message content needs translation
        if (!engine.needsTranslation(textToTranslate)) return;

        final String finalSender = senderPrefix;
        engine.translateAsync(textToTranslate, translated -> {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.gui == null || client.gui.getChat() == null) return;

            client.execute(() -> {
                String prefix = config.chatPrefix != null ? config.chatPrefix : "  §b[VI] §f";
                Component translatedComponent = Component.literal(prefix + finalSender + translated);
                client.gui.getChat().addMessage(translatedComponent);
            });
        });
    }
}
