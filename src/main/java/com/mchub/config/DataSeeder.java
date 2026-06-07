package com.mchub.config;

import com.mchub.enums.CompetitionType;
import com.mchub.enums.CourseType;
import com.mchub.enums.LearningPathType;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.*;
import com.mchub.repositories.*;
import com.mchub.services.VoiceLessonSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final VoiceLessonRepository lessonRepository;
    private final ReadingGuideRepository readingGuideRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionRecordRepository competitionRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final VoiceLessonSearchService lessonSearchService;
    private final PlanDefinitionRepository planDefinitionRepository;

    @Override
    public void run(String... args) {
        seedPlanDefinitions();
        // DataSeeder disabled — data imported via import_data.py from MCHub_DataEntry_CLEAN.xlsx
        if (true) return;
        log.info("🧹 Cleaning existing lesson and competition data for fresh seed...");
        lessonRepository.deleteAll();
        lessonSearchService.clearIndex();
        readingGuideRepository.deleteAll();
        userStatsRepository.deleteAll();
        competitionRepository.deleteAll();
        competitionRecordRepository.deleteAll();

        log.info("🌱 Seeding 20 standalone voice lessons...");

        List<VoiceLesson> standaloneLesson = new ArrayList<>();

        // ── GALA DINNER (5 bài) ─────────────────────────────────────────
        standaloneLesson.addAll(List.of(
                VoiceLesson.builder().title("Khai Mạc Gala Dinner Cuối Năm")
                        .category(VoiceLessonCategory.GALA).difficulty("Medium")
                        .description(
                                "Luyện tập lời khai mạc Gala Dinner trang trọng, tạo không khí ấm áp và hứng khởi ngay từ đầu chương trình.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Kính thưa quý vị đại biểu, quý khách, cùng toàn thể gia đình công ty thân mến!\n\nChào mừng quý vị đến với Gala Dinner thường niên – đêm hội tụ của những thành tích vượt trội và những kết nối đáng nhớ. Năm qua là một hành trình đầy thử thách, nhưng cũng tràn ngập những chiến thắng mà chúng ta đã cùng nhau tạo dựng.\n\nTối nay, chúng ta không chỉ kỷ niệm những con số. Chúng ta tôn vinh tinh thần, sự cống hiến và tình đồng đội đã làm nên điều đó. Xin hãy để lại những bộn bề của năm cũ bên ngoài cánh cửa này, và bước vào một đêm thực sự thuộc về chúng ta.\n\nChương trình tối nay hứa hẹn nhiều bất ngờ, xúc cảm và kỷ niệm không thể nào quên. Một lần nữa, xin kính chào và chúc quý vị một buổi tối thật tuyệt vời!")
                        .evaluationHint(
                                "Giọng điệu: trang trọng nhưng ấm áp, nhịp độ vừa phải, nhấn mạnh vào các từ khóa cảm xúc.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(25).description(
                                        "Phát âm chuẩn, rõ từng âm tiết trong các từ trang trọng như 'kính thưa', 'trân trọng'")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(25).description(
                                        "Nhịp điệu đều đặn, trang trọng, không gấp gáp; nhấn trọng âm đúng từ khóa")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description(
                                                "Tốc độ 110–130 wpm; ngắt nghỉ tự nhiên sau các mệnh đề quan trọng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(20).description(
                                        "Truyền cảm hứng hứng khởi và ấm áp; nhấn mạnh cảm xúc ở những từ khóa chủ chốt")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(10)
                                        .description("Đọc đúng nội dung, không bỏ sót hay thêm từ").build()))
                        .targetWpmMin(110).targetWpmMax(130).passingScore(70)
                        .build(),
                VoiceLesson.builder().title("Dẫn Phần Cocktail Reception")
                        .category(VoiceLessonCategory.GALA).difficulty("Easy")
                        .description(
                                "Kỹ năng dẫn dắt phần cocktail reception, tạo không khí giao lưu thoải mái trước Gala chính thức.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1511578314322-379afb476865?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Kính thưa quý vị!\n\nTrong khi chúng ta chờ đợi chương trình chính thức bắt đầu, đây là khoảng thời gian quý báu để quý vị kết nối, gặp gỡ những gương mặt mới và cùng nhau nhâm nhi những ly cocktail tinh tế được đội ngũ pha chế tài năng của chúng tôi chuẩn bị.\n\nXin giới thiệu một vài điểm nhấn trong không gian tối nay: góc chụp ảnh sáng tạo bên phải khán phòng, khu trưng bày thành tích năm qua ở khu vực trung tâm, và đặc biệt là bàn buffet canapé cao cấp ngay phía trước.\n\nChương trình chính sẽ bắt đầu sau khoảng 20 phút nữa. Chúc quý vị một buổi tối giao lưu thật vui vẻ và ý nghĩa!")
                        .evaluationHint("Phong cách nhẹ nhàng, thân thiện, không quá trang nghiêm. Tốc độ vừa phải.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(20)
                                        .description("Phát âm rõ ràng, tự nhiên trong ngữ điệu thân thiện").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20)
                                        .description("Nhịp điệu nhẹ nhàng, thoải mái, không gò bó").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(25)
                                        .description("Tốc độ 120–140 wpm; thể hiện sự tự tin và thư thái").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(25)
                                        .description(
                                                "Giọng thân thiện, nhiệt tình nhưng không ồn ào; tạo cảm giác chào đón")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(10)
                                        .description("Truyền đạt đủ thông tin hướng dẫn đến khách mời").build()))
                        .targetWpmMin(120).targetWpmMax(140).passingScore(65)
                        .build(),
                VoiceLesson.builder().title("Giới Thiệu Ban Lãnh Đạo Tại Gala")
                        .category(VoiceLessonCategory.GALA).difficulty("Medium")
                        .description(
                                "Kỹ thuật giới thiệu ban lãnh đạo tại sự kiện Gala với sự trang trọng và tôn trọng đúng nghi thức.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Kính thưa quý vị!\n\nĐể chương trình được bắt đầu một cách trọn vẹn, cho phép tôi có vinh dự giới thiệu những vị lãnh đạo đáng kính đã không ngừng định hướng và dẫn dắt chúng ta trong suốt hành trình vừa qua.\n\nXin trân trọng giới thiệu: Ông Nguyễn Văn An – Tổng Giám đốc, người đã gắn bó và xây dựng công ty từ những ngày đầu thành lập. Tiếp theo, xin chào đón Bà Trần Thị Mai – Giám đốc Điều hành, với những đóng góp xuất sắc trong việc mở rộng thị trường khu vực. Và đặc biệt, ông Lê Hoàng Nam – Giám đốc Sáng tạo, người đứng sau những chiến lược đột phá đã làm thay đổi diện mạo thương hiệu của chúng ta.\n\nXin quý vị một tràng pháo tay thật nồng nhiệt để chào đón ban lãnh đạo!")
                        .evaluationHint("Phát âm rõ tên và chức danh, giọng trang trọng và tôn kính, không vội vàng.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(35).description(
                                        "Phát âm chính xác từng tên người và chức danh; không đọc sai hay ngập ngừng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(25)
                                        .description("Nhịp điệu trang trọng; trọng âm rõ ở tên và chức vụ quan trọng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description("Tốc độ 90–110 wpm khi giới thiệu tên; tránh nuốt chữ").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(10).description(
                                        "Giọng kính trọng, tôn vinh; không thiếu nhiệt tình cũng không thái quá")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(10)
                                        .description("Đọc đúng toàn bộ tên và chức danh theo kịch bản").build()))
                        .targetWpmMin(90).targetWpmMax(110).passingScore(72)
                        .build(),
                VoiceLesson.builder().title("Dẫn Chương Trình Văn Nghệ Gala")
                        .category(VoiceLessonCategory.GALA).difficulty("Medium")
                        .description("Kỹ năng giới thiệu và kết nối các tiết mục văn nghệ trong đêm Gala Dinner.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Kính thưa quý vị!\n\nChúng ta vừa trải qua một bữa tiệc ẩm thực tuyệt vời. Và giờ đây, hãy để âm nhạc và nghệ thuật tiếp tục nuôi dưỡng tâm hồn chúng ta trong phần chương trình văn nghệ đặc sắc.\n\nMở đầu cho phần trình diễn tối nay, tôi xin trân trọng giới thiệu tiết mục đặc biệt từ ban nhạc Jazz Horizon – những nghệ sĩ đã đồng hành cùng nhiều sự kiện đẳng cấp trong và ngoài nước. Bản nhạc mang tên 'Evening Star' sẽ đưa chúng ta vào một không gian âm nhạc sâu lắng và tinh tế.\n\nXin quý vị giữ yên lặng và tận hưởng khoảnh khắc đặc biệt này. Xin mời!")
                        .evaluationHint(
                                "Giọng điệu hào hứng nhưng không quá kích động. Giới thiệu nghệ sĩ với sự trân trọng.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(20)
                                        .description("Phát âm tên ban nhạc và tên bài hát rõ ràng, đúng âm").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(25)
                                        .description(
                                                "Nhịp điệu hứng khởi ở phần mở đầu; chuyển sang nhẹ nhàng khi mời xem")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20).description(
                                        "Tốc độ linh hoạt 115–140 wpm; nhanh hơn khi tạo hứng, chậm khi dẫn vào tiết mục")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(30)
                                        .description(
                                                "Truyền được cảm giác trân trọng nghệ thuật và sự hào hứng chân thật")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng tên tiết mục và thông tin giới thiệu").build()))
                        .targetWpmMin(115).targetWpmMax(140).passingScore(68)
                        .build(),
                VoiceLesson.builder().title("Kết Thúc Và Tiễn Khách Gala")
                        .category(VoiceLessonCategory.GALA).difficulty("Easy")
                        .description(
                                "Lời kết thúc chương trình Gala và tiễn khách một cách chu đáo, để lại ấn tượng tốt đẹp.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Kính thưa toàn thể quý vị!\n\nChúng ta đã cùng nhau trải qua một đêm thực sự đáng nhớ – từ những tiếng cười, những giọt nước mắt xúc động khi nhận giải thưởng, đến những khoảnh khắc giao lưu đầy ấm áp giữa mọi người.\n\nThay mặt ban tổ chức, tôi xin gửi lời cảm ơn chân thành nhất đến tất cả quý vị đã dành thời gian và tình cảm để hiện diện trong đêm nay. Mỗi sự có mặt của quý vị chính là nguồn động lực to lớn cho chúng tôi.\n\nXin nhắc quý vị nhớ lấy phần quà kỷ niệm được đặt tại bàn tiếp tân khi ra về. Hẹn gặp lại quý vị trong những sự kiện tuyệt vời hơn nữa trong năm tới!\n\nChúc quý vị một đêm về an toàn và ngọt ngào. Xin trân trọng cảm ơn!")
                        .evaluationHint("Giọng ấm áp, chân thành, kết thúc nhẹ nhàng. Không vội vàng.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(20)
                                        .description("Phát âm rõ, chân thành; không bị nuốt chữ dù giọng mềm mại")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20)
                                        .description("Nhịp điệu chậm rãi, nhẹ nhàng phù hợp phần kết thúc").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description(
                                                "Tốc độ 100–120 wpm; không vội vàng; cảm giác trọn vẹn và chỉn chu")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(35).description(
                                        "Sự chân thành và biết ơn được thể hiện qua giọng; không máy móc hay chiếu lệ")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng nội dung cảm ơn và hướng dẫn tiễn khách").build()))
                        .targetWpmMin(100).targetWpmMax(120).passingScore(65)
                        .build()));

        // ── GENERAL (5 bài) ─────────────────────────────────────────────
        standaloneLesson.addAll(List.of(
                VoiceLesson.builder().title("Bài Khởi Động Giọng Buổi Sáng")
                        .category(VoiceLessonCategory.GENERAL).difficulty("Easy")
                        .description(
                                "Bài luyện khởi động giọng cơ bản giúp làm ấm dây thanh và lấy hơi đúng cách trước khi dẫn chương trình.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Chào buổi sáng! Hôm nay chúng ta bắt đầu với bài luyện khởi động giọng.\n\nĐầu tiên, hãy hít thở sâu từ bụng, không phải từ ngực. Đặt tay lên bụng và cảm nhận bụng phình ra khi hít vào, xẹp xuống khi thở ra.\n\nBây giờ, luyện nguyên âm kéo dài: A... E... I... O... U... Mỗi âm kéo dài ít nhất 5 giây, giọng đều và không đứt.\n\nTiếp theo, luyện phụ âm bật: Ba-ba-ba... Pa-pa-pa... Ta-ta-ta... Ka-ka-ka... Rõ ràng, dứt khoát.\n\nCuối cùng, đọc câu luyện giọng cổ điển: 'Phòng không phòng, phòng không phòng không' – tập trung vào sự rõ ràng của từng âm tiết.\n\nLuyện mỗi bài 3 lần, hơi thở đều và ổn định. Giọng tốt bắt đầu từ hơi thở đúng!")
                        .evaluationHint("Đánh giá độ rõ ràng của từng âm tiết và sự ổn định của hơi thở.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(40)
                                        .description("Mỗi âm tiết phải rõ ràng, sắc nét; không nuốt hay pha âm")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20)
                                        .description("Nhịp đều đặn, ổn định giữa các cụm luyện tập").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description("Tốc độ chậm có kiểm soát; không vội trong bài khởi động").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(5)
                                        .description("Không yêu cầu cảm xúc – bài kỹ thuật thuần túy").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(15)
                                        .description("Đọc đúng trình tự các chuỗi luyện âm").build()))
                        .targetWpmMin(80).targetWpmMax(110).passingScore(60)
                        .build(),
                VoiceLesson.builder().title("Kỹ Thuật Nhấn Nhá Và Ngắt Nghỉ")
                        .category(VoiceLessonCategory.GENERAL).difficulty("Medium")
                        .description(
                                "Luyện tập kỹ thuật nhấn nhá trọng âm và ngắt nghỉ đúng chỗ để tạo hiệu ứng diễn đạt chuyên nghiệp.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1589903308904-1010c2294adc?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Một MC chuyên nghiệp không chỉ đọc đúng chữ, mà còn biết NHẤN đúng chỗ và NGHỈ đúng lúc.\n\nHãy đọc đoạn sau và chú ý dấu gạch chân là nhấn mạnh, dấu | là ngắt ngắn, dấu || là ngắt dài:\n\n'Kính thưa | quý vị đại biểu, || quý khách quý mến! | Hôm nay, | chúng ta cùng nhau | tụ họp nơi đây, || không phải để nhìn lại | những gì đã qua, || mà để cùng nhau | VIẾT TIẾP | những trang sử MỚI.'\n\nĐọc lại lần 2, lần này tự quyết định chỗ nhấn và nghỉ theo cảm nhận của bạn.\n\nGhi nhớ: Nghỉ trước một ý quan trọng tạo sự chú ý. Nghỉ sau một câu quan trọng tạo không gian suy nghĩ. Đừng bao giờ ngắt giữa chừng một ý tưởng chưa hoàn chỉnh.")
                        .evaluationHint("Đánh giá việc nhấn trọng âm đúng chỗ và các điểm ngắt nghỉ tự nhiên.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(20)
                                        .description(
                                                "Phát âm rõ tại các điểm nhấn mạnh; không bị mờ nhạt ở từ quan trọng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(35)
                                        .description(
                                                "Nhấn trọng âm đúng vị trí; ngắt nghỉ đúng thời điểm theo cấu trúc câu")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(25).description(
                                        "Tốc độ thay đổi có chủ đích; chậm hơn trước điểm nhấn, nhanh hơn sau chuyển tiếp")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(15)
                                        .description("Trọng âm tạo cảm giác tự nhiên, không máy móc hay cứng nhắc")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng nội dung đoạn văn mẫu").build()))
                        .targetWpmMin(110).targetWpmMax(140).passingScore(68)
                        .build(),
                VoiceLesson.builder().title("Kiểm Soát Tốc Độ Nói")
                        .category(VoiceLessonCategory.GENERAL).difficulty("Medium")
                        .description(
                                "Bài luyện kiểm soát tempo nói – biết khi nào nên nói nhanh, khi nào cần chậm lại để tạo hiệu quả.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1560523159-4a9692d222f9?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Tốc độ nói là vũ khí bí mật của MC. Quá nhanh – khán giả không kịp theo. Quá chậm – họ mất tập trung.\n\nPhần 1 – NÓI CHẬM (80 wpm): 'Chào... mừng... quý vị... đến với... sự kiện... đặc biệt... tối... nay.' Cảm nhận sức nặng của từng từ.\n\nPhần 2 – TỐC ĐỘ TRUNG BÌNH (130 wpm): 'Chào mừng quý vị đến với sự kiện đặc biệt tối nay. Chúng tôi rất vui được đón tiếp quý vị trong không gian ấm cúng này.'\n\nPhần 3 – NÓI NHANH tạo hứng khởi (160+ wpm): 'Và bây giờ, hãy cùng chào đón tiết mục đặc biệt mà tất cả chúng ta đã mong chờ suốt cả đêm nay!'\n\nBí quyết: Chậm khi công bố tin quan trọng. Vừa khi kể chuyện. Nhanh khi tạo hứng khởi và giới thiệu tiết mục.")
                        .evaluationHint(
                                "Mục tiêu WPM: 120-140 cho đoạn thông thường. Đánh giá biến đổi tốc độ có chủ đích.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15)
                                        .description("Phát âm rõ kể cả khi nói nhanh; không bị méo âm khi tăng tốc")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(25).description(
                                        "Nhịp điệu ổn định trong từng vùng tốc độ; không bị loạn nhịp khi chuyển cấp độ")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(40).description(
                                        "Phân biệt rõ 3 vùng tốc độ (chậm/trung bình/nhanh); chuyển đổi có chủ đích, không ngẫu nhiên")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(15).description(
                                        "Cảm xúc phù hợp từng vùng tốc độ: trọng tâm khi chậm, hứng khởi khi nhanh")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng ba đoạn mẫu theo đúng tốc độ yêu cầu").build()))
                        .targetWpmMin(110).targetWpmMax(160).passingScore(68)
                        .build(),
                VoiceLesson.builder().title("Xử Lý Tình Huống Mất Điện Thoại Nhắc")
                        .category(VoiceLessonCategory.GENERAL).difficulty("Hard")
                        .description(
                                "Luyện tập ứng xử khi bị mất prompter hoặc giấy tờ – kỹ năng ứng biến linh hoạt của MC chuyên nghiệp.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1546953304-5d96f43c2e94?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Tình huống: Bạn đang dẫn chương trình, đột nhiên màn hình nhắc lời tắt, hoặc gió thổi bay tờ giấy. Xử lý thế nào?\n\nKỹ thuật 1 – BRIDGE: Dùng câu cầu nối để kéo dài thời gian: 'Và đây là điều mà tôi muốn quý vị thực sự ghi nhớ...' – trong khi đó, bình tĩnh nhớ lại nội dung.\n\nKỹ thuật 2 – RECAP: Tóm tắt lại điều vừa nói: 'Như tôi vừa chia sẻ, chúng ta đang ở giai đoạn quan trọng của chương trình...' Điều này vừa giúp bạn lấy thời gian, vừa củng cố thông điệp cho khán giả.\n\nKỹ thuật 3 – ENGAGE: Hỏi khán giả: 'Quý vị nghĩ điều gì sẽ xảy ra tiếp theo?' Tạo tương tác trong khi bạn bình tĩnh lấy lại nhịp.\n\nLuyện tập: Đọc đoạn kịch bản 30 giây, sau đó đột ngột dừng lại, áp dụng một trong 3 kỹ thuật trên và tiếp tục dẫn tự nhiên.")
                        .evaluationHint(
                                "Đánh giá sự bình tĩnh, tự nhiên và khả năng chuyển tiếp mượt mà khi xử lý tình huống.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15)
                                        .description("Phát âm vẫn rõ ràng dù đang ứng biến; không bị vấp hay lắp")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20)
                                        .description(
                                                "Nhịp điệu tự nhiên khi chuyển sang câu cầu nối; không lộ sự gián đoạn")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description(
                                                "Tốc độ ổn định, không đột ngột tăng vọt hay giảm mạnh khi xử lý sự cố")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(25).description(
                                        "Giọng bình tĩnh, tự tin; không để khán giả cảm nhận sự hoảng loạn hay mất phương hướng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(20).description(
                                        "Áp dụng đúng kỹ thuật BRIDGE/RECAP/ENGAGE; chuyển tiếp liền mạch trở lại nội dung chính")
                                        .build()))
                        .targetWpmMin(110).targetWpmMax(145).passingScore(72)
                        .build(),
                VoiceLesson.builder().title("Luyện Giọng Với Micro Chuyên Nghiệp")
                        .category(VoiceLessonCategory.GENERAL).difficulty("Easy")
                        .description(
                                "Kỹ thuật sử dụng micro đúng cách – khoảng cách, góc độ và kiểm soát âm lượng khi dùng micro cầm tay.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1588681664899-f142ff2dc9b1?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Micro là người bạn đồng hành không thể thiếu của MC. Nhưng không phải ai cũng biết dùng đúng cách.\n\nKhoảng cách lý tưởng: Giữ micro cách miệng khoảng 10-15cm. Quá gần gây méo tiếng. Quá xa giọng sẽ nhỏ và thiếu uy.\n\nGóc độ: Giữ micro thẳng đứng, hơi chếch 15-20 độ về phía sau. Không chĩa thẳng vào miệng.\n\nKiểm tra âm thanh: 'Xin chào, một hai ba, kiểm tra micro. Một hai ba.' – Nói với âm lượng bình thường, không cần hét.\n\nLuyện với đoạn sau và tưởng tượng bạn đang cầm micro thật: 'Kính thưa quý vị, tôi là MC tối nay. Chúng tôi rất vui được phục vụ quý vị trong buổi tối đặc biệt này. Chương trình sẽ bắt đầu sau ít phút nữa, xin quý vị vui lòng ổn định chỗ ngồi.'\n\nGhi nhớ: Micro khuếch đại tất cả – cả giọng tốt lẫn giọng chưa tốt. Luyện tập đúng kỹ thuật từ đầu!")
                        .evaluationHint("Đánh giá độ đều của âm lượng và sự rõ ràng ở từng âm tiết.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(30)
                                        .description("Phát âm rõ từng âm tiết; đặc biệt chú ý phụ âm cuối không bị mất")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20)
                                        .description("Nhịp điệu đều đặn, không bị ngắt quãng bất thường").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20).description(
                                        "Tốc độ vừa phải 120–135 wpm; không quá nhanh hay quá chậm cho đoạn kiểm tra micro")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(15)
                                        .description("Giọng tự nhiên, tự tin; không bị căng thẳng hay run rẩy").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(15)
                                        .description("Đọc đúng đoạn hướng dẫn và đoạn luyện tập").build()))
                        .targetWpmMin(120).targetWpmMax(135).passingScore(62)
                        .build()));

        // ── WEDDING nâng cao (5 bài) ──────────────────────────────────────
        standaloneLesson.addAll(List.of(
                VoiceLesson.builder().title("Dẫn Lễ Rước Dâu Ngoài Trời")
                        .category(VoiceLessonCategory.WEDDING).difficulty("Hard")
                        .description(
                                "Kỹ thuật dẫn lễ rước dâu ngoài trời với âm thanh tự nhiên, xử lý tiếng ồn và duy trì cảm xúc.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1519225421980-715cb0215aed?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Lễ rước dâu ngoài trời mang một vẻ đẹp riêng, nhưng cũng đặt ra thử thách đặc biệt cho người MC.\n\n'Kính thưa quý vị! Khoảnh khắc mà tất cả chúng ta đã mong chờ đã đến. Từ xa xa, trong làn gió nhẹ của buổi chiều tà, chú rể Minh Tuấn đang bước ra đón người bạn đời của đời mình.'\n\n[Nhấn mạnh: Dừng lại khi đám đông xôn xao, điều chỉnh âm lượng khi có tiếng gió]\n\n'Và kìa! Cô dâu Thanh Hà xinh đẹp trong tà áo trắng tinh khôi, từng bước chân nhẹ nhàng như đang bước ra từ một giấc mơ đẹp. Hai trái tim, một hành trình.'\n\nKỹ năng quan trọng: Luôn hướng mặt về phía khán giả, không quay lưng. Giọng phải đủ lớn nhưng không la hét. Chú ý âm thanh từ môi trường xung quanh để điều chỉnh kịp thời.")
                        .evaluationHint(
                                "Giọng cảm xúc, đủ âm lượng cho không gian ngoài trời. Nhịp độ chậm rãi, trang trọng.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(20).description(
                                        "Phát âm rõ kể cả khi phải nói to hơn bình thường để lấn át tiếng ồn ngoài trời")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(25).description(
                                        "Nhịp điệu chậm rãi, trang trọng; biết dừng khi đám đông ồn để không bị chìm tiếng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20).description(
                                        "Tốc độ 90–115 wpm; chậm hơn trong không gian ngoài trời để giọng truyền xa hơn")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(30).description(
                                        "Cảm xúc lãng mạn và trang trọng; những từ mô tả cô dâu chú rể phải có sức nặng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng nội dung và tên nhân vật trong đoạn kịch bản").build()))
                        .targetWpmMin(90).targetWpmMax(115).passingScore(72)
                        .build(),
                VoiceLesson.builder().title("Thông Báo Bất Ngờ Từ Cô Dâu Chú Rể")
                        .category(VoiceLessonCategory.WEDDING).difficulty("Medium")
                        .description(
                                "Cách dẫn dắt những thông báo bất ngờ hoặc màn trình diễn đặc biệt từ cô dâu chú rể trong đám cưới.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1606800052052-a08af7148866?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Đây là kỹ năng quan trọng – tạo sự bất ngờ và hứng khởi cho khán giả trước một màn trình diễn không nằm trong chương trình ban đầu.\n\n'Thưa quý vị! Chú rể Hùng Anh có một điều rất đặc biệt muốn dành tặng cho cô dâu Phương Linh tối nay. Một điều mà chính Phương Linh cũng chưa biết...'\n\n[Tạo sự hồi hộp bằng cách dừng lại]\n\n'Anh ấy đã dành 3 tháng để học một bài hát – bài hát đầu tiên mà họ cùng nghe trong buổi hẹn hò đầu tiên. Và tối nay, trước mặt tất cả chúng ta...'\n\n[Nhấn mạnh, giọng xúc động]\n\n'...anh ấy sẽ tự mình hát tặng người con gái anh yêu. Hùng Anh ơi, sân khấu là của anh!'\n\nBí quyết: Xây dựng kỳ vọng từng bước, mỗi câu tiết lộ thêm một chút, tạo đỉnh điểm cảm xúc trước khi giới thiệu.")
                        .evaluationHint(
                                "Đánh giá khả năng xây dựng cảm xúc theo từng bước và thời điểm ngắt để tạo hiệu ứng.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15)
                                        .description("Phát âm rõ tên cô dâu chú rể và các từ khóa cảm xúc").build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(25)
                                        .description(
                                                "Nhịp điệu leo thang theo từng câu; mỗi câu tiếp theo tạo thêm kỳ vọng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description("Chậm dần về cuối để tạo đỉnh điểm; ngắt đúng lúc trước câu kết")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(35).description(
                                        "Giọng hồi hộp, xúc động và hứng khởi tăng dần; câu kết phải bùng nổ cảm xúc")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng nội dung và thứ tự các chi tiết tạo bất ngờ").build()))
                        .targetWpmMin(105).targetWpmMax(130).passingScore(70)
                        .build(),
                VoiceLesson.builder().title("Dẫn Phần Giao Lưu Khán Giả Đám Cưới")
                        .category(VoiceLessonCategory.WEDDING).difficulty("Medium")
                        .description(
                                "Kỹ năng tổ chức và dẫn dắt các mini game hoặc giao lưu khán giả trong tiệc cưới một cách vui tươi và tự nhiên.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1583939003579-730e3918a45a?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "'Thưa quý vị! Để không khí buổi tiệc thêm phần rộn ràng, chúng tôi có một trò chơi nhỏ dành cho những ai muốn ghi tên mình vào danh sách... nhận quà!'\n\n[Tạo tiếng cười và sự hứng thú]\n\n'Luật chơi rất đơn giản: Tôi sẽ hỏi một câu hỏi về cô dâu chú rể, ai giơ tay nhanh nhất và trả lời đúng sẽ nhận phần quà từ ban tổ chức!'\n\n'Câu hỏi đầu tiên: Cô dâu chú rể gặp nhau ở đâu lần đầu tiên?' [Dừng lại, nhìn khán giả]\n\n'Ồ, nhiều người biết quá! Anh kia, xin mời!' [Chỉ về phía một khán giả giơ tay]\n\nKỹ năng quan trọng: Luôn giữ nhịp độ nhanh, năng lượng cao. Khen ngợi tất cả người tham gia dù đúng hay sai. Không để khoảng trống im lặng kéo dài quá 3 giây.")
                        .evaluationHint("Giọng vui tươi, năng lượng cao. Đánh giá sự tự nhiên và khả năng tương tác.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15)
                                        .description("Phát âm rõ kể cả khi nói nhanh; không bị nuốt chữ khi hứng khởi")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20)
                                        .description(
                                                "Nhịp điệu sôi động, energetic; không để câu nào bị kéo dài lê thê")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(25).description(
                                        "Tốc độ nhanh 135–155 wpm để duy trì năng lượng; không để khoảng lặng quá 3 giây")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(35).description(
                                        "Năng lượng vui tươi, nhiệt tình, tự nhiên; không bị giả tạo hay gượng ép")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng luật chơi và câu hỏi trong mini game").build()))
                        .targetWpmMin(135).targetWpmMax(155).passingScore(68)
                        .build(),
                VoiceLesson.builder().title("Đọc Lời Chúc Từ Người Thân Vắng Mặt")
                        .category(VoiceLessonCategory.WEDDING).difficulty("Hard")
                        .description(
                                "Kỹ thuật đọc thư hoặc lời chúc từ người thân không thể có mặt – đòi hỏi cảm xúc chân thật và kiểm soát giọng.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1465495976277-4387d4b0b4c6?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Đây là khoảnh khắc đặc biệt nhất và cũng cảm xúc nhất trong đám cưới. MC cần truyền tải cảm xúc của người viết một cách chân thật.\n\n'Thưa quý vị, thưa cô dâu chú rể! Có một người không thể có mặt tại đây tối nay, nhưng trái tim của người đó luôn dõi theo từng bước của hai em. Bà ngoại Phương Linh, từ Hà Nội, đã gửi những dòng chữ này để tôi đọc thay bà...'\n\n[Giọng chậm lại, xúc động nhưng kiểm soát]\n\n'Con Linh yêu quý của bà! Nhìn con trong tà áo cô dâu hôm nay, bà tưởng như thấy lại hình ảnh mẹ con ngày xưa. Con đã lớn khôn, trưởng thành và xinh đẹp biết bao. Bà chỉ tiếc không được ở đây để ôm con vào lòng...'\n\nGhi nhớ: Đọc chậm hơn bình thường. Cho phép bản thân bị cảm xúc nhưng không mất kiểm soát. Nếu nghẹn ngào, dừng lại một nhịp, hít thở nhẹ rồi tiếp tục.")
                        .evaluationHint(
                                "Đánh giá độ chân thật của cảm xúc, kiểm soát giọng khi xúc động và nhịp điệu đọc.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15).description(
                                        "Phát âm rõ kể cả khi giọng xúc động; không bị vỡ giọng hay nức nở mất kiểm soát")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20)
                                        .description("Nhịp đọc thư chậm, trịnh trọng; có điểm dừng cảm xúc tự nhiên")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description(
                                                "Tốc độ 80–100 wpm khi đọc thư; cho phép cảm xúc thấm vào từng câu")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(40).description(
                                        "Cảm xúc chân thật từ người đọc; truyền tải được nỗi nhớ thương của người vắng mặt")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng nội dung lá thư theo kịch bản").build()))
                        .targetWpmMin(80).targetWpmMax(100).passingScore(72)
                        .build(),
                VoiceLesson.builder().title("Dẫn Chương Trình Đám Cưới Song Ngữ")
                        .category(VoiceLessonCategory.WEDDING).difficulty("Hard")
                        .description(
                                "Kỹ năng dẫn chương trình đám cưới có khách mời quốc tế – chuyển đổi liền mạch giữa tiếng Việt và tiếng Anh.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Dẫn song ngữ đòi hỏi sự chuyển đổi mượt mà và giữ nguyên cảm xúc qua hai ngôn ngữ.\n\n'Kính thưa quý vị! Ladies and gentlemen!\n\nChào mừng quý vị đến với hôn lễ của Minh và Sarah. Welcome to the wedding ceremony of Minh and Sarah.\n\nHôm nay là ngày đặc biệt không chỉ với gia đình, mà còn với những người bạn đến từ khắp nơi trên thế giới. Today is a special day not only for the families, but also for friends who have come from all corners of the world.\n\nChúng tôi xin gửi lời cảm ơn chân thành nhất đến tất cả quý vị đã hiện diện. We sincerely thank all of you for being here with us tonight.'\n\nBí quyết: Không dịch từng chữ, mà truyền đạt ý. Giữ cùng mức năng lượng trong cả hai ngôn ngữ. Ngắt giữa hai ngôn ngữ tự nhiên, không vội.")
                        .evaluationHint(
                                "Đánh giá sự trôi chảy khi chuyển đổi ngôn ngữ và mức độ tự nhiên trong phát âm tiếng Anh.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(35).description(
                                        "Phát âm tiếng Anh chuẩn; âm /th/, /r/, /v/ không bị pha giọng Việt quá nặng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20).description(
                                        "Nhịp điệu tự nhiên trong cả hai ngôn ngữ; không bị cứng hay lạc nhịp khi chuyển")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description("Tốc độ 100–120 wpm; chậm rãi và rõ ràng khi dẫn song ngữ")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(15).description(
                                        "Cảm xúc nhất quán qua cả hai ngôn ngữ; không bị giảm nhiệt khi chuyển sang tiếng Anh")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(10)
                                        .description("Nội dung tiếng Anh truyền đạt đúng ý so với đoạn tiếng Việt")
                                        .build()))
                        .targetWpmMin(100).targetWpmMax(120).passingScore(72)
                        .build()));

        // ── TALKSHOW nâng cao (5 bài) ────────────────────────────────────
        standaloneLesson.addAll(List.of(
                VoiceLesson.builder().title("Mở Đầu Talkshow Với Câu Hỏi Provocative")
                        .category(VoiceLessonCategory.TALKSHOW).difficulty("Hard")
                        .description(
                                "Kỹ thuật mở đầu chương trình bằng câu hỏi gây tranh cãi để thu hút sự chú ý tối đa ngay từ đầu.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "'Nếu bạn có thể xóa đi một quyết định trong quá khứ, bạn có làm không?'\n\n[Dừng lại 3 giây, nhìn thẳng vào camera]\n\n'Câu hỏi tưởng chừng đơn giản này đã khiến hàng triệu người mất ngủ. Và tối nay, chúng ta sẽ ngồi xuống với người đã trả lời: Không. Dù bất kể điều gì đã xảy ra.'\n\n'Chào mừng đến với Unfiltered – nơi chúng ta không nói những điều người ta muốn nghe, mà nói những điều người ta cần nghe. Tôi là Minh Khoa, và đây là chương trình của những câu chuyện thật.'\n\nKỹ thuật: Câu hỏi provocative không cần trả lời ngay – nó là cái móc giữ khán giả. Giọng phải dứt khoát, không hỏi mà như khẳng định. Ánh mắt thẳng, không nhìn lơ đãng.")
                        .evaluationHint("Đánh giá sức mạnh của câu hỏi mở đầu, độ tự tin và khả năng tạo sự tò mò.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15).description(
                                        "Phát âm dứt khoát, mạnh mẽ; câu hỏi mở đầu phải được phát âm như một tuyên bố")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20).description(
                                        "Nhịp điệu đột ngột dừng sau câu hỏi provocative; khoảng lặng 3 giây được tính vào nhịp")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20).description(
                                        "Chậm và mạnh ở câu hỏi đầu; tăng tốc khi xây dựng context; vừa phải khi giới thiệu chương trình")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(40).description(
                                        "Giọng tự tin, bí ẩn và kéo người xem vào; câu hỏi phải tạo được cảm giác muốn biết câu trả lời")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng nội dung câu hỏi và phần giới thiệu chương trình")
                                        .build()))
                        .targetWpmMin(100).targetWpmMax(130).passingScore(72)
                        .build(),
                VoiceLesson.builder().title("Xử Lý Khách Mời Lạc Đề")
                        .category(VoiceLessonCategory.TALKSHOW).difficulty("Hard")
                        .description(
                                "Kỹ năng điều hướng khéo léo khi khách mời đang nói sang chủ đề khác mà không làm mất lòng họ.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1478737270197-2468169085b0?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Tình huống: Bạn đang phỏng vấn về chủ đề khởi nghiệp, nhưng khách mời đã nói suốt 4 phút về cuộc hôn nhân của họ.\n\nPhương pháp BRIDGE:\n\n'Và tôi rất xúc động khi nghe câu chuyện gia đình của anh. Tôi tin rằng chính nền tảng đó đã cho anh sức mạnh để...' [kết nối về chủ đề chính] '...bước vào thế giới kinh doanh với tất cả sự dũng cảm. Vậy anh có thể kể cho chúng tôi nghe về quyết định đầu tiên anh đưa ra khi bắt đầu khởi nghiệp?'\n\nPhương pháp INTERRUPT GRACEFULLY:\n\n'Xin lỗi anh cho tôi ngắt một chút – điều anh vừa nói rất thú vị và chúng ta sẽ quay lại sau. Nhưng tôi muốn tận dụng thời gian để hỏi anh về...'\n\nGhi nhớ: Không bao giờ ngắt thô lỗ. Luôn ghi nhận những gì khách mời vừa nói trước khi chuyển hướng.")
                        .evaluationHint(
                                "Đánh giá sự khéo léo trong việc chuyển hướng và duy trì mối quan hệ tốt với khách mời.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15)
                                        .description("Phát âm rõ, lịch sự; giọng không bị căng hay gắt khi ngắt lời")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20).description(
                                        "Nhịp điệu liền mạch khi BRIDGE; không có khoảng lặng vụng về khi chuyển hướng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description(
                                                "Tốc độ vừa phải 115–135 wpm; không vội vàng hay kéo dài khi redirect")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(30).description(
                                        "Giọng chân thành khi ghi nhận câu chuyện khách mời; nhiệt tình khi dẫn về chủ đề chính")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(15).description(
                                        "Áp dụng đúng kỹ thuật BRIDGE hoặc INTERRUPT GRACEFULLY; chuyển tiếp hợp lý")
                                        .build()))
                        .targetWpmMin(115).targetWpmMax(135).passingScore(72)
                        .build(),
                VoiceLesson.builder().title("Đặt Câu Hỏi Follow-Up Sắc Bén")
                        .category(VoiceLessonCategory.TALKSHOW).difficulty("Hard")
                        .description(
                                "Luyện kỹ năng lắng nghe sâu và đặt câu hỏi tiếp nối đúng thời điểm để khai thác câu chuyện hay nhất.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "MC talkshow giỏi không đọc câu hỏi từ giấy. Họ LẮNG NGHE và hỏi những gì cần hỏi.\n\nVí dụ 1:\nKhách mời: 'Tôi từng thất bại hoàn toàn vào năm 2019.'\nCâu hỏi tệ: 'Vâng, hãy kể tiếp về sự nghiệp của bạn.'\nCâu hỏi tốt: 'Thất bại hoàn toàn – ý anh là thất bại theo nghĩa nào? Tài chính, hay còn điều gì khác?'\n\nVí dụ 2:\nKhách mời: 'Đó là quyết định khó nhất tôi từng đưa ra.'\nCâu hỏi tệ: 'Cảm ơn anh, vậy bước tiếp theo anh làm gì?'\nCâu hỏi tốt: 'Có ai biết về quyết định đó không? Gia đình anh phản ứng thế nào?'\n\nLuyện tập: Đọc câu trả lời của khách mời bên dưới và nghĩ ra 3 câu follow-up khác nhau:\n'Tôi đã rời khỏi công việc ổn định để theo đuổi đam mê nhiếp ảnh vào năm 30 tuổi.'")
                        .evaluationHint(
                                "Đánh giá sự nhạy bén trong việc nắm bắt điểm thú vị và đặt câu hỏi đúng trọng tâm.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15)
                                        .description(
                                                "Phát âm câu hỏi rõ ràng; từ nghi vấn phải nổi bật, không bị mờ nhạt")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(15)
                                        .description("Nhịp điệu câu hỏi tự nhiên; không đọc như kịch bản có sẵn")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20).description(
                                        "Tốc độ vừa phải; câu hỏi follow-up được đặt ra chậm rãi, không vội vàng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(25).description(
                                        "Giọng thể hiện sự tò mò thật sự; không máy móc hay đọc từ danh sách chuẩn bị sẵn")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(25).description(
                                        "Câu hỏi follow-up chạm đúng điểm mấu chốt trong câu trả lời của khách mời")
                                        .build()))
                        .targetWpmMin(110).targetWpmMax(130).passingScore(72)
                        .build(),
                VoiceLesson.builder().title("Dẫn Talkshow Live Với Khán Giả Trường Quay")
                        .category(VoiceLessonCategory.TALKSHOW).difficulty("Medium")
                        .description(
                                "Kỹ thuật tương tác song song với khán giả trong trường quay và khán giả xem qua màn hình.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Dẫn live talkshow là cân bằng giữa hai thế giới: khán giả ngồi trước mặt và hàng triệu người xem tại nhà.\n\n'Chào mừng đến với Tonight Live! Trước mặt tôi là 200 khán giả nhiệt tình nhất Hà Nội tối nay!' [Hướng về phía khán giả, tạo tiếng vỗ tay]\n\n'Và chào tất cả mọi người đang xem qua màn hình – dù bạn đang ở đâu, căn phòng này cũng có một ghế dành cho bạn.'\n\n[Sau đó quay về khách mời]\n\n'Thưa anh Thanh, tôi có một câu hỏi đến từ khán giả online – bạn Hương ở TP.HCM hỏi: Điều gì đã khiến anh không bỏ cuộc trong những ngày tối tăm nhất?'\n\nBí quyết: Đừng ưu tiên một nhóm khán giả. Camera = cánh cửa kết nối với hàng triệu người. Khán giả trường quay = năng lượng sống cho chương trình.")
                        .evaluationHint("Đánh giá khả năng chuyển đổi tự nhiên giữa các đối tượng khán giả khác nhau.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(20).description(
                                        "Phát âm rõ khi nói với từng đối tượng; không bị giảm chất lượng khi chuyển hướng")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20).description(
                                        "Nhịp điệu liền mạch khi chuyển giữa khán giả trường quay, camera và khách mời")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20).description(
                                        "Tốc độ 120–140 wpm; nhanh hơn khi tạo hứng với khán giả, vừa hơn khi hỏi khách mời")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(30).description(
                                        "Năng lượng nhất quán với cả hai nhóm khán giả; tạo cảm giác ai cũng được chú ý")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(10).description(
                                        "Đọc đúng câu hỏi từ khán giả online và nội dung giới thiệu chương trình")
                                        .build()))
                        .targetWpmMin(120).targetWpmMax(140).passingScore(68)
                        .build(),
                VoiceLesson.builder().title("Kết Thúc Talkshow Để Lại Dư Âm")
                        .category(VoiceLessonCategory.TALKSHOW).difficulty("Medium")
                        .description(
                                "Nghệ thuật kết thúc chương trình với lời kết đáng nhớ, khuyến khích hành động và giữ khán giả quay lại lần sau.")
                        .thumbnailUrl(
                                "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=800&h=450&fit=crop&auto=format")
                        .content(
                                "Kết thúc talkshow không phải là dừng lại – đó là cú đánh cuối cùng in sâu vào tâm trí khán giả.\n\n'Chúng ta đã cùng nhau trải qua gần một giờ với những câu chuyện truyền cảm hứng, những góc nhìn thay đổi cách chúng ta nghĩ và sống.'\n\n'Câu hỏi tôi muốn để lại cho quý vị tối nay là: Từ ngày mai, bạn sẽ thay đổi một điều nhỏ gì trong cuộc sống của mình?'\n\n[Dừng lại, nhìn thẳng vào camera]\n\n'Đôi khi, một điều nhỏ đó lại là khởi đầu của tất cả.\n\nCảm ơn khách mời đặc biệt của chúng ta – anh Thanh Tùng. Cảm ơn quý vị đã ở lại đến tận cuối. Và hẹn gặp lại vào tuần tới, cùng giờ này, tại đây. Tôi là Minh Khoa – chúc quý vị một đêm ngủ ngon và một ngày mai tươi sáng.'\n\nBí quyết: Câu hỏi để lại = lý do khán giả suy nghĩ về bạn sau khi tắt TV. Lời cảm ơn = chân thành, không vội vàng.")
                        .evaluationHint("Đánh giá sức mạnh của lời kết và khả năng tạo ấn tượng lâu dài.")
                        .evaluationCriteria(List.of(
                                VoiceLesson.EvaluationCriteria.builder().aspect("PRONUNCIATION").weight(15).description(
                                        "Phát âm chậm rãi, rõ ràng; câu hỏi để lại phải được phát âm như đinh đóng cột")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("RHYTHM").weight(20).description(
                                        "Nhịp điệu giảm dần về cuối; khoảng lặng sau câu hỏi để lại là bắt buộc")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("PACING").weight(20)
                                        .description(
                                                "Tốc độ 105–125 wpm; chậm lại rõ rệt ở câu hỏi để lại và lời cảm ơn")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("EMOTION").weight(40).description(
                                        "Giọng ấm áp, sâu lắng và chân thành; tạo được dư âm cảm xúc sau khi kết thúc")
                                        .build(),
                                VoiceLesson.EvaluationCriteria.builder().aspect("ACCURACY").weight(5)
                                        .description("Đọc đúng lời kết, câu hỏi để lại và phần cảm ơn").build()))
                        .targetWpmMin(105).targetWpmMax(125).passingScore(68)
                        .build()));

        lessonRepository.saveAll(standaloneLesson);
        log.info("✅ Successfully seeded 20 standalone lessons");

        log.info("🌱 Seeding Reading Guides...");

        ReadingGuide introGuide = ReadingGuide.builder()
                .title("The Professional MC Mindset")
                .content(
                        "# Stage Presence & Mindset\n\nBeing a Master of Ceremonies is more than just reading a script. It's about controlling the energy of the room.\n\n## Key Principles\n1. **Authority**: You are the captain of the ship.\n2. **Empathy**: Connect with your audience's emotions.\n3. **Adaptability**: Be ready for the unexpected.")
                .author("MCHub Elite")
                .build();

        ReadingGuide protocolGuide = ReadingGuide.builder()
                .title("International Event Protocol")
                .content(
                        "# Diplomatic & International Protocol\n\nWhen hosting international summits, the order of precedence is critical.\n\n## Seating Arrangements\n- The guest of honor always sits to the right of the host.\n- Flags must be displayed according to strict diplomatic standards.")
                .author("Protocol Expert")
                .build();

        readingGuideRepository.saveAll(Arrays.asList(introGuide, protocolGuide));
        log.info("✅ Successfully seeded Reading Guides");

        log.info("🌱 Seeding Mock Gamification Users, Stats & Active Arena Competitions...");

        List<User> existingUsers = userRepository.findAll();
        List<User> usersToUse = new ArrayList<>();

        if (existingUsers.isEmpty()) {
            log.info("⚠️ No existing users found. Seeding 5 default mock users...");
            String[][] mockUsersData = {
                    { "mai@mchub.vn", "Nguyễn Mai Anh",
                            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150" },
                    { "nam@mchub.vn", "Trần Hoàng Nam",
                            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150" },
                    { "thao@mchub.vn", "Lê Thu Thảo",
                            "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150" },
                    { "huy@mchub.vn", "Phạm Minh Huy",
                            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150" },
                    { "tri@mchub.vn", "Vũ Minh Trí",
                            "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150" }
            };
            for (String[] data : mockUsersData) {
                User user = User.builder()
                        .name(data[1])
                        .email(data[0])
                        .avatar(data[2])
                        .password(passwordEncoder.encode("password123"))
                        .role(UserRole.CLIENT)
                        .isVerified(true)
                        .isActive(true)
                        .build();
                user = userRepository.save(user);
                usersToUse.add(user);
            }
        } else {
            log.info("✅ Found {} existing users in database. Using them for gamification seeding.",
                    existingUsers.size());
            usersToUse.addAll(existingUsers);
        }

        for (int i = 0; i < usersToUse.size(); i++) {
            User u = usersToUse.get(i);
            int currentStreak, longestStreak, sessions;
            double totalHours, cumulativeXP, weeklyXP;
            String currentTier;

            if (i == 0) {
                currentStreak = 22;
                longestStreak = 35;
                totalHours = 55.4;
                sessions = 120;
                cumulativeXP = 26500.0;
                weeklyXP = 3200.0;
                currentTier = "ELITE_LEGEND";
            } else if (i == 1) {
                currentStreak = 15;
                longestStreak = 24;
                totalHours = 42.5;
                sessions = 98;
                cumulativeXP = 12500.0;
                weeklyXP = 2100.0;
                currentTier = "DIAMOND";
            } else if (i == 2) {
                currentStreak = 8;
                longestStreak = 18;
                totalHours = 28.0;
                sessions = 64;
                cumulativeXP = 7600.0;
                weeklyXP = 1500.0;
                currentTier = "PLATINUM";
            } else if (i == 3) {
                currentStreak = 4;
                longestStreak = 10;
                totalHours = 14.5;
                sessions = 35;
                cumulativeXP = 3800.0;
                weeklyXP = 850.0;
                currentTier = "GOLD";
            } else if (i == 4) {
                currentStreak = 2;
                longestStreak = 6;
                totalHours = 8.2;
                sessions = 18;
                cumulativeXP = 1400.0;
                weeklyXP = 300.0;
                currentTier = "SILVER";
            } else {
                currentStreak = (i * 3) % 7;
                longestStreak = currentStreak + (i % 5) + 1;
                totalHours = 2.5 + (i * 0.7);
                sessions = 6 + (i * 2);
                cumulativeXP = 350.0 + (i * 150.0);
                weeklyXP = 100.0 + (i * 30.0);
                if (cumulativeXP >= 20000)
                    currentTier = "ELITE_LEGEND";
                else if (cumulativeXP >= 10000)
                    currentTier = "DIAMOND";
                else if (cumulativeXP >= 5000)
                    currentTier = "PLATINUM";
                else if (cumulativeXP >= 2000)
                    currentTier = "GOLD";
                else if (cumulativeXP >= 800)
                    currentTier = "SILVER";
                else
                    currentTier = "BRONZE";
            }

            userStatsRepository.save(UserStats.builder()
                    .userId(u.getId())
                    .currentStreak(currentStreak)
                    .longestStreak(longestStreak)
                    .totalPracticeHours(totalHours)
                    .totalSessions(sessions)
                    .cumulativeXP(cumulativeXP)
                    .weeklyXP(weeklyXP)
                    .currentTier(currentTier)
                    .lastPracticeTime(Instant.now().minus(i % 5, ChronoUnit.HOURS))
                    .build());
        }

        List<VoiceLesson> allLessons = lessonRepository.findAll();
        VoiceLesson firstLesson = allLessons.get(0);
        VoiceLesson secondLesson = allLessons.get(1);

        Competition weeklyArena = competitionRepository.save(Competition.builder()
                .title("Đấu Trường Biên Tập Viên Bản Tin Tối")
                .description(
                        "Luyện tập kỹ thuật nhấn nhá, nhịp điệu và truyền cảm giọng đọc cho bản tin thời sự 19h của VTV. Top 3 sẽ nhận danh hiệu Champion Badge.")
                .type(CompetitionType.WEEKLY)
                .challengeScriptId(firstLesson.getId())
                .startDate(Instant.now().minus(3, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(4, ChronoUnit.DAYS))
                .active(true)
                .build());

        Competition dailyArena = competitionRepository.save(Competition.builder()
                .title("Thử Thách Gala Dinner Năng Động")
                .description(
                        "Dành riêng cho kịch bản dẫn chương trình Gala Dinner doanh nghiệp cuối năm. Tông giọng yêu cầu vui tươi, tràn đầy năng lượng.")
                .type(CompetitionType.DAILY)
                .challengeScriptId(secondLesson.getId())
                .startDate(Instant.now().minus(12, ChronoUnit.HOURS))
                .endDate(Instant.now().plus(12, ChronoUnit.HOURS))
                .active(true)
                .build());

        if (!usersToUse.isEmpty()) {
            User u0 = usersToUse.get(0);
            competitionRecordRepository.save(CompetitionRecord.builder()
                    .competitionId(weeklyArena.getId()).userId(u0.getId()).userName(u0.getName())
                    .userAvatar(u0.getAvatar())
                    .bestAccuracy(94.5).bestRhythm(92.0).practiceHours(2.4).attemptCount(6).pointsEarned(150.0)
                    .lastUpdated(Instant.now().minus(4, ChronoUnit.HOURS)).build());
        }
        if (usersToUse.size() > 1) {
            User u1 = usersToUse.get(1);
            competitionRecordRepository.save(CompetitionRecord.builder()
                    .competitionId(weeklyArena.getId()).userId(u1.getId()).userName(u1.getName())
                    .userAvatar(u1.getAvatar())
                    .bestAccuracy(91.2).bestRhythm(88.5).practiceHours(1.6).attemptCount(4).pointsEarned(120.0)
                    .lastUpdated(Instant.now().minus(2, ChronoUnit.HOURS)).build());
            competitionRecordRepository.save(CompetitionRecord.builder()
                    .competitionId(dailyArena.getId()).userId(u1.getId()).userName(u1.getName())
                    .userAvatar(u1.getAvatar())
                    .bestAccuracy(93.0).bestRhythm(91.5).practiceHours(1.8).attemptCount(5).pointsEarned(140.0)
                    .lastUpdated(Instant.now().minus(1, ChronoUnit.HOURS)).build());
        }
        if (usersToUse.size() > 2) {
            User u2 = usersToUse.get(2);
            competitionRecordRepository.save(CompetitionRecord.builder()
                    .competitionId(weeklyArena.getId()).userId(u2.getId()).userName(u2.getName())
                    .userAvatar(u2.getAvatar())
                    .bestAccuracy(88.0).bestRhythm(85.2).practiceHours(1.2).attemptCount(3).pointsEarned(95.0)
                    .lastUpdated(Instant.now().minus(10, ChronoUnit.HOURS)).build());
        }
        if (!usersToUse.isEmpty()) {
            User u0 = usersToUse.get(0);
            competitionRecordRepository.save(CompetitionRecord.builder()
                    .competitionId(dailyArena.getId()).userId(u0.getId()).userName(u0.getName())
                    .userAvatar(u0.getAvatar())
                    .bestAccuracy(90.5).bestRhythm(89.0).practiceHours(1.0).attemptCount(3).pointsEarned(110.0)
                    .lastUpdated(Instant.now().minus(3, ChronoUnit.HOURS)).build());
        }
        if (usersToUse.size() > 3) {
            User u3 = usersToUse.get(3);
            competitionRecordRepository.save(CompetitionRecord.builder()
                    .competitionId(dailyArena.getId()).userId(u3.getId()).userName(u3.getName())
                    .userAvatar(u3.getAvatar())
                    .bestAccuracy(87.2).bestRhythm(84.5).practiceHours(0.8).attemptCount(2).pointsEarned(90.0)
                    .lastUpdated(Instant.now().minus(5, ChronoUnit.HOURS)).build());
        }

        log.info("✅ Successfully seeded gamification users, stats and dual arenas");

        // ── Structured Courses + Milestone Courses ───────────────────
        log.info("🌱 Seeding Structured Courses & Milestone Courses...");
        courseRepository.deleteAll();
        enrollmentRepository.deleteAll();

        // ── Course 1: MC Đám Cưới ─────────────────────────────────────
        log.info("🌱 Seeding Course 1: MC Đám Cưới...");

        List<VoiceLesson> weddingLessons = lessonRepository.saveAll(Arrays.asList(
                VoiceLesson.builder()
                        .title("Khai mạc lễ vu quy – Lời chào đầu tiên")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Easy")
                        .description(
                                "Thực hành lời khai mạc trang trọng và ấm áp cho lễ vu quy theo phong tục Việt Nam.")
                        .content(
                                "Kính thưa quý vị quan khách, thưa hai họ!\nHôm nay, trong không khí ấm cúng và tràn đầy tình cảm, chúng ta cùng hội tụ về đây để chứng kiến một sự kiện trọng đại trong cuộc đời của đôi uyên ương. Thay mặt gia đình hai bên, tôi – người dẫn chương trình – xin trân trọng tuyên bố khai mạc lễ vu quy của gia đình. Xin kính mời toàn thể quý vị cùng lắng nghe trong tiếng nhạc hòa tấu, để những khoảnh khắc đẹp nhất của ngày hôm nay được khắc sâu trong tâm trí mỗi người.")
                        .build(),
                VoiceLesson.builder()
                        .title("Giới thiệu đại diện hai họ")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Easy")
                        .description("Luyện cách giới thiệu tên, chức danh và quan hệ của đại diện gia đình hai bên.")
                        .content(
                                "Kính thưa quý vị! Để bắt đầu buổi lễ, tôi xin trân trọng giới thiệu những vị đại diện cao quý của hai gia đình. Về phía gia đình nhà trai, chúng ta hân hạnh chào đón ông bà và các cô chú đã không quản đường xa về chung vui. Về phía gia đình nhà gái, chúng ta xin kính chào đến ông bà chủ nhà cùng toàn thể họ hàng thân thuộc. Xin kính mời quý vị dành một tràng pháo tay nồng nhiệt để chào đón các đại diện hai gia đình.")
                        .build(),
                VoiceLesson.builder()
                        .title("Dẫn lễ dạm ngõ – Trao trầu cau")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Easy")
                        .description("Thực hành dẫn nghi thức trao trầu cau và lễ vật theo truyền thống.")
                        .content(
                                "Theo phong tục truyền thống của người Việt, nghi lễ trao trầu cau là cầu nối thiêng liêng giữa hai gia đình. Kính mời đại diện gia đình nhà trai trao lễ vật gồm cau trầu, rượu và bánh phu thê đến gia đình nhà gái. Mâm lễ hôm nay được chuẩn bị chu đáo, thể hiện lòng thành kính và mong muốn xây dựng mối quan hệ bền vững giữa hai họ. Xin kính mời đại diện gia đình nhà gái nhận lễ vật trong sự chứng kiến của toàn thể quý vị.")
                        .build(),
                VoiceLesson.builder()
                        .title("Nghi thức trao nhẫn – Lời trao gửi tình yêu")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Medium")
                        .description("Dẫn dắt khoảnh khắc trao nhẫn với giọng điệu cảm xúc và lãng mạn.")
                        .content(
                                "Thưa quý vị, đây là khoảnh khắc đặc biệt nhất, thiêng liêng nhất của ngày hôm nay. Chiếc nhẫn tròn không có điểm bắt đầu hay kết thúc – biểu tượng cho tình yêu vĩnh cửu, không giới hạn. Kính mời chú rể đặt nhẫn lên ngón tay áp út của cô dâu và đọc lời thề hứa từ trái tim. Và giờ đây, xin kính mời cô dâu đáp lại tình cảm chân thành đó. Cả hội trường lắng đọng trong khoảnh khắc thiêng liêng không thể quên này.")
                        .build(),
                VoiceLesson.builder()
                        .title("Kính mời cha mẹ lên sân khấu – Lễ ra mắt")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Medium")
                        .description("Dẫn nghi thức ra mắt cha mẹ hai bên với ngôn từ trang trọng và cảm xúc.")
                        .content(
                                "Thưa quý vị kính mến! Có những người đã hi sinh cả cuộc đời để cho đôi trẻ có ngày hôm nay – đó chính là cha và mẹ. Xin kính mời song thân gia đình nhà trai và nhà gái cùng lên sân khấu. Đây là khoảnh khắc xúc động nhất khi hai gia đình chính thức trở thành một, khi những bậc sinh thành nhìn nhau qua ánh mắt hạnh phúc, biết rằng con cái của mình đã tìm được bến đỗ bình yên. Xin kính mời quý vị một tràng vỗ tay thật nồng nhiệt.")
                        .build(),
                VoiceLesson.builder()
                        .title("Phát biểu chúc mừng từ đại diện hai họ")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Medium")
                        .description("Kỹ năng dẫn dắt và chuyển tiếp các bài phát biểu từ đại diện gia đình.")
                        .content(
                                "Kính thưa quý vị! Trong không khí hân hoan của ngày vui trọng đại, chúng ta hân hạnh được lắng nghe những lời chúc tốt đẹp từ đại diện hai gia đình. Xin trân trọng kính mời ông – đại diện gia đình nhà trai – lên phát biểu đôi lời chúc mừng. Sau đó, chúng ta cùng lắng nghe ý kiến từ phía gia đình nhà gái. Những lời nói hôm nay sẽ trở thành hành trang quý giá theo đôi trẻ suốt hành trình hôn nhân phía trước.")
                        .build(),
                VoiceLesson.builder()
                        .title("Cắt bánh cưới và rót rượu champagne")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Medium")
                        .description("Dẫn các nghi thức hiện đại như cắt bánh và rót champagne với sự hứng khởi.")
                        .content(
                                "Và bây giờ, một trong những nghi thức thú vị nhất của đêm tiệc – lễ cắt bánh cưới! Chiếc bánh nhiều tầng này không chỉ là một tác phẩm nghệ thuật ẩm thực mà còn là biểu tượng cho những tầng hạnh phúc chồng chất trong cuộc sống vợ chồng. Xin kính mời cô dâu chú rể cùng nắm tay nhau thực hiện nhát cắt đầu tiên. Sau đó, kính mời hai bạn nâng ly champagne – những bong bóng nhỏ bé đang nhảy múa trong ánh đèn như tượng trưng cho niềm vui không tắt.")
                        .build(),
                VoiceLesson.builder()
                        .title("Xử lý sự cố kỹ thuật trong lễ cưới")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Hard")
                        .description(
                                "Kỹ thuật giữ bình tĩnh và ứng phó linh hoạt khi xảy ra sự cố âm thanh hoặc chậm trễ.")
                        .content(
                                "Thưa quý vị! Trong lúc chúng ta chờ đợi hệ thống âm thanh được kiểm tra lại, tôi xin phép chia sẻ một điều thú vị về đôi uyên ương của chúng ta. Đôi trẻ đã gặp nhau trong một hoàn cảnh rất đặc biệt – và câu chuyện tình yêu của họ chính là bằng chứng rằng duyên số luôn tìm được nhau dù có bất cứ trở ngại nào. Xin quý vị hãy thư giãn và thưởng thức thêm ít phút – chương trình sẽ tiếp tục ngay sau đây. Cảm ơn sự kiên nhẫn của quý vị!")
                        .build(),
                VoiceLesson.builder()
                        .title("Tiết mục văn nghệ và tương tác khán giả")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Hard")
                        .description("Dẫn các tiết mục văn nghệ và tổ chức minigame tương tác với khách mời.")
                        .content(
                                "Quý vị thân mến, chúng ta vừa có những khoảnh khắc đầy xúc cảm và trang trọng, giờ hãy cùng nhau tạo thêm những kỷ niệm vui vẻ! Tôi cần một số tình nguyện viên đặc biệt cho trò chơi tiếp theo. Ai đã từng có lần bị ngã vì quá hồi hộp khi gặp người mình yêu thích? Giơ tay nào! Tuyệt vời, chúng ta đang có những con người dũng cảm nhất trong hội trường. Cùng nhau làm cho đêm nay trở nên không thể nào quên nhé!")
                        .build(),
                VoiceLesson.builder()
                        .title("Kết thúc buổi lễ – Lời cảm ơn và tiễn khách")
                        .category(VoiceLessonCategory.WEDDING)
                        .difficulty("Easy")
                        .description("Dẫn phần kết thúc lễ cưới với lời cảm ơn chân thành và tiễn khách lịch sự.")
                        .content(
                                "Thưa quý vị kính mến! Thời gian trôi qua thật nhanh, buổi lễ hôn phối hôm nay đã đi đến những giây phút cuối cùng thật đẹp đẽ và trọn vẹn. Thay mặt gia đình hai bên, tôi xin gửi lời cảm ơn chân thành nhất đến toàn thể quý vị đã hiện diện và dành những tình cảm ấm áp nhất cho đôi trẻ. Chúc đôi uyên ương mãi mãi hạnh phúc bên nhau, trăm năm gắn bó, và gia đình luôn thuận hòa, sung túc. Xin kính chào và hẹn gặp lại quý vị trong những dịp vui sắp tới!")
                        .build()));

        List<ReadingGuide> weddingGuides = readingGuideRepository.saveAll(Arrays.asList(
                ReadingGuide.builder()
                        .title("Bí quyết dẫn lễ cưới: Từ vu quy đến tiệc cưới")
                        .content(
                                "# Bí Quyết Dẫn Lễ Cưới Việt Nam\n\n## Cấu trúc buổi lễ\nMột lễ cưới truyền thống Việt Nam thường gồm 3 phần chính: Lễ vu quy (tại nhà gái), Lễ thành hôn (tại nhà trai hoặc nhà hàng), và Tiệc cưới.\n\n## Nguyên tắc vàng\n1. **Tông giọng**: Ấm áp, trang trọng nhưng không cứng nhắc. Linh hoạt chuyển đổi giữa trang trọng và vui tươi.\n2. **Nhịp điệu**: Chậm rãi ở các phần nghi lễ, sôi động hơn ở phần tiệc.\n3. **Kiểm soát cảm xúc**: MC không được khóc theo khán giả – hãy là điểm tựa cảm xúc cho cả hội trường.\n\n## Xử lý tình huống\n- Trễ giờ: Pha trò hoặc kể câu chuyện về đôi trẻ để giữ không khí\n- Micro hỏng: Nói to hơn, di chuyển gần khán giả, báo kỹ thuật viên ngay\n- Khách say rượu gây ồn: Dùng humour nhẹ nhàng để điều hướng sự chú ý")
                        .author("Chuyên gia lễ cưới Nguyễn Hà Anh")
                        .category("WEDDING")
                        .thumbnail("https://images.unsplash.com/photo-1519741497674-611481863552?w=400")
                        .build(),
                ReadingGuide.builder()
                        .title("Ngôn ngữ và phong tục trong đám cưới 3 miền")
                        .content(
                                "# Đặc Thù Đám Cưới Ba Miền Việt Nam\n\n## Miền Bắc\n- Lễ nghi trang trọng, nhiều nghi thức truyền thống\n- Giọng điệu dẫn chương trình: chậm rãi, thanh cao, chuẩn mực\n- Từ ngữ: 'Kính thưa', 'trân trọng', 'hân hạnh'\n\n## Miền Trung\n- Phong tục đặc sắc, âm nhạc dân gian\n- Cần tìm hiểu phong tục địa phương trước khi dẫn\n- Tránh những điều kiêng kỵ vùng miền\n\n## Miền Nam\n- Không khí vui vẻ, phóng khoáng, thân thiện\n- Khán giả thích tương tác và minigame\n- Ngôn ngữ gần gũi, hài hước nhẹ nhàng được ưa chuộng\n\n## Lưu ý khi dẫn đám cưới hỗn hợp văn hóa\nLuôn xác nhận với gia đình về phong tục ưu tiên và điều chỉnh kịch bản phù hợp.")
                        .author("MC Trần Minh Hoàng")
                        .category("WEDDING")
                        .thumbnail("https://images.unsplash.com/photo-1522673607200-164d1b6ce486?w=400")
                        .build(),
                ReadingGuide.builder()
                        .title("Kịch bản mẫu: Lễ thành hôn tại nhà hàng")
                        .content(
                                "# Kịch Bản Mẫu Lễ Thành Hôn\n\n## Phần 1: Đón khách & Khai mạc (19:00 – 19:30)\n- Nhạc đệm nhẹ nhàng khi khách vào\n- MC giới thiệu bản thân và chào đón\n- Trình chiếu video về đôi uyên ương\n\n## Phần 2: Nghi lễ chính (19:30 – 20:15)\n- Dẫn đám đông đứng chào cô dâu chú rể bước vào\n- Giới thiệu đại diện hai họ phát biểu\n- Nghi thức trao nhẫn\n- Ký hôn thú\n\n## Phần 3: Tiệc & Văn nghệ (20:15 – 22:00)\n- Mời bàn tiệc\n- Chương trình văn nghệ xen kẽ\n- Minigame tương tác\n- Cắt bánh, rót rượu\n- Cảm ơn và kết thúc\n\n## Tips chuyên nghiệp\nLuôn có kịch bản dự phòng và kiểm tra kỹ lịch trình với ban tổ chức trước 2 giờ.")
                        .author("MCHub Elite Training")
                        .category("WEDDING")
                        .thumbnail("https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=400")
                        .build()));

        List<String> weddingLessonIds = weddingLessons.stream()
                .map(VoiceLesson::getId).collect(java.util.stream.Collectors.toList());
        List<String> weddingGuideIds = weddingGuides.stream()
                .map(ReadingGuide::getId).collect(java.util.stream.Collectors.toList());

        courseRepository.save(Course.builder()
                .title("MC Đám Cưới")
                .shortDescription("Làm chủ nghệ thuật dẫn chương trình đám cưới từ lễ vu quy đến tiệc cưới.")
                .description(
                        "Khóa học toàn diện cho MC đám cưới Việt Nam. Bao gồm kỹ thuật dẫn lễ vu quy, lễ thành hôn, điều phối phần trao nhẫn, giới thiệu quan khách và xử lý tình huống bất ngờ. Hoàn thành 10 bài luyện giọng, 3 bài đọc và bài kiểm tra để nhận chứng chỉ MC Đám Cưới.")
                .slug("mc-dam-cuoi")
                .type(CourseType.WEDDING_MC)
                .learningPathType(LearningPathType.STRUCTURED_COURSE)
                .thumbnail("https://images.unsplash.com/photo-1519741497674-611481863552?w=800")
                .difficulty("BEGINNER")
                .estimatedHours(10)
                .lessonIds(weddingLessonIds)
                .readingIds(weddingGuideIds)
                .passingScore(70)
                .isActive(true)
                .quizQuestions(Arrays.asList(
                        Course.QuizQuestion.builder()
                                .question(
                                        "Trong lễ vu quy truyền thống Việt Nam, MC nên sử dụng tông giọng như thế nào?")
                                .options(Arrays.asList("Vui nhộn, hài hước để tạo không khí sôi động",
                                        "Trang trọng, ấm áp và cảm xúc phù hợp với nghi lễ thiêng liêng",
                                        "Nhanh và gọn để tiết kiệm thời gian",
                                        "Nghiêm túc và lạnh lùng như hội nghị doanh nghiệp"))
                                .correctIndex(1)
                                .explanation(
                                        "Lễ vu quy là nghi thức thiêng liêng, cần tông giọng trang trọng nhưng vẫn mang hơi ấm cảm xúc để kết nối với hai gia đình.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Khi hệ thống âm thanh gặp sự cố trong đám cưới, MC nên làm gì đầu tiên?")
                                .options(Arrays.asList("Dừng chương trình và chờ kỹ thuật viên sửa xong",
                                        "Giữ bình tĩnh, dùng câu chuyện về đôi trẻ để lấp thời gian trong lúc chờ sửa",
                                        "Xin lỗi liên tục và tỏ ra lo lắng để khán giả thông cảm",
                                        "Kết thúc buổi lễ sớm hơn kế hoạch"))
                                .correctIndex(1)
                                .explanation(
                                        "Giữ bình tĩnh là ưu tiên số một. Kể chuyện về đôi trẻ hoặc pha trò nhẹ nhàng giúp duy trì không khí trong khi sự cố được xử lý.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Nghi thức trao nhẫn trong đám cưới có ý nghĩa biểu tượng gì?")
                                .options(Arrays.asList("Thể hiện giá trị tài chính của gia đình",
                                        "Chiếc nhẫn tròn tượng trưng cho tình yêu vĩnh cửu, không có điểm bắt đầu hay kết thúc",
                                        "Đây chỉ là thủ tục hành chính trong hôn nhân",
                                        "Nhẫn thể hiện quyền sở hữu trong hôn nhân"))
                                .correctIndex(1)
                                .explanation(
                                        "Chiếc nhẫn cưới hình tròn là biểu tượng của tình yêu trọn vẹn và vĩnh cửu – đây là thông điệp MC cần truyền tải trong khoảnh khắc trao nhẫn.")
                                .category("THEORY")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Khi giới thiệu đại diện hai họ, MC cần cung cấp thông tin gì?")
                                .options(Arrays.asList("Chỉ cần nêu tên, không cần chức danh hay quan hệ",
                                        "Tên, mối quan hệ với gia đình và đôi khi chức danh xã hội nếu phù hợp",
                                        "Tuổi tác và thành tích học tập của từng người",
                                        "Chỉ giới thiệu những người có chức vụ quan trọng"))
                                .correctIndex(1)
                                .explanation(
                                        "Giới thiệu đầy đủ tên, mối quan hệ và chức danh (nếu phù hợp) thể hiện sự tôn trọng và giúp khán giả hiểu rõ vai trò của từng người.")
                                .category("ETIQUETTE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question(
                                        "Trong đám cưới miền Nam Việt Nam, phong cách dẫn chương trình thường như thế nào?")
                                .options(Arrays.asList("Nghiêm túc, ít tương tác như đám cưới miền Bắc",
                                        "Vui vẻ, phóng khoáng, khán giả thích tương tác và minigame",
                                        "Chỉ đọc kịch bản, không ứng xử linh hoạt",
                                        "Dùng nhiều từ Hán Việt và ngôn ngữ cổ điển"))
                                .correctIndex(1)
                                .explanation(
                                        "Đám cưới miền Nam có không khí thân thiện, cởi mở. Khán giả hào hứng tham gia tương tác, minigame vui nhộn được đón nhận tích cực.")
                                .category("THEORY")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("MC nên làm gì khi có khách mời bắt đầu gây ồn hoặc không chú ý?")
                                .options(Arrays.asList("Nhắc nhở thẳng thắn và gay gắt trước toàn hội trường",
                                        "Dùng humour nhẹ nhàng hoặc câu hỏi tương tác để điều hướng sự chú ý",
                                        "Bỏ qua hoàn toàn và tiếp tục kịch bản",
                                        "Dừng chương trình cho đến khi hội trường yên lặng"))
                                .correctIndex(1)
                                .explanation(
                                        "Humour nhẹ nhàng và tương tác là cách chuyên nghiệp để điều hướng sự chú ý mà không tạo ra tình huống xấu hổ cho ai.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Trước khi dẫn đám cưới, MC nên làm gì ít nhất 2 giờ trước khi bắt đầu?")
                                .options(Arrays.asList("Chỉ cần đọc lại kịch bản một lần",
                                        "Kiểm tra lịch trình với ban tổ chức, thử âm thanh và gặp gỡ đại diện hai gia đình",
                                        "Ăn uống no đủ để có năng lượng dẫn chương trình",
                                        "Đến đúng giờ khai mạc là đủ"))
                                .correctIndex(1)
                                .explanation(
                                        "Chuẩn bị kỹ càng trước sự kiện giúp MC nắm rõ lịch trình, xử lý vấn đề kỹ thuật và hiểu mong muốn của gia đình.")
                                .category("ETIQUETTE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Phát âm chuẩn từ 'hôn nhân' trong tiếng Việt miền Bắc là gì?")
                                .options(Arrays.asList("Nhấn mạnh âm 'hôn' và đọc nhẹ 'nhân'",
                                        "Đọc đều hai âm tiết, 'hôn' thanh bằng, 'nhân' thanh ngang",
                                        "Kéo dài âm 'nhân' để tạo cảm xúc", "Đọc nhanh như một từ đơn"))
                                .correctIndex(1)
                                .explanation(
                                        "Phát âm chuẩn 'hôn nhân' cần đọc đều hai âm tiết với đúng thanh điệu: 'hôn' (thanh bằng) và 'nhân' (thanh ngang).")
                                .category("PRONUNCIATION")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Khi dẫn nghi thức cắt bánh cưới, MC nên tạo không khí như thế nào?")
                                .options(Arrays.asList("Giải thích chi tiết thành phần và cách làm bánh",
                                        "Tạo không khí hào hứng, ý nghĩa – bánh nhiều tầng biểu tượng cho hạnh phúc chồng chất",
                                        "Chỉ thông báo ngắn gọn 'Mời cô dâu chú rể cắt bánh'",
                                        "Đọc thơ dài để tăng tính trang trọng"))
                                .correctIndex(1)
                                .explanation(
                                        "Nghi thức cắt bánh là khoảnh khắc vui nhộn – MC nên xây dựng không khí hứng khởi và kết nối ý nghĩa biểu tượng của chiếc bánh với tình yêu đôi trẻ.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Lời kết thúc buổi lễ cưới cần đảm bảo những yếu tố gì?")
                                .options(Arrays.asList("Cảm ơn ngắn gọn và nhanh chóng kết thúc",
                                        "Cảm ơn chân thành đến khách mời, chúc phúc đôi trẻ và mời khách ở lại tiệc",
                                        "Tổng kết toàn bộ chương trình đã diễn ra", "Chỉ cần thông báo giờ kết thúc"))
                                .correctIndex(1)
                                .explanation(
                                        "Lời kết cần có 3 thành phần: cảm ơn khách mời, lời chúc phúc ý nghĩa cho đôi trẻ, và lời mời tiếp tục thưởng thức tiệc – tạo cảm giác trọn vẹn.")
                                .category("ETIQUETTE")
                                .build()))
                .build());
        log.info("✅ Seeded Course 1: MC Đám Cưới");

        // ── Course 2: Sự Kiện Doanh Nghiệp ──────────────────────────
        log.info("🌱 Seeding Course 2: Sự Kiện Doanh Nghiệp...");

        List<VoiceLesson> corporateLessons = lessonRepository.saveAll(Arrays.asList(
                VoiceLesson.builder()
                        .title("Khai mạc hội nghị cấp cao – Tông giọng chuyên nghiệp")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Medium")
                        .description("Luyện tập lời khai mạc hội nghị với phong cách lịch sự, tự tin và thẩm quyền.")
                        .content(
                                "Kính thưa Hội đồng quản trị, Ban điều hành và toàn thể quý vị đại biểu!\nThay mặt Ban tổ chức, tôi trân trọng tuyên bố khai mạc Hội nghị Thường niên năm nay. Đây là diễn đàn quan trọng để chúng ta cùng nhìn lại những thành tựu đã đạt được, đánh giá thực trạng và định hướng chiến lược cho những năm tiếp theo. Sự hiện diện của quý vị chính là minh chứng cho cam kết phát triển bền vững của toàn tổ chức. Tôi tin tưởng rằng những thảo luận hôm nay sẽ tạo ra những giá trị thiết thực cho sự phát triển chung.")
                        .build(),
                VoiceLesson.builder()
                        .title("Giới thiệu diễn giả và chuyên gia")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Medium")
                        .description("Kỹ năng giới thiệu diễn giả với thông tin đầy đủ, chính xác và ấn tượng.")
                        .content(
                                "Kính thưa quý vị! Để bắt đầu phiên làm việc đầu tiên, tôi trân trọng giới thiệu diễn giả đặc biệt của chúng ta. Ông/bà là chuyên gia hàng đầu trong lĩnh vực quản trị doanh nghiệp với hơn hai mươi năm kinh nghiệm thực tiễn. Với vai trò Tổng Giám đốc điều hành tại nhiều tập đoàn lớn trong khu vực, diễn giả đã dẫn dắt thành công nhiều dự án chuyển đổi chiến lược mang tầm quốc tế. Xin kính mời quý vị dành tràng pháo tay nồng nhiệt để chào đón diễn giả lên chia sẻ.")
                        .build(),
                VoiceLesson.builder()
                        .title("Dẫn phiên thảo luận bàn tròn")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Hard")
                        .description(
                                "Kỹ năng điều phối thảo luận, quản lý thời gian và đặt câu hỏi trong panel discussion.")
                        .content(
                                "Chúng ta đã nghe những quan điểm sâu sắc từ các diễn giả. Bây giờ, tôi muốn mở rộng cuộc thảo luận và tạo cơ hội cho mọi người cùng chia sẻ. Câu hỏi đầu tiên tôi muốn đặt ra là: trong bối cảnh chuyển đổi số đang diễn ra mạnh mẽ, đâu là yếu tố then chốt giúp doanh nghiệp duy trì lợi thế cạnh tranh? Xin mời diễn giả bên trái cho ý kiến trước, sau đó chúng ta sẽ nghe phản hồi từ các bên còn lại. Chúng ta có khoảng 15 phút cho phần thảo luận này.")
                        .build(),
                VoiceLesson.builder()
                        .title("Khai mạc Gala Dinner cuối năm")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Medium")
                        .description("Dẫn gala dinner với phong cách lịch sự, sang trọng và tạo không khí vui tươi.")
                        .content(
                                "Kính thưa toàn thể quý vị! Chào mừng quý vị đến với Dạ tiệc Gala Dinner Tổng kết năm – đêm hội đặc biệt nhất trong năm của chúng ta. Năm qua là một hành trình đầy thách thức nhưng cũng tràn đầy tự hào. Mỗi thành tích chúng ta đạt được là kết quả của sự cống hiến không ngừng nghỉ từ từng thành viên trong đại gia đình của chúng ta. Tối nay, hãy cùng nhau tạm gác bỏ những áp lực công việc, để tận hưởng một buổi tối ấm áp bên những đồng nghiệp, những người bạn đồng hành đáng quý.")
                        .build(),
                VoiceLesson.builder()
                        .title("Lễ trao giải thưởng nhân viên xuất sắc")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Medium")
                        .description("Dẫn các phần trao giải với sự trang trọng, ghi nhận đúng giá trị đóng góp.")
                        .content(
                                "Và bây giờ, chúng ta đến với khoảnh khắc được mong chờ nhất của buổi tối – lễ vinh danh những cá nhân và tập thể xuất sắc nhất năm qua. Những gương mặt được xướng tên tối nay là đại diện cho tinh thần cống hiến, sáng tạo và không ngừng vươn lên của toàn thể cán bộ nhân viên. Xin kính mời Tổng Giám đốc lên trao giải thưởng 'Nhân Viên của Năm'. Và người vinh dự nhận giải thưởng cao quý này là... Xin chúc mừng!")
                        .build(),
                VoiceLesson.builder()
                        .title("Lễ ra mắt sản phẩm mới")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Hard")
                        .description("Dẫn sự kiện ra mắt sản phẩm với sự hứng khởi, tạo kỳ vọng và ấn tượng mạnh.")
                        .content(
                                "Thưa quý vị! Sau nhiều tháng nghiên cứu, phát triển và hoàn thiện, hôm nay chúng tôi vô cùng tự hào được công bố sự ra đời của một sản phẩm sẽ thay đổi cách chúng ta tiếp cận vấn đề này. Đây không chỉ là một sản phẩm mới – đây là kết tinh của tầm nhìn, công nghệ đỉnh cao và sự thấu hiểu sâu sắc nhu cầu của khách hàng. Xin mời quý vị cùng đếm ngược và chứng kiến khoảnh khắc lịch sử: 3... 2... 1... Trân trọng giới thiệu đến quý vị!")
                        .build(),
                VoiceLesson.builder()
                        .title("Điều phối Q&A với lãnh đạo cấp cao")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Hard")
                        .description("Kỹ năng điều phối phiên hỏi đáp, chắt lọc câu hỏi và quản lý thời gian hiệu quả.")
                        .content(
                                "Chúng ta sẽ bước vào phần được nhiều người mong đợi – phiên hỏi đáp trực tiếp với Ban lãnh đạo. Tôi xin nhắc nhở quý vị: mỗi câu hỏi nên được trình bày ngắn gọn, tập trung vào một vấn đề cụ thể để chúng ta có thể giải quyết được nhiều thắc mắc nhất trong thời gian cho phép. Ai có câu hỏi đầu tiên? Xin mời vị khách ở phía bên trái... Đây là câu hỏi rất thực tế. Kính mời Giám đốc Chiến lược cho ý kiến về vấn đề này.")
                        .build(),
                VoiceLesson.builder()
                        .title("Kết nối mạng lưới – Giới thiệu trong networking event")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Easy")
                        .description("Kỹ năng dẫn các hoạt động kết nối và giới thiệu trong sự kiện networking.")
                        .content(
                                "Thưa quý vị, đây là cơ hội tuyệt vời để chúng ta kết nối và mở rộng mạng lưới quan hệ chuyên nghiệp. Trong 30 phút tiếp theo, tôi mời quý vị tham gia vào các cuộc trò chuyện có chủ đích. Tôi sẽ giới thiệu một vài kỹ thuật đơn giản để mở đầu cuộc trò chuyện hiệu quả: bắt đầu bằng câu hỏi mở về dự án hiện tại, sau đó tìm điểm chung để xây dựng kết nối. Hãy nhớ rằng, mỗi mối quan hệ ở đây đều có tiềm năng trở thành sự hợp tác có giá trị.")
                        .build(),
                VoiceLesson.builder()
                        .title("Xử lý tình huống khó trong hội nghị")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Hard")
                        .description("Ứng phó với câu hỏi nhạy cảm, tranh luận nóng và các tình huống bất ngờ.")
                        .content(
                                "Trong các hội nghị lớn, đôi khi xuất hiện những câu hỏi nhạy cảm hoặc tranh luận vượt ngoài kế hoạch. Với tư cách MC, nhiệm vụ của chúng ta là giữ cuộc thảo luận ở mức xây dựng và chuyên nghiệp. Khi một vị khách đặt câu hỏi có tính chất chỉ trích, tôi xin phép diễn giải câu hỏi theo hướng tích cực hơn để chúng ta có thể thảo luận một cách hiệu quả. Cảm ơn vị khách đã nêu lên một điểm quan trọng – đây chính là điều mà chúng ta cần cùng nhau tìm ra giải pháp.")
                        .build(),
                VoiceLesson.builder()
                        .title("Tổng kết và bế mạc hội nghị")
                        .category(VoiceLessonCategory.CORPORATE)
                        .difficulty("Medium")
                        .description("Tóm lược kết quả hội nghị và kết thúc sự kiện một cách chuyên nghiệp.")
                        .content(
                                "Kính thưa quý vị! Chúng ta đã trải qua một ngày làm việc đầy hiệu quả và ý nghĩa. Những thảo luận sâu sắc, những cam kết mạnh mẽ được đưa ra trong hôm nay là nền tảng để chúng ta bước vào giai đoạn mới với niềm tin và quyết tâm cao nhất. Thay mặt Ban tổ chức, tôi xin trân trọng cảm ơn toàn thể quý vị diễn giả, đại biểu và các đối tác đã dành thời gian quý báu tham dự. Đây là sự kiện thành công nhờ sự đóng góp của tất cả mọi người. Xin tuyên bố bế mạc Hội nghị và kính chúc quý vị sức khỏe, thành công!")
                        .build()));

        List<ReadingGuide> corporateGuides = readingGuideRepository.saveAll(Arrays.asList(
                ReadingGuide.builder()
                        .title("Nghệ thuật dẫn hội nghị cấp cao")
                        .content(
                                "# Nghệ Thuật Dẫn Hội Nghị Cấp Cao\n\n## Nguyên tắc cốt lõi\n1. **Thẩm quyền**: Giọng điệu tự tin, dứt khoát nhưng không áp đặt\n2. **Kiến thức nền**: Hiểu về ngành nghề của tổ chức để đặt câu hỏi thông minh\n3. **Quản lý thời gian**: Tuân thủ lịch trình nghiêm ngặt\n\n## Kỹ thuật dẫn panel discussion\n- Chuẩn bị 5-7 câu hỏi dự phòng cho mỗi chủ đề\n- Đặt câu hỏi mở, tránh câu hỏi Yes/No\n- Biết khi nào cần ngắt lời để đảm bảo cân bằng thời gian\n- Tóm tắt ý kiến trước khi chuyển sang câu hỏi tiếp theo\n\n## Ăn mặc và ngôn ngữ cơ thể\n- Trang phục business formal (đen/xanh navy)\n- Đứng thẳng, nhìn thẳng vào khán giả\n- Không chạm tóc hoặc mặt khi đang nói")
                        .author("MC Lê Văn Đức – Chuyên gia sự kiện doanh nghiệp")
                        .category("CORPORATE")
                        .thumbnail("https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=400")
                        .build(),
                ReadingGuide.builder()
                        .title("Giao thức quốc tế trong sự kiện doanh nghiệp đa quốc gia")
                        .content(
                                "# Giao Thức Quốc Tế Trong Sự Kiện Doanh Nghiệp\n\n## Thứ tự ưu tiên khi giới thiệu\n1. Chức vụ cao nhất trước\n2. Khách quốc tế được ưu tiên giới thiệu đầu\n3. Người cao tuổi được giới thiệu trước người trẻ ở cùng cấp bậc\n\n## Bilingua hosting\n- Sử dụng Tiếng Anh trước khi dịch sang Tiếng Việt\n- Tốc độ nói chậm hơn 20% so với bình thường\n- Tránh idioms và slang khó dịch\n\n## Nhạy cảm văn hóa\n- Tìm hiểu trước về quốc tịch và văn hóa của diễn giả quốc tế\n- Tránh hài hước về chính trị, tôn giáo\n- Khi nghi ngờ về phát âm tên nước ngoài, hỏi trực tiếp diễn giả trước buổi lễ")
                        .author("International Events Protocol Institute")
                        .category("CORPORATE")
                        .thumbnail("https://images.unsplash.com/photo-1560439514-4e9645039924?w=400")
                        .build(),
                ReadingGuide.builder()
                        .title("Kịch bản mẫu: Gala Dinner doanh nghiệp cuối năm")
                        .content(
                                "# Kịch Bản Mẫu Gala Dinner Doanh Nghiệp\n\n## Timeline\n- 18:00: Đón khách, cocktail reception\n- 19:00: Khai mạc chính thức\n- 19:15: Phát biểu của Ban Lãnh đạo\n- 19:45: Trao giải thưởng\n- 20:30: Tiệc tối, văn nghệ xen kẽ\n- 22:00: Bế mạc\n\n## Kịch bản dẫn khai mạc\n'Kính thưa Hội đồng quản trị, Ban điều hành và toàn thể gia đình [Tên công ty]! Chào mừng quý vị đến với đêm hội đặc biệt...'\n\n## Kịch bản trao giải\n'Và người vinh dự nhận giải thưởng [Tên giải] năm nay là...'\n\n## Lưu ý quan trọng\nLuôn xác nhận lại danh sách người nhận giải với HR trước buổi lễ tối thiểu 1 ngày. Phát âm sai tên nhân viên là lỗi nghiêm trọng không thể chấp nhận.")
                        .author("MCHub Corporate Training")
                        .category("CORPORATE")
                        .thumbnail("https://images.unsplash.com/photo-1551818255-e6e10975bc17?w=400")
                        .build()));

        List<String> corporateLessonIds = corporateLessons.stream()
                .map(VoiceLesson::getId).collect(java.util.stream.Collectors.toList());
        List<String> corporateGuideIds = corporateGuides.stream()
                .map(ReadingGuide::getId).collect(java.util.stream.Collectors.toList());

        courseRepository.save(Course.builder()
                .title("Sự Kiện Doanh Nghiệp")
                .shortDescription(
                        "Kỹ năng dẫn chương trình chuyên nghiệp cho hội nghị, gala dinner và lễ ra mắt sản phẩm.")
                .description(
                        "Phát triển kỹ năng MC doanh nghiệp đẳng cấp: dẫn hội nghị cấp cao, gala dinner cuối năm, lễ trao giải thưởng và ra mắt sản phẩm. Tập trung vào phong cách dẫn chương trình trang trọng, xử lý thời gian và tương tác với doanh nhân cấp cao.")
                .slug("su-kien-doanh-nghiep")
                .type(CourseType.CORPORATE_EVENT)
                .learningPathType(LearningPathType.STRUCTURED_COURSE)
                .thumbnail("https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800")
                .difficulty("INTERMEDIATE")
                .estimatedHours(12)
                .lessonIds(corporateLessonIds)
                .readingIds(corporateGuideIds)
                .passingScore(70)
                .isActive(true)
                .quizQuestions(Arrays.asList(
                        Course.QuizQuestion.builder()
                                .question("Khi giới thiệu diễn giả trong hội nghị quốc tế, thứ tự ưu tiên là gì?")
                                .options(Arrays.asList("Giới thiệu theo thứ tự bảng chữ cái tên",
                                        "Chức vụ cao nhất trước, khách quốc tế được ưu tiên giới thiệu đầu",
                                        "Giới thiệu theo thứ tự ngồi trên sân khấu",
                                        "Người trẻ tuổi nhất được giới thiệu đầu tiên"))
                                .correctIndex(1)
                                .explanation(
                                        "Giao thức quốc tế quy định giới thiệu theo thứ bậc chức vụ, với khách quốc tế được ưu tiên để thể hiện sự tôn trọng.")
                                .category("ETIQUETTE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Trong phiên Q&A tại hội nghị, MC nên xử lý câu hỏi nhạy cảm như thế nào?")
                                .options(Arrays.asList("Từ chối không cho hỏi và chuyển sang câu hỏi khác",
                                        "Diễn giải câu hỏi theo hướng xây dựng để tạo thảo luận tích cực",
                                        "Trả lời thẳng câu hỏi mà không qua diễn giả", "Ngắt micro của người hỏi"))
                                .correctIndex(1)
                                .explanation(
                                        "MC có vai trò điều phối – kỹ năng 'reframe' câu hỏi nhạy cảm thành câu hỏi mang tính xây dựng giúp duy trì tính chuyên nghiệp của hội nghị.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Tốc độ nói lý tưởng khi dẫn song ngữ (bilingual) trong hội nghị quốc tế là?")
                                .options(Arrays.asList("Nói nhanh hơn 20% để tiết kiệm thời gian",
                                        "Chậm hơn 20% so với bình thường để người nghe kịp theo dõi",
                                        "Giữ nguyên tốc độ bình thường",
                                        "Nói nhanh khi tiếng Việt, chậm khi tiếng Anh"))
                                .correctIndex(1)
                                .explanation(
                                        "Khi dẫn song ngữ, giảm tốc độ nói khoảng 20% giúp người nghe không phải liên tục diễn giải, đặc biệt với khán giả không thành thạo ngôn ngữ.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Trang phục phù hợp nhất cho MC dẫn hội nghị doanh nghiệp cấp cao là?")
                                .options(Arrays.asList("Trang phục sặc sỡ, nổi bật để thu hút chú ý",
                                        "Business formal – vest đen hoặc xanh navy, trang nhã và lịch sự",
                                        "Casual jeans và áo polo để thể hiện sự gần gũi",
                                        "Áo dài truyền thống trong mọi hoàn cảnh"))
                                .correctIndex(1)
                                .explanation(
                                        "Business formal (vest tối màu) là chuẩn mực trong môi trường doanh nghiệp cấp cao – thể hiện sự tôn trọng và chuyên nghiệp.")
                                .category("ETIQUETTE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Khi dẫn lễ ra mắt sản phẩm, yếu tố nào quan trọng nhất để tạo ấn tượng?")
                                .options(Arrays.asList("Đọc đầy đủ thông số kỹ thuật của sản phẩm",
                                        "Xây dựng kỳ vọng và tạo khoảnh khắc reveal đầy cảm xúc",
                                        "Giải thích giá cả và chính sách khuyến mãi", "Trình chiếu slide thật nhiều"))
                                .correctIndex(1)
                                .explanation(
                                        "Lễ ra mắt sản phẩm thành công phụ thuộc vào cách MC xây dựng anticipation (kỳ vọng) và tạo ra khoảnh khắc reveal đáng nhớ, không phải thông tin kỹ thuật.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Trong gala dinner, MC nên xử lý tình huống trễ giờ như thế nào?")
                                .options(Arrays.asList(
                                        "Thông báo thẳng thắn rằng chương trình bị trễ và xin lỗi nhiều lần",
                                        "Lấp thời gian bằng câu chuyện thú vị, trivia hoặc nhạc nền mà không làm lộ sự trễ giờ",
                                        "Bỏ bớt một số tiết mục trong kịch bản", "Mời khán giả ra ngoài chờ"))
                                .correctIndex(1)
                                .explanation(
                                        "MC chuyên nghiệp 'mua thời gian' một cách tự nhiên thay vì để khán giả nhận thấy sự chậm trễ, giữ không khí tích cực trong suốt sự kiện.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question(
                                        "Khi điều phối panel discussion, số câu hỏi dự phòng nên chuẩn bị cho mỗi chủ đề là?")
                                .options(Arrays.asList("1-2 câu hỏi là đủ",
                                        "5-7 câu hỏi để đảm bảo có phương án dự phòng",
                                        "Không cần chuẩn bị trước, đặt câu hỏi ngẫu hứng", "Hơn 10 câu hỏi chi tiết"))
                                .correctIndex(1)
                                .explanation(
                                        "Chuẩn bị 5-7 câu hỏi dự phòng cho mỗi chủ đề giúp MC linh hoạt ứng phó khi thảo luận diễn ra khác kế hoạch hoặc cần đào sâu hơn.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Điều gì KHÔNG nên làm khi phát âm tên diễn giả nước ngoài?")
                                .options(Arrays.asList("Hỏi trực tiếp diễn giả về cách phát âm tên trước buổi lễ",
                                        "Đoán tùy tiện và phát âm theo ý mình mà không xác nhận",
                                        "Lắng nghe cách người tổ chức phát âm và học theo",
                                        "Yêu cầu ban tổ chức cung cấp phiên âm"))
                                .correctIndex(1)
                                .explanation(
                                        "Phát âm sai tên diễn giả là thiếu tôn trọng nghiêm trọng. Luôn xác nhận cách phát âm trước sự kiện bằng cách hỏi trực tiếp hoặc tra cứu.")
                                .category("ETIQUETTE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Trong hội nghị, tại sao MC không nên đặt câu hỏi Yes/No cho diễn giả?")
                                .options(Arrays.asList("Vì câu hỏi Yes/No quá dễ và ai cũng biết câu trả lời",
                                        "Vì câu hỏi Yes/No cắt ngắn thảo luận – câu hỏi mở tạo ra nhiều nội dung giá trị hơn",
                                        "Vì diễn giả không thích trả lời Yes/No",
                                        "Vì quy định của hội nghị không cho phép"))
                                .correctIndex(1)
                                .explanation(
                                        "Câu hỏi mở (What, How, Why) tạo không gian cho diễn giả chia sẻ sâu hơn, tạo ra nội dung có giá trị cho khán giả thay vì câu trả lời ngắn.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question(
                                        "Trước khi trao giải thưởng nhân viên, MC cần xác nhận điều gì với bộ phận HR?")
                                .options(Arrays.asList("Chỉ cần có danh sách tên là đủ",
                                        "Danh sách chính xác người nhận giải và cách phát âm đúng tên của họ",
                                        "Giá trị hiện kim của giải thưởng để thông báo cho khán giả",
                                        "Ảnh chụp của từng người nhận giải"))
                                .correctIndex(1)
                                .explanation(
                                        "Phát âm sai tên nhân viên trong buổi lễ vinh danh là lỗi không thể chấp nhận. Xác nhận danh sách và cách phát âm ít nhất 1 ngày trước là bắt buộc.")
                                .category("ETIQUETTE")
                                .build()))
                .build());
        log.info("✅ Seeded Course 2: Sự Kiện Doanh Nghiệp");

        // ── Course 3: MC Talkshow ─────────────────────────────────────
        log.info("🌱 Seeding Course 3: MC Talkshow...");

        List<VoiceLesson> talkshowLessons = lessonRepository.saveAll(Arrays.asList(
                VoiceLesson.builder()
                        .title("Khai mạc talkshow – Tạo sức hút ngay từ đầu")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Medium")
                        .description(
                                "Kỹ thuật mở đầu talkshow bằng câu hỏi hoặc câu chuyện gây tò mò, thu hút khán giả ngay lập tức.")
                        .content(
                                "Chào buổi tối! Câu hỏi của tôi dành cho tất cả mọi người tối nay: điều gì khiến bạn thức dậy mỗi buổi sáng với niềm hứng khởi, dù thế giới bên ngoài có đang xảy ra điều gì? Đó chính là câu hỏi mà khách mời của chúng ta tối nay đã tự hỏi bản thân mình hàng ngày trong suốt hành trình phi thường của họ. Tôi là [tên MC], và đây là chương trình [tên talkshow] – nơi những câu chuyện thật, những bài học sâu sắc và những cảm xúc chân thật được chia sẻ. Chúng ta bắt đầu nhé!")
                        .build(),
                VoiceLesson.builder()
                        .title("Giới thiệu khách mời – Nghệ thuật tạo hứng đợi")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Medium")
                        .description("Cách giới thiệu khách mời tạo sự tò mò và kỳ vọng trước khi họ xuất hiện.")
                        .content(
                                "Khách mời của chúng ta tối nay là người đã từ bỏ công việc thu nhập triệu đô để theo đuổi một giấc mơ mà nhiều người cho là điên rồ. Họ đã thất bại 7 lần trước khi đạt được thành công vang dội mà cả thế giới biết đến hôm nay. Nhưng điều thú vị nhất – họ nói rằng mỗi lần thất bại là một món quà. Quý vị có muốn biết bí mật đằng sau câu nói đó không? Xin nhiệt liệt chào đón vị khách đặc biệt của chương trình chúng ta tối nay!")
                        .build(),
                VoiceLesson.builder()
                        .title("Đặt câu hỏi sâu sắc – Kỹ thuật phỏng vấn")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Hard")
                        .description(
                                "Nghệ thuật đặt câu hỏi đào sâu, theo dõi câu trả lời và khai thác câu chuyện thú vị.")
                        .content(
                                "Bạn đã đề cập đến khoảnh khắc đó thay đổi tất cả. Tôi muốn chúng ta dừng lại một chút ở đây. Bởi vì khi bạn nói điều đó, tôi nhận ra có điều gì đó rất quan trọng đằng sau những từ đó. Cảm giác chính xác của bạn lúc ấy là gì – không phải những gì bạn nghĩ sau này khi nhìn lại, mà ngay chính khoảnh khắc đó, trong giây phút đó, trái tim bạn đang nói gì? Đó là loại câu trả lời mà chúng ta muốn nghe, và tôi tin khán giả cũng muốn nghe điều đó.")
                        .build(),
                VoiceLesson.builder()
                        .title("Điều phối tranh luận – Giữ cân bằng và công bằng")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Hard")
                        .description(
                                "Kỹ năng điều phối khi hai khách mời có quan điểm trái chiều mà không thiên vị bên nào.")
                        .content(
                                "Chúng ta đang có hai quan điểm rất thú vị và đều có cơ sở vững chắc ở đây. Tôi muốn đảm bảo cả hai phía đều được lắng nghe một cách công bằng. Để làm rõ hơn: một bên cho rằng công nghệ là giải phóng, một bên lo ngại nó đang tạo ra sự phụ thuộc. Câu hỏi tôi muốn cả hai cùng suy nghĩ là: có thể cả hai điều đó đều đúng cùng một lúc không? Và nếu đúng như vậy, chúng ta cần điều gì để đảm bảo mặt tích cực áp đảo mặt tiêu cực? Xin mời bạn trả lời trước.")
                        .build(),
                VoiceLesson.builder()
                        .title("Giữ nhịp chương trình – Đẩy nhanh hoặc làm chậm đúng lúc")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Hard")
                        .description("Cảm nhận và điều chỉnh tempo của chương trình để giữ khán giả luôn engaged.")
                        .content(
                                "Chúng ta đã đi qua 20 phút đầu với những chia sẻ rất sâu sắc và đôi khi nặng nề về cảm xúc. Bây giờ tôi muốn nhẹ nhàng hơn một chút – bởi vì những câu chuyện hay nhất thường xen kẽ giữa sâu lắng và vui tươi. Bạn có thể chia sẻ một kỷ niệm buồn cười, một khoảnh khắc mà nếu nhìn lại bây giờ bạn chỉ có thể cười và nói 'ôi trời, hồi đó mình ngây thơ thế'? Điều đó cũng là một phần quan trọng của hành trình đó.")
                        .build(),
                VoiceLesson.builder()
                        .title("Kết nối cảm xúc với khách mời và khán giả")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Medium")
                        .description(
                                "Tạo kết nối cảm xúc chân thật giữa MC, khách mời và khán giả trong studio hoặc trực tiếp.")
                        .content(
                                "Trước khi chúng ta tiếp tục, tôi muốn hỏi khán giả trong trường quay: có ai trong số quý vị đã từng trải qua tình huống tương tự – khi bạn đứng trước một lựa chọn khó khăn và không biết đường nào đúng? Giơ tay nào. Đó là lý do tại sao câu chuyện của khách mời chúng ta tối nay không chỉ là câu chuyện của một cá nhân – đó là câu chuyện của tất cả chúng ta. Cảm ơn bạn đã dũng cảm chia sẻ điều này. Câu chuyện của bạn đang chạm đến trái tim của rất nhiều người.")
                        .build(),
                VoiceLesson.builder()
                        .title("Xử lý khoảnh khắc im lặng và xúc động")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Hard")
                        .description(
                                "Kỹ năng xử lý khi khách mời xúc động, khóc hoặc im lặng – biết khi nào nên chờ, khi nào nên can thiệp.")
                        .content(
                                "Hãy để khoảnh khắc này đọng lại. Không cần vội vã. Đây là điều quan trọng nhất bạn đã nói tối nay. Và tôi muốn khán giả cảm nhận điều đó. [Dừng lại 5 giây] Bạn ổn không? Uống thêm nước nhé. Không cần xin lỗi vì những cảm xúc này – đây chính là điều làm cho cuộc trò chuyện của chúng ta trở nên thật và quý giá. Khi bạn sẵn sàng, chúng ta sẽ tiếp tục. Và nếu bạn muốn dừng ở đây, điều đó cũng hoàn toàn ổn.")
                        .build(),
                VoiceLesson.builder()
                        .title("Tương tác với khán giả trực tiếp trong studio")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Medium")
                        .description(
                                "Kỹ năng mở rộng cuộc trò chuyện ra khán giả studio một cách tự nhiên và hiệu quả.")
                        .content(
                                "Bây giờ tôi muốn mời khán giả trong trường quay cùng tham gia. Có ai có câu hỏi cho khách mời của chúng ta không? Nhưng tôi có một điều kiện – câu hỏi của bạn phải là câu hỏi mà bạn thật sự muốn biết câu trả lời, không phải câu hỏi mà bạn nghĩ sẽ nghe hay. Câu hỏi chân thật nhất bao giờ cũng là câu hỏi thú vị nhất. Ai có câu hỏi đó? Người phụ nữ ở hàng thứ ba từ trên xuống, xin mời!")
                        .build(),
                VoiceLesson.builder()
                        .title("Tóm lược và chuyển tiếp chủ đề mượt mà")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Medium")
                        .description("Kỹ năng tóm tắt ý vừa thảo luận và chuyển sang chủ đề mới một cách tự nhiên.")
                        .content(
                                "Vậy là chúng ta vừa khám phá ra rằng hành trình từ thất bại đến thành công không phải là đường thẳng, mà là một mê cung đầy những bước ngoặt bất ngờ. Điều đó dẫn tôi đến câu hỏi tiếp theo – và đây là điều tôi tò mò nhất kể từ khi đọc về bạn: trong tất cả những gì bạn đã học được, điều nào là điều mà bạn ước gì ai đó đã nói với bạn từ lúc 20 tuổi? Điều đó sẽ thay đổi con đường của bạn như thế nào?")
                        .build(),
                VoiceLesson.builder()
                        .title("Kết thúc talkshow – Lời kết ấn tượng và call-to-action")
                        .category(VoiceLessonCategory.TALKSHOW)
                        .difficulty("Medium")
                        .description(
                                "Kỹ thuật kết thúc chương trình để lại ấn tượng sâu sắc và khuyến khích hành động từ khán giả.")
                        .content(
                                "Chúng ta đã đi qua một hành trình dài tối nay – từ những đỉnh cao đến những vực thẳm, từ nỗi sợ hãi đến lòng dũng cảm. Và điều tôi muốn quý vị mang về nhà tối nay không phải là một công thức hay một bài học có thể ghi vào vở – mà là một câu hỏi: trong cuộc đời của bạn, điều gì đang chờ đợi được bạn dám thử? Cảm ơn khách mời đặc biệt của chúng ta đã chia sẻ những điều vô cùng quý giá. Và cảm ơn quý vị khán giả – đến tuần sau!")
                        .build()));

        List<ReadingGuide> talkshowGuides = readingGuideRepository.saveAll(Arrays.asList(
                ReadingGuide.builder()
                        .title("Nghệ thuật phỏng vấn sâu: Từ surface đến insight")
                        .content(
                                "# Nghệ Thuật Phỏng Vấn Sâu\n\n## Các tầng câu hỏi\n1. **Surface**: 'Bạn làm gì?' → Thông tin cơ bản\n2. **Story**: 'Điều đó xảy ra như thế nào?' → Câu chuyện\n3. **Feeling**: 'Bạn cảm thấy gì lúc đó?' → Cảm xúc\n4. **Meaning**: 'Điều đó có nghĩa gì với bạn?' → Insight\n\n## Kỹ thuật 'follow the energy'\n- Chú ý khi mắt khách mời sáng lên hoặc giọng thay đổi\n- Đó là dấu hiệu có câu chuyện quan trọng phía sau\n- Dừng kịch bản và đào sâu vào điểm đó\n\n## Im lặng là công cụ mạnh\n- Sau câu trả lời sâu sắc, dừng lại 3-5 giây\n- Im lặng tạo không gian để khách mời tiếp tục chia sẻ\n- Đừng sợ khoảng lặng – đó là dấu hiệu cuộc trò chuyện đang đi vào chiều sâu")
                        .author("Nhà báo Phạm Thị Lan – Chuyên gia phỏng vấn truyền hình")
                        .category("TALKSHOW")
                        .thumbnail("https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=400")
                        .build(),
                ReadingGuide.builder()
                        .title("Điều phối tranh luận: Giữ lửa nhưng không để cháy")
                        .content(
                                "# Điều Phối Tranh Luận Trong Talkshow\n\n## Nguyên tắc vàng\n- **Không thiên vị**: MC là người dẫn đường, không phải người phán xét\n- **Steel-manning**: Luôn trình bày quan điểm của mỗi bên ở dạng mạnh nhất\n- **Bridge building**: Tìm điểm chung giữa các quan điểm trái chiều\n\n## Kỹ thuật kiểm soát tranh luận nóng\n1. Hạ nhiệt bằng câu hỏi làm rõ: 'Ý bạn là...?'\n2. Redirect: 'Điều đó nhắc tôi đến câu hỏi quan trọng hơn...'\n3. Time-out: 'Hãy để chúng ta dừng lại một chút và...'\n\n## Dấu hiệu tranh luận đang mất kiểm soát\n- Giọng nói to và nhanh hơn\n- Khách mời ngắt lời nhau\n- Ngôn ngữ trở nên cá nhân hóa\nHành động ngay lập tức khi thấy dấu hiệu này.")
                        .author("Chuyên gia truyền thông Nguyễn Văn Tùng")
                        .category("TALKSHOW")
                        .thumbnail("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400")
                        .build(),
                ReadingGuide.builder()
                        .title("Kết nối cảm xúc trên truyền hình: Kỹ thuật và đạo đức")
                        .content(
                                "# Kết Nối Cảm Xúc Trên Truyền Hình\n\n## Tại sao kết nối cảm xúc quan trọng\n- Khán giả nhớ cảm xúc lâu hơn thông tin\n- Kết nối cảm xúc tạo ra engagement thật sự\n- Câu chuyện cảm xúc được chia sẻ nhiều hơn\n\n## Kỹ thuật tạo kết nối\n1. **Active listening**: Lắng nghe toàn thân, không chỉ tai\n2. **Mirroring**: Phản chiếu cảm xúc một cách tự nhiên\n3. **Validation**: Xác nhận cảm xúc trước khi tiếp tục\n4. **Universal connect**: Kết nối câu chuyện cá nhân với trải nghiệm phổ quát\n\n## Ranh giới đạo đức\n- Không khai thác đau khổ để tăng rating\n- Cho khách mời quyền dừng lại bất cứ lúc nào\n- Không push để khách mời chia sẻ điều họ không muốn\n- Trauma không phải entertainment – xử lý với sự tôn trọng và cẩn thận")
                        .author("MCHub Talkshow Masterclass")
                        .category("TALKSHOW")
                        .thumbnail("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400")
                        .build()));

        List<String> talkshowLessonIds = talkshowLessons.stream()
                .map(VoiceLesson::getId).collect(java.util.stream.Collectors.toList());
        List<String> talkshowGuideIds = talkshowGuides.stream()
                .map(ReadingGuide::getId).collect(java.util.stream.Collectors.toList());

        courseRepository.save(Course.builder()
                .title("MC Talkshow")
                .shortDescription(
                        "Nghệ thuật dẫn talkshow, phỏng vấn và điều phối thảo luận nhóm trên truyền hình và sân khấu.")
                .description(
                        "Khóa học chuyên sâu về kỹ năng dẫn talkshow và phỏng vấn. Học cách đặt câu hỏi sâu sắc, điều phối tranh luận, giữ nhịp chương trình và tạo kết nối cảm xúc với khách mời. Áp dụng cho format truyền hình, podcast và sự kiện sân khấu.")
                .slug("mc-talkshow")
                .type(CourseType.TALKSHOW_MC)
                .learningPathType(LearningPathType.STRUCTURED_COURSE)
                .thumbnail("https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=800")
                .difficulty("INTERMEDIATE")
                .estimatedHours(14)
                .lessonIds(talkshowLessonIds)
                .readingIds(talkshowGuideIds)
                .passingScore(70)
                .isActive(true)
                .quizQuestions(Arrays.asList(
                        Course.QuizQuestion.builder()
                                .question("Trong phỏng vấn talkshow, 'follow the energy' có nghĩa là gì?")
                                .options(Arrays.asList("Đảm bảo chương trình luôn vui vẻ và sôi động",
                                        "Chú ý khi giọng hoặc mắt khách mời thay đổi – đó là dấu hiệu câu chuyện quan trọng và cần đào sâu",
                                        "Theo dõi mức độ năng lượng của khán giả",
                                        "Đặt câu hỏi theo thứ tự từ dễ đến khó"))
                                .correctIndex(1)
                                .explanation(
                                        "'Follow the energy' là kỹ thuật phỏng vấn chuyên nghiệp: khi khách mời thể hiện sự hứng khởi hoặc cảm xúc, đó là tín hiệu có câu chuyện thú vị cần khai thác sâu hơn.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Khi khách mời talkshow bắt đầu xúc động và im lặng, MC nên làm gì?")
                                .options(Arrays.asList("Ngay lập tức đặt câu hỏi khác để phá vỡ sự im lặng awkward",
                                        "Cho phép khoảnh khắc lặng đọng lại, bày tỏ sự đồng cảm và để khách mời quyết định tiếp tục",
                                        "Chuyển ngay sang khách mời khác", "Xin lỗi và thay đổi chủ đề"))
                                .correctIndex(1)
                                .explanation(
                                        "Im lặng sau câu trả lời cảm xúc là thiêng liêng – không nên phá vỡ nó. Cho khán giả và khách mời cùng cảm nhận khoảnh khắc đó trước khi tiếp tục.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Trong 4 tầng câu hỏi phỏng vấn, tầng nào tạo ra insight sâu sắc nhất?")
                                .options(Arrays.asList("Surface: 'Bạn làm gì?'",
                                        "Meaning: 'Điều đó có nghĩa gì với bạn?'",
                                        "Story: 'Điều đó xảy ra như thế nào?'", "Feeling: 'Bạn cảm thấy gì lúc đó?'"))
                                .correctIndex(1)
                                .explanation(
                                        "Tầng Meaning (ý nghĩa) là sâu nhất – khai thác được giá trị, triết lý và bài học cốt lõi từ trải nghiệm của khách mời, tạo ra nội dung có giá trị nhất cho khán giả.")
                                .category("THEORY")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Khi điều phối tranh luận, 'steel-manning' có nghĩa là gì?")
                                .options(Arrays.asList("Dùng giọng mạnh mẽ để kiểm soát tranh luận",
                                        "Trình bày quan điểm của mỗi bên ở dạng mạnh và thuyết phục nhất có thể",
                                        "Đứng về phía quan điểm có lý hơn",
                                        "Ngăn chặn tranh luận trước khi nó bắt đầu"))
                                .correctIndex(1)
                                .explanation(
                                        "Steel-manning là kỹ thuật công bằng: trình bày quan điểm của mỗi bên ở dạng tốt nhất của nó, đảm bảo cuộc tranh luận dựa trên lập luận thực chất.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Dấu hiệu nào cho thấy tranh luận trong talkshow đang mất kiểm soát?")
                                .options(Arrays.asList("Khách mời nói chậm hơn và suy nghĩ kỹ hơn",
                                        "Giọng to hơn, khách mời ngắt lời nhau, ngôn ngữ trở nên cá nhân hóa",
                                        "Khán giả im lặng chú ý", "Các bên đồng ý với nhau nhiều hơn"))
                                .correctIndex(1)
                                .explanation(
                                        "Ba dấu hiệu cảnh báo: giọng to và nhanh, ngắt lời lẫn nhau, và dùng ngôn ngữ tấn công cá nhân – MC cần can thiệp ngay lập tức khi thấy những dấu hiệu này.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question(
                                        "Ranh giới đạo đức quan trọng nhất khi khai thác cảm xúc trong talkshow là gì?")
                                .options(Arrays.asList("Luôn cần xin phép khách mời trước khi phát sóng",
                                        "Không khai thác đau khổ để tăng rating – trauma không phải entertainment",
                                        "Không để khách mời khóc trên sóng", "Chỉ hỏi những câu hỏi tích cực"))
                                .correctIndex(1)
                                .explanation(
                                        "MC có trách nhiệm với khách mời và khán giả: sự cảm xúc cần xuất phát từ kết nối chân thật, không phải từ việc 'khai thác' để tạo drama hay tăng view.")
                                .category("ETIQUETTE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Kỹ thuật 'redirect' trong điều phối tranh luận được dùng khi nào?")
                                .options(Arrays.asList("Khi chương trình sắp hết giờ",
                                        "Khi tranh luận đang leo thang và cần chuyển hướng sang câu hỏi quan trọng hơn",
                                        "Khi khách mời không trả lời được câu hỏi",
                                        "Khi cần giới thiệu sản phẩm quảng cáo"))
                                .correctIndex(1)
                                .explanation(
                                        "Redirect là công cụ kiểm soát tinh tế: thay vì dừng tranh luận đột ngột, MC dẫn dắt cuộc trò chuyện sang hướng xây dựng hơn bằng một câu hỏi mới.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Mục đích chính của lời kết thúc talkshow là gì?")
                                .options(Arrays.asList("Tóm tắt toàn bộ nội dung đã thảo luận",
                                        "Để lại ấn tượng và câu hỏi khiến khán giả tiếp tục suy nghĩ sau khi tắt TV",
                                        "Quảng bá tập tiếp theo của chương trình", "Cảm ơn nhà tài trợ và đối tác"))
                                .correctIndex(1)
                                .explanation(
                                        "Lời kết hiệu quả không tóm tắt – mà để lại một câu hỏi, một cảm xúc hoặc một thách thức khiến khán giả tiếp tục suy nghĩ về chủ đề sau khi chương trình kết thúc.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question(
                                        "Khi mời khán giả studio đặt câu hỏi, điều kiện quan trọng MC nên đặt ra là gì?")
                                .options(Arrays.asList("Câu hỏi phải ngắn gọn, không quá 1 câu",
                                        "Câu hỏi phải là điều họ thật sự muốn biết, không phải câu hỏi nghe hay",
                                        "Câu hỏi phải liên quan đến chủ đề chính của chương trình",
                                        "Chỉ nhận câu hỏi từ khán giả được chọn sẵn"))
                                .correctIndex(1)
                                .explanation(
                                        "Câu hỏi chân thật từ khán giả luôn tạo ra những khoảnh khắc tốt nhất trong talkshow. MC cần khuyến khích tính xác thực thay vì câu hỏi 'nghe hay nhưng rỗng tuếch'.")
                                .category("TECHNIQUE")
                                .build(),
                        Course.QuizQuestion.builder()
                                .question("Kỹ thuật 'active listening' trong phỏng vấn talkshow nghĩa là gì?")
                                .options(Arrays.asList("Ghi chép đầy đủ những gì khách mời nói",
                                        "Lắng nghe bằng toàn thân – ánh mắt, ngôn ngữ cơ thể, không chỉ bằng tai",
                                        "Luôn gật đầu liên tục để thể hiện sự chú ý",
                                        "Đặt câu hỏi tiếp nối ngay sau mỗi câu trả lời"))
                                .correctIndex(1)
                                .explanation(
                                        "Active listening là lắng nghe toàn diện: ánh mắt, tư thế, biểu cảm khuôn mặt đều truyền đạt rằng bạn đang thật sự hiện diện và quan tâm đến những gì khách mời nói.")
                                .category("TECHNIQUE")
                                .build()))
                .build());
        log.info("✅ Seeded Course 3: MC Talkshow");

        // ── Milestone Courses (Roadmap) ──────────────────────────────
        log.info("🌱 Seeding 3 Milestone Courses for Learning Roadmap...");

        List<VoiceLesson> allSeededLessons = lessonRepository.findAll();
        List<ReadingGuide> allSeededGuides = readingGuideRepository.findAll();

        int lessonCount = allSeededLessons.size();
        int guideCount = allSeededGuides.size();

        // Distribute the first 30 of the generic 50 lessons across milestone courses
        List<String> foundationLessonIds = allSeededLessons.subList(0, Math.min(10, lessonCount))
                .stream().map(VoiceLesson::getId).collect(java.util.stream.Collectors.toList());
        List<String> intermediateLessonIds = allSeededLessons
                .subList(Math.min(10, lessonCount), Math.min(20, lessonCount))
                .stream().map(VoiceLesson::getId).collect(java.util.stream.Collectors.toList());
        List<String> advancedLessonIds = allSeededLessons.subList(Math.min(20, lessonCount), Math.min(30, lessonCount))
                .stream().map(VoiceLesson::getId).collect(java.util.stream.Collectors.toList());

        String guide1Id = guideCount > 0 ? allSeededGuides.get(0).getId() : null;
        String guide2Id = guideCount > 1 ? allSeededGuides.get(1).getId() : null;

        courseRepository.save(Course.builder()
                .title("Foundation")
                .shortDescription(
                        "Master the fundamentals of MC voice: breath control, pronunciation, and basic pacing.")
                .description(
                        "Build the core technical skills every professional MC needs. This milestone covers diaphragmatic breathing, crisp consonant articulation, tonal control, and basic audience connection. Complete all 10 practice sessions and pass the assessment to unlock Vocal Mastery.")
                .slug("milestone-foundation")
                .learningPathType(LearningPathType.MILESTONE_PATH)
                .difficulty("BEGINNER")
                .estimatedHours(8)
                .lessonIds(foundationLessonIds)
                .readingIds(guide1Id != null ? List.of(guide1Id) : List.of())
                .passingScore(65)
                .isActive(true)
                .build());

        courseRepository.save(Course.builder()
                .title("Vocal Mastery")
                .shortDescription("Develop advanced vocal dynamics, emotional delivery, and rhythm variation.")
                .description(
                        "Deepen your craft with advanced vocal techniques. Learn to modulate pitch and pace for dramatic effect, read room energy and adapt your delivery in real time, and harness emotional resonance to keep audiences captivated. Requires Foundation completion.")
                .slug("milestone-vocal-mastery")
                .learningPathType(LearningPathType.MILESTONE_PATH)
                .difficulty("INTERMEDIATE")
                .estimatedHours(12)
                .lessonIds(intermediateLessonIds)
                .readingIds(guide2Id != null ? List.of(guide2Id) : List.of())
                .passingScore(70)
                .isActive(true)
                .build());

        courseRepository.save(Course.builder()
                .title("Elite Performance")
                .shortDescription("Professional-grade techniques for large venues, live TV, and high-stakes events.")
                .description(
                        "The final milestone separates good MCs from elite performers. Cover live broadcast standards, multi-lingual ceremony protocols, crisis improvisation, and commanding 1000+ person venues. Complete this to earn your Elite MC Certificate.")
                .slug("milestone-elite-performance")
                .learningPathType(LearningPathType.MILESTONE_PATH)
                .difficulty("ADVANCED")
                .estimatedHours(18)
                .lessonIds(advancedLessonIds)
                .readingIds(List.of())
                .passingScore(75)
                .isActive(true)
                .build());

        log.info("✅ Successfully seeded 3 Milestone Courses");
        lessonSearchService.reindexLessons(lessonRepository.findAll());
    }

    private void seedPlanDefinitions() {
        if (planDefinitionRepository.count() > 0) return;
        log.info("🌱 Seeding default plan definitions...");

        planDefinitionRepository.saveAll(List.of(
            PlanDefinition.builder()
                .plan(SubscriptionPlan.FREE)
                .displayName("Miễn Phí")
                .tagline("Bắt đầu hành trình của bạn")
                .description("Truy cập miễn phí vào thư viện bài luyện cơ bản. Thích hợp cho người mới bắt đầu khám phá.")
                .priceVnd(0)
                .durationDays(0)
                .aiSessionLimit(3)
                .badge(null)
                .urgencyText(null)
                .socialProof(null)
                .highlights(List.of("3 buổi AI coaching/tháng", "Thư viện bài luyện cơ bản", "Lịch sử luyện tập 7 ngày"))
                .comparisonEntries(List.of())
                .active(true)
                .build(),
            PlanDefinition.builder()
                .plan(SubscriptionPlan.BASIC)
                .displayName("Cơ Bản")
                .tagline("Luyện đều, tiến đều")
                .description("Phù hợp cho MC bán chuyên muốn cải thiện đều đặn mỗi tháng với phản hồi AI chuyên sâu.")
                .priceVnd(199000)
                .durationDays(30)
                .aiSessionLimit(20)
                .badge("Phổ biến")
                .urgencyText(null)
                .socialProof("Hơn 1.200 MC đang dùng gói này")
                .highlights(List.of("20 buổi AI coaching/tháng", "Toàn bộ thư viện 50+ bài", "Phân tích WER/CER chi tiết", "Lịch sử không giới hạn", "Huy hiệu & bảng xếp hạng"))
                .comparisonEntries(List.of())
                .active(true)
                .build(),
            PlanDefinition.builder()
                .plan(SubscriptionPlan.FULL)
                .displayName("Chuyên Nghiệp")
                .tagline("Dành cho MC nghiêm túc")
                .description("Không giới hạn luyện tập. Phân tích sâu nhất. Công cụ xây dựng sự nghiệp MC chuyên nghiệp.")
                .priceVnd(399000)
                .durationDays(30)
                .aiSessionLimit(999)
                .badge("Được chọn nhiều nhất")
                .urgencyText("Chỉ còn 8 chỗ tuần này")
                .socialProof("92% MC chuyên nghiệp chọn gói này")
                .highlights(List.of("Không giới hạn AI coaching", "Ưu tiên xử lý phân tích giọng", "Script builder & teleprompter", "Báo cáo tiến bộ hàng tuần", "Hỗ trợ 1-1 qua chat", "Chứng chỉ hoàn thành khóa học"))
                .comparisonEntries(List.of())
                .active(true)
                .build(),
            PlanDefinition.builder()
                .plan(SubscriptionPlan.ANNUAL)
                .displayName("Năm")
                .tagline("Tiết kiệm 40% so với tháng")
                .description("Cam kết dài hạn, giá tốt nhất. Đầy đủ mọi tính năng Professional cộng thêm quyền lợi độc quyền.")
                .priceVnd(2388000)
                .durationDays(365)
                .aiSessionLimit(999)
                .badge("Tiết kiệm nhất")
                .urgencyText("Giá ưu đãi — có thể điều chỉnh bất kỳ lúc nào")
                .socialProof("Tiết kiệm 1.400.000₫ so với trả theo tháng")
                .highlights(List.of("Tất cả quyền lợi Chuyên Nghiệp", "Thanh toán 1 lần, dùng cả năm", "Ưu tiên truy cập tính năng mới", "Badge độc quyền Annual Member", "Hỗ trợ ưu tiên 24/7"))
                .comparisonEntries(List.of())
                .active(true)
                .build()
        ));
        log.info("✅ Seeded 4 plan definitions");
    }
}
