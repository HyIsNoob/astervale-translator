package com.universal.translator.gui;

import com.universal.translator.UniversalTranslatorMod;
import com.universal.translator.config.SupportedLanguage;
import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class TranslatorConfigScreen extends Screen {

    private final Screen parentScreen;
    private final TranslatorConfig config;
    private final TranslationEngine engine;

    private Button masterButton;
    private Button targetLangButton;
    private Button chatButton;
    private Button chatModeButton;
    private Button tooltipButton;
    private Button tooltipShiftButton;
    private Button outgoingButton;
    private Button outgoingTargetButton;
    private Button ignoreEnglishButton;
    private Button systemChatButton;
    private Button selfChatButton;
    private Button openFolderButton;
    private Button clearCacheButton;

    private EditBox testInputBox;
    private Button testTranslateButton;
    private String testResult = "Type foreign text and click Test to verify translation";

    public TranslatorConfigScreen(Screen parentScreen) {
        super(Component.literal("Universal Auto Translator Config"));
        this.parentScreen = parentScreen;
        this.config = UniversalTranslatorMod.getConfig();
        this.engine = UniversalTranslatorMod.getEngine();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 32;
        int btnWidth = 155;
        int btnHeight = 20;

        // Row 1: Master Switch & Target Language (Incoming)
        masterButton = addRenderableWidget(Button.builder(getMasterButtonText(), btn -> {
            config.toggleMaster();
            btn.setMessage(getMasterButtonText());
        }).bounds(centerX - 160, startY, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Master switch to enable or disable all translation features.")))
          .build());

        targetLangButton = addRenderableWidget(Button.builder(getTargetLangButtonText(), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new LanguageSelectionScreen(
                    this,
                    Component.literal("Select Incoming Target Language"),
                    config.getSupportedLanguage(),
                    selected -> {
                        config.setTargetLanguage(selected.getCode());
                        targetLangButton.setMessage(getTargetLangButtonText());
                    }
                ));
            }
        }).bounds(centerX + 5, startY, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Target language for incoming foreign chat & item lore.\nClick to open scrollable list.")))
          .build());

        // Row 2: Chat Translation & Chat Mode (BELOW / REPLACE)
        chatButton = addRenderableWidget(Button.builder(getChatButtonText(), btn -> {
            config.toggleChat();
            btn.setMessage(getChatButtonText());
        }).bounds(centerX - 160, startY + 22, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Automatically translate foreign chat messages from other players in real time.")))
          .build());

        chatModeButton = addRenderableWidget(Button.builder(getChatModeButtonText(), btn -> {
            config.toggleChatReplaceMode();
            btn.setMessage(getChatModeButtonText());
        }).bounds(centerX + 5, startY + 22, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("BELOW: Shows translation line under original message.\nREPLACE: Cleanly replaces original message with translation.")))
          .build());

        // Row 3: Item Tooltips & Shift Mode
        tooltipButton = addRenderableWidget(Button.builder(getTooltipButtonText(), btn -> {
            config.toggleTooltip();
            btn.setMessage(getTooltipButtonText());
        }).bounds(centerX - 160, startY + 44, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Translate custom item names and RPG lore lines when hovering over items in inventories.")))
          .build());

        tooltipShiftButton = addRenderableWidget(Button.builder(getTooltipShiftButtonText(), btn -> {
            config.toggleTooltipHoldShift();
            btn.setMessage(getTooltipShiftButtonText());
        }).bounds(centerX + 5, startY + 44, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("ALWAYS: Item lore is always translated.\nHOLD SHIFT: Translation is only shown while holding the Shift key.")))
          .build());

        // Row 4: Outgoing Translation & Outgoing Target Language
        outgoingButton = addRenderableWidget(Button.builder(getOutgoingButtonText(), btn -> {
            config.toggleOutgoingTranslation();
            btn.setMessage(getOutgoingButtonText());
        }).bounds(centerX - 160, startY + 66, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Automatically translate what YOU type into foreign language before sending.\nPrefix with '!' or '//' to bypass.")))
          .build());

        outgoingTargetButton = addRenderableWidget(Button.builder(getOutgoingTargetButtonText(), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new LanguageSelectionScreen(
                    this,
                    Component.literal("Select Outgoing Target Language"),
                    config.getOutgoingSupportedLanguage(),
                    selected -> {
                        config.setOutgoingTargetLanguage(selected.getCode());
                        outgoingTargetButton.setMessage(getOutgoingTargetButtonText());
                    }
                ));
            }
        }).bounds(centerX + 5, startY + 66, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Target language for your outgoing chat messages.\nClick to open scrollable list.")))
          .build());

        // Row 5: Ignore English & Broadcasts
        ignoreEnglishButton = addRenderableWidget(Button.builder(getIgnoreEnglishButtonText(), btn -> {
            config.toggleIgnoreEnglish();
            btn.setMessage(getIgnoreEnglishButtonText());
        }).bounds(centerX - 160, startY + 88, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Skip translating messages already written in English or Latin alphabet (recommended for global servers).")))
          .build());

        systemChatButton = addRenderableWidget(Button.builder(getSystemChatButtonText(), btn -> {
            config.toggleSystemMessages();
            btn.setMessage(getSystemChatButtonText());
        }).bounds(centerX + 5, startY + 88, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Translate system notices, server broadcasts, and action bar text.")))
          .build());

        // Row 6: Translate Self Chat & (Open Folder / Clear Cache)
        selfChatButton = addRenderableWidget(Button.builder(getSelfChatButtonText(), btn -> {
            config.toggleIgnoreSelfChat();
            btn.setMessage(getSelfChatButtonText());
        }).bounds(centerX - 160, startY + 110, btnWidth, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Translate your own chat messages.\nTurn ON for offline testing & debugging, OFF for normal gameplay.")))
          .build());

        openFolderButton = addRenderableWidget(Button.builder(Component.literal("Open Folder"), btn -> {
            try {
                Path configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
                net.minecraft.Util.getPlatform().openPath(configDir);
            } catch (Exception e) {
                testResult = "Error opening folder: " + e.getMessage();
            }
        }).bounds(centerX + 5, startY + 110, 75, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Open the Minecraft config directory to view or edit custom lexicon files.")))
          .build());

        clearCacheButton = addRenderableWidget(Button.builder(Component.literal("Clear Cache"), btn -> {
            if (engine != null && engine.getCache() != null) {
                int count = engine.getCache().size();
                engine.getCache().clear();
                testResult = "Cache wiped (" + count + " items reset).";
            }
        }).bounds(centerX + 85, startY + 110, 75, btnHeight)
          .tooltip(Tooltip.create(Component.literal("Wipe both memory and disk translation cache files to reset all stored translations.")))
          .build());

        // Row 7: Live Translation Test Section
        int testHeaderY = startY + 134;
        int testInputY = testHeaderY + 13;

        testInputBox = new EditBox(this.font, centerX - 160, testInputY, 235, 20, Component.literal("Test Input"));
        testInputBox.setValue("안녕하세요! 상장폐지 위험이 있습니다.");
        addRenderableWidget(testInputBox);

        testTranslateButton = addRenderableWidget(Button.builder(Component.literal("Test"), btn -> {
            String input = testInputBox.getValue();
            if (input == null || input.isBlank()) {
                testResult = "Please enter text to translate!";
                return;
            }
            testResult = "Translating into " + config.getSupportedLanguage().getEnglishName() + "...";
            engine.translateDirectAsync(input, res -> {
                if (minecraft != null) {
                    minecraft.execute(() -> {
                        testResult = res;
                    });
                }
            });
        }).bounds(centerX + 80, testInputY, 80, 20)
          .tooltip(Tooltip.create(Component.literal("Send the text to Google Translate engine to test translation immediately.")))
          .build());

        // Done Button
        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            config.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(centerX - 75, this.height - 24, 150, 20)
          .tooltip(Tooltip.create(Component.literal("Save all configuration settings and return to previous menu.")))
          .build());
    }

    private Component getMasterButtonText() {
        return Component.literal("Mod: " + (config.masterEnabled ? "§aENABLED" : "§cDISABLED"));
    }

    private Component getTargetLangButtonText() {
        SupportedLanguage lang = config.getSupportedLanguage();
        return Component.literal("In Target: §b" + lang.getEnglishName() + " §7(" + lang.getCode().toUpperCase() + ") ▾");
    }

    private Component getChatButtonText() {
        return Component.literal("Chat: " + (config.chatTranslationEnabled ? "§aON" : "§cOFF"));
    }

    private Component getChatModeButtonText() {
        return Component.literal("Chat Mode: " + (config.chatReplaceMode ? "§eREPLACE" : "§aBELOW"));
    }

    private Component getOutgoingButtonText() {
        return Component.literal("Translate Sent: " + (config.outgoingTranslationEnabled ? "§aON" : "§cOFF"));
    }

    private Component getOutgoingTargetButtonText() {
        SupportedLanguage lang = config.getOutgoingSupportedLanguage();
        return Component.literal("Send Target: §b" + lang.getEnglishName() + " §7(" + lang.getCode().toUpperCase() + ") ▾");
    }

    private Component getSystemChatButtonText() {
        return Component.literal("Broadcasts: " + (config.translateSystemMessages ? "§aON" : "§cOFF"));
    }

    private Component getTooltipButtonText() {
        return Component.literal("Item Lore: " + (config.tooltipTranslationEnabled ? "§aON" : "§cOFF"));
    }

    private Component getTooltipShiftButtonText() {
        return Component.literal("Lore Mode: " + (config.tooltipHoldShift ? "§eHOLD SHIFT" : "§aALWAYS"));
    }

    private Component getIgnoreEnglishButtonText() {
        return Component.literal("Ignore English: " + (config.ignoreEnglish ? "§aON" : "§cOFF"));
    }

    private Component getSelfChatButtonText() {
        return Component.literal("Translate Self: " + (!config.ignoreSelfChat ? "§aON" : "§cOFF"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        // Header Title & Subtitle
        graphics.drawCenteredString(this.font, Component.literal("§6§lUNIVERSAL AUTO TRANSLATOR §7(1.21.4)"), centerX, 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("§8Real-time Multilingual Chat & Item Tooltip Translation"), centerX, 22, 0x888888);

        // Test Section Title
        int testHeaderY = 166;
        graphics.drawString(this.font, Component.literal("§eLive Translation Tester (Supports 100+ languages):"), centerX - 160, testHeaderY, 0xAAAAAA);

        // Result Line (Placed safely below the input box)
        int resultY = testHeaderY + 36;
        graphics.drawString(this.font, Component.literal("§7Result: §a" + testResult), centerX - 160, resultY, 0xFFFFFF);

        // Hint at bottom
        graphics.drawCenteredString(this.font, Component.literal("§8Tip: Press 'V' to configure | Type '!' or '//' in chat to bypass outgoing translation"), centerX, this.height - 36, 0x666666);
    }

    @Override
    public void onClose() {
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
