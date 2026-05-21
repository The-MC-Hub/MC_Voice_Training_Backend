package com.mchub.repositories;

import com.mchub.models.CourseEnrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends MongoRepository<CourseEnrollment, String> {
    Optional<CourseEnrollment> findByUserIdAndCourseId(String userId, String courseId);
    List<CourseEnrollment> findByUserId(String userId);
    List<CourseEnrollment> findByCourseId(String courseId);
    boolean existsByUserIdAndCourseId(String userId, String courseId);
    long countByCourseId(String courseId);
    long countByCourseIdAndIsCompletedTrue(String courseId);
}
