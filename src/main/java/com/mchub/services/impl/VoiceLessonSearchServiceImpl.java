package com.mchub.services.impl;

import com.mchub.enums.VoiceLessonCategory;
import com.mchub.models.VoiceLesson;
import com.mchub.models.VoiceLessonSearchDocument;
import com.mchub.repositories.VoiceLessonRepository;
import com.mchub.repositories.VoiceLessonSearchRepository;
import com.mchub.services.VoiceLessonSearchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VoiceLessonSearchServiceImpl implements VoiceLessonSearchService {

  private final VoiceLessonRepository lessonRepository;

  @Autowired(required = false)
  private VoiceLessonSearchRepository searchRepository;

  public VoiceLessonSearchServiceImpl(VoiceLessonRepository lessonRepository) {
    this.lessonRepository = lessonRepository;
  }

  @Override
  @Async
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

  @Override
  @Async
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

  @Override
  public List<VoiceLesson> searchLessons(String searchTerm, VoiceLessonCategory category) {
    String cleanTerm = searchTerm == null ? "" : searchTerm.trim();

    if (cleanTerm.isEmpty()) {
      if (category == null) {
        return lessonRepository.findByIsActiveTrue();
      }
      return lessonRepository.findByCategoryAndIsActiveTrue(category);
    }

    if (searchRepository != null) {
      try {
        List<VoiceLessonSearchDocument> docs = searchRepository.searchByText(cleanTerm);

        if (!docs.isEmpty()) {
          List<String> ids =
              docs.stream().map(VoiceLessonSearchDocument::getId).filter(Objects::nonNull).toList();

          Map<String, VoiceLesson> lessonMap =
              lessonRepository.findAllById(ids).stream()
                  .filter(VoiceLesson::isActive)
                  .collect(Collectors.toMap(VoiceLesson::getId, Function.identity(), (a, b) -> a));

          List<VoiceLesson> ordered = new ArrayList<>();
          for (String id : ids) {
            VoiceLesson l = lessonMap.get(id);
            if (l != null) {
              if (category == null || l.getCategory() == category) {
                ordered.add(l);
              }
            }
          }

          if (!ordered.isEmpty()) {
            return ordered;
          }
        }
      } catch (Exception e) {
        log.warn(
            "Elasticsearch search failed, falling back to Mongo in-memory filter: {}",
            e.getMessage());
      }
    }

    return fallbackSearch(cleanTerm, category);
  }

  @Override
  public long reindexAll() {
    if (searchRepository == null) {
      log.warn("Elasticsearch repository is disabled/unavailable. Skipping reindex.");
      return 0;
    }

    List<VoiceLesson> activeLessons = lessonRepository.findByIsActiveTrue();
    List<VoiceLessonSearchDocument> docs = activeLessons.stream().map(this::toDocument).toList();

    searchRepository.saveAll(docs);
    log.info("Reindexed {} active voice lessons into Elasticsearch", docs.size());
    return docs.size();
  }

  private List<VoiceLesson> fallbackSearch(String term, VoiceLessonCategory category) {
    List<VoiceLesson> source =
        category == null
            ? lessonRepository.findByIsActiveTrue()
            : lessonRepository.findByCategoryAndIsActiveTrue(category);

    String lowerTerm = term.toLowerCase(Locale.ROOT);
    return source.stream()
        .filter(
            l ->
                (l.getTitle() != null && l.getTitle().toLowerCase(Locale.ROOT).contains(lowerTerm))
                    || (l.getContent() != null
                        && l.getContent().toLowerCase(Locale.ROOT).contains(lowerTerm))
                    || (l.getDescription() != null
                        && l.getDescription().toLowerCase(Locale.ROOT).contains(lowerTerm)))
        .sorted(
            Comparator.comparing((VoiceLesson l) -> scoreFallbackMatch(l, lowerTerm)).reversed())
        .toList();
  }

  private int scoreFallbackMatch(VoiceLesson l, String lowerTerm) {
    int score = 0;
    if (l.getTitle() != null && l.getTitle().toLowerCase(Locale.ROOT).contains(lowerTerm)) {
      score += 10;
    }
    if (l.getContent() != null && l.getContent().toLowerCase(Locale.ROOT).contains(lowerTerm)) {
      score += 3;
    }
    if (l.getDescription() != null
        && l.getDescription().toLowerCase(Locale.ROOT).contains(lowerTerm)) {
      score += 1;
    }
    return score;
  }

  private VoiceLessonSearchDocument toDocument(VoiceLesson lesson) {
    VoiceLessonSearchDocument doc = new VoiceLessonSearchDocument();
    doc.setId(lesson.getId());
    doc.setTitle(lesson.getTitle());
    doc.setContent(lesson.getContent());
    doc.setCategory(lesson.getCategory());
    doc.setDifficulty(lesson.getDifficulty());
    doc.setDescription(lesson.getDescription());
    return doc;
  }
}
