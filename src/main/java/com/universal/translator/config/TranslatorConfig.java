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

    // AI Engine Configuration
    public String engineMode = "GOOGLE_WEB"; // "GOOGLE_WEB" or "GEMINI_API"
    public String geminiApiKey = "";
    public String geminiModel = "gemini-flash-lite-latest";
    public boolean showFallbackNotice = true;

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
                this.engineMode = loaded.engineMode != null ? loaded.engineMode : this.engineMode;
                this.geminiApiKey = loaded.geminiApiKey != null ? loaded.geminiApiKey : this.geminiApiKey;
                
                // Auto-migrate retired 404 models
                String m = loaded.geminiModel != null ? loaded.geminiModel.trim() : "";
                if (m.isBlank() || "gemini-1.5-flash".equalsIgnoreCase(m) || "gemini-2.5-flash".equalsIgnoreCase(m) || "gemini-2.5-flash-lite".equalsIgnoreCase(m)) {
                    this.geminiModel = "gemini-flash-lite-latest";
                } else {
                    if (m.startsWith("models/")) {
                        m = m.substring("models/".length());
                    }
                    this.geminiModel = m;
                }
                
                this.showFallbackNotice = loaded.showFallbackNotice;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load config: {}", e.getMessage());
        }
    }

    public void toggleEngineMode() {
        this.engineMode = isGeminiMode() ? "GOOGLE_WEB" : "GEMINI_API";
        save();
    }

    public boolean isGeminiMode() {
        return "GEMINI_API".equalsIgnoreCase(this.engineMode);
    }

    public void setGeminiApiKey(String key) {
        this.geminiApiKey = key != null ? key.trim() : "";
        save();
    }

    public String getGeminiModel() {
        if (geminiModel == null || geminiModel.isBlank()) {
            return "gemini-flash-lite-latest";
        }
        String m = geminiModel.trim();
        if (m.startsWith("models/")) {
            m = m.substring("models/".length());
        }
        return m;
    }

    public void setGeminiModel(String model) {
        if (model != null && !model.isBlank()) {
            String m = model.trim();
            if (m.startsWith("models/")) {
                m = m.substring("models/".length());
            }
            this.geminiModel = m;
        } else {
            this.geminiModel = "gemini-flash-lite-latest";
        }
        save();
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

    public void setOutgoingTargetLanguage(String code) {
        SupportedLanguage lang = SupportedLanguage.fromCode(code);
        this.outgoingTargetLanguage = lang.getCode();
        save();
    }

    public void cycleOutgoingTargetLanguage() {
        SupportedLanguage next = getOutgoingSupportedLanguage().next();
        setOutgoingTargetLanguage(next.getCode());
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

    public void setTargetLanguage(String code) {
        SupportedLanguage lang = SupportedLanguage.fromCode(code);
        this.targetLanguage = lang.getCode();
        this.chatPrefix = "  §b[" + lang.getCode().toUpperCase() + "] §f";
        this.tooltipPrefix = "§b[" + lang.getCode().toUpperCase() + "] §7";
        save();
    }

    public void cycleTargetLanguage() {
        SupportedLanguage next = getSupportedLanguage().next();
        setTargetLanguage(next.getCode());
    }

    public Path getConfigFile() {
        return configFile;
    }
}
