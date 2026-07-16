package com.mchub.repositories;

import com.mchub.models.Announcement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends MongoRepository<Announcement, String> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    List<Announcement> findByStatusOrderByCreatedAtDesc(Announcement.AnnouncementStatus status);
}
