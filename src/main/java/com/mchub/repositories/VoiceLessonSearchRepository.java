package com.mchub.repositories;

import com.mchub.models.VoiceLessonSearchDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceLessonSearchRepository extends ElasticsearchRepository<VoiceLessonSearchDocument, String> {

  @Query("""
      {
        "multi_match": {
          "query": "?0",
          "fields": ["title^4", "description^3", "content^2", "difficulty", "category"],
          "type": "best_fields"
        }
      }
      """)
  List<VoiceLessonSearchDocument> searchByText(String query);
}