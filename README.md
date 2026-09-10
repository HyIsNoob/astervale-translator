# Universal Auto Translator (NeoForge 1.21.4)
Client-Side Real-Time Chat & Item Tooltip Translator using Google Translate

A lightweight, 100% client-side Minecraft mod for NeoForge 1.21.4. Seamlessly translates in-game player chat, system notifications, and item tooltips (names and lore) into your native language in real-time with zero FPS drop.

---

## Features

- Real-Time Incoming Chat Translation:
  - Automatically intercepts player messages and server announcements in foreign languages (Korean, Japanese, Chinese, Russian, Vietnamese, etc.).
  - **Two Chat Modes**:
    - `BELOW`: Shows original message, then adds the translated message line below.
    - `REPLACE`: Directly replaces the original message with the clean translated text (ideal for compact chat).
  - Automatically filters out your own chat messages to prevent duplicate echo.
  - Optional toggle for server broadcast announcements.
- Outgoing Chat Auto-Translation (Send-Translate):
  - Type in Vietnamese, English, or any language, and the mod automatically translates your message into the server's language (e.g. Korean) before sending!
  - Commands (`/spawn`, `/help`, etc.) are NEVER touched.
  - Bypass prefix: Type `!` or `//` at the beginning of a message to send raw untranslated text.
- Item Tooltip & Lore Translation:
  - Translates custom item names, weapon stats, armor descriptions, and RPG lore on hover.
  - Two modes: Always show translation OR hold Shift to reveal translation.
- 17 Global Target Languages:
  - English, Vietnamese, Japanese, Korean, Simplified Chinese, Traditional Chinese, Spanish, Portuguese, French, German, Russian, Indonesian, Thai, Tagalog, Turkish, Arabic, Italian.
- In-Game GUI Menu (Press 'V'):
  - Intuitive graphical config screen accessible anytime with keybind `V` or `/translator`.
  - Built-in Live Translation Tester to test any text in real-time.
  - One-click buttons to Clear Cache and Open Config Folder directly in your file explorer.
- Two-Tier Caching System (0ms Latency):
  - L1 RAM Cache: Instant in-memory lookup.
  - L2 Disk Cache: Persists all translated phrases to disk (`.minecraft/config/universal_translator_cache.json`). Re-hovering an item takes 0ms.
- Customizable Game Lexicon:
  - Includes a customizable dictionary file (`.minecraft/config/universal_translator_lexicon.json`) for server-specific jargon, economy terms, and gem names.
- 100% Client-Side Stealth:
  - Configured with `displayTest = "IGNORE_ALL_VERSION"` and `clientSideOnly = true`.
  - Works on any server without server-side mod installation or handshake requirements. Completely safe and undetectable.

---

## Controls & Commands

- Press `V`: Open Universal Auto Translator Config GUI in-game.
- `/translator`: Open config menu via chat command.
- `/translate <text>`: Instantly test translation in chat.
- `!<text>` or `//<text>`: Send raw untranslated chat message (bypasses outgoing translation).

---

## Installation

1. Copy `universal_translator-1.21.4-1.0.0.jar` into your `.minecraft/mods/` folder.
2. Launch Minecraft with NeoForge 1.21.4 and enjoy.

---

## Configuration

Configuration file location:  
`.minecraft/config/universal_translator.json`

```json
{
  "masterEnabled": true,
  "chatTranslationEnabled": true,
  "tooltipTranslationEnabled": true,
  "tooltipHoldShift": false,
  "translateSystemMessages": true,
  "ignoreSelfChat": true,
  "targetLanguage": "vi",
  "sourceLanguage": "auto",
  "ignoreEnglish": true,
  "chatPrefix": "  §b[VI] §f",
  "tooltipPrefix": "§b[VI] §7"
}
```

---

## Technical Specifications

- Platform: NeoForge 1.21.4
- Java Version: Java 21
- Translation Backend: Google Mobile Web Engine with GTX, MyMemory, and Custom Lexicon fallbacks
- Threading: Non-blocking Worker Thread Pool (Daemon)
- Network Impact: Zero bandwidth overhead on cached items

---

## License

MIT License. Free for use in any modpack.
