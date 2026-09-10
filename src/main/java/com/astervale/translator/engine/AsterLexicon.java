package com.astervale.translator.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Curated lexicon for Aster Vale server mechanics:
 * Stock market (증권), mining/gems (보석), jobs (직업), and RPG gear (장비).
 */
public class AsterLexicon {

    private static final Map<String, String> LEXICON = new LinkedHashMap<>();

    static {
        // Stock Market & Economy Terms (from reverse engineering)
        LEXICON.put("상장폐지", "Hủy niêm yết");
        LEXICON.put("주식", "Chứng khoán");
        LEXICON.put("증권", "Chứng khoán");
        LEXICON.put("매수", "Mua vào");
        LEXICON.put("매도", "Bán ra");
        LEXICON.put("체결", "Khớp lệnh");
        LEXICON.put("보유량", "Số lượng sở hữu");
        LEXICON.put("평단가", "Giá vốn trung bình");
        LEXICON.put("수익률", "Tỷ suất sinh lời");
        LEXICON.put("현재가", "Giá hiện tại");
        LEXICON.put("시세", "Giá thị trường");
        LEXICON.put("호가", "Sổ lệnh");
        LEXICON.put("시장가", "Giá thị trường");
        LEXICON.put("지정가", "Giá giới hạn");
        LEXICON.put("강도", "Lực mua (Strength)");

        // Gems & Minerals
        LEXICON.put("오팔", "Ngọc Opal");
        LEXICON.put("사파이어", "Ngọc Sapphire");
        LEXICON.put("루비", "Ngọc Ruby");
        LEXICON.put("월장석", "Đá Mặt Trăng");
        LEXICON.put("블랙 다이아몬드", "Kim Cương Đen");
        LEXICON.put("다이아몬드", "Kim Cương");
        LEXICON.put("에메랄드", "Ngọc Lục Bảo");

        // Jobs & Daily Life (생활 일지)
        LEXICON.put("생활 일지", "Nhật ký Đời Sống");
        LEXICON.put("대장장이", "Thợ Rèn");
        LEXICON.put("광부", "Thợ Mỏ");
        LEXICON.put("농부", "Nông Dân");
        LEXICON.put("요리사", "Đầu Bếp");
        LEXICON.put("요리", "Nấu Ăn");
        LEXICON.put("낚시꾼", "Ngư Dân");
        LEXICON.put("낚시", "Câu Cá");

        // Server System & RPG Lore
        LEXICON.put("별들의 축복", "Phước Lành Của Các Vì Sao");
        LEXICON.put("어린 수룡 갑옷", "Giáp Thủy Long Con");
        LEXICON.put("수룡", "Thủy Long");
        LEXICON.put("가호", "Gia Hộ (Blessing)");
        LEXICON.put("주민등록증", "Thẻ Căn Cước");
        LEXICON.put("업적", "Thành Tựu");
        LEXICON.put("정기 점검", "Bảo trì định kỳ");
        LEXICON.put("긴급 점검", "Bảo trì khẩn cấp");
        LEXICON.put("서버 오픈", "Máy chủ mở cửa");
    }

    /**
     * Pre-process text replacing known server-specific jargon.
     */
    public static String applyPreprocess(String text) {
        if (text == null || text.isBlank()) return text;
        String processed = text;
        for (Map.Entry<String, String> entry : LEXICON.entrySet()) {
            processed = processed.replace(entry.getKey(), entry.getValue());
        }
        return processed;
    }

    public static Map<String, String> getLexicon() {
        return LEXICON;
    }
}
