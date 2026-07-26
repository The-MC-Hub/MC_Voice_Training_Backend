package com.mchub.repositories;

import com.mchub.models.Announcement;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends MongoRepository<Announcement, String> {

  List<Announcement> findAllByOrderByCreatedAtDesc();

  List<Announcement> findByStatusOrderByCreatedAtDesc(Announcement.AnnouncementStatus status);
}
