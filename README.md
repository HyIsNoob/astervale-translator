# Universal Auto Translator (NeoForge 1.21.4)
Client-Side Real-Time Chat & Item Tooltip Translator using Google Translate

A lightweight, 100% client-side Minecraft mod for NeoForge 1.21.4. Seamlessly translates in-game player chat, system notifications, and item tooltips (names and lore) into your native language in real-time with zero FPS drop.

---

## Features

- Real-Time Chat Translation:
  - Automatically intercepts player messages and server announcements in foreign languages (Korean, Japanese, Chinese, Russian, etc.).
  - Translates asynchronously in the background and displays the translated text directly below the original message.
- Item Tooltip & Lore Translation:
  - Translates custom item names, weapon stats, armor descriptions, and RPG lore on hover.
  - No manual keybinds required.
- Two-Tier Caching System (0ms Latency):
  - L1 RAM Cache: Instant in-memory lookup.
  - L2 Disk Cache: Persists all translated phrases to disk (`.minecraft/config/universal_translator_cache.json`). Re-hovering an item takes 0ms.
- Customizable Game Lexicon:
  - Includes a customizable dictionary file (`.minecraft/config/universal_translator_lexicon.json`) for server-specific jargon, economy terms, and gem names.
- 100% Client-Side Stealth:
  - Configured with `displayTest = "IGNORE_ALL_VERSION"` and `clientSideOnly = true`.
  - Works on any server without server-side mod installation or handshake requirements. Completely safe and undetectable.

---

## Installation

1. Download the latest `.jar` file (`universal_translator-1.21.4-1.0.0.jar`) from GitHub Releases or GitHub Actions Artifacts.
2. Place the `.jar` file into your `.minecraft/mods/` folder.
3. Launch Minecraft with NeoForge 1.21.4 and enjoy.

---

## Configuration

Configuration file location:  
`.minecraft/config/universal_translator.json`

```json
{
  "targetLanguage": "vi",
  "sourceLanguage": "auto",
  "chatTranslationEnabled": true,
  "tooltipTranslationEnabled": true,
  "translateItemName": true,
  "translateItemLore": true,
  "chatPrefix": "  §b[VI] §f",
  "tooltipPrefix": "§b[VI] §7",
  "translateAllForeignText": true
}
```

### Configuration Options

- `targetLanguage`: The language code to translate into (e.g. `vi` for Vietnamese, `en` for English, `es` for Spanish, `ru` for Russian).
- `sourceLanguage`: Source language code or `auto` for automatic detection.
- `chatTranslationEnabled`: Enable or disable automatic chat translation.
- `tooltipTranslationEnabled`: Enable or disable item tooltip/lore translation.
- `chatPrefix`: Custom chat prefix and color codes for translated messages.
- `tooltipPrefix`: Custom tooltip prefix and color codes for translated lore lines.

---

## Technical Specifications

- Platform: NeoForge 1.21.4
- Java Version: Java 21
- Translation Backend: Google Translate Web API with MyMemory fallback
- Threading: Non-blocking Worker Thread Pool (Daemon)
- Network Impact: Zero bandwidth overhead on cached items

---

## License

MIT License. Free for use in any modpack.
