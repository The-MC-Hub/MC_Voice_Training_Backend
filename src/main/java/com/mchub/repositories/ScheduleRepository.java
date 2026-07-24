package com.mchub.repositories;

import com.mchub.models.Schedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends MongoRepository<Schedule, String> {

    List<Schedule> findByMc(String mcId);

    void deleteByIdAndMc(String id, String mcId);
}
