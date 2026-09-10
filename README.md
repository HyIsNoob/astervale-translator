# 🌐 Aster Vale Auto Translator (NeoForge 1.21.4)
**Mod Dịch Tự Động Tiếng Hàn Sang Tiếng Việt Cho Khung Chat & Item Tooltips**

Một bản mod **100% Client-Side** được thiết kế riêng cho người chơi máy chủ Minecraft **Aster Vale** (`1.224.237.11:25565`) trên nền tảng **NeoForge 1.21.4 (Java 21)**.

---

## ⚡ Các Tính Năng Nổi Bật

1. **Tự Động Dịch Khung Chat Thời Gian Thực:**
   * Tự động nhận diện mọi tin nhắn của người chơi và thông báo hệ thống chứa ký tự tiếng Hàn (`[\uac00-\ud7a3]`).
   * Dịch ngầm qua luồng bất đồng bộ (Async Thread) và hiển thị dòng dịch tiếng Việt màu xanh lơ `[VI]` nổi bật ngay bên dưới tin nhắn gốc.
   * Không làm giảm FPS, không bị khựng game.

2. **Tự Động Dịch Item Tooltips & Lore (Trang Bị, Vũ Khí, Sách Kỹ Năng):**
   * Quét và dịch toàn bộ tên và các dòng mô tả (Lore) của vật phẩm tiếng Hàn khi rê chuột trong kho đồ hoặc chợ.
   * Cơ chế **L1 RAM Cache** + **L2 File Cache JSON**: Lần rê chuột thứ 2 trở đi tốc độ là **0ms tức thì**!

3. **Từ Điển Thuật Ngữ Aster Vale Tích Hợp Sẵn:**
   * Đã được huấn luyện sẵn với các thuật ngữ riêng của server Aster Vale:
     * **Chứng khoán & Kinh tế:** `상장폐지` (Hủy niêm yết), `주식` (Chứng khoán), `매수` (Mua vào), `매도` (Bán ra), `평단가` (Giá vốn trung bình), `수익률` (Tỷ suất sinh lời)...
     * **Khoáng sản & Đá quý:** `오팔` (Ngọc Opal), `사파이어` (Ngọc Sapphire), `루비` (Ngọc Ruby), `월장석` (Đá Mặt Trăng), `블랙 다이아몬드` (Kim Cương Đen)...
     * **Nghề nghiệp & Đời sống:** `생활 일지` (Nhật ký Đời Sống), `대장장이` (Thợ Rèn), `광부` (Thợ Mỏ), `요리` (Nấu Ăn)...
     * **Trang bị & Kỹ năng:** `어린 수룡 갑옷` (Giáp Thủy Long Con), `별들의 축복` (Phước Lành Các Vì Sao)...

4. **Tàng Hình 100% (Client-Side Stealth):**
   * Mod được cấu hình cờ `displayTest = "IGNORE_ALL_VERSION"` và `clientSideOnly = true`.
   * Server Aster Vale không yêu cầu cài đặt mod này, không thực hiện packet handshake, **hoàn toàn vô hình trước anti-cheat**.

---

## 📥 Hướng Dẫn Cài Đặt

1. Tải file mod dạng `.jar` (ví dụ: `astervale-translator-1.21.4-1.0.0.jar`) từ mục **Releases** hoặc **Actions Artifacts**.
2. Mở thư mục chứa game của launcher:
   * Nhấn phím `Windows + R` $\rightarrow$ gõ `%appdata%` $\rightarrow$ tìm thư mục launcher của bạn (hoặc `.minecraft`).
   * Mở thư mục `mods/`.
3. Bỏ file `astervale-translator-1.21.4-1.0.0.jar` vào thư mục `mods/`.
4. Khởi động game và vào server Aster Vale để trải nghiệm!

---

## ⚙️ Cấu Hình Tùy Chỉnh

File cấu hình được tự động tạo tại:  
`.minecraft/config/astervale_translator.json`

```json
{
  "chatTranslationEnabled": true,
  "tooltipTranslationEnabled": true,
  "chatPrefix": "  §b[VI] §f",
  "tooltipPrefix": "§b[VI] §7",
  "translateItemName": true,
  "translateItemLore": true
}
```

Bộ nhớ đệm bản dịch được lưu tại:  
`.minecraft/config/astervale_translator_cache.json` (bạn có thể xem hoặc chỉnh sửa trực tiếp các từ dịch theo ý muốn).
