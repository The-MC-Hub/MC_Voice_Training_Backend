package com.mchub.repositories;

import com.mchub.models.Announcement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    List<Announcement> findByStatusOrderByCreatedAtDesc(Announcement.AnnouncementStatus status);
}
