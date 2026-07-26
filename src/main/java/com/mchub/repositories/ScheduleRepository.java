package com.mchub.repositories;

import com.mchub.models.Schedule;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends MongoRepository<Schedule, String> {

  List<Schedule> findByMc(String mcId);

  void deleteByIdAndMc(String id, String mcId);
}
