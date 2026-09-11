package com.universal.translator.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.function.BiConsumer;
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
    private final java.util.Set<String> inFlightRequests = java.util.concurrent.ConcurrentHashMap.newKeySet();

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

    public static class TranslationOutcome {
        public final String text;
        public final boolean isFallback;

        public TranslationOutcome(String text, boolean isFallback) {
            this.text = text;
            this.isFallback = isFallback;
        }
    }

    /**
     * Asynchronously translates the given text and invokes callback with result and fallback status.
     */
    public void translateAsyncDetailed(String rawText, BiConsumer<String, Boolean> callback) {
        if (!config.masterEnabled || rawText == null || rawText.isBlank() || !needsTranslation(rawText)) {
            return;
        }

        String trimmed = rawText.trim();
        String cached = cache.get(trimmed);
        if (cached != null) {
            callback.accept(cached, false);
            return;
        }

        // Deduplicate in-flight requests
        if (!inFlightRequests.add(trimmed)) {
            return;
        }

        executor.submit(() -> {
            try {
                TranslationOutcome outcome = translateSyncWithOutcome(trimmed);
                if (outcome != null && outcome.text != null && !outcome.text.isBlank() && !outcome.text.equalsIgnoreCase(trimmed)) {
                    cache.put(trimmed, outcome.text);
                    callback.accept(outcome.text, outcome.isFallback);
                }
            } catch (Exception e) {
                LOGGER.debug("Async translation error for '{}': {}", trimmed, e.getMessage());
            } finally {
                inFlightRequests.remove(trimmed);
            }
        });
    }

    /**
     * Asynchronously translates the given text and invokes callback with result.
     */
    public void translateAsync(String rawText, Consumer<String> callback) {
        translateAsyncDetailed(rawText, (translated, isFallback) -> callback.accept(translated));
    }

    /**
     * Direct asynchronous translation bypassing filters (used for live GUI testing and commands).
     */
    public void translateDirectAsync(String rawText, Consumer<String> callback) {
        if (rawText == null || rawText.isBlank()) {
            callback.accept("Empty input text");
            return;
        }
        String trimmed = rawText.trim();
        executor.submit(() -> {
            try {
                TranslationOutcome outcome = translateSyncWithOutcome(trimmed);
                String translated = outcome.text;
                callback.accept(translated != null && !translated.isBlank() ? translated : trimmed);
            } catch (Exception e) {
                callback.accept("Translation error: " + e.getMessage());
            }
        });
    }

    /**
     * Synchronous translation call returning TranslationOutcome (includes isFallback flag).
     */
    public TranslationOutcome translateSyncWithOutcome(String text) {
        String sl = config.sourceLanguage != null ? config.sourceLanguage : "auto";
        String tl = config.targetLanguage != null ? config.targetLanguage : "vi";
        return translateSyncTargetWithOutcome(text, sl, tl);
    }

    public String translateSync(String text) {
        return translateSyncWithOutcome(text).text;
    }

    public String translateSyncTarget(String text, String sl, String tl) {
        return translateSyncTargetWithOutcome(text, sl, tl).text;
    }

    /**
     * Synchronous translation call for any source and target language pair.
     */
    public TranslationOutcome translateSyncTargetWithOutcome(String text, String sl, String tl) {
        if (text == null || text.isBlank()) return new TranslationOutcome(text, false);
        String clean = text.trim();

        // 1. Check Cache
        String cacheKey = "[" + tl + "]" + clean;
        String cached = cache.get(cacheKey);
        if (cached != null) return new TranslationOutcome(cached, false);

        // Also check un-prefixed cache if target matches default
        if (tl.equalsIgnoreCase(config.targetLanguage)) {
            String defaultCached = cache.get(clean);
            if (defaultCached != null) return new TranslationOutcome(defaultCached, false);
        }

        // 2. Check exact match in custom lexicon (if translating to Vietnamese)
        if ("vi".equalsIgnoreCase(tl)) {
            String exactLexicon = lexicon.lookupExact(clean);
            if (exactLexicon != null) {
                cache.put(cacheKey, exactLexicon);
                cache.put(clean, exactLexicon);
                return new TranslationOutcome(exactLexicon, false);
            }
        }

        boolean isFallback = false;
        String translated = null;

        // 3. Primary AI Option: Google Gemini API (if enabled in config)
        if (config.isGeminiMode()) {
            if (config.geminiApiKey != null && !config.geminiApiKey.isBlank()) {
                try {
                    translated = queryGemini(clean, tl, config.geminiApiKey.trim());
                } catch (Exception e) {
                    LOGGER.warn("Gemini API failed for '{}': {}. Falling back to Google Translate.", clean, e.getMessage());
                    isFallback = true;
                }
            } else {
                isFallback = true;
            }
        }

        // 4. Primary Web / Fallback: Google Mobile Web Engine (High reliability, bypasses 429 rate limit)
        if (translated == null || translated.isBlank()) {
            translated = queryGoogleMobile(clean, sl, tl);
        }

        // 5. Secondary: Google GTX API
        if (translated == null || translated.isBlank()) {
            translated = queryGoogleGtx(clean, sl, tl);
        }

        // 6. Tertiary: MyMemory API
        if (translated == null || translated.isBlank()) {
            translated = queryMyMemory(clean, sl, tl);
        }

        // 7. Offline Fallback: Custom Lexicon replacement
        if (translated == null || translated.isBlank()) {
            if ("vi".equalsIgnoreCase(tl)) {
                String fallbackLexicon = lexicon.applyPreprocess(clean);
                if (!fallbackLexicon.equals(clean)) {
                    translated = fallbackLexicon;
                }
            }
        }

        if (translated != null && !translated.isBlank()) {
            cache.put(cacheKey, translated);
            if (tl.equalsIgnoreCase(config.targetLanguage)) {
                cache.put(clean, translated);
            }
            return new TranslationOutcome(translated, isFallback);
        }

        return new TranslationOutcome(clean, isFallback);
    }

    private static final Pattern RESULT_CONTAINER_PATTERN = Pattern.compile("(?s)<div[^>]*class=\"result-container\"[^>]*>(.*?)</div>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern NUMERIC_ENTITY_PATTERN = Pattern.compile("&#(\\d+);");
    private static final Pattern HEX_ENTITY_PATTERN = Pattern.compile("&#x([0-9a-fA-F]+);");

    private String queryGoogleMobile(String text, String sl, String tl) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format("https://translate.google.com/m?sl=%s&tl=%s&q=%s", sl, tl, encoded);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                String body = response.body();
                java.util.regex.Matcher m = RESULT_CONTAINER_PATTERN.matcher(body);
                if (m.find()) {
                    String rawResult = m.group(1);
                    String cleanResult = HTML_TAG_PATTERN.matcher(rawResult).replaceAll("");
                    return unescapeHtml(cleanResult.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Google Mobile translation error: {}", e.getMessage());
        }
        return null;
    }

    private String queryGoogleGtx(String text, String sl, String tl) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format("https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s", sl, tl, encoded);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return parseGoogleTranslateJson(response.body());
            }
        } catch (Exception e) {
            LOGGER.debug("Google GTX Translate request error: {}", e.getMessage());
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
                    .timeout(Duration.ofSeconds(6))
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
                            return unescapeHtml(transText.getAsString().trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("MyMemory request error: {}", e.getMessage());
        }
        return null;
    }

    private String unescapeHtml(String input) {
        if (input == null || input.isEmpty()) return input;
        String s = input.replace("&quot;", "\"")
                        .replace("&apos;", "'")
                        .replace("&#39;", "'")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&nbsp;", " ");

        java.util.regex.Matcher numMatcher = NUMERIC_ENTITY_PATTERN.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (numMatcher.find()) {
            try {
                int code = Integer.parseInt(numMatcher.group(1));
                numMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (Exception ignored) {}
        }
        numMatcher.appendTail(sb);
        s = sb.toString();

        java.util.regex.Matcher hexMatcher = HEX_ENTITY_PATTERN.matcher(s);
        sb = new StringBuilder();
        while (hexMatcher.find()) {
            try {
                int code = Integer.parseInt(hexMatcher.group(1), 16);
                hexMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (Exception ignored) {}
        }
        hexMatcher.appendTail(sb);
        return sb.toString();
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

    /**
     * Query Google Gemini REST API directly with RPG gaming prompt.
     */
    public String queryGemini(String text, String tl, String apiKey) throws Exception {
        String targetLangName = "Vietnamese";
        try {
            targetLangName = com.universal.translator.config.SupportedLanguage.fromCode(tl).getEnglishName();
        } catch (Exception ignored) {}

        String model = config.geminiModel != null && !config.geminiModel.isBlank() ? config.geminiModel.trim() : "gemini-1.5-flash";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();

        String prompt = "You are a professional game translator. Translate the following Minecraft chat message, item lore, or NPC dialogue accurately into "
                + targetLangName + " using natural gaming and RPG terminology. "
                + "Do NOT add any notes, explanations, or conversational filler. Output ONLY the raw translated text.\n\nText: " + text;

        part.addProperty("text", prompt);
        parts.add(part);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        root.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.1);
        genConfig.addProperty("maxOutputTokens", 300);
        root.add("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(2500))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + " " + response.body());
        }

        JsonObject resObj = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray candidates = resObj.getAsJsonArray("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            JsonObject cand = candidates.get(0).getAsJsonObject();
            JsonObject candContent = cand.getAsJsonObject("content");
            if (candContent != null) {
                JsonArray candParts = candContent.getAsJsonArray("parts");
                if (candParts != null && !candParts.isEmpty()) {
                    String out = candParts.get(0).getAsJsonObject().get("text").getAsString().trim();
                    if (out.startsWith("\"") && out.endsWith("\"") && out.length() >= 2) {
                        out = out.substring(1, out.length() - 1).trim();
                    }
                    return out;
                }
            }
        }

        throw new RuntimeException("No translation candidate in Gemini response");
    }

    /**
     * Tests a Gemini API Key on a background thread and invokes callback with result.
     */
    public void testGeminiApiKey(String apiKey, Consumer<String> callback) {
        if (apiKey == null || apiKey.isBlank()) {
            callback.accept("§cAPI Key cannot be empty!");
            return;
        }

        executor.submit(() -> {
            try {
                String testPhrase = "안녕하세요! 반갑습니다.";
                String translated = queryGemini(testPhrase, "vi", apiKey.trim());
                if (translated != null && !translated.isBlank()) {
                    callback.accept("§a✔ API Valid! Result: §f" + translated);
                } else {
                    callback.accept("§c✖ Empty translation response from Gemini");
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && msg.contains("API_KEY_INVALID")) {
                    callback.accept("§c✖ Invalid API Key! Check your key on Google AI Studio.");
                } else if (msg != null && msg.contains("429")) {
                    callback.accept("§c✖ Quota Exceeded (HTTP 429)! Rate limit reached.");
                } else {
                    callback.accept("§c✖ " + (msg != null && msg.length() > 60 ? msg.substring(0, 60) + "..." : msg));
                }
            }
        });
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
