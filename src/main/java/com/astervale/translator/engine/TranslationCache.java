package com.astervale.translator.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two-tier cache: L1 Memory (ConcurrentHashMap) for 0ms lookups,
 * and L2 Disk (.minecraft/config/astervale_translator_cache.json) for persistence.
 */
public class TranslationCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationCache.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private Path cacheFilePath;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public TranslationCache() {}

    public void init(Path configDirectory) {
        try {
            if (!Files.exists(configDirectory)) {
                Files.createDirectories(configDirectory);
            }
            this.cacheFilePath = configDirectory.resolve("astervale_translator_cache.json");
            load();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize translation cache file: {}", e.getMessage());
        }
    }

    public String get(String source) {
        if (source == null) return null;
        return cache.get(source.trim());
    }

    public void put(String source, String translated) {
        if (source == null || translated == null) return;
        String cleanSource = source.trim();
        String cleanTrans = translated.trim();
        if (cleanSource.isEmpty() || cleanTrans.isEmpty()) return;

        cache.put(cleanSource, cleanTrans);
        dirty.set(true);
    }

    public boolean contains(String source) {
        if (source == null) return false;
        return cache.containsKey(source.trim());
    }

    private void load() {
        if (cacheFilePath == null || !Files.exists(cacheFilePath)) return;
        try (Reader reader = Files.newBufferedReader(cacheFilePath)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                cache.putAll(loaded);
                LOGGER.info("Loaded {} cached translations from disk.", cache.size());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load translation cache: {}", e.getMessage());
        }
    }

    public synchronized void save() {
        if (!dirty.get() || cacheFilePath == null) return;
        try (Writer writer = Files.newBufferedWriter(cacheFilePath)) {
            GSON.toJson(cache, writer);
            dirty.set(false);
        } catch (Exception e) {
            LOGGER.warn("Failed to save translation cache to disk: {}", e.getMessage());
        }
    }

    public int size() {
        return cache.size();
    }
}
