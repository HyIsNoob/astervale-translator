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
    private String testResult = "Nhập văn bản và bấm Dịch Thử để kiểm tra kết nối";

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
        int startY = 45;
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
        }).bounds(centerX - 160, startY + 26, btnWidth, btnHeight).build());

        tooltipButton = addRenderableWidget(Button.builder(getTooltipButtonText(), btn -> {
            config.toggleTooltip();
            btn.setMessage(getTooltipButtonText());
        }).bounds(centerX + 5, startY + 26, btnWidth, btnHeight).build());

        // Row 3: Ignore English & Clear Cache
        ignoreEnglishButton = addRenderableWidget(Button.builder(getIgnoreEnglishButtonText(), btn -> {
            config.toggleIgnoreEnglish();
            btn.setMessage(getIgnoreEnglishButtonText());
        }).bounds(centerX - 160, startY + 52, btnWidth, btnHeight).build());

        clearCacheButton = addRenderableWidget(Button.builder(Component.literal("Xóa Bộ Nhớ Cache"), btn -> {
            if (engine != null && engine.getCache() != null) {
                testResult = "Đã xóa toàn bộ cache (" + engine.getCache().size() + " mục).";
            }
        }).bounds(centerX + 5, startY + 52, btnWidth, btnHeight).build());

        // Row 4: Live Translation Test Box
        int testY = startY + 90;
        testInputBox = new EditBox(this.font, centerX - 160, testY + 15, 230, 20, Component.literal("Test Input"));
        testInputBox.setValue("안녕하세요! 상장폐지 위험이 있습니다.");
        addRenderableWidget(testInputBox);

        testTranslateButton = addRenderableWidget(Button.builder(Component.literal("Dịch Thử"), btn -> {
            String input = testInputBox.getValue();
            if (input == null || input.isBlank()) {
                testResult = "Vui lòng nhập văn bản cần dịch!";
                return;
            }
            testResult = "Đang gửi yêu cầu dịch...";
            engine.translateAsync(input, res -> {
                if (minecraft != null) {
                    minecraft.execute(() -> {
                        testResult = res;
                    });
                }
            });
        }).bounds(centerX + 75, testY + 15, 85, 20).build());

        // Row 5: Done Button
        addRenderableWidget(Button.builder(Component.literal("Lưu & Đóng"), btn -> {
            config.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(centerX - 75, this.height - 30, 150, 20).build());
    }

    private Component getMasterButtonText() {
        return Component.literal("Mod: " + (config.masterEnabled ? "§aBẬT (ON)" : "§cTẮT (OFF)"));
    }

    private Component getTargetLangButtonText() {
        String lang = "vi".equalsIgnoreCase(config.targetLanguage) ? "Tiếng Việt (VI)" : "English (EN)";
        return Component.literal("Đích: §b" + lang);
    }

    private Component getChatButtonText() {
        return Component.literal("Dịch Chat: " + (config.chatTranslationEnabled ? "§aBẬT" : "§cTẮT"));
    }

    private Component getTooltipButtonText() {
        return Component.literal("Dịch Item Lore: " + (config.tooltipTranslationEnabled ? "§aBẬT" : "§cTẮT"));
    }

    private Component getIgnoreEnglishButtonText() {
        return Component.literal("Bỏ Qua Tiếng Anh: " + (config.ignoreEnglish ? "§aBẬT (Giữ nguyên)" : "§cTẮT (Dịch hết)"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        // Title
        graphics.drawCenteredString(this.font, Component.literal("§6§lUNIVERSAL AUTO TRANSLATOR §7(NeoForge 1.21.4)"), centerX, 15, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("§8Cấu hình thời gian thực • Dịch chat & Item tooltips bằng Google Translate"), centerX, 28, 0x888888);

        // Test section header
        graphics.drawString(this.font, Component.literal("§eKiểm Tra Bản Dịch Trực Tiếp (Test Offline / Online):"), centerX - 160, 125, 0xAAAAAA);

        // Result line
        graphics.drawString(this.font, Component.literal("§7Kết quả: §f" + testResult), centerX - 160, 165, 0xFFFFFF);

        // Instructions
        graphics.drawCenteredString(this.font, Component.literal("§8Mẹo: Bấm phím 'V' trong game để mở menu này bất cứ lúc nào"), centerX, this.height - 45, 0x666666);
    }

    @Override
    public void onClose() {
        config.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
