package com.universal.translator.listener;

import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatEventListener {

    // Regex to match Minecraft player chat prefixes: [Rank] [Title] <Player>, Player:
    private static final Pattern PLAYER_PATTERN = Pattern.compile(
            "^(?<sender>.*?(?:<[a-zA-Z0-9_\\uAC00-\\uD7A3]{1,20}>|[a-zA-Z0-9_\\uAC00-\\uD7A3\\s]{2,20}:)\\s*)(?<content>.+)$"
    );

    // Regex to match custom NPC dialogue formats: [NPC] Name: text, Name » text, 【Name】text, ★ Name ★ : text, Name > text
    private static final Pattern NPC_DIALOGUE_PATTERN = Pattern.compile(
            "^(?<npc>(?:[【\\[][^】\\]]+[】\\]]|[★☆✦◆◇][^★☆✦◆◇]+[★☆✦◆◇]|[a-zA-Z0-9_\\uAC00-\\uD7A3]{2,16})\\s*(?:»|:|—|-|>)\\s*)(?<dialogue>.+)$"
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

        // Action Bar Overlay Messages (displayed right above hotbar)
        if (event.isOverlay()) {
            handleActionBar(event);
            return;
        }

        handleChat(event);
    }

    /**
     * In-place translation for Action Bar overlay messages (e.g. server timers, skill alerts, quest hints).
     */
    private void handleActionBar(ClientChatReceivedEvent.System event) {
        Component message = event.getMessage();
        if (message == null) return;

        String rawText = message.getString();
        if (rawText.isBlank() || isDecorativeSeparator(rawText)) return;

        String tag = "[" + config.targetLanguage.toUpperCase() + "]";
        if (rawText.contains(tag) || rawText.contains("[VI]") || rawText.contains("[EN]")) return;

        if (!engine.needsTranslation(rawText)) return;

        String dominantColor = extractColorCode(message, rawText);
        String cached = engine.getCache().get(rawText.trim());

        if (cached != null && !cached.isBlank()) {
            event.setMessage(Component.literal(dominantColor + cached));
            return;
        }

        // Not in cache: suppress the raw foreign action bar to prevent flashing, translate asynchronously
        event.setCanceled(true);
        engine.translateAsyncDetailed(rawText.trim(), (translated, isFallback) -> {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.gui == null) return;
            client.execute(() -> {
                if (translated != null && !translated.isBlank()) {
                    String fbTag = isFallback ? "§6[FB] " : "";
                    client.gui.setOverlayMessage(Component.literal(fbTag + dominantColor + translated), false);
                } else {
                    client.gui.setOverlayMessage(message, false);
                }
            });
        });
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
                // Prioritize vibrant colors over white/gray
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

    /**
     * Builds interactive translated component with hover-to-view-original and click-to-copy.
     */
    private Component buildInteractiveComponent(String prefix, String dominantColor, String translatedText, String originalText, boolean isMention, boolean isFallback) {
        String fbTag = (isFallback && config.showFallbackNotice) ? "§6[FB] " : "";
        String mention = isMention ? "§6§l🔔 §r" : "";
        String fullText = mention + fbTag + prefix + dominantColor + translatedText;
        MutableComponent comp = Component.literal(fullText);

        if (originalText != null && !originalText.isBlank()) {
            String hoverHeader = isFallback
                    ? "§6§l[Google Translate Fallback]§r\n§e(Gemini API bận, hết Quota hoặc lỗi kết nối)\n\n§e§l[Bản Gốc / Original Text]§r\n§f"
                    : "§e§l[Bản Gốc / Original Text]§r\n§f";
            Style interactiveStyle = comp.getStyle()
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal(hoverHeader + originalText + "\n\n§a§o» Nhấp chuột để sao chép (Click to copy)")))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, originalText));
            comp.setStyle(interactiveStyle);
        }

        return comp;
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

        // Extract sender prefix if present (Player chat or Custom NPC dialogue)
        String senderPrefix = "";
        String textToTranslate = rawText.trim();

        Matcher playerMatcher = PLAYER_PATTERN.matcher(textToTranslate);
        Matcher npcMatcher = NPC_DIALOGUE_PATTERN.matcher(textToTranslate);

        if (playerMatcher.find()) {
            senderPrefix = playerMatcher.group("sender");
            textToTranslate = playerMatcher.group("content").trim();
        } else if (npcMatcher.find()) {
            senderPrefix = npcMatcher.group("npc");
            textToTranslate = npcMatcher.group("dialogue").trim();
        }

        // Skip decorative separator lines (e.g. "-----", "=====") to preserve server borders
        if (isDecorativeSeparator(rawText) || isDecorativeSeparator(textToTranslate)) {
            return;
        }

        // Check if message is from the local player
        Minecraft client = Minecraft.getInstance();
        boolean isMention = false;

        if (client != null && client.getUser() != null) {
            String myUsername = client.getUser().getName();
            if (config.ignoreSelfChat && !senderPrefix.isEmpty()) {
                if (senderPrefix.contains("<" + myUsername + ">") || senderPrefix.startsWith(myUsername + ":")) {
                    return;
                }
            }

            // Check if local player is mentioned in the chat
            if (!myUsername.isBlank() && rawText.toLowerCase().contains(myUsername.toLowerCase())) {
                isMention = true;
                // Play notification chime sound
                try {
                    client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F));
                } catch (Exception ignored) {
                }
            }
        }

        // Check if message content needs translation
        if (!engine.needsTranslation(textToTranslate)) return;

        final String contentToTranslate = textToTranslate;
        final String finalSender = senderPrefix;
        final String dominantColor = extractColorCode(message, rawText);
        final String langTag = "§b[" + config.targetLanguage.toUpperCase() + "] ";
        final boolean finalMention = isMention;

        // Check if translation is already cached
        String cached = engine.getCache().get(contentToTranslate);
        if (cached != null && !cached.isBlank()) {
            if (config.chatReplaceMode) {
                Component translatedComp = buildInteractiveComponent(finalSender, dominantColor, cached, contentToTranslate, finalMention, false);
                event.setMessage(translatedComp);
            } else {
                if (client != null && client.gui != null && client.gui.getChat() != null) {
                    Component translatedComp = Component.literal("  " + (finalMention ? "§6🔔 " : "") + langTag + dominantColor + cached);
                    client.gui.getChat().addMessage(translatedComp);
                }
            }
            return;
        }

        // Translation not in cache yet
        if (config.chatReplaceMode) {
            // Suppress the original raw message
            event.setCanceled(true);

            engine.translateAsyncDetailed(contentToTranslate, (translated, isFallback) -> {
                if (client == null || client.gui == null || client.gui.getChat() == null) return;

                client.execute(() -> {
                    if (translated != null && !translated.isBlank() && !translated.equalsIgnoreCase(contentToTranslate)) {
                        Component translatedComp = buildInteractiveComponent(finalSender, dominantColor, translated, contentToTranslate, finalMention, isFallback);
                        client.gui.getChat().addMessage(translatedComp);
                    } else {
                        client.gui.getChat().addMessage(message);
                    }
                });
            });
        } else {
            // BELOW mode: let original show, add translated below with matching color
            engine.translateAsyncDetailed(contentToTranslate, (translated, isFallback) -> {
                if (client == null || client.gui == null || client.gui.getChat() == null) return;

                client.execute(() -> {
                    String actualTag = (isFallback && config.showFallbackNotice) ? "§6[" + config.targetLanguage.toUpperCase() + "•FB] " : langTag;
                    Component translatedComp = Component.literal("  " + (finalMention ? "§6🔔 " : "") + actualTag + dominantColor + translated);
                    client.gui.getChat().addMessage(translatedComp);
                });
            });
        }
    }
}
