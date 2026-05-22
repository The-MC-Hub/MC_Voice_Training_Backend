# BM25 Search cho bài học voice

Tài liệu này giải thích cách hệ thống đang tìm kiếm bài học trong backend, vì sao lại dùng Elasticsearch với BM25, và tại sao vẫn cần MongoDB fallback. Mục tiêu là để bạn đọc code không bị lẫn giữa ba lớp khác nhau:

1. MongoDB là nơi lưu dữ liệu thật.
2. Elasticsearch là nơi tối ưu tìm kiếm.
3. Service Java là nơi điều phối và đảm bảo hệ thống luôn trả kết quả.

## 1. BM25 là gì

BM25 là thuật toán xếp hạng tài liệu được dùng rất phổ biến trong các hệ thống search text. Ý tưởng của nó không phải chỉ là “có chứa từ khóa hay không”, mà là “tài liệu nào liên quan hơn thì đứng cao hơn”.

Với bài học voice, điều này quan trọng vì người dùng có thể tìm bằng nhiều kiểu khác nhau:

- tìm theo tiêu đề bài học
- tìm theo mô tả
- tìm theo nội dung bên trong lesson
- tìm theo category hoặc difficulty

Nếu chỉ dùng `contains` hoặc lọc thủ công, kết quả thường bị phẳng và không có thứ tự hợp lý. BM25 giải quyết phần xếp hạng này tốt hơn.

## 2. Kiến trúc hiện tại

Luồng search đang chia thành 3 lớp:

### MongoDB
MongoDB giữ dữ liệu lesson gốc. Đây là nguồn dữ liệu chuẩn của hệ thống. Tất cả lesson thật, đầy đủ field, đều nằm ở đây.

### Elasticsearch
Elasticsearch chỉ giữ bản sao phục vụ tìm kiếm. Bản sao này được map vào model [VoiceLessonSearchDocument.java](src/main/java/com/mchub/models/VoiceLessonSearchDocument.java).

### Service Java
Service [VoiceLessonSearchService.java](src/main/java/com/mchub/services/VoiceLessonSearchService.java) quyết định:

- lúc nào ghi index
- lúc nào xóa index
- lúc nào search qua Elasticsearch
- lúc nào rơi về fallback MongoDB

Nói ngắn gọn: MongoDB lưu dữ liệu, Elasticsearch xếp hạng, service điều phối.

## 3. Luồng search thực tế

Khi frontend gọi API `GET /api/v1/voice/lessons?search=...`, controller trong [VoiceController.java](src/main/java/com/mchub/controllers/VoiceController.java) sẽ kiểm tra:

- có `search` hay không
- có `category` hay không

Nếu có từ khóa search, controller gọi:

`voiceService.searchLessons(search, category)`

Từ đây luồng chạy đi như sau:

### Bước 1: Chuẩn hóa từ khóa
Service dùng `normalize()` để đưa search term về dạng ổn định:

- chuyển về chữ thường
- bỏ khoảng trắng đầu cuối

Mục đích là tránh sai lệch kiểu `Voice`, `voice`, ` voice `.

### Bước 2: Ưu tiên Elasticsearch
Service gọi repository search:

`searchRepository.searchByText(normalizedSearch)`

Repository này nằm trong [VoiceLessonSearchRepository.java](src/main/java/com/mchub/repositories/VoiceLessonSearchRepository.java) và dùng query `multi_match`.

### Bước 3: Elasticsearch trả về kết quả đã xếp hạng
Elasticsearch không trả về lesson đầy đủ ngay. Nó thường trả về document search theo thứ tự liên quan.

Service lấy danh sách `id` từ kết quả đó, sau đó gọi MongoDB để load lại lesson thật:

`lessonRepository.findAllById(lessonIds)`

### Bước 4: Giữ nguyên thứ tự BM25
Đây là chỗ rất quan trọng.

MongoDB `findAllById` không đảm bảo thứ tự giống như search result. Vì vậy service phải map lại theo đúng thứ tự `lessonIds` từ Elasticsearch.

Nhờ vậy:

- Elasticsearch quyết định bài nào liên quan hơn
- MongoDB chỉ cung cấp dữ liệu đầy đủ để trả về cho frontend

### Bước 5: Trả về frontend
Kết quả cuối cùng được trả về dưới dạng DTO, không phải document search.

## 4. Vì sao repository dùng `multi_match`

Trong [VoiceLessonSearchRepository.java](src/main/java/com/mchub/repositories/VoiceLessonSearchRepository.java), query đang tìm trên nhiều field:

- `title^4`
- `description^3`
- `content^2`
- `difficulty`
- `category`

Chữ `^4`, `^3`, `^2` là weight boost. Nó nói với search engine rằng:

- match ở title quan trọng nhất
- match ở description cũng rất quan trọng
- match ở content thì vẫn có giá trị nhưng thấp hơn

Điều này hợp lý vì người dùng thường nhập từ khóa ngắn, và nếu lesson nào có từ đó ở title thì khả năng cao là liên quan nhất.

## 5. VoiceLessonSearchDocument dùng để làm gì

File [VoiceLessonSearchDocument.java](src/main/java/com/mchub/models/VoiceLessonSearchDocument.java) định nghĩa dữ liệu đưa vào Elasticsearch.

Nó có các field chính:

- `id`
- `title`
- `description`
- `content`
- `category`
- `difficulty`
- `createdAt`
- `updatedAt`

Ý nghĩa từng field:

- `title`, `description`, `content`: là nội dung được search
- `category`, `difficulty`: giúp lọc hoặc mở rộng truy vấn
- `id`: để quay lại MongoDB lấy dữ liệu thật
- `createdAt`, `updatedAt`: hỗ trợ sắp xếp hoặc debug về sau

`@Document(indexName = "voice_lessons")` cho biết đây là index tên `voice_lessons` trong Elasticsearch.

`@Setting(settingPath = "/elasticsearch/settings.json")` cho biết index này dùng settings riêng, thường để cấu hình analyzer hoặc tokenizer.

## 6. Vì sao cần reindex

Chỉ ghi index khi lesson thay đổi là chưa đủ. Có nhiều trường hợp phải rebuild lại toàn bộ search index:

- thay đổi analyzer
- sửa cấu trúc document
- mất dữ liệu index
- mới thêm dữ liệu seed

Vì vậy service có `clearIndex()` và `reindexLessons(...)`.

Luồng của reindex là:

1. xóa toàn bộ dữ liệu search cũ
2. đọc danh sách lesson từ MongoDB
3. index lại từng lesson một

Điều này giúp Elasticsearch và MongoDB đồng bộ lại từ đầu.

## 7. Tại sao vẫn cần fallback MongoDB

Không nên phụ thuộc hoàn toàn vào Elasticsearch. Vì search engine có thể gặp các vấn đề sau:

- ES chưa chạy
- sai username/password
- index chưa tồn tại
- network lỗi
- mapping lỗi

Nếu search chỉ dựa vào Elasticsearch, chỉ cần ES lỗi là toàn bộ search bài học hỏng.

Cho nên service có fallback. Khi Elasticsearch fail, code chuyển sang `fallbackSearch(...)`.

Fallback này:

- lấy lesson từ MongoDB
- tách từ khóa thành tokens
- chấm điểm đơn giản theo field
- sort lại theo score

Nó không thông minh bằng BM25 nhưng đủ để giao diện vẫn hoạt động.

## 8. Cách fallback chấm điểm

Trong fallback, code cho điểm theo mức độ xuất hiện của từ khóa:

- title: +6
- description: +3
- content: +1
- category: +2
- difficulty: +1.5

Đây là một heuristic đơn giản, không phải BM25 thật.

Mục đích của nó là:

- giữ search hoạt động khi ES lỗi
- không để user thấy trang trắng hoặc kết quả rỗng chỉ vì search engine chết
- vẫn ưu tiên bài học có liên quan cao hơn

## 9. Vì sao có cả `indexLesson` và `deleteLesson`

Khi lesson được tạo hoặc sửa, service gọi `indexLesson(lesson)` để đẩy dữ liệu sang Elasticsearch.

Khi lesson bị xóa, service gọi `deleteLesson(id)` để gỡ document khỏi index.

Hai hàm này giúp search index không bị lệch với dữ liệu thật.

Nếu không làm vậy, sẽ xuất hiện các lỗi kiểu:

- lesson đã xóa nhưng vẫn search ra
- lesson mới cập nhật nhưng search vẫn trả bản cũ

## 10. Ý nghĩa của các comment trong code

Các comment đã được thêm vào [VoiceLessonSearchService.java](src/main/java/com/mchub/services/VoiceLessonSearchService.java) không phải để “trang trí”. Chúng giúp người đọc hiểu nhanh:

- đoạn nào đang index
- đoạn nào đang xóa index
- đoạn nào ưu tiên Elasticsearch
- đoạn nào là fallback
- đoạn nào tạo document search

Nói cách khác, comment chỉ ra ranh giới giữa logic search thật và logic dự phòng.

## 11. Tóm tắt ngắn

Nếu đọc cực ngắn, thì hệ thống đang làm như sau:

1. Lesson được lưu ở MongoDB.
2. Một bản sao search được đẩy sang Elasticsearch.
3. Khi user search, backend hỏi Elasticsearch trước.
4. Elasticsearch trả kết quả đã xếp hạng bằng BM25.
5. Backend lấy dữ liệu thật từ MongoDB rồi trả về frontend.
6. Nếu Elasticsearch lỗi, backend rơi về MongoDB search cục bộ để không mất tính năng.

## 12. File liên quan

- [VoiceLessonSearchService.java](src/main/java/com/mchub/services/VoiceLessonSearchService.java)
- [VoiceLessonSearchRepository.java](src/main/java/com/mchub/repositories/VoiceLessonSearchRepository.java)
- [VoiceLessonSearchDocument.java](src/main/java/com/mchub/models/VoiceLessonSearchDocument.java)
- [VoiceController.java](src/main/java/com/mchub/controllers/VoiceController.java)
- [VoiceService.java](src/main/java/com/mchub/services/VoiceService.java)
- [VoiceServiceImpl.java](src/main/java/com/mchub/services/impl/VoiceServiceImpl.java)
