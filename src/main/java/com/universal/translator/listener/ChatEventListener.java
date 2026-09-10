package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;
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

    /**
     * Outgoing Chat Translation: Translates what YOU type and send to the server.
     */
    @SubscribeEvent
    public void onClientSendMessage(ClientChatEvent event) {
        if (!config.masterEnabled || !config.outgoingTranslationEnabled) return;

        String original = event.getMessage();
        if (original == null || original.isBlank()) return;

        // Never translate commands
        if (original.startsWith("/")) return;

        // Bypass prefix: if user types '!hello' or '//hello', strip prefix and send raw
        if (original.startsWith("!") || original.startsWith("//")) {
            event.setMessage(original.substring(original.startsWith("//") ? 2 : 1));
            return;
        }

        String targetLang = config.outgoingTargetLanguage != null ? config.outgoingTargetLanguage : "ko";

        // Translate outgoing message before sending to server
        try {
            String translated = engine.translateSyncTarget(original, "auto", targetLang);
            if (translated != null && !translated.isBlank() && !translated.equalsIgnoreCase(original)) {
                event.setMessage(translated);

                // Friendly local chat confirmation
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.gui != null && client.gui.getChat() != null) {
                    String infoTag = "§7[Sent -> " + targetLang.toUpperCase() + "]: §8" + original + " §7» §f" + translated;
                    client.gui.getChat().addMessage(Component.literal(infoTag));
                }
            }
        } catch (Exception e) {
            // Keep original message if error occurs
        }
    }

    @SubscribeEvent
    public void onPlayerChat(ClientChatReceivedEvent.Player event) {
        handleChat(event);
    }

    @SubscribeEvent
    public void onSystemChat(ClientChatReceivedEvent.System event) {
        if (!config.translateSystemMessages) return;
        handleChat(event);
    }

    private void handleChat(ClientChatReceivedEvent event) {
        if (!config.chatTranslationEnabled || event == null) return;
        Component message = event.getMessage();
        if (message == null) return;

        String rawText = message.getString();
        if (rawText.isBlank()) return;

        // Skip messages that already contain translation tag
        String tag = "[" + config.targetLanguage.toUpperCase() + "]";
        if (rawText.contains(tag) || rawText.contains("[VI]") || rawText.contains("[EN]") || rawText.contains("[TRANS]")) return;

        // Extract sender prefix if present (e.g. "<HyIsNoob> ")
        String senderPrefix = "";
        String textToTranslate = rawText.trim();

        Matcher matcher = SENDER_PATTERN.matcher(textToTranslate);
        if (matcher.find()) {
            senderPrefix = matcher.group("sender");
            textToTranslate = matcher.group("content").trim();
        }

        // Check if message is from the local player
        if (config.ignoreSelfChat && !senderPrefix.isEmpty() && Minecraft.getInstance().getUser() != null) {
            String myUsername = Minecraft.getInstance().getUser().getName();
            if (senderPrefix.contains("<" + myUsername + ">") || senderPrefix.startsWith(myUsername + ":")) {
                return;
            }
        }

        // Check if message content needs translation
        if (!engine.needsTranslation(textToTranslate)) return;

        final String contentToTranslate = textToTranslate;
        final String finalSender = senderPrefix;
        final String currentTag = config.chatPrefix != null ? config.chatPrefix : "  §b[" + config.targetLanguage.toUpperCase() + "] §f";

        // Check if translation is already cached
        String cached = engine.getCache().get(contentToTranslate);
        if (cached != null && !cached.isBlank()) {
            if (config.chatReplaceMode) {
                event.setMessage(Component.literal(finalSender + "§b[" + config.targetLanguage.toUpperCase() + "] §f" + cached));
            } else {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.gui != null && client.gui.getChat() != null) {
                    client.gui.getChat().addMessage(Component.literal(currentTag + finalSender + cached));
                }
            }
            return;
        }

        // Translation not in cache yet
        if (config.chatReplaceMode) {
            // Suppress the original raw message
            event.setCanceled(true);

            engine.translateAsync(contentToTranslate, translated -> {
                Minecraft client = Minecraft.getInstance();
                if (client == null || client.gui == null || client.gui.getChat() == null) return;

                client.execute(() -> {
                    if (translated != null && !translated.isBlank() && !translated.equalsIgnoreCase(contentToTranslate)) {
                        client.gui.getChat().addMessage(Component.literal(finalSender + "§b[" + config.targetLanguage.toUpperCase() + "] §f" + translated));
                    } else {
                        client.gui.getChat().addMessage(message);
                    }
                });
            });
        } else {
            // BELOW mode: let original show, add translated below
            engine.translateAsync(contentToTranslate, translated -> {
                Minecraft client = Minecraft.getInstance();
                if (client == null || client.gui == null || client.gui.getChat() == null) return;

                client.execute(() -> {
                    Component translatedComponent = Component.literal(currentTag + finalSender + translated);
                    client.gui.getChat().addMessage(translatedComponent);
                });
            });
        }
    }
}
