package com.mchub.config;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.models.PlanDefinition;
import com.mchub.repositories.PlanDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

// Lesson/course/competition seed data is imported via import_data.py
// (MCHub_DataEntry_CLEAN.xlsx) instead of code-based seeding.
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PlanDefinitionRepository planDefinitionRepository;

    @Override
    public void run(String... args) {
        seedPlanDefinitions();
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
