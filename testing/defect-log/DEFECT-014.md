## DEFECT-014: `POST /admin/announcements/{id}/send` trả HTTP 200 "Đang gửi..." dù announcement đã SENT — guard `@Async` bị nuốt exception, không phản hồi lỗi cho client

- **Module:** Marketing & Communication (Announcement send)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-ANN-20 (system test UC-10, gọi `send` lần 2 trên announcement đã ở trạng thái SENT).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `POST /api/v1/admin/announcements/{id}/send` trên `mchub_test`.

### Root Cause

`AnnouncementService.approveAndSend(String id, List<String> recipientIds)` có guard đúng:
```java
@Async
public void approveAndSend(String id, List<String> recipientIds) {
    Announcement ann = getById(id);
    if (ann.getStatus() == Announcement.AnnouncementStatus.SENT) {
        throw new AppException(ErrorCode.VALIDATION_FAILED, "Already sent");
    }
    ...
```
Nhưng method có `@Async` — khi `AnnouncementController.send()` gọi method này, Spring trả về ngay lập tức (fire-and-forget, `void` return type không có `Future`/`CompletableFuture` để bắt kết quả), controller trả HTTP 200 `"Đang gửi email hàng loạt..."` **trước khi** method async thực sự chạy. Guard `if (status == SENT) throw` chạy ở background thread SAU KHI response đã gửi về client — exception bị Spring's `SimpleAsyncUncaughtExceptionHandler` bắt và chỉ log ra console server, **không bao giờ đến được HTTP response**.

Kết quả: Admin gọi `send` lần 2 (vô tình double-click, hoặc gọi lại do nhầm lẫn UI) trên 1 announcement **đã gửi rồi** vẫn nhận HTTP 200 thành công giả — không có cách nào (qua API) để biết lệnh gửi lần 2 đã thực sự bị chặn ở background.

**May mắn:** vì method thoát sớm ở dòng `throw` (trước khi loop gửi email thực sự), announcement KHÔNG bị gửi email lần 2 thật sự (guard vẫn có tác dụng chặn hành động thực tế) — chỉ có UX/observability bị sai (client tưởng thành công nhưng thực ra không có gì xảy ra ở background).

### Impact nghiệp vụ

- Admin không có cách nào phân biệt "gửi thành công" với "gửi bị chặn do đã gửi rồi" qua response API — cả 2 trường hợp đều trả về y hệt `{"message": "Đang gửi email hàng loạt..."}`.
- Nếu admin double-click nút gửi trên UI (race condition phổ biến khi request đầu tiên chưa kịp cập nhật UI trạng thái `SENT`), request thứ 2 "thành công giả" — dễ gây admin nhầm tưởng hệ thống bug/gửi thiếu, report sai vấn đề.
- Đây là pattern nguy hiểm hơn với overload `approveAndSend(String id)` (không tham số `recipientIds`, dùng cho luồng UI chính) — có cùng lỗi cấu trúc y hệt, chưa test riêng vì controller hiện chỉ expose overload có `recipientIds`.

### Evidence

```
$ curl -X POST ".../announcements/{id}/send" -d '{"recipientIds":["..."]}'   # lần 1
HTTP 200 {"message":"Đang gửi email hàng loạt..."}
→ status DRAFT → SENT đúng, 1 email gửi thật

$ curl -X POST ".../announcements/{id}/send" -d '{"recipientIds":["..."]}'   # lần 2, đã SENT
HTTP 200 {"message":"Đang gửi email hàng loạt..."}   ← SAI, phải là lỗi

# Server log xác nhận exception bị nuốt:
ERROR o.s.a.i.SimpleAsyncUncaughtExceptionHandler - Unexpected exception occurred invoking async method:
public void com.mchub.services.AnnouncementService.approveAndSend(java.lang.String,java.util.List)
com.mchub.exception.AppException: Already sent
```

Source: `AnnouncementService.java` dòng 101-104 (`@Async` + guard throw không propagate); `AnnouncementController.java` dòng 97-105 (`send()` không có cách nhận kết quả async).

### Status

**Fixed (2026-07-18).** Áp dụng đúng đề xuất: thêm kiểm tra trạng thái `SENT` đồng bộ TRONG CONTROLLER (`AnnouncementController.send()`), trước khi gọi `approveAndSend()` — dùng `announcementService.getById(id)` (đã có sẵn) để đọc status trước, ném `AppException(VALIDATION_FAILED, "Announcement already sent")` ngay nếu đã `SENT`, không chạm tới method `@Async` nữa trong trường hợp này.

**Verify live (port 5555, `mchub_test`)** — tái hiện đúng kịch bản gốc:
1. Tạo announcement mới → gửi lần 1 (scoped tới 1 QA recipient) → 200 "Đang gửi email hàng loạt..." → status chuyển `SENT`.
2. Gửi lần 2 (đã `SENT`) → **HTTP 400 "Announcement already sent"** ngay lập tức (trước fix: vẫn 200 giả, lỗi thật bị nuốt trong `@Async`).

Cập nhật test hồi quy: `AnnouncementControllerTest$Send` bổ sung 2 test mới cho việc stub `getById()` (test cũ chưa cần stub vì controller chưa gọi `getById` trước khi sửa) + 1 test mới `rejectsWhenAlreadySent` xác nhận guard đồng bộ hoạt động và `approveAndSend()` không hề được gọi khi bị chặn. `AnnouncementControllerTest`/`AnnouncementServiceTest` — 27/27 PASS.

Chưa áp dụng riêng cho overload `approveAndSend(id)` (không tham số `recipientIds`) vì controller hiện tại chỉ dùng overload có `recipientIds` — guard mới ở tầng Controller áp dụng chung cho cả 2 overload vì được chặn trước khi gọi bất kỳ overload nào.
