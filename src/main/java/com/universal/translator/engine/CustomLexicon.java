package com.universal.translator.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom lexicon dictionary for game terminology, item names, and jargon.
 * Users can customize this file at: .minecraft/config/universal_translator_lexicon.json
 */
public class CustomLexicon {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomLexicon.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, String> lexicon = new LinkedHashMap<>();
    private Path lexiconFilePath;

    public void init(Path configDirectory) {
        this.lexiconFilePath = configDirectory.resolve("universal_translator_lexicon.json");
        if (Files.exists(lexiconFilePath)) {
            load();
        } else {
            seedDefaults();
            save();
        }
    }

    private void seedDefaults() {
        // Stock Market & Economy Terms
        lexicon.put("상장폐지", "Hủy niêm yết");
        lexicon.put("주식", "Chứng khoán");
        lexicon.put("증권", "Chứng khoán");
        lexicon.put("매수", "Mua vào");
        lexicon.put("매도", "Bán ra");
        lexicon.put("체결", "Khớp lệnh");
        lexicon.put("보유량", "Số lượng sở hữu");
        lexicon.put("평단가", "Giá vốn trung bình");
        lexicon.put("수익률", "Tỷ suất sinh lời");
        lexicon.put("현재가", "Giá hiện tại");
        lexicon.put("시세", "Giá thị trường");
        lexicon.put("호가", "Sổ lệnh");
        lexicon.put("시장가", "Giá thị trường");
        lexicon.put("지정가", "Giá giới hạn");
        lexicon.put("강도", "Lực mua");

        // Gems & Minerals
        lexicon.put("오팔", "Ngọc Opal");
        lexicon.put("사파이어", "Ngọc Sapphire");
        lexicon.put("루비", "Ngọc Ruby");
        lexicon.put("월장석", "Đá Mặt Trăng");
        lexicon.put("블랙 다이아몬드", "Kim Cương Đen");
        lexicon.put("다이아몬드", "Kim Cương");
        lexicon.put("에메랄드", "Ngọc Lục Bảo");

        // Jobs & Life
        lexicon.put("생활 일지", "Nhật ký Đời Sống");
        lexicon.put("대장장이", "Thợ Rèn");
        lexicon.put("광부", "Thợ Mỏ");
        lexicon.put("농부", "Nông Dân");
        lexicon.put("요리사", "Đầu Bếp");
        lexicon.put("요리", "Nấu Ăn");
        lexicon.put("낚시꾼", "Ngư Dân");
        lexicon.put("낚시", "Câu Cá");

        // Server System & RPG Lore
        lexicon.put("별들의 축복", "Phước Lành Của Các Vì Sao");
        lexicon.put("어린 수룡 갑옷", "Giáp Thủy Long Con");
        lexicon.put("수룡", "Thủy Long");
        lexicon.put("가호", "Gia Hộ");
        lexicon.put("주민등록증", "Thẻ Căn Cước");
        lexicon.put("업적", "Thành Tựu");
        lexicon.put("정기 점검", "Bảo trì định kỳ");
        lexicon.put("긴급 점검", "Bảo trì khẩn cấp");
        lexicon.put("서버 오픈", "Máy chủ mở cửa");
    }

    private void load() {
        try (Reader reader = Files.newBufferedReader(lexiconFilePath)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                lexicon.putAll(loaded);
                LOGGER.info("Loaded {} custom lexicon terms from {}", lexicon.size(), lexiconFilePath.getFileName());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load custom lexicon: {}", e.getMessage());
        }
    }

    public synchronized void save() {
        if (lexiconFilePath == null) return;
        try (Writer writer = Files.newBufferedWriter(lexiconFilePath)) {
            GSON.toJson(lexicon, writer);
        } catch (Exception e) {
            LOGGER.warn("Failed to save custom lexicon: {}", e.getMessage());
        }
    }

    public String applyPreprocess(String text) {
        if (text == null || text.isBlank() || lexicon.isEmpty()) return text;
        String processed = text;
        for (Map.Entry<String, String> entry : lexicon.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }
        return processed;
    }

    public Map<String, String> getLexicon() {
        return lexicon;
    }
}
