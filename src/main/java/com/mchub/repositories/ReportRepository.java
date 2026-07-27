package com.mchub.repositories;

import com.mchub.enums.ReportStatus;
import com.mchub.models.Report;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
  List<Report> findByReporterId(String reporterId);

  List<Report> findByReportedId(String reportedId);

  List<Report> findByStatus(ReportStatus status);

  long countByStatus(ReportStatus status);
}
