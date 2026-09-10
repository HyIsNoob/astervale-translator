package com.astervale.translator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TranslatorConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatorConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean chatTranslationEnabled = true;
    public boolean tooltipTranslationEnabled = true;
    public String chatPrefix = "  §b[VI] §f";
    public String tooltipPrefix = "§b[VI] §7";
    public boolean translateItemName = true;
    public boolean translateItemLore = true;

    private transient Path configFile;

    public void init(Path configDirectory) {
        try {
            if (!Files.exists(configDirectory)) {
                Files.createDirectories(configDirectory);
            }
            this.configFile = configDirectory.resolve("astervale_translator.json");
            if (Files.exists(this.configFile)) {
                load();
            } else {
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize config: {}", e.getMessage());
        }
    }

    public void load() {
        if (configFile == null || !Files.exists(configFile)) return;
        try (Reader reader = Files.newBufferedReader(configFile)) {
            TranslatorConfig loaded = GSON.fromJson(reader, TranslatorConfig.class);
            if (loaded != null) {
                this.chatTranslationEnabled = loaded.chatTranslationEnabled;
                this.tooltipTranslationEnabled = loaded.tooltipTranslationEnabled;
                this.chatPrefix = loaded.chatPrefix != null ? loaded.chatPrefix : this.chatPrefix;
                this.tooltipPrefix = loaded.tooltipPrefix != null ? loaded.tooltipPrefix : this.tooltipPrefix;
                this.translateItemName = loaded.translateItemName;
                this.translateItemLore = loaded.translateItemLore;
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
}
