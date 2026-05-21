package com.mchub.repositories;

import com.mchub.enums.CourseType;
import com.mchub.enums.LearningPathType;
import com.mchub.models.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findByIsActiveTrue();
    List<Course> findByTypeAndIsActiveTrue(CourseType type);
    List<Course> findByLearningPathTypeAndIsActiveTrue(LearningPathType type);
    Optional<Course> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
