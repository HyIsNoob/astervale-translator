package com.universal.translator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TranslatorConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatorConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Master Switch
    public boolean masterEnabled = true;

    // Translation Features
    public boolean chatTranslationEnabled = true;
    public boolean chatReplaceMode = false; // If true, replaces original chat message; if false, adds translation below
    public boolean tooltipTranslationEnabled = true;
    public boolean tooltipHoldShift = false; // If true, only show translated tooltips when holding SHIFT
    public boolean translateSystemMessages = true; // Translate system & server announcements
    public boolean ignoreSelfChat = true; // Don't translate own chat messages
    public boolean translateItemName = true;
    public boolean translateItemLore = true;

    // Outgoing Reverse Translation (translates what YOU type and send)
    public boolean outgoingTranslationEnabled = false;
    public String outgoingTargetLanguage = "ko"; // Default to Korean (e.g. for Aster Vale) or any target

    // Language Target: "vi", "en", "ja", "ko", etc.
    public String targetLanguage = "vi";
    public String sourceLanguage = "auto";

    // Exceptions & Filters
    public boolean ignoreEnglish = true; // If true, pure English/Latin messages won't be translated
    public List<String> exceptionWords = new ArrayList<>(); // Words to ignore

    // Formatting & Prefixes
    public String chatPrefix = "  §b[VI] §f";
    public String tooltipPrefix = "§b[VI] §7";

    public boolean translateAllForeignText = true;

    private transient Path configFile;

    public void init(Path configDirectory) {
        try {
            if (!Files.exists(configDirectory)) {
                Files.createDirectories(configDirectory);
            }
            this.configFile = configDirectory.resolve("universal_translator.json");
            if (Files.exists(this.configFile)) {
                load();
            } else {
                seedDefaultExceptions();
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize config: {}", e.getMessage());
        }
    }

    private void seedDefaultExceptions() {
        if (exceptionWords.isEmpty()) {
            exceptionWords.add("GG");
            exceptionWords.add("AFK");
            exceptionWords.add("LOL");
            exceptionWords.add("PVP");
            exceptionWords.add("PVE");
            exceptionWords.add("VIP");
            exceptionWords.add("MVP");
        }
    }

    public void load() {
        if (configFile == null || !Files.exists(configFile)) return;
        try (Reader reader = Files.newBufferedReader(configFile)) {
            TranslatorConfig loaded = GSON.fromJson(reader, TranslatorConfig.class);
            if (loaded != null) {
                this.masterEnabled = loaded.masterEnabled;
                this.chatTranslationEnabled = loaded.chatTranslationEnabled;
                this.chatReplaceMode = loaded.chatReplaceMode;
                this.tooltipTranslationEnabled = loaded.tooltipTranslationEnabled;
                this.tooltipHoldShift = loaded.tooltipHoldShift;
                this.translateSystemMessages = loaded.translateSystemMessages;
                this.ignoreSelfChat = loaded.ignoreSelfChat;
                this.outgoingTranslationEnabled = loaded.outgoingTranslationEnabled;
                this.outgoingTargetLanguage = loaded.outgoingTargetLanguage != null ? loaded.outgoingTargetLanguage : this.outgoingTargetLanguage;
                this.translateItemName = loaded.translateItemName;
                this.translateItemLore = loaded.translateItemLore;
                this.targetLanguage = loaded.targetLanguage != null ? loaded.targetLanguage : this.targetLanguage;
                this.sourceLanguage = loaded.sourceLanguage != null ? loaded.sourceLanguage : this.sourceLanguage;
                this.ignoreEnglish = loaded.ignoreEnglish;
                if (loaded.exceptionWords != null) {
                    this.exceptionWords = loaded.exceptionWords;
                }
                this.chatPrefix = loaded.chatPrefix != null ? loaded.chatPrefix : this.chatPrefix;
                this.tooltipPrefix = loaded.tooltipPrefix != null ? loaded.tooltipPrefix : this.tooltipPrefix;
                this.translateAllForeignText = loaded.translateAllForeignText;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load config: {}", e.getMessage());
        }
    }

    public void save() {
        if (configFile == null) return;
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            LOGGER.warn("Failed to save config: {}", e.getMessage());
        }
    }

    public void toggleMaster() {
        this.masterEnabled = !this.masterEnabled;
        save();
    }

    public void toggleChat() {
        this.chatTranslationEnabled = !this.chatTranslationEnabled;
        save();
    }

    public void toggleChatReplaceMode() {
        this.chatReplaceMode = !this.chatReplaceMode;
        save();
    }

    public void toggleOutgoingTranslation() {
        this.outgoingTranslationEnabled = !this.outgoingTranslationEnabled;
        save();
    }

    public SupportedLanguage getOutgoingSupportedLanguage() {
        return SupportedLanguage.fromCode(this.outgoingTargetLanguage);
    }

    public void cycleOutgoingTargetLanguage() {
        SupportedLanguage next = getOutgoingSupportedLanguage().next();
        this.outgoingTargetLanguage = next.getCode();
        save();
    }

    public void toggleTooltip() {
        this.tooltipTranslationEnabled = !this.tooltipTranslationEnabled;
        save();
    }

    public void toggleTooltipHoldShift() {
        this.tooltipHoldShift = !this.tooltipHoldShift;
        save();
    }

    public void toggleSystemMessages() {
        this.translateSystemMessages = !this.translateSystemMessages;
        save();
    }

    public void toggleIgnoreSelfChat() {
        this.ignoreSelfChat = !this.ignoreSelfChat;
        save();
    }

    public void toggleIgnoreEnglish() {
        this.ignoreEnglish = !this.ignoreEnglish;
        save();
    }

    public SupportedLanguage getSupportedLanguage() {
        return SupportedLanguage.fromCode(this.targetLanguage);
    }

    public void cycleTargetLanguage() {
        SupportedLanguage next = getSupportedLanguage().next();
        this.targetLanguage = next.getCode();
        this.chatPrefix = "  §b[" + next.getCode().toUpperCase() + "] §f";
        this.tooltipPrefix = "§b[" + next.getCode().toUpperCase() + "] §7";
        save();
    }

    public Path getConfigFile() {
        return configFile;
    }
}
