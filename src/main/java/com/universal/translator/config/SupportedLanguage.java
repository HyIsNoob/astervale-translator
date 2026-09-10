package com.universal.translator.config;

/**
 * Supported Target Languages for CurseForge global players.
 */
public enum SupportedLanguage {
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt"),
    ENGLISH("en", "English", "English"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    CHINESE_SIMP("zh-CN", "Chinese (Simp)", "简体中文"),
    CHINESE_TRAD("zh-TW", "Chinese (Trad)", "繁體中文"),
    SPANISH("es", "Spanish", "Español"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    RUSSIAN("ru", "Russian", "Русский"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    THAI("th", "Thai", "ไทย"),
    TAGALOG("tl", "Tagalog", "Filipino"),
    TURKISH("tr", "Turkish", "Türkçe"),
    ARABIC("ar", "Arabic", "العربية"),
    ITALIAN("it", "Italian", "Italiano");

    private final String code;
    private final String englishName;
    private final String nativeName;

    SupportedLanguage(String code, String englishName, String nativeName) {
        this.code = code;
        this.englishName = englishName;
        this.nativeName = nativeName;
    }

    public String getCode() {
        return code;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getNativeName() {
        return nativeName;
    }

    public String getDisplayName() {
        return englishName + " (" + code.toUpperCase() + ")";
    }

    public static SupportedLanguage fromCode(String code) {
        if (code == null) return VIETNAMESE;
        for (SupportedLanguage lang : values()) {
            if (lang.code.equalsIgnoreCase(code) || lang.name().equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return VIETNAMESE;
    }

    public SupportedLanguage next() {
        SupportedLanguage[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
