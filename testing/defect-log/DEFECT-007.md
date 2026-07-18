## DEFECT-007: `GET /admin/logs?limit=N` bỏ qua hoàn toàn tham số `limit` — luôn trả tối đa 200 log bất kể client yêu cầu

- **Module:** Log (System Log admin viewer)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-ADM-33 (system test UC-09, tiếp nối phiên trước).
- **Severity:** Minor
- **Priority:** P2
- **Môi trường:** LIVE — gọi thật `GET /api/v1/admin/logs?limit=10` trên `mchub_test`.

### Root Cause

`LogController.getLogs()` nhận `@RequestParam(defaultValue = "200") int limit` và truyền xuống `logService.getLogs(level, source, limit)`, nhưng `LogServiceImpl.getLogs()` **không hề dùng tham số `limit`** trong thân hàm:

```java
public List<SystemLog> getLogs(String level, String source, int limit) {
    if (level != null && source != null)
        return logRepository.findTop200ByLevelAndSourceOrderByTimestampDesc(...);
    if (level != null)
        return logRepository.findTop200ByLevelOrderByTimestampDesc(...);
    if (source != null)
        return logRepository.findTop200BySourceOrderByTimestampDesc(...);
    return logRepository.findTop200ByOrderByTimestampDesc();
}
```

Tất cả 4 nhánh gọi các method Spring Data đặt tên cứng `findTop200By...` — con số `200` nằm trong TÊN METHOD (Spring Data derived query), không phải biến động theo tham số `limit` truyền vào. Tham số `limit` của method là dead code — không bao giờ được đọc.

### Impact nghiệp vụ

Client (admin dashboard UI) gọi `?limit=10` mong đợi chỉ nhận 10 bản ghi log mới nhất để hiển thị gọn, nhưng thực tế luôn nhận về tối đa 200 bản ghi — gây lãng phí băng thông/render không cần thiết ở UI, và làm tham số `limit` trên API trở thành giao diện giả (không có tác dụng thật). Không phải lỗi bảo mật hay mất dữ liệu — chỉ là hành vi API không khớp hợp đồng (contract) mà tham số công khai `limit` ngụ ý.

### Evidence

```
$ curl "http://localhost:5555/api/v1/admin/logs?limit=10" -H "Authorization: Bearer <admin-jwt>"
HTTP 200
{"data": [... 200 phần tử ...]}   # kỳ vọng tối đa 10, thực tế 200
```
Xác nhận qua source `LogServiceImpl.java` dòng 110-119 — tham số `limit` của method không xuất hiện trong thân hàm.

### Status

**Fixed (2026-07-18).** Thay 4 method `findTop200By...` (hardcode) bằng 4 method `Pageable`-based tương ứng trong `SystemLogRepository`, dùng `PageRequest.of(0, cappedLimit)` trong `LogServiceImpl.getLogs()` — `limit` client truyền vào giờ thực sự áp dụng, cap tối đa ở 200 (giữ nguyên giới hạn an toàn cũ để tránh query quá lớn).

**Verify live (port 5555, `mchub_test`):** `GET /admin/logs?limit=5` → đúng 5 bản ghi (trước fix: luôn 200). `GET /admin/logs` (mặc định) → 200 bản ghi, đúng như cũ. `LogServiceImplTest` — 10/10 PASS (đã bổ sung 1 test case mới xác nhận `limit` được áp dụng đúng và cap ở 200).
