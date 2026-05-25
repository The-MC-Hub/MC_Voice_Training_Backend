package com.mchub.services;

import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.VoiceLesson;
import com.mchub.models.VoiceLessonSearchDocument;
import com.mchub.repositories.VoiceLessonRepository;
import com.mchub.repositories.VoiceLessonSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VoiceLessonSearchService {

    private final VoiceLessonRepository lessonRepository;

    @Autowired(required = false)
    private VoiceLessonSearchRepository searchRepository;

    public VoiceLessonSearchService(VoiceLessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    // Lưu từng lesson vào Elasticsearch để BM25 có dữ liệu xếp hạng.
    public void indexLesson(VoiceLesson lesson) {
        if (lesson == null || lesson.getId() == null) {
            return;
        }

        if (searchRepository == null) return;
        try {
            searchRepository.save(toDocument(lesson));
        } catch (Exception e) {
            log.warn("Failed to index voice lesson {}", lesson.getId(), e);
        }
    }

    // Xóa lesson khỏi index khi dữ liệu gốc bị xóa trong Mongo.
    public void deleteLesson(String id) {
        if (id == null || id.isBlank()) {
            return;
        }

        if (searchRepository == null) return;
        try {
            searchRepository.deleteById(id);
        } catch (Exception e) {
            log.warn("Failed to remove voice lesson {} from search index", id, e);
        }
    }

    // Dùng cho reindex/backfill khi cần tạo lại toàn bộ search index.
    public void clearIndex() {
        if (searchRepository == null) return;
        try {
            searchRepository.deleteAll();
        } catch (Exception e) {
            log.warn("Failed to clear voice lesson search index", e);
        }
    }

    // Reindex toàn bộ lesson để đồng bộ lại dữ liệu tìm kiếm.
    public void reindexLessons(List<VoiceLesson> lessons) {
        clearIndex();
        if (lessons == null || lessons.isEmpty()) {
            return;
        }

        lessons.forEach(this::indexLesson);
    }

    // Ưu tiên Elasticsearch/BM25, nếu lỗi thì rơi về Mongo + scoring cục bộ.
    public List<VoiceLesson> searchLessons(String searchTerm, VoiceLessonCategory category) {
        String normalizedSearch = normalize(searchTerm);
        if (normalizedSearch.isBlank()) {
            return category == null ? lessonRepository.findAll() : lessonRepository.findByCategory(category);
        }

        try {
            // Lấy danh sách id theo độ liên quan từ search index.
            List<VoiceLessonSearchDocument> hits = searchRepository.searchByText(normalizedSearch);
            if (!hits.isEmpty()) {
                List<String> lessonIds = hits.stream()
                        .map(VoiceLessonSearchDocument::getId)
                        .filter(Objects::nonNull)
                        .toList();

                // Hydrate lại lesson đầy đủ từ Mongo nhưng vẫn giữ thứ tự xếp hạng của BM25.
                Map<String, VoiceLesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
                        .collect(Collectors.toMap(VoiceLesson::getId, Function.identity(), (left, right) -> left));

                return lessonIds.stream()
                        .map(lessonMap::get)
                        .filter(Objects::nonNull)
                        .filter(lesson -> category == null || lesson.getCategory() == category)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Elasticsearch search failed for lesson query '{}', falling back to Mongo/local search",
                    searchTerm, e);
        }

        return fallbackSearch(normalizedSearch, category);
    }

    // Scoring cục bộ để giao diện vẫn search được khi Elasticsearch không sẵn sàng.
    private List<VoiceLesson> fallbackSearch(String searchTerm, VoiceLessonCategory category) {
        List<VoiceLesson> lessons = category == null ? lessonRepository.findAll()
                : lessonRepository.findByCategory(category);
        if (searchTerm.isBlank()) {
            return lessons;
        }

        String[] tokens = searchTerm.split("\\s+");
        List<ScoredLesson> scoredLessons = new ArrayList<>();
        for (VoiceLesson lesson : lessons) {
            double score = scoreLesson(lesson, tokens);
            if (score > 0) {
                scoredLessons.add(new ScoredLesson(lesson, score));
            }
        }

        scoredLessons.sort(Comparator
                .comparingDouble(ScoredLesson::score).reversed()
                .thenComparing(scored -> scored.lesson().getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())));

        return scoredLessons.stream().map(ScoredLesson::lesson).toList();
    }

    // Chấm điểm đơn giản theo field để fallback vẫn có thứ tự kết quả hợp lý.
    private double scoreLesson(VoiceLesson lesson, String[] tokens) {
        String title = normalize(lesson.getTitle());
        String description = normalize(lesson.getDescription());
        String content = normalize(lesson.getContent());
        String category = lesson.getCategory() != null ? lesson.getCategory().name().toLowerCase(Locale.ROOT) : "";
        String difficulty = normalize(lesson.getDifficulty());

        double score = 0.0;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (title.contains(token)) {
                score += 6;
            }
            if (description.contains(token)) {
                score += 3;
            }
            if (content.contains(token)) {
                score += 1;
            }
            if (category.contains(token)) {
                score += 2;
            }
            if (difficulty.contains(token)) {
                score += 1.5;
            }
        }
        return score;
    }

    // Tạo document tìm kiếm gọn nhẹ từ lesson gốc.
    private VoiceLessonSearchDocument toDocument(VoiceLesson lesson) {
        return VoiceLessonSearchDocument.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .content(lesson.getContent())
                .category(lesson.getCategory())
                .difficulty(lesson.getDifficulty())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    // Chuẩn hóa text để search không bị lệch do hoa/thường hoặc khoảng trắng.
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private record ScoredLesson(VoiceLesson lesson, double score) {
    }
}