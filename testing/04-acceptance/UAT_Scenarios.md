# Kịch bản Kiểm thử Chấp nhận (UAT) — MC Voice Training

**Người viết:** QA Tester (độc lập)
**Ngày tạo:** 2026-07-18
**Đối tượng đọc:** Product Owner / người quyết định go-live — tài liệu này KHÔNG dùng thuật ngữ kỹ thuật (không HTTP method, không mã trạng thái, không curl).
**Nguồn đối chiếu:** `docs/use-cases/UC-01..UC-10*.md`, `testing/03-system/*.md`, `testing/defect-log/DEFECT-001..024.md`, `testing/traceability/Traceability_Matrix.md`.

**Cách đọc "Trạng thái xác nhận":**
- **PASS** — luồng chạy trơn tru đúng như một người dùng thật kỳ vọng.
- **PASS với lưu ý** — luồng hoàn thành được nhưng có điểm gợn (trải nghiệm không hoàn hảo, dữ liệu hiển thị sai một phần).
- **FAIL** — luồng bị gãy ở một bước, người dùng thật không thể đi tiếp hoặc bị lừa bởi dữ liệu sai.

---

## A. Khách vãng lai (Guest — chưa đăng nhập)

### Kịch bản KH-01: Khách mới khám phá sản phẩm và dùng thử trước khi đăng ký

Một người chưa từng biết tới sản phẩm vào trang chủ, xem thử vài MC nổi bật, thử phân tích giọng nói miễn phí để đánh giá chất lượng AI trước khi quyết định có bỏ công đăng ký hay không — đây là hành vi khách hàng tiềm năng thật sự phổ biến nhất với sản phẩm dạng "thử trước khi mua".

**Bước:**
1. Khách vào trang chủ, xem thống kê luyện tập nổi bật của cộng đồng (số lượt luyện tập, số MC đang hoạt động...).
2. Khách xem danh sách MC nổi bật/công khai để đánh giá "sản phẩm này có MC giỏi thật không".
3. Khách chọn một bài luyện mẫu, dùng tính năng "luyện tập thử cho khách" (không cần tài khoản) để nghe AI chấm điểm giọng đọc của mình.
4. Khách muốn thử lại lần 2 để so sánh — hệ thống áp cơ chế thời gian chờ (cooldown) để tránh lạm dụng, khách xem còn bao lâu được thử lại.
5. Khách hài lòng, quyết định bấm "Đăng ký" để có tài khoản đầy đủ.

**Kết quả mong đợi:** Toàn bộ luồng khám phá diễn ra mượt mà không cần đăng nhập ở bất kỳ bước nào cho tới khi khách chủ động chọn đăng ký.

**Trạng thái xác nhận:** **FAIL** — luồng dùng thử bị gãy ở bước 3. Khi khách vãng lai gọi tính năng "luyện tập thử cho khách", hệ thống báo lỗi hệ thống chung chung thay vì cho nghe kết quả chấm điểm (**DEFECT-008, Major** — thiếu một tham số bắt buộc trong luồng guest khiến hệ thống trả lỗi 500). Bước 4 (xem cooldown) cũng gãy tương tự do endpoint chưa được mở công khai cho khách chưa đăng nhập (**DEFECT-010, Major**). Về mặt trải nghiệm thật: một khách hàng tiềm năng vào thử tính năng "dùng thử miễn phí" — vốn là mồi câu chuyển đổi quan trọng nhất với người chưa có tài khoản — sẽ gặp lỗi ngay từ lần thử đầu tiên và nhiều khả năng rời trang mà không đăng ký.

---

### Kịch bản KH-02: Khách xem bảng giá và mã khuyến mãi trước khi cân nhắc đăng ký

Khách muốn biết chi phí trước khi bỏ thời gian tạo tài khoản — hành vi rất phổ biến, nhiều khách sẽ rời trang ngay nếu không xem được giá.

**Bước:**
1. Khách vào trang bảng giá, xem danh sách gói cước (giá, thời hạn, quyền lợi).
2. Khách xem có ưu đãi giới hạn thời gian (flash-deal) đang chạy hay không.
3. Khách thử nhập một mã giảm giá để xem giá sau khi áp mã, trước khi quyết định đăng ký để thanh toán.

**Kết quả mong đợi:** Khách xem được đầy đủ bảng giá, ưu đãi và preview giá sau giảm mà không cần đăng nhập — đúng như mọi trang bán hàng online tiêu chuẩn.

**Trạng thái xác nhận:** **FAIL** — cả 3 bước đều bị chặn. Khách vãng lai gọi xem bảng giá, xem flash-deal, hoặc áp mã giảm giá đều nhận lỗi truy cập bị từ chối, dù đội phát triển đã chủ đích thiết kế 3 tính năng này là công khai (có ghi chú ngay trong code) (**DEFECT-001, Critical**). Đây là lỗi có tác động kinh doanh trực tiếp và rõ ràng nhất trong toàn bộ 24 defect: trang định giá — nơi quyết định khách có đăng ký hay không — hiển thị trống hoặc lỗi cho đúng nhóm người mà nó được thiết kế để phục vụ (khách chưa có tài khoản). Ưu đãi giới hạn thời gian dùng để thu hút người mới hoàn toàn mất tác dụng marketing vì không ai xem được nếu chưa đăng nhập.

---

### Kịch bản KH-03: Khách tìm MC theo nhu cầu sự kiện trước khi liên hệ

Khách muốn tìm một MC phù hợp phong cách/thể loại sự kiện của mình trước khi quyết định dùng nền tảng.

**Bước:**
1. Khách vào trang khám phá MC công khai, lọc theo thể loại/phong cách (category).
2. Khách xem chi tiết hồ sơ một MC (kinh nghiệm, chứng chỉ, đánh giá).
3. Khách xem bảng xếp hạng cộng đồng để biết ai đang là MC nổi bật.

**Kết quả mong đợi:** Bộ lọc trả đúng kết quả theo thể loại chọn; hồ sơ MC hiển thị thông tin phù hợp cho mục đích "khách tham khảo" (không lộ thông tin riêng tư như email cá nhân).

**Trạng thái xác nhận:** **PASS với lưu ý** — khách xem được danh sách MC và bảng xếp hạng bình thường, nhưng bộ lọc theo thể loại không có tác dụng, trả về toàn bộ danh sách bất kể chọn gì (**DEFECT-020, Major**) — khách tìm MC "MC đám cưới" vẫn thấy lẫn MC "MC hội nghị", gây khó chịu khi danh sách dài. Ngoài ra hồ sơ công khai đang lộ email cá nhân của MC ra ngoài — MC không mong muốn khách vãng lai (kể cả không có ý định thuê thật) lấy được email liên hệ trực tiếp ngoài kênh chính thức của nền tảng, và trường "verified" hiển thị có thể gây hiểu lầm về tình trạng xác minh (**DEFECT-021, Major**).

---

## B. Client (người dùng luyện giọng)

### Kịch bản CL-01: Đăng ký, xác minh, luyện tập buổi đầu tiên và nhận thưởng người mới

Đây là luồng "ngày đầu tiên" của mọi user mới — nếu gãy ở đây thì mất khách ngay từ vòng đầu.

**Bước:**
1. Client đăng ký tài khoản bằng email.
2. Client nhận và nhập mã OTP xác minh email.
3. Client đăng nhập vào hệ thống.
4. Client xem tiến trình quest onboarding (nhiệm vụ dành cho người mới).
5. Client hoàn thành bài luyện đầu tiên → quest tự động ghi nhận tiến trình.
6. Client hoàn thành đủ quest, nhận voucher ưu đãi người mới.

**Kết quả mong đợi:** Toàn bộ chuỗi 6 bước chạy liền mạch, không có bước nào yêu cầu can thiệp thủ công.

**Trạng thái xác nhận:** **PASS** — đây là luồng nghiệp vụ duy nhất trong toàn bộ 10 module đạt PASS tuyệt đối, 0 defect trên toàn bộ chuỗi đăng ký → xác minh → đăng nhập → onboarding quest → nhận voucher. Trải nghiệm "ngày đầu tiên" của người dùng mới hoạt động đúng như thiết kế.

---

### Kịch bản CL-02: Client đổi mật khẩu (quên mật khẩu) rồi đăng nhập lại ngay sau đó

Luồng "quên mật khẩu" là một trong những luồng có tần suất sử dụng cao và nhạy cảm nhất — user bị khoá ngoài tài khoản của chính mình là trải nghiệm tệ nhất có thể xảy ra.

**Bước:**
1. Client quên mật khẩu, bấm "Quên mật khẩu", nhận email/OTP đặt lại.
2. Client đặt mật khẩu mới thành công.
3. Client đăng nhập ngay bằng mật khẩu mới.
4. Client thao tác bình thường trong ứng dụng (xem hồ sơ, luyện tập...).

**Kết quả mong đợi:** Client đăng nhập được ngay sau khi đổi mật khẩu và sử dụng ứng dụng bình thường.

**Trạng thái xác nhận:** **FAIL — nghiêm trọng nhất trong toàn bộ đợt kiểm thử.** Do lỗi lệch múi giờ giữa thời điểm lưu "đã đổi mật khẩu" và thời điểm phát hành phiên đăng nhập mới (**DEFECT-016, Critical/P0**), Client đổi mật khẩu xong đăng nhập lại **KHÔNG dùng được tài khoản trong nhiều giờ liền** — mọi thao tác cần đăng nhập đều báo lỗi hệ thống. Vì máy chủ đặt tại Việt Nam chạy 24/7, lỗi này lặp lại theo chu kỳ mỗi ngày (rơi vào khung giờ đổi mật khẩu 07:00–14:00 giờ Việt Nam thì bị khoá hoàn toàn cho tới tối). Đây chính xác là luồng "đổi mật khẩu xong không dùng được tài khoản" — kịch bản người dùng thật gặp phải sẽ nghĩ tài khoản bị hack/mất, gây mất niềm tin nghiêm trọng và có thể tạo làn sóng khiếu nại/rời bỏ. Ngoài ra, khi lỗi này xảy ra, việc xem thông tin tài khoản hiện tại (`/auth/me`) cũng trả lỗi hệ thống chung chung thay vì thông báo rõ ràng "phiên đăng nhập không hợp lệ" (**DEFECT-015, Major**), khiến Client càng khó tự hiểu vấn đề và khó biết cách khắc phục (đăng nhập lại sau).

---

### Kịch bản CL-03: Client nâng cấp gói cước và thanh toán

Đây là luồng tạo doanh thu trực tiếp — độ tin cậy ở đây ảnh hưởng thẳng tới tiền thật.

**Bước:**
1. Client đã đăng nhập, xem bảng giá các gói cước.
2. Client áp mã giảm giá.
3. Client tạo đơn thanh toán cho gói đã chọn.
4. Client hoàn tất thanh toán qua cổng thanh toán (PayOS), hệ thống tự xác nhận qua webhook.
5. Client xem trạng thái thanh toán, gói cước được kích hoạt.

**Kết quả mong đợi:** Toàn bộ luồng thanh toán diễn ra chính xác, gói cước được ghi nhận đúng, không có sai lệch số tiền hay quyền lợi.

**Trạng thái xác nhận:** **PASS với lưu ý** — với Client đã đăng nhập, toàn bộ luồng xem giá → áp mã → tạo đơn → thanh toán → xác nhận đều chạy đúng (lỗi DEFECT-001 chỉ ảnh hưởng Guest, không ảnh hưởng Client đã đăng nhập). Tuy nhiên có một lưu ý rủi ro tài chính: nếu Client thao tác nâng cấp/hạ cấp gói cước gần như đồng thời (ví dụ mở 2 tab, hoặc bấm nút 2 lần do mạng chậm), có khả năng xảy ra tình trạng ghi đè gói cước không đúng thứ tự (race condition) khiến gói cuối cùng được lưu không phải gói Client thực sự muốn (**DEFECT-002, Major**) — cần lưu ý đây là rủi ro tài chính dù xác suất xảy ra trong thao tác thông thường không cao. Riêng luồng mua khoá học lẻ có giá 0đ (khoá học miễn phí/khuyến mãi) bị tính sai giá — trả về 500đ thay vì 0đ (**DEFECT-006, Major**) — ảnh hưởng trực tiếp tới các chiến dịch tặng khoá học miễn phí.

---

### Kịch bản CL-04: Client học một khoá học trọn vẹn từ đầu đến khi nhận chứng chỉ

Đây là luồng giá trị cốt lõi của sản phẩm — lý do khách hàng trả tiền.

**Bước:**
1. Client xem lộ trình học và danh sách khoá học.
2. Client đăng ký một khoá học (đã mua ở kịch bản CL-03, hoặc nhận khoá học được tặng).
3. Client đọc bài lý thuyết trong khoá học.
4. Client hoàn thành các bài luyện trong khoá học.
5. Client nộp bài quiz cuối khoá.
6. Client xem chứng chỉ đã đạt được.

**Kết quả mong đợi:** Toàn bộ hành trình học tập hoàn thành mượt mà, chứng chỉ được cấp đúng sau khi đạt yêu cầu.

**Trạng thái xác nhận:** **FAIL với lưu ý** — hầu hết luồng chạy đúng (đăng ký khoá học, hoàn thành bài luyện, nộp quiz, nhận chứng chỉ đều PASS), NGOẠI TRỪ bước đọc bài lý thuyết: Client gọi xem bài đọc lý thuyết bị từ chối truy cập do thiếu cấu hình công khai đúng (**DEFECT-013, Major**) — một Client đã trả tiền cho khoá học vẫn không đọc được nội dung lý thuyết, đây là một phần giá trị cốt lõi mà khách đã trả tiền để nhận. Ghi chú thêm: tính năng ghi chú/highlight bài đọc phụ thuộc dữ liệu bài đọc phải tồn tại sẵn (hiện chưa có công cụ cho Admin tự tạo bài đọc, phải chèn thủ công) — đây là khoảng trống chức năng cần Product Owner xác nhận có nằm trong phạm vi release hiện tại hay không.

---

### Kịch bản CL-05: Client luyện tập thường xuyên và theo dõi tiến bộ qua thời gian

Luồng "sử dụng lặp lại" quyết định khả năng giữ chân người dùng (retention).

**Bước:**
1. Client luyện tập hàng ngày, xem chuỗi ngày đăng nhập liên tiếp (streak).
2. Client tạm dừng vài ngày, muốn "đóng băng" streak để không bị mất chuỗi.
3. Client phân tích giọng nói AI cho một bài luyện, xem điểm số.
4. Client xem lại lịch sử luyện tập và thống kê độ khó thích ứng theo thời gian.
5. Client tham gia bảng xếp hạng cộng đồng, xem thứ hạng của bản thân.

**Kết quả mong đợi:** Client theo dõi được tiến bộ cá nhân liên tục, tính năng đóng băng streak bảo vệ đúng thành tích khi tạm nghỉ.

**Trạng thái xác nhận:** **FAIL với lưu ý** — luyện tập, phân tích giọng nói, xem lịch sử và thống kê độ khó đều PASS tốt. Tuy nhiên tính năng "đóng băng streak" không có tác dụng thực tế dù API trả về thành công — streak vẫn bị mất bình thường (**DEFECT-017, Minor**) — Client tưởng đã bảo vệ được chuỗi ngày luyện tập của mình nhưng thực chất không được bảo vệ, phát hiện ra sẽ gây thất vọng dù không ảnh hưởng chức năng luyện tập chính. Xem thứ hạng bản thân cũng gặp lỗi hệ thống khi phiên đăng nhập có vấn đề (liên quan cùng nguyên nhân kỹ thuật với DEFECT-015).

---

## C. MC (người dùng đã xác minh vai trò MC)

### Kịch bản MC-01: MC thiết lập hồ sơ và được khách hàng khám phá công khai

Đây là luồng "lên sóng" của một MC mới — quyết định họ có được khách tìm thấy hay không.

**Bước:**
1. MC cập nhật hồ sơ cá nhân (tiểu sử, phong cách dẫn, kinh nghiệm).
2. MC thêm chứng chỉ hành nghề để tăng độ tin cậy.
3. MC chờ Admin duyệt chứng chỉ.
4. Khách vãng lai/Client tìm thấy hồ sơ MC qua trang khám phá công khai, xem chi tiết.

**Kết quả mong đợi:** Hồ sơ MC được cập nhật đầy đủ, chứng chỉ được duyệt đúng quy trình, hồ sơ hiển thị công khai chính xác cho người tìm kiếm.

**Trạng thái xác nhận:** **FAIL** — cập nhật hồ sơ cơ bản hoạt động (dù có lưu ý kỹ thuật: cập nhật một phần thông tin có thể âm thầm xoá mất trường "phong cách dẫn"/"tính cách" nếu Client/MC chỉ gửi một phần dữ liệu thay vì đầy đủ form — **DEFECT-005, Major**, MC có thể mất nội dung đã nhập trước đó mà không có cảnh báo). Nghiêm trọng hơn: toàn bộ luồng thêm chứng chỉ, Admin duyệt chứng chỉ, và xoá chứng chỉ đều báo lỗi hệ thống (route đã lỗi thời/không còn hoạt động — **DEFECT-019, Major**) — MC hoàn toàn không thể thêm chứng chỉ mới để tăng uy tín hồ sơ. Đây là điểm nghẽn trực tiếp cho việc MC mới gia nhập nền tảng và xây dựng độ tin cậy trước khách hàng.

---

### Kịch bản MC-02: MC theo dõi hiệu quả hoạt động qua dashboard cá nhân

**Bước:**
1. MC đăng nhập, vào dashboard MC xem thống kê (số lượt luyện tập, điểm trung bình, tốc độ nói...).
2. MC theo dõi thống kê này định kỳ để cải thiện kỹ năng.

**Kết quả mong đợi:** Chỉ MC mới xem được dashboard dành riêng cho vai trò MC; dữ liệu hiển thị đúng và có ý nghĩa.

**Trạng thái xác nhận:** **PASS với lưu ý — vấn đề phân quyền, không phải MC gặp phải trực tiếp.** MC xem dashboard của chính mình bình thường. Tuy nhiên phát hiện: bất kỳ Client nào (không có vai trò MC) cũng gọi được thẳng vào dashboard này (**DEFECT-018, Major**) — do thiếu điều kiện kiểm tra vai trò MC. Không rò rỉ dữ liệu của MC khác (Client chỉ thấy dữ liệu rỗng của chính họ), nhưng đây là lỗi vi phạm ranh giới vai trò rõ ràng: một tính năng được quảng bá/thiết kế "dành riêng cho MC" lại truy cập được bởi Client thường — gây nhầm lẫn trải nghiệm và là rủi ro tiềm ẩn nếu sau này dashboard được mở rộng thêm dữ liệu nhạy cảm hơn (thu nhập, hợp đồng...) mà vẫn dùng cùng luật phân quyền sai này.

---

### Kịch bản MC-03: MC bị báo cáo và cần xem kết quả xử lý từ Admin

**Bước:**
1. Một Client gửi báo cáo vi phạm liên quan tới MC.
2. Admin xem danh sách báo cáo, xử lý báo cáo (chấp nhận/từ chối/xử phạt).
3. MC (hoặc Client báo cáo) xem lại trạng thái báo cáo của mình.

**Kết quả mong đợi:** Quy trình báo cáo — xử lý — phản hồi khép kín, minh bạch, không gây tranh cãi về việc xử lý ra sao.

**Trạng thái xác nhận:** **PASS với lưu ý** — gửi báo cáo, Admin xem danh sách và xử lý báo cáo hợp lệ đều chạy đúng. Có 2 lưu ý: gửi báo cáo với loại vi phạm không hợp lệ trả về lỗi hệ thống khó hiểu thay vì thông báo rõ ràng "loại báo cáo không hợp lệ" (**DEFECT-023, Major**, ảnh hưởng toàn hệ thống vì đây là nguyên nhân gốc lặp lại ở nhiều module khác); và Admin xử lý một báo cáo không tồn tại (ví dụ do link cũ/đã bị xoá) nhận lỗi hệ thống thay vì thông báo "không tìm thấy báo cáo" (**DEFECT-024, Minor**). Cả hai không chặn luồng chính nhưng làm thông báo lỗi gây hiểu lầm cho người vận hành.

---

## D. Admin

### Kịch bản AD-01: Admin quản lý người dùng và xử lý sự cố tài khoản hàng ngày

Đây là công việc vận hành thường nhật của Admin — độ tin cậy quyết định Admin có xử lý được yêu cầu hỗ trợ khách hàng kịp thời hay không.

**Bước:**
1. Admin xem tổng quan dashboard (số user, doanh thu, hoạt động...).
2. Admin xem danh sách người dùng, tìm một tài khoản cụ thể theo yêu cầu hỗ trợ.
3. Admin đổi trạng thái tài khoản (khoá/mở khoá) khi cần xử lý vi phạm.
4. Admin gửi email đặt lại mật khẩu hộ user không tự làm được.
5. Admin đổi gói cước thủ công cho user (xử lý khiếu nại thanh toán).
6. Admin xem audit log để tra cứu lịch sử thao tác khi cần đối chiếu.

**Kết quả mong đợi:** Toàn bộ công cụ vận hành hoạt động chính xác, Admin xử lý được yêu cầu hỗ trợ khách hàng mà không cần can thiệp kỹ thuật.

**Trạng thái xác nhận:** **PASS** — toàn bộ chuỗi thao tác quản trị người dùng cốt lõi (xem dashboard, danh sách user, đổi trạng thái, gửi email reset, đổi gói cước thủ công, audit log) đều hoạt động đúng, không phát hiện defect nào chặn luồng này. Đây là một trong các module vận hành ổn định nhất của hệ thống.

**Lưu ý liên đới quan trọng:** Dù công cụ "gửi email đặt lại mật khẩu hộ user" (bước 4) tự nó hoạt động đúng, kết quả cuối cùng mà user nhận được vẫn bị chặn bởi **DEFECT-016** (xem kịch bản CL-02) — Admin gửi email đặt lại mật khẩu thành công, nhưng user thực hiện xong vẫn không đăng nhập lại được trong nhiều giờ. Admin sẽ nhận được khiếu nại lặp lại "tôi đã đổi mật khẩu theo hướng dẫn nhưng vẫn không vào được" — tạo thêm tải công việc hỗ trợ không đáng có cho tới khi lỗi gốc được fix.

---

### Kịch bản AD-02: Admin xử lý báo cáo vi phạm và ra thông báo toàn hệ thống

Luồng kết hợp kiểm duyệt nội dung và truyền thông — quan trọng khi có sự cố cần thông báo gấp tới toàn bộ user.

**Bước:**
1. Admin xem danh sách báo cáo vi phạm mới.
2. Admin xử lý từng báo cáo (chấp nhận/từ chối, có thể khoá tài khoản vi phạm).
3. Admin soạn một thông báo mới (ví dụ: thông báo bảo trì, hoặc cảnh báo cộng đồng).
4. Admin xem trước nội dung email của thông báo trước khi gửi.
5. Admin duyệt và gửi thông báo tới toàn bộ user hoặc nhóm user theo gói cước.

**Kết quả mong đợi:** Chu trình kiểm duyệt — thông báo khép kín, thông báo khẩn được gửi đi đáng tin cậy tới đúng đối tượng.

**Trạng thái xác nhận:** **FAIL với lưu ý** — xem/xử lý báo cáo, soạn thông báo, xem trước email, lọc người nhận theo gói cước đều PASS tốt. Điểm gãy: khi Admin **gửi lại** một thông báo (ví dụ lần gửi đầu bị lỗi một phần, cần gửi bù), hệ thống có cơ chế xử lý nền (chạy ngầm) nuốt mất lỗi phát sinh mà không báo cho Admin biết việc gửi lại có thành công hay không (**DEFECT-014, Major**) — Admin không có cách nào chắc chắn thông báo khẩn (ví dụ cảnh báo bảo mật, thông báo sự cố thanh toán) đã thực sự tới tay toàn bộ user hay chưa, đặc biệt rủi ro nếu đây là thông báo có tính chất khẩn cấp cần đảm bảo độ phủ.

---

### Kịch bản AD-03: Admin thực hiện bảo trì hệ thống (migration) và tra cứu log khi có sự cố

**Bước:**
1. Admin chạy migration cơ sở dữ liệu khi cần cập nhật cấu trúc dữ liệu cho tính năng mới.
2. Có sự cố xảy ra, Admin tra cứu log hệ thống theo bộ lọc để xác định nguyên nhân.

**Kết quả mong đợi:** Migration chạy an toàn, không có rủi ro nhầm lẫn môi trường; log tra cứu đúng và đủ số lượng cần xem.

**Trạng thái xác nhận:** **FAIL** — công cụ chạy migration đang có cấu hình gắn cứng tên cơ sở dữ liệu trong code thay vì đọc từ cấu hình môi trường hiện tại (**DEFECT-003, Critical**) — rủi ro thực tế: nếu Admin thao tác trên môi trường thật (production) mà công cụ vẫn ngầm trỏ vào một database khác (ví dụ database test) do tên bị gắn cứng, dữ liệu production sẽ không được cập nhật đúng như Admin tưởng, hoặc ngược lại có nguy cơ chạy nhầm migration vào sai môi trường — đây là loại lỗi cấu hình có thể gây sự cố dữ liệu nghiêm trọng, QA đã chủ động KHÔNG thực thi thử nghiệm trực tiếp trên môi trường thật để tránh rủi ro, chỉ xác nhận qua rà soát code. Về tra cứu log: bộ lọc "giới hạn số lượng kết quả" (limit) không có tác dụng, Admin không kiểm soát được số lượng log trả về, gây khó khăn khi cần xem nhanh một số lượng nhỏ log gần nhất trong lúc xử lý sự cố khẩn cấp (**DEFECT-007, Minor**).

---

## Kết luận: Khuyến nghị Go/No-Go

**Khuyến nghị chung: NO-GO cho tới khi DEFECT-016 được fix và xác nhận qua retest.**

### 1. Defect phải chặn go-live (business-critical, người dùng thật gặp phải)

- **DEFECT-016 (Critical/P0)** — Đổi mật khẩu xong không đăng nhập lại được trong nhiều giờ. Đây là lỗi **duy nhất trong 24 defect đủ nghiêm trọng để tự nó là lý do NO-GO**: nó chặn hoàn toàn một luồng nghiệp vụ lõi (khôi phục truy cập tài khoản), xảy ra với MỌI user thực hiện đổi mật khẩu (không phải edge case hiếm), lặp lại theo chu kỳ hàng ngày do đặc thù múi giờ máy chủ, và tạo trải nghiệm tệ nhất có thể (user tưởng bị mất tài khoản/bị hack). Bắt buộc fix và retest đầy đủ (kể cả retest ở các khung giờ khác nhau trong ngày, vì lỗi phụ thuộc thời điểm) trước khi cân nhắc go-live.

### 2. Defect có tác động kinh doanh rõ ràng, nên fix trước go-live dù không tự nó là lý do chặn tuyệt đối

- **DEFECT-001 (Critical)** — Khách vãng lai không xem được bảng giá/ưu đãi. Trực tiếp làm mất khách hàng tiềm năng ở bước quan trọng nhất của phễu chuyển đổi.
- **DEFECT-008 / DEFECT-010 (Major)** — Tính năng "dùng thử miễn phí cho khách" (mồi câu chuyển đổi chính) bị lỗi ngay từ lần thử đầu.
- **DEFECT-019 (Major)** — MC không thêm được chứng chỉ hành nghề, chặn việc xây dựng uy tín hồ sơ — ảnh hưởng trực tiếp tới nguồn cung MC trên nền tảng.
- **DEFECT-013 (Major)** — Client đã trả tiền cho khoá học không đọc được bài lý thuyết đã mua.
- **DEFECT-018 (Major)** — Client truy cập được dashboard dành riêng cho MC — vi phạm ranh giới vai trò, dù chưa lộ dữ liệu nhạy cảm.
- **DEFECT-006 (Major)** — Khoá học giá 0đ (khuyến mãi/tặng) bị tính sai giá — ảnh hưởng trực tiếp các chiến dịch marketing tặng khoá học.
- **DEFECT-003 (Critical)** — Rủi ro cấu hình sai môi trường khi chạy migration — rủi ro vận hành, nên fix trước khi bất kỳ migration nào chạy trên môi trường thật.

### 3. Defect chủ yếu là kỹ thuật/backend, ít hoặc không nhìn thấy trực tiếp bởi người dùng cuối — có thể xếp sau go-live (P2/backlog) nếu áp lực thời gian

- DEFECT-002 (race condition đổi gói cước — xác suất thấp trong thao tác thường), DEFECT-004 (dead code, không dùng trong production), DEFECT-005 (mất field khi partial update hồ sơ MC — cần fix nhưng ít xảy ra nếu FE luôn gửi đủ form), DEFECT-007 (bộ lọc log không lọc theo limit — chỉ Admin thấy), DEFECT-009 (TTS demo sai URL — cần xác nhận mức độ dùng thật), DEFECT-011/012 (thống kê Admin sai — không ảnh hưởng user cuối), DEFECT-014 (gửi lại thông báo — chỉ ảnh hưởng khi Admin cần gửi bù), DEFECT-015 (thông báo lỗi sai mã — gây khó hiểu nhưng không chặn), DEFECT-017 (đóng băng streak vô tác dụng — Minor, ảnh hưởng cảm xúc không ảnh hưởng chức năng), DEFECT-020/021 (bộ lọc MC + lộ email — nên fix sớm vì liên quan quyền riêng tư nhưng không chặn luồng), DEFECT-022 (ghi nhận click bài đăng cho khách — chỉ ảnh hưởng số liệu thống kê), DEFECT-023/024 (thông báo lỗi sai mã ở luồng báo cáo — gây khó hiểu không chặn).

**Tóm tắt cho người quyết định:** Nếu chỉ được chọn một điều kiện tối thiểu để cho phép go-live, đó là DEFECT-016 phải được fix và retest xác nhận trước — vì đây là lỗi duy nhất khiến người dùng thật bị khoá ngoài chính tài khoản của họ mà không có cách tự khắc phục. Nhóm mục 2 nên được fix cùng đợt vì đều ảnh hưởng trực tiếp tới trải nghiệm chuyển đổi khách hàng mới và uy tín MC — hai yếu tố sống còn cho một nền tảng còn non trẻ. Nhóm mục 3 có thể lên lịch fix sau go-live mà không tạo rủi ro kinh doanh tức thời.
