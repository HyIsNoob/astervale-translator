package com.universal.translator.gui;

import com.universal.translator.UniversalTranslatorMod;
import com.universal.translator.config.SupportedLanguage;
import com.universal.translator.config.TranslatorConfig;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private Button systemChatButton;
    private Button tooltipButton;
    private Button tooltipShiftButton;
    private Button ignoreEnglishButton;
    private Button ignoreSelfButton;
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
        int startY = 36;
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

        // Row 2: Chat Translation & System Messages
        chatButton = addRenderableWidget(Button.builder(getChatButtonText(), btn -> {
            config.toggleChat();
            btn.setMessage(getChatButtonText());
        }).bounds(centerX - 160, startY + 23, btnWidth, btnHeight).build());

        systemChatButton = addRenderableWidget(Button.builder(getSystemChatButtonText(), btn -> {
            config.toggleSystemMessages();
            btn.setMessage(getSystemChatButtonText());
        }).bounds(centerX + 5, startY + 23, btnWidth, btnHeight).build());

        // Row 3: Item Tooltips & Shift Mode
        tooltipButton = addRenderableWidget(Button.builder(getTooltipButtonText(), btn -> {
            config.toggleTooltip();
            btn.setMessage(getTooltipButtonText());
        }).bounds(centerX - 160, startY + 46, btnWidth, btnHeight).build());

        tooltipShiftButton = addRenderableWidget(Button.builder(getTooltipShiftButtonText(), btn -> {
            config.toggleTooltipHoldShift();
            btn.setMessage(getTooltipShiftButtonText());
        }).bounds(centerX + 5, startY + 46, btnWidth, btnHeight).build());

        // Row 4: Ignore English & Ignore Own Chat
        ignoreEnglishButton = addRenderableWidget(Button.builder(getIgnoreEnglishButtonText(), btn -> {
            config.toggleIgnoreEnglish();
            btn.setMessage(getIgnoreEnglishButtonText());
        }).bounds(centerX - 160, startY + 69, btnWidth, btnHeight).build());

        ignoreSelfButton = addRenderableWidget(Button.builder(getIgnoreSelfButtonText(), btn -> {
            config.toggleIgnoreSelfChat();
            btn.setMessage(getIgnoreSelfButtonText());
        }).bounds(centerX + 5, startY + 69, btnWidth, btnHeight).build());

        // Row 5: Open Config Folder & Clear Cache
        openFolderButton = addRenderableWidget(Button.builder(Component.literal("Open Config Folder"), btn -> {
            try {
                Path configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
                net.minecraft.Util.getPlatform().openPath(configDir);
            } catch (Exception e) {
                testResult = "Error opening folder: " + e.getMessage();
            }
        }).bounds(centerX - 160, startY + 92, btnWidth, btnHeight).build());

        clearCacheButton = addRenderableWidget(Button.builder(Component.literal("Clear Cache"), btn -> {
            if (engine != null && engine.getCache() != null) {
                int count = engine.getCache().size();
                engine.getCache().clear();
                testResult = "Cache wiped (" + count + " items reset).";
            }
        }).bounds(centerX + 5, startY + 92, btnWidth, btnHeight).build());

        // Row 6: Live Translation Test Section
        int testHeaderY = startY + 118;
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
        }).bounds(centerX + 80, testInputY, 80, 20).build());

        // Done Button
        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            config.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(centerX - 75, this.height - 26, 150, 20).build());
    }

    private Component getMasterButtonText() {
        return Component.literal("Mod: " + (config.masterEnabled ? "§aENABLED" : "§cDISABLED"));
    }

    private Component getTargetLangButtonText() {
        SupportedLanguage lang = config.getSupportedLanguage();
        return Component.literal("Target: §b" + lang.getEnglishName() + " §7(" + lang.getCode().toUpperCase() + ")");
    }

    private Component getChatButtonText() {
        return Component.literal("Chat: " + (config.chatTranslationEnabled ? "§aON" : "§cOFF"));
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

    private Component getIgnoreSelfButtonText() {
        return Component.literal("Ignore Own Chat: " + (config.ignoreSelfChat ? "§aON" : "§cOFF"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        // Header Title & Subtitle
        graphics.drawCenteredString(this.font, Component.literal("§6§lUNIVERSAL AUTO TRANSLATOR §7(1.21.4)"), centerX, 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("§8Real-time Multilingual Chat & Item Tooltip Translation"), centerX, 22, 0x888888);

        // Test Section Title
        int testHeaderY = 154;
        graphics.drawString(this.font, Component.literal("§eLive Translation Tester (Supports 100+ languages):"), centerX - 160, testHeaderY, 0xAAAAAA);

        // Result Line (Placed safely below the input box)
        int resultY = testHeaderY + 36;
        graphics.drawString(this.font, Component.literal("§7Result: §a" + testResult), centerX - 160, resultY, 0xFFFFFF);

        // Hint at bottom
        graphics.drawCenteredString(this.font, Component.literal("§8Tip: Press 'V' in-game to open this menu anytime"), centerX, this.height - 38, 0x666666);
    }

    @Override
    public void onClose() {
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
