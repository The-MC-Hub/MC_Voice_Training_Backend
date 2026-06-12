package com.mchub.services.impl;

import com.mchub.dto.*;
import com.mchub.enums.CourseType;
import com.mchub.enums.LearningPathType;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.*;
import com.mchub.repositories.*;
import com.mchub.services.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;
    private final VoiceLessonRepository lessonRepository;
    private final ReadingGuideRepository readingGuideRepository;
    private final UserRepository userRepository;

    // ================================================================
    //  Public / User
    // ================================================================

    @Override
    public List<String> getAllCourseTypes() {
        return Arrays.stream(CourseType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDTO> getAllActiveCourses() {
        return courseRepository.findByIsActiveTrue().stream()
                .map(c -> toSummaryDTO(c, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDTO> getCoursesByType(CourseType type) {
        return courseRepository.findByTypeAndIsActiveTrue(type).stream()
                .map(c -> toSummaryDTO(c, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDTO> getMilestoneCourses(String userId) {
        List<String> difficultyOrder = List.of("BEGINNER", "INTERMEDIATE", "ADVANCED");

        List<Course> milestones = courseRepository
                .findByLearningPathTypeAndIsActiveTrue(LearningPathType.MILESTONE_PATH).stream()
                .sorted(Comparator.comparingInt(c -> {
                    int idx = difficultyOrder.indexOf(c.getDifficulty().toUpperCase());
                    return idx == -1 ? 99 : idx;
                }))
                .collect(Collectors.toList());

        Set<String> completedIds = userId == null ? Set.of()
                : enrollmentRepository.findByUserId(userId).stream()
                        .filter(CourseEnrollment::isCompleted)
                        .map(CourseEnrollment::getCourseId)
                        .collect(Collectors.toSet());

        List<CourseResponseDTO> result = new ArrayList<>();
        boolean previousCompleted = true;

        for (Course c : milestones) {
            CourseResponseDTO.EnrollmentProgressDTO progress = userId != null
                    ? enrollmentRepository.findByUserIdAndCourseId(userId, c.getId())
                            .map(this::toProgressDTO)
                            .orElse(null)
                    : null;

            String status;
            if (progress != null && progress.isCompleted()) {
                status = "Completed";
            } else if (progress != null) {
                status = "In Progress";
            } else if (previousCompleted) {
                status = "In Progress";
            } else {
                status = "Locked";
            }

            CourseResponseDTO dto = toSummaryDTO(c, progress);
            dto.setStatus(status);
            dto.setLearningPathType(c.getLearningPathType());
            result.add(dto);

            previousCompleted = completedIds.contains(c.getId());
        }

        return result;
    }

    @Override
    public CourseResponseDTO getCourseDetail(String courseId, String userId) {
        Course course = findCourse(courseId);
        CourseResponseDTO.EnrollmentProgressDTO progress = userId != null
                ? enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                        .map(this::toProgressDTO)
                        .orElse(null)
                : null;
        return toDetailDTO(course, progress);
    }

    @Override
    public ReadingGuide getReadingGuide(String id) {
        return readingGuideRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND, "Reading Guide not found: " + id));
    }

    // ================================================================
    //  Enrollment & Progress
    // ================================================================

    @Override
    public CourseResponseDTO.EnrollmentProgressDTO enroll(String courseId, String userId) {
        findCourse(courseId);
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new AppException(ErrorCode.COURSE_ALREADY_ENROLLED, "Already enrolled");
        }
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .build();
        return toProgressDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    public CourseResponseDTO.EnrollmentProgressDTO completeLesson(String courseId, String lessonId, String userId) {
        CourseEnrollment enrollment = findEnrollment(userId, courseId);
        if (!enrollment.getCompletedLessonIds().contains(lessonId)) {
            enrollment.getCompletedLessonIds().add(lessonId);
            recalcCompletion(enrollment, findCourse(courseId));
            enrollmentRepository.save(enrollment);
        }
        return toProgressDTO(enrollment);
    }

    @Override
    public CourseResponseDTO.EnrollmentProgressDTO completeReading(String courseId, String readingId, String userId) {
        CourseEnrollment enrollment = findEnrollment(userId, courseId);
        if (!enrollment.getCompletedReadingIds().contains(readingId)) {
            enrollment.getCompletedReadingIds().add(readingId);
            recalcCompletion(enrollment, findCourse(courseId));
            enrollmentRepository.save(enrollment);
        }
        return toProgressDTO(enrollment);
    }

    @Override
    public QuizResultDTO submitQuiz(String courseId, String userId, QuizSubmitRequest request) {
        Course course = findCourse(courseId);
        CourseEnrollment enrollment = findEnrollment(userId, courseId);

        List<Course.QuizQuestion> questions = course.getQuizQuestions();
        if (request.getAnswers().size() != questions.size()) {
            throw new AppException(ErrorCode.QUIZ_ANSWER_MISMATCH,
                    "Expected " + questions.size() + " answers, got " + request.getAnswers().size());
        }

        int correct = 0;
        List<QuizResultDTO.QuestionFeedback> feedback = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Course.QuizQuestion q = questions.get(i);
            int given = request.getAnswers().get(i);
            boolean isCorrect = given == q.getCorrectIndex();
            if (isCorrect) correct++;
            feedback.add(QuizResultDTO.QuestionFeedback.builder()
                    .questionIndex(i)
                    .question(q.getQuestion())
                    .yourAnswer(given)
                    .correctAnswer(q.getCorrectIndex())
                    .correct(isCorrect)
                    .explanation(q.getExplanation())
                    .build());
        }

        int score = (int) Math.round((double) correct / questions.size() * 100);
        boolean passed = score >= course.getPassingScore();

        enrollment.setQuizScore(score);
        enrollment.setQuizAttempts(enrollment.getQuizAttempts() + 1);
        recalcCompletion(enrollment, course);
        enrollmentRepository.save(enrollment);

        // Issue certificate if passed and not already issued
        String certId = null;
        boolean certEarned = false;
        if (passed && !certificateRepository.existsByUserIdAndCourseId(userId, courseId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
            Certificate cert = Certificate.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .courseName(course.getTitle())
                    .userName(user.getName())
                    .completionScore(score)
                    .isVerified(true)
                    .build();
            certId = certificateRepository.save(cert).getId();
            certEarned = true;
            log.info(">>> Certificate issued: user={} course={} score={}", userId, courseId, score);
        }

        return QuizResultDTO.builder()
                .score(score)
                .correctCount(correct)
                .totalQuestions(questions.size())
                .passed(passed)
                .passingScore(course.getPassingScore())
                .certificateEarned(certEarned)
                .certificateId(certId)
                .feedback(feedback)
                .build();
    }

    @Override
    public List<CourseResponseDTO> getMyEnrolledCourses(String userId) {
        List<CourseEnrollment> enrollments = enrollmentRepository.findByUserId(userId);
        return enrollments.stream().map(e -> {
            Course course = courseRepository.findById(e.getCourseId()).orElse(null);
            if (course == null) return null;
            return toSummaryDTO(course, toProgressDTO(e));
        }).filter(dto -> dto != null).collect(Collectors.toList());
    }

    @Override
    public List<CertificateResponseDTO> getMyCertificates(String userId) {
        return certificateRepository.findByUserId(userId).stream()
                .map(this::toCertDTO)
                .collect(Collectors.toList());
    }

    // ================================================================
    //  Admin CRUD
    // ================================================================

    @Override
    public CourseResponseDTO createCourse(SaveCourseRequest req) {
        if (courseRepository.existsBySlug(req.getSlug())) {
            throw new AppException(ErrorCode.COURSE_SLUG_EXISTS, "Slug already exists: " + req.getSlug());
        }
        Course course = fromRequest(req, new Course());
        return toDetailDTO(courseRepository.save(course), null);
    }

    @Override
    public CourseResponseDTO updateCourse(String courseId, SaveCourseRequest req) {
        Course existing = findCourse(courseId);
        // Allow slug update only if unchanged or not taken by another
        if (!existing.getSlug().equals(req.getSlug()) && courseRepository.existsBySlug(req.getSlug())) {
            throw new AppException(ErrorCode.COURSE_SLUG_EXISTS, "Slug already exists: " + req.getSlug());
        }
        Course updated = fromRequest(req, existing);
        return toDetailDTO(courseRepository.save(updated), null);
    }

    @Override
    public void deleteCourse(String courseId) {
        findCourse(courseId);
        courseRepository.deleteById(courseId);
    }

    @Override
    public List<CourseResponseDTO> getAllCoursesAdmin() {
        return courseRepository.findAll().stream()
                .map(c -> {
                    CourseResponseDTO dto = toSummaryDTO(c, null);
                    dto.setTotalEnrollments(enrollmentRepository.countByCourseId(c.getId()));
                    dto.setTotalCompletions(enrollmentRepository.countByCourseIdAndIsCompletedTrue(c.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private Course findCourse(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND, "Course not found: " + id));
    }

    private CourseEnrollment findEnrollment(String userId, String courseId) {
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND,
                        "Not enrolled in course: " + courseId));
    }

    private void recalcCompletion(CourseEnrollment e, Course c) {
        int total = c.getLessonIds().size() + c.getReadingIds().size() + 1; // +1 for quiz
        int done = e.getCompletedLessonIds().size()
                + e.getCompletedReadingIds().size()
                + (e.getQuizScore() != null ? 1 : 0);
        e.setCompletionRate(Math.round((double) done / total * 1000) / 10.0);

        boolean allLessons = e.getCompletedLessonIds().containsAll(c.getLessonIds());
        boolean allReadings = e.getCompletedReadingIds().containsAll(c.getReadingIds());
        boolean quizPassed = e.getQuizScore() != null && e.getQuizScore() >= c.getPassingScore();
        if (allLessons && allReadings && quizPassed && !e.isCompleted()) {
            e.setCompleted(true);
            e.setCompletedAt(LocalDateTime.now());
        }
    }

    private Course fromRequest(SaveCourseRequest req, Course target) {
        target.setTitle(req.getTitle());
        target.setShortDescription(req.getShortDescription());
        target.setDescription(req.getDescription());
        target.setSlug(req.getSlug());
        target.setType(req.getType());
        target.setThumbnail(req.getThumbnail());
        target.setDifficulty(req.getDifficulty());
        target.setEstimatedHours(req.getEstimatedHours());
        target.setLessonIds(req.getLessonIds() != null ? req.getLessonIds() : new ArrayList<>());
        target.setReadingIds(req.getReadingIds() != null ? req.getReadingIds() : new ArrayList<>());
        target.setPassingScore(req.getPassingScore() > 0 ? req.getPassingScore() : 70);
        target.setActive(req.isActive());
        if (req.getQuizQuestions() != null) {
            target.setQuizQuestions(req.getQuizQuestions().stream()
                    .map(q -> Course.QuizQuestion.builder()
                            .question(q.getQuestion())
                            .options(q.getOptions())
                            .correctIndex(q.getCorrectIndex())
                            .explanation(q.getExplanation())
                            .category(q.getCategory())
                            .build())
                    .collect(Collectors.toList()));
        }
        return target;
    }

    private CourseResponseDTO toSummaryDTO(Course c, CourseResponseDTO.EnrollmentProgressDTO progress) {
        return CourseResponseDTO.builder()
                .id(c.getId())
                .title(c.getTitle())
                .shortDescription(c.getShortDescription())
                .slug(c.getSlug())
                .type(c.getType())
                .learningPathType(c.getLearningPathType())
                .thumbnail(c.getThumbnail())
                .difficulty(c.getDifficulty())
                .estimatedHours(c.getEstimatedHours())
                .totalLessons(c.getLessonIds().size())
                .totalReadings(c.getReadingIds().size())
                .totalQuizQuestions(c.getQuizQuestions().size())
                .passingScore(c.getPassingScore())
                .isActive(c.isActive())
                .createdAt(c.getCreatedAt())
                .myProgress(progress)
                .build();
    }

    private CourseResponseDTO toDetailDTO(Course c, CourseResponseDTO.EnrollmentProgressDTO progress) {
        CourseResponseDTO dto = toSummaryDTO(c, progress);
        dto.setDescription(c.getDescription());

        // Populate lessons
        if (!c.getLessonIds().isEmpty()) {
            List<VoiceLesson> lessons = lessonRepository.findAllById(c.getLessonIds());
            dto.setLessons(lessons.stream().map(l -> VoiceLessonResponseDTO.builder()
                    .id(l.getId()).title(l.getTitle()).category(l.getCategory())
                    .difficulty(l.getDifficulty()).description(l.getDescription())
                    .thumbnailUrl(l.getThumbnailUrl()).build())
                    .collect(Collectors.toList()));
        }

        // Populate readings
        if (!c.getReadingIds().isEmpty()) {
            List<ReadingGuide> readings = readingGuideRepository.findAllById(c.getReadingIds());
            dto.setReadings(readings.stream().map(r -> CourseResponseDTO.ReadingGuideDTO.builder()
                    .id(r.getId()).title(r.getTitle()).category(r.getCategory())
                    .thumbnail(r.getThumbnail()).author(r.getAuthor()).build())
                    .collect(Collectors.toList()));
        }

        // Quiz questions — omit correctIndex for public view
        dto.setQuizQuestions(c.getQuizQuestions().stream()
                .map(q -> CourseResponseDTO.QuizQuestionDTO.builder()
                        .question(q.getQuestion())
                        .options(q.getOptions())
                        .category(q.getCategory())
                        .build())
                .collect(Collectors.toList()));

        return dto;
    }

    private CourseResponseDTO.EnrollmentProgressDTO toProgressDTO(CourseEnrollment e) {
        return CourseResponseDTO.EnrollmentProgressDTO.builder()
                .enrollmentId(e.getId())
                .completedLessonIds(e.getCompletedLessonIds())
                .completedReadingIds(e.getCompletedReadingIds())
                .quizScore(e.getQuizScore())
                .quizAttempts(e.getQuizAttempts())
                .completionRate(e.getCompletionRate())
                .isCompleted(e.isCompleted())
                .enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt())
                .build();
    }

    private CertificateResponseDTO toCertDTO(Certificate c) {
        return CertificateResponseDTO.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .courseId(c.getCourseId())
                .courseName(c.getCourseName())
                .userName(c.getUserName())
                .completionScore(c.getCompletionScore())
                .imageUrl(c.getImageUrl())
                .isVerified(c.isVerified())
                .issuedAt(c.getIssuedAt())
                .build();
    }
}
