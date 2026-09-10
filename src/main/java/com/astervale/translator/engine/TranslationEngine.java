package com.astervale.translator.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * High-performance, non-blocking translation engine.
 * Detects Korean Hangul, applies AsterLexicon, calls Google Translate API,
 * and maintains L1 RAM + L2 Disk cache.
 */
public class TranslationEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationEngine.class);
    private static final Pattern HANGUL_PATTERN = Pattern.compile("[\\uac00-\\ud7a3]");

    private final TranslationCache cache;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;

    public TranslationEngine(TranslationCache cache) {
        this.cache = cache;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "AsterTranslator-Worker");
            t.setDaemon(true);
            return t;
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AsterTranslator-CacheSaver");
            t.setDaemon(true);
            return t;
        });

        // Auto-save cache every 60 seconds
        this.scheduler.scheduleAtFixedRate(this.cache::save, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Check if a string contains any Korean Hangul syllable.
     */
    public boolean containsKorean(String text) {
        if (text == null || text.isEmpty()) return false;
        return HANGUL_PATTERN.matcher(text).find();
    }

    /**
     * Asynchronously translates the given text if it contains Korean.
     * Invokes callback with the translated result.
     */
    public void translateAsync(String rawText, Consumer<String> callback) {
        if (rawText == null || rawText.isBlank() || !containsKorean(rawText)) {
            return;
        }

        String trimmed = rawText.trim();
        String cached = cache.get(trimmed);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        executor.submit(() -> {
            try {
                String translated = translateSync(trimmed);
                if (translated != null && !translated.isBlank()) {
                    cache.put(trimmed, translated);
                    callback.accept(translated);
                }
            } catch (Exception e) {
                LOGGER.debug("Async translation failed for '{}': {}", trimmed, e.getMessage());
            }
        });
    }

    /**
     * Synchronous translation call (for background workers or cache lookups).
     */
    public String translateSync(String text) {
        if (text == null || text.isBlank()) return text;
        String clean = text.trim();

        // 1. Check Cache
        String cached = cache.get(clean);
        if (cached != null) return cached;

        // 2. Pre-process known specific game terms
        String preprocessed = AsterLexicon.applyPreprocess(clean);

        // If after preprocessing there's no Korean left, we are done
        if (!containsKorean(preprocessed)) {
            cache.put(clean, preprocessed);
            return preprocessed;
        }

        // 3. Query Translation Services (Google Translate -> Fallback: MyMemory)
        String translated = queryGoogleTranslate(preprocessed);
        if (translated == null || translated.isBlank()) {
            translated = queryMyMemory(preprocessed);
        }

        if (translated != null && !translated.isBlank()) {
            cache.put(clean, translated);
            return translated;
        }

        return preprocessed;
    }

    private String queryGoogleTranslate(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=ko&tl=vi&dt=t&q=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return parseGoogleTranslateJson(response.body());
            }
        } catch (Exception e) {
            LOGGER.debug("Google Translate query failed: {}", e.getMessage());
        }
        return null;
    }

    private String queryMyMemory(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=ko|vi";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "AsterValeTranslator/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                JsonElement root = JsonParser.parseString(response.body());
                if (root.isJsonObject()) {
                    JsonElement resData = root.getAsJsonObject().get("responseData");
                    if (resData != null && resData.isJsonObject()) {
                        JsonElement transText = resData.getAsJsonObject().get("translatedText");
                        if (transText != null && transText.isJsonPrimitive()) {
                            return transText.getAsString().trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("MyMemory translation query failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Parses the Google Translate GTX response array: [[["translated", "source", ...], ...], ...]
     */
    private String parseGoogleTranslateJson(String jsonStr) {
        try {
            JsonElement element = JsonParser.parseString(jsonStr);
            if (!element.isJsonArray()) return null;

            JsonArray rootArray = element.getAsJsonArray();
            if (rootArray.isEmpty() || !rootArray.get(0).isJsonArray()) return null;

            JsonArray sentences = rootArray.get(0).getAsJsonArray();
            StringBuilder sb = new StringBuilder();

            for (JsonElement sentenceElem : sentences) {
                if (sentenceElem.isJsonArray()) {
                    JsonArray sentenceArr = sentenceElem.getAsJsonArray();
                    if (!sentenceArr.isEmpty() && sentenceArr.get(0).isJsonPrimitive()) {
                        sb.append(sentenceArr.get(0).getAsString());
                    }
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            LOGGER.warn("Failed to parse Google Translate response: {}", e.getMessage());
            return null;
        }
    }

    public TranslationCache getCache() {
        return cache;
    }

    public void shutdown() {
        cache.save();
        executor.shutdown();
        scheduler.shutdown();
    }
}
