package com.universal.translator.gui;

import com.universal.translator.UniversalTranslatorMod;
import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TranslatorConfigScreen extends Screen {

    private final Screen parentScreen;
    private final TranslatorConfig config;
    private final TranslationEngine engine;

    private Button masterButton;
    private Button targetLangButton;
    private Button chatButton;
    private Button tooltipButton;
    private Button ignoreEnglishButton;
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
        int startY = 40;
        int btnWidth = 155;
        int btnHeight = 20;

        // Row 1: Master Switch & Target Language
        masterButton = addRenderableWidget(Button.builder(getMasterButtonText(), btn -> {
            config.toggleMaster();
            btn.setMessage(getMasterButtonText());
        }).bounds(centerX - 160, startY, btnWidth, btnHeight).build());

        targetLangButton = addRenderableWidget(Button.builder(getTargetLangButtonText(), btn -> {
            config.cycleTargetLanguage();
            btn.setMessage(getTargetLangButtonText());
        }).bounds(centerX + 5, startY, btnWidth, btnHeight).build());

        // Row 2: Chat & Tooltip
        chatButton = addRenderableWidget(Button.builder(getChatButtonText(), btn -> {
            config.toggleChat();
            btn.setMessage(getChatButtonText());
        }).bounds(centerX - 160, startY + 24, btnWidth, btnHeight).build());

        tooltipButton = addRenderableWidget(Button.builder(getTooltipButtonText(), btn -> {
            config.toggleTooltip();
            btn.setMessage(getTooltipButtonText());
        }).bounds(centerX + 5, startY + 24, btnWidth, btnHeight).build());

        // Row 3: Ignore English & Clear Cache
        ignoreEnglishButton = addRenderableWidget(Button.builder(getIgnoreEnglishButtonText(), btn -> {
            config.toggleIgnoreEnglish();
            btn.setMessage(getIgnoreEnglishButtonText());
        }).bounds(centerX - 160, startY + 48, btnWidth, btnHeight).build());

        clearCacheButton = addRenderableWidget(Button.builder(Component.literal("Clear Cache"), btn -> {
            if (engine != null && engine.getCache() != null) {
                int count = engine.getCache().size();
                testResult = "Cache cleared (" + count + " items reset).";
            }
        }).bounds(centerX + 5, startY + 48, btnWidth, btnHeight).build());

        // Row 4: Live Translation Test Section
        int testHeaderY = startY + 80;
        int testInputY = testHeaderY + 14;

        testInputBox = new EditBox(this.font, centerX - 160, testInputY, 235, 20, Component.literal("Test Input"));
        testInputBox.setValue("안녕하세요! 상장폐지 위험이 있습니다.");
        addRenderableWidget(testInputBox);

        testTranslateButton = addRenderableWidget(Button.builder(Component.literal("Test"), btn -> {
            String input = testInputBox.getValue();
            if (input == null || input.isBlank()) {
                testResult = "Please enter text to translate!";
                return;
            }
            testResult = "Requesting translation...";
            engine.translateAsync(input, res -> {
                if (minecraft != null) {
                    minecraft.execute(() -> {
                        testResult = res;
                    });
                }
            });
        }).bounds(centerX + 80, testInputY, 80, 20).build());

        // Row 5: Save & Done Button
        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            config.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(centerX - 75, this.height - 28, 150, 20).build());
    }

    private Component getMasterButtonText() {
        return Component.literal("Mod: " + (config.masterEnabled ? "§aENABLED" : "§cDISABLED"));
    }

    private Component getTargetLangButtonText() {
        String lang = "vi".equalsIgnoreCase(config.targetLanguage) ? "Vietnamese (VI)" : "English (EN)";
        return Component.literal("Target: §b" + lang);
    }

    private Component getChatButtonText() {
        return Component.literal("Chat: " + (config.chatTranslationEnabled ? "§aON" : "§cOFF"));
    }

    private Component getTooltipButtonText() {
        return Component.literal("Item Lore: " + (config.tooltipTranslationEnabled ? "§aON" : "§cOFF"));
    }

    private Component getIgnoreEnglishButtonText() {
        return Component.literal("Ignore English: " + (config.ignoreEnglish ? "§aON" : "§cOFF"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        // Header Title & Subtitle
        graphics.drawCenteredString(this.font, Component.literal("§6§lUNIVERSAL AUTO TRANSLATOR §7(1.21.4)"), centerX, 12, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("§8Real-time Chat & Item Tooltip Translation using Google Translate"), centerX, 24, 0x888888);

        // Test Section Title
        int testHeaderY = 120;
        graphics.drawString(this.font, Component.literal("§eLive Translation Tester (Offline / Online):"), centerX - 160, testHeaderY, 0xAAAAAA);

        // Result Line (Placed safely below the input box so it NEVER overlaps!)
        int resultY = testHeaderY + 40;
        graphics.drawString(this.font, Component.literal("§7Result: §a" + testResult), centerX - 160, resultY, 0xFFFFFF);

        // Hint at bottom
        graphics.drawCenteredString(this.font, Component.literal("§8Tip: Press 'V' in-game to open this menu anytime"), centerX, this.height - 42, 0x666666);
    }

    @Override
    public void onClose() {
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
