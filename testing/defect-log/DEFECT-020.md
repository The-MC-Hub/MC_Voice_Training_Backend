## DEFECT-020: `GET /public/mcs?category=` bỏ qua hoàn toàn tham số lọc — trả về TOÀN BỘ MC bất kể category truyền vào

- **Module:** User & MC Profile (Public MC Discovery)
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-PROF-16 (system test UC-02), xác nhận qua đọc code `PublicServiceImpl.discoverMCs()`.
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — `GET /api/v1/public/mcs?category=WEDDING_MC` trên `mchub_test`.

### Root Cause

`PublicController.discoverMCs()` nhận `@RequestParam(required = false) String category` và truyền xuống `publicService.discoverMCs(category)`. Nhưng `PublicServiceImpl.discoverMCs(String category)`:
```java
public List<MCProfileResponseDTO> discoverMCs(String category) {
    List<MCProfile> profiles = mcProfileRepository.findAll();
    ...
    return profiles.stream()
            .map(profile -> mcProfileMapper.toResponseDTO(profile, userMap.get(profile.getUser())))
            .toList();
}
```
Tham số `category` được nhận vào nhưng **KHÔNG hề được dùng ở bất kỳ đâu trong method** — không có `.filter()` nào đối chiếu `category` với `profile.getStyles()` hay bất kỳ field nào. Kết quả: mọi giá trị `category` (kể cả giá trị không hợp lệ/không tồn tại trong enum `CourseType`) đều trả về CÙNG MỘT danh sách — toàn bộ MC profile trong hệ thống.

### Impact nghiệp vụ

UC-02 tính năng #9 "Khám phá hồ sơ MC công khai — tìm kiếm/lọc hồ sơ MC theo chuyên mục" hoàn toàn không hoạt động — người dùng công khai (chưa đăng nhập) chọn lọc theo category trên trang khám phá MC sẽ luôn thấy TOÀN BỘ MC, không đúng với lựa chọn họ đã chọn. Với hệ thống có nhiều MC thuộc nhiều category khác nhau, đây là lỗi UX nghiêm trọng — trang tìm kiếm mất tác dụng lọc, có thể khiến người dùng khó tìm đúng MC phù hợp nhu cầu (VD: tìm MC đám cưới nhưng thấy lẫn MC hội nghị doanh nghiệp).

### Evidence

Đối chiếu trực tiếp mã nguồn — `PublicServiceImpl.java` dòng 91-106: tham số `category` chỉ xuất hiện trong signature method, không xuất hiện lại trong thân method. Test thực tế với 1 MC profile duy nhất trong DB test không đủ để chứng minh qua so sánh kết quả (vì chỉ có 1 kết quả dù lọc hay không), nhưng đối chiếu code xác nhận chắc chắn đây là dead parameter — không có nhánh logic nào xử lý nó.

### Status

**Fixed (2026-07-18).** Bổ sung filter trong `discoverMCs()` theo đúng đề xuất, lọc theo `MCProfile.styles`:
```java
if (category != null && !category.isBlank()) {
    profiles = profiles.stream()
        .filter(p -> p.getStyles() != null && p.getStyles().contains(category))
        .toList();
}
```

**Verify live (port 5555, `mchub_test`)** — tạo thêm 1 MC thứ 2 với `styles:["CORPORATE_EVENT"]` khác MC gốc (`styles:["WEDDING","CORPORATE_EVENT"]`) để có dữ liệu phân biệt được:
- `GET /public/mcs` (không filter) → 2 kết quả.
- `GET /public/mcs?category=CORPORATE_EVENT` → đúng 2 kết quả (cả 2 MC đều có style này).
- `GET /public/mcs?category=WEDDING_MC` → 0 kết quả (không MC nào có style trùng khớp) — xác nhận filter hoạt động thật, không phải trùng hợp ngẫu nhiên như lần test trước (khi chỉ có 1 MC fixture).

`PublicServiceImplTest`/`PublicControllerTest` — 18/18 PASS.
