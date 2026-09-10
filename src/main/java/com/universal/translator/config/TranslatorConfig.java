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
    public boolean tooltipTranslationEnabled = true;
    public boolean translateItemName = true;
    public boolean translateItemLore = true;

    // Language Target: "vi" (Vietnamese) or "en" (English) or custom
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
                this.tooltipTranslationEnabled = loaded.tooltipTranslationEnabled;
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

    public void toggleTooltip() {
        this.tooltipTranslationEnabled = !this.tooltipTranslationEnabled;
        save();
    }

    public void toggleIgnoreEnglish() {
        this.ignoreEnglish = !this.ignoreEnglish;
        save();
    }

    public void cycleTargetLanguage() {
        if ("vi".equalsIgnoreCase(this.targetLanguage)) {
            this.targetLanguage = "en";
            this.chatPrefix = "  §b[EN] §f";
            this.tooltipPrefix = "§b[EN] §7";
        } else {
            this.targetLanguage = "vi";
            this.chatPrefix = "  §b[VI] §f";
            this.tooltipPrefix = "§b[VI] §7";
        }
        save();
    }
}
