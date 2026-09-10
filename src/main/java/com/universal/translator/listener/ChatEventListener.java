package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatEventListener {

    // Regex to match Minecraft player chat prefixes with multiple title brackets: [Title] [Rank] <Player>, Player:
    private static final Pattern SENDER_PATTERN = Pattern.compile(
            "^(?<sender>(?:\\[[^\\]]+\\]\\s*)*(?:<[a-zA-Z0-9_\\uAC00-\\uD7A3]{1,20}>\\s*:?\\s*|[a-zA-Z0-9_\\uAC00-\\uD7A3]{2,20}:\\s+))(?<content>.+)$"
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

    /**
     * Checks if a line is purely a decorative separator / border (e.g. "-----", "=====", "✦ ━━━━ ✦").
     */
    private boolean isDecorativeSeparator(String text) {
        if (text == null || text.isBlank()) return false;
        String stripped = text.replaceAll("[\\s\\-=~_*#|<>+•✦★━─═≡—–\\[\\](){}:.]", "");
        return stripped.isEmpty() && text.trim().length() >= 3;
    }

    /**
     * Extracts the dominant / primary color from a Minecraft Component or legacy formatted text.
     */
    private String extractColorCode(Component message, String rawText) {
        // 1. Check legacy section sign codes in text if present
        if (rawText != null && rawText.contains("§")) {
            Matcher m = Pattern.compile("§([0-9a-fk-or])").matcher(rawText);
            String found = null;
            while (m.find()) {
                String c = m.group(1);
                // Prioritize vibrant colors (yellow, gold, aqua, green, red, purple) over white/gray
                if ("0123456789abcde".indexOf(c) >= 0 && !c.equals("7") && !c.equals("8") && !c.equals("f")) {
                    return "§" + c;
                }
                found = "§" + c;
            }
            if (found != null && !found.equals("§r")) {
                return found;
            }
        }

        // 2. Check Style in Component tree
        if (message != null) {
            Style s = message.getStyle();
            if (s != null && s.getColor() != null) {
                String name = s.getColor().serialize();
                ChatFormatting fmt = ChatFormatting.getByName(name);
                if (fmt != null) return fmt.toString();
            }
            for (Component sibling : message.getSiblings()) {
                Style ss = sibling.getStyle();
                if (ss != null && ss.getColor() != null) {
                    String name = ss.getColor().serialize();
                    ChatFormatting fmt = ChatFormatting.getByName(name);
                    if (fmt != null && fmt != ChatFormatting.WHITE && fmt != ChatFormatting.GRAY) {
                        return fmt.toString();
                    }
                }
            }
        }

        return "§f"; // Default to clean white
    }

    private void handleChat(ClientChatReceivedEvent event) {
        if (!config.masterEnabled || !config.chatTranslationEnabled || event == null) return;
        Component message = event.getMessage();
        if (message == null) return;

        String rawText = message.getString();
        if (rawText.isBlank()) return;

        // Skip messages that already contain translation tag
        String tag = "[" + config.targetLanguage.toUpperCase() + "]";
        if (rawText.contains(tag) || rawText.contains("[VI]") || rawText.contains("[EN]") || rawText.contains("[TRANS]")) return;

        // Extract sender prefix if present (e.g. "[주민] <HyIsNoob> ")
        String senderPrefix = "";
        String textToTranslate = rawText.trim();

        Matcher matcher = SENDER_PATTERN.matcher(textToTranslate);
        if (matcher.find()) {
            senderPrefix = matcher.group("sender");
            textToTranslate = matcher.group("content").trim();
        }

        // Skip decorative separator lines (e.g. "-----", "=====") to preserve server borders
        if (isDecorativeSeparator(rawText) || isDecorativeSeparator(textToTranslate)) {
            return;
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
        final String dominantColor = extractColorCode(message, rawText);
        final String langTag = "§b[" + config.targetLanguage.toUpperCase() + "] ";

        // Check if translation is already cached
        String cached = engine.getCache().get(contentToTranslate);
        if (cached != null && !cached.isBlank()) {
            if (config.chatReplaceMode) {
                event.setMessage(Component.literal(finalSender + langTag + dominantColor + cached));
            } else {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.gui != null && client.gui.getChat() != null) {
                    client.gui.getChat().addMessage(Component.literal("  " + langTag + dominantColor + cached));
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
                        client.gui.getChat().addMessage(Component.literal(finalSender + langTag + dominantColor + translated));
                    } else {
                        client.gui.getChat().addMessage(message);
                    }
                });
            });
        } else {
            // BELOW mode: let original show, add translated below with matching color
            engine.translateAsync(contentToTranslate, translated -> {
                Minecraft client = Minecraft.getInstance();
                if (client == null || client.gui == null || client.gui.getChat() == null) return;

                client.execute(() -> {
                    Component translatedComponent = Component.literal("  " + langTag + dominantColor + translated);
                    client.gui.getChat().addMessage(translatedComponent);
                });
            });
        }
    }
}
