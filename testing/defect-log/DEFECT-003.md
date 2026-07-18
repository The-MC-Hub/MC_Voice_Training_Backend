## DEFECT-003: `POST /admin/migrate-db` hardcode tên database, bỏ qua cấu hình môi trường — nguy cơ đọc production/ghi đè database khác dù đang chạy ở môi trường test

- **Module:** Admin Dashboard (UC-09) — `DatabaseMigrationService.migrateFromMcHub()`
- **Ngày phát hiện:** 2026-07-17 (phát hiện qua đọc code TRƯỚC khi Execute TC-ADM-28, KHÔNG phải qua Execute thật — đã chủ động dừng lại không gọi endpoint này)
- **Test case liên quan:** TC-ADM-28 (`testing/03-system/UC-09-Admin-Dashboard_TestCases.md`) — **cố tình KHÔNG Execute**
- **Severity:** **Critical** (khả năng đọc dữ liệu production thật + ghi đè/xóa toàn bộ 1 database khác, không tôn trọng cấu hình môi trường test đang dùng)
- **Priority:** P0
- **Môi trường phát hiện:** Đọc source `src/main/java/com/mchub/services/impl/DatabaseMigrationService.java`, không phải qua Execute API thật.

### Root Cause (quan sát được, không tự sửa)
```java
public void migrateFromMcHub() {
    String sourceDbName = "mchub";           // hardcode — KHÔNG đọc từ application.properties/MONGODB_DATABASE
    String targetDbName = "voice-tranning";   // hardcode — KHÔNG đọc từ config
    ...
    MongoDatabase sourceDb = mongoClient.getDatabase(sourceDbName);
    MongoDatabase targetDb = mongoClient.getDatabase(targetDbName);
    ...
    for (String colName : collectionsToMigrate) {
        ...
        targetCol.drop();                      // XÓA TOÀN BỘ collection đích trước khi ghi lại
        for (Document doc : sourceCol.find()) {
            targetCol.insertOne(doc);
        }
    }
}
```
`mongoClient` là bean dùng chung kết nối tới **cùng 1 cluster Atlas** (`MainDatabase`) bất kể `spring.data.mongodb.database` được cấu hình là gì (`mchub`, `mchub_test`, hay giá trị khác). Vì `migrateFromMcHub()` tự gọi `mongoClient.getDatabase("mchub")` và `mongoClient.getDatabase("voice-tranning")` bằng tên cố định, **nó hoàn toàn phớt lờ** biến môi trường/override mà ứng dụng đang chạy — bất kể server đang "nghĩ" mình chạy trên `mchub_test`, hàm này vẫn thao tác thẳng vào 2 database cố định khác.

### Tại sao đây là rủi ro nghiêm trọng
1. **Đọc (source):** `sourceDb = mongoClient.getDatabase("mchub")` — đây chính là **database production thật** (đã xác nhận ở `testing/00-plan/Test_Plan.md` mục 6: `MONGODB_URI` gốc trỏ `/mchub`). Bất kỳ ai gọi endpoint này ở bất kỳ môi trường nào (kể cả server đang chạy với config trỏ `mchub_test` như QA đang dùng) đều **đọc thẳng dữ liệu thật của user thật**.
2. **Ghi (target):** `targetCol.drop()` rồi insert lại — **xóa sạch và ghi đè hoàn toàn** database `voice-tranning`. Nếu `voice-tranning` là database mặc định dùng cho *dev thông thường* (giá trị fallback thấy trong `application.properties`: `spring.data.mongodb.database=${MONGODB_DATABASE:voice-tranning}` — đây chính là **default khi KHÔNG set biến môi trường**), việc gọi migrate có thể xóa sạch dữ liệu của bất kỳ dev nào đang chạy backend local mà quên set `MONGODB_DATABASE`.
3. **QA hoàn toàn không kiểm soát được** database `voice-tranning` — nó không phải `mchub_test` mà QA tạo riêng để test an toàn. Việc endpoint `/admin/migrate-db` có thể bị gọi (dù vô tình, dù cố ý test) sẽ **ảnh hưởng ngoài phạm vi `mchub_test`** mà toàn bộ chiến lược test đã cam kết cô lập.

### Impact nghiệp vụ
- Rủi ro rò rỉ/thao túng dữ liệu người dùng thật (đọc từ `mchub` production) từ MỘT LỆNH GỌI API duy nhất, chỉ cần có quyền ADMIN — không có safeguard theo môi trường (dev/staging/prod).
- Rủi ro xóa sạch dữ liệu bất kỳ ai đang dùng database mặc định `voice-tranning` (developer local không set `MONGODB_DATABASE` sẽ vô tình dùng chung tên này).
- Đây là endpoint **ADMIN-only** nên rủi ro từ bên ngoài thấp hơn, nhưng rủi ro nội bộ (admin bấm nhầm nút migrate trên UI, hoặc QA/dev vô tình gọi test) là **rất cao** vì không có cảnh báo, không có dry-run, không có confirm 2 bước.

### Evidence
Trích trực tiếp source `DatabaseMigrationService.java` dòng 22-23, 42 (xem code block ở mục Root Cause).

### Status
**Fixed (2026-07-18).** `DatabaseMigrationService.migrateFromMcHub()` không còn hardcode tên database:
1. Source/target DB đọc từ `mchub.migration.source-db`/`mchub.migration.target-db` (env `MIGRATION_SOURCE_DB`/`MIGRATION_TARGET_DB`), không có default hardcode — nếu chưa set, method từ chối chạy với `IllegalStateException` rõ ràng.
2. Thêm feature flag `mchub.migration.enabled` (mặc định `false`) — phải bật tường minh mới chạy được.
3. Thêm guard so sánh `targetDbName` với `spring.data.mongodb.database` (database instance đang chạy thật) — từ chối migrate nếu trùng, tránh tự ghi đè database đang dùng.
4. Chưa gỡ endpoint (giữ nguyên theo yêu cầu, không tự ý xoá tính năng) — chỉ khoá an toàn bằng cấu hình.

Đã chạy `mvn test -Dtest=AdminControllerTest,AdminServiceImplTest` — 41/41 PASS, không hồi quy.
