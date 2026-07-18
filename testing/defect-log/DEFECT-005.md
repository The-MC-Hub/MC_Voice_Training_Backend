## DEFECT-005: `MCProfileServiceImpl.updateProfile()` silently wipes `personality`/`hostingStyle` on any partial update

- **Module:** MCProfile (MC dashboard settings)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi viết unit test JUnit thật (`MCProfileServiceImplTest.java`), test tự nhiên fail khi giả định partial update giữ nguyên các field không được client gửi lên.
- **Severity:** Major (mất dữ liệu người dùng âm thầm, không có thông báo lỗi)
- **Priority:** P1
- **Môi trường:** Unit test thuần (JUnit5 + Mockito, không cần DB thật).

### Root Cause

`MCProfile.java`:
```java
private String personality = "";
private String hostingStyle = "";
```
Hai field này có default value là chuỗi rỗng `""`, KHÔNG phải `null`.

`MCProfileServiceImpl.updateProfile()`:
```java
if (profileData.getPersonality() != null) {
    existing.setPersonality(profileData.getPersonality());
}
if (profileData.getHostingStyle() != null) {
    existing.setHostingStyle(profileData.getHostingStyle());
}
```
Guard chỉ kiểm tra `!= null` — nhưng vì field default là `""` chứ không phải `null`, bất kỳ request nào dựng `MCProfile` payload theo kiểu `new MCProfile()` rồi chỉ set 1-2 field (ví dụ chỉ đổi `biography`) đều khiến `personality`/`hostingStyle` mang giá trị `""` (không phải `null`) và **ghi đè vô điều kiện** lên dữ liệu đã lưu trước đó — dù người dùng chưa hề động tới 2 field này.

So sánh với các field khác trong cùng method (`languages`, `styles`) đã được bảo vệ đúng bằng cả `!= null && !isEmpty()`:
```java
if (profileData.getLanguages() != null && !profileData.getLanguages().isEmpty()) {
    existing.setLanguages(profileData.getLanguages());
}
```
→ Đây chính là pattern đúng cần áp dụng cho `personality`/`hostingStyle` (`isBlank()` thay vì chỉ `!= null`), nhưng 2 field String lại thiếu check này — không nhất quán trong cùng 1 method.

### Impact nghiệp vụ

Bất kỳ tính năng frontend nào gọi API cập nhật profile MC theo kiểu "chỉ gửi field vừa sửa" (partial update — pattern rất phổ biến cho form single-field save) sẽ vô tình xóa sạch `personality` và `hostingStyle` của MC nếu request DTO map thành `new MCProfile()` rồi chỉ set field đang sửa. Người dùng không nhận được lỗi nào — dữ liệu biến mất âm thầm ở lần save tiếp theo.

### Evidence

Unit test `MCProfileServiceImplTest.UpdateProfile.partialUpdateSilentlyWipesPersonalityAndHostingStyle` xác nhận: profile có sẵn `personality="Cheerful"`, `hostingStyle="Formal"`; sau khi gọi `updateProfile()` chỉ với payload đổi `biography`, kết quả `personality`/`hostingStyle` bị xóa thành `""`. Test PASS nghĩa là xác nhận đúng hành vi lỗi thật (không phải bug được fix), lưu tại `src/test/java/com/mchub/services/impl/MCProfileServiceImplTest.java`.

### Status

**Open.** Đề xuất dev sửa 1 trong 2 hướng (không phải QA quyết định):
1. Đổi guard thành `!= null && !isBlank()` cho cả `personality` và `hostingStyle`, đồng nhất với pattern đã dùng cho `languages`/`styles`.
2. Nếu cố tình muốn cho phép "xóa về rỗng", cần đổi kiểu request DTO ở tầng Controller sang `Optional<String>` hoặc dùng field-presence map (ví dụ `Map<String,Object>` như `AuthServiceImpl.updateSettings()` đã làm) để phân biệt được "client không gửi field" và "client gửi rỗng có chủ đích".
