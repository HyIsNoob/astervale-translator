package com.universal.translator.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.universal.translator.config.TranslatorConfig;
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
 * Universal, high-performance, non-blocking translation engine.
 * Supports auto-detection of Korean, Japanese, Chinese, Russian, and any foreign language.
 * Connects to Google Translate Web API with MyMemory fallback, exceptions, and two-tier caching.
 */
public class TranslationEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationEngine.class);

    // Foreign scripts detection (Korean, Japanese, Chinese, Cyrillic)
    private static final Pattern CJK_CYRILLIC_PATTERN = Pattern.compile(
            "[\\uac00-\\ud7a3" + // Hangul Syllables (Korean)
            "\\u3040-\\u30ff" + // Hiragana & Katakana (Japanese)
            "\\u4e00-\\u9fff" + // CJK Unified Ideographs (Chinese/Kanji)
            "\\u0400-\\u04ff]"  // Cyrillic (Russian/Ukrainian)
    );

    // Pure English / ASCII Latin pattern
    private static final Pattern ASCII_LATIN_PATTERN = Pattern.compile("^[\\p{ASCII}]+$");

    private final TranslationCache cache;
    private final CustomLexicon lexicon;
    private final TranslatorConfig config;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;

    public TranslationEngine(TranslationCache cache, CustomLexicon lexicon, TranslatorConfig config) {
        this.cache = cache;
        this.lexicon = lexicon;
        this.config = config;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "UniversalTranslator-Worker");
            t.setDaemon(true);
            return t;
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UniversalTranslator-CacheSaver");
            t.setDaemon(true);
            return t;
        });

        // Auto-save cache every 60 seconds
        this.scheduler.scheduleAtFixedRate(this.cache::save, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Checks if the text needs translation based on scripts, exceptions, and settings.
     */
    public boolean needsTranslation(String text) {
        if (!config.masterEnabled) return false;
        if (text == null || text.isBlank()) return false;

        String clean = text.trim();

        // Check custom word exceptions
        if (config.exceptionWords != null) {
            for (String exc : config.exceptionWords) {
                if (clean.equalsIgnoreCase(exc)) return false;
            }
        }

        // If ignoreEnglish is enabled and text is pure ASCII/English, do NOT translate
        if (config.ignoreEnglish && ASCII_LATIN_PATTERN.matcher(clean).matches()) {
            return false;
        }

        if (config.translateAllForeignText) {
            return CJK_CYRILLIC_PATTERN.matcher(clean).find() || (clean.length() > 2 && !clean.matches("^[\\x00-\\x7F]*$"));
        }

        return CJK_CYRILLIC_PATTERN.matcher(clean).find();
    }

    /**
     * Asynchronously translates the given text and invokes callback with result.
     */
    public void translateAsync(String rawText, Consumer<String> callback) {
        if (!config.masterEnabled || rawText == null || rawText.isBlank() || !needsTranslation(rawText)) {
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
                if (translated != null && !translated.isBlank() && !translated.equalsIgnoreCase(trimmed)) {
                    cache.put(trimmed, translated);
                    callback.accept(translated);
                }
            } catch (Exception e) {
                LOGGER.debug("Async translation error for '{}': {}", trimmed, e.getMessage());
            }
        });
    }

    /**
     * Synchronous translation call (preprocesses lexicon, queries Google / MyMemory).
     */
    public String translateSync(String text) {
        if (text == null || text.isBlank()) return text;
        String clean = text.trim();

        // 1. Check Cache
        String cached = cache.get(clean);
        if (cached != null) return cached;

        // 2. Apply Custom Lexicon
        String preprocessed = lexicon.applyPreprocess(clean);
        if (!needsTranslation(preprocessed)) {
            cache.put(clean, preprocessed);
            return preprocessed;
        }

        String sl = config.sourceLanguage != null ? config.sourceLanguage : "auto";
        String tl = config.targetLanguage != null ? config.targetLanguage : "vi";

        // 3. Query Google Translate (Primary)
        String translated = queryGoogleTranslate(preprocessed, sl, tl);

        // 4. Fallback to MyMemory if Google failed
        if (translated == null || translated.isBlank()) {
            translated = queryMyMemory(preprocessed, sl, tl);
        }

        if (translated != null && !translated.isBlank()) {
            cache.put(clean, translated);
            return translated;
        }

        return preprocessed;
    }

    private String queryGoogleTranslate(String text, String sl, String tl) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format("https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s", sl, tl, encoded);

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
            LOGGER.debug("Google Translate request error: {}", e.getMessage());
        }
        return null;
    }

    private String queryMyMemory(String text, String sl, String tl) {
        try {
            String sourcePair = sl.equalsIgnoreCase("auto") ? "ko" : sl;
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format("https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s", encoded, sourcePair, tl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "UniversalTranslator/1.0")
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
            LOGGER.debug("MyMemory request error: {}", e.getMessage());
        }
        return null;
    }

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
        lexicon.save();
        executor.shutdown();
        scheduler.shutdown();
    }
}
