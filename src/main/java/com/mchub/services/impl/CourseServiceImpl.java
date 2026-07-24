package com.mchub.services.impl;

import com.mchub.dto.*;
import com.mchub.enums.CourseType;
import com.mchub.enums.LearningPathType;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.*;
import com.mchub.repositories.*;
import com.mchub.services.CourseService;
import com.mchub.services.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final GamificationService gamificationService;
    private final CaseStudyRepository caseStudyRepository;
    private final PracticeSessionRepository practiceSessionRepository;

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
    public List<CourseResponseDTO> getAllActiveCourses(String userId) {
        List<Course> courses = courseRepository.findByIsActiveTrue();
        return mapWithProgress(courses, userId);
    }

    @Override
    public List<CourseResponseDTO> getCoursesByType(CourseType type, String userId) {
        List<Course> courses = courseRepository.findByTypeAndIsActiveTrue(type);
        return mapWithProgress(courses, userId);
    }

    private List<CourseResponseDTO> mapWithProgress(List<Course> courses, String userId) {
        if (userId == null || courses.isEmpty()) {
            return courses.stream().map(c -> toSummaryDTO(c, null)).collect(Collectors.toList());
        }
        // Batch-fetch all enrollments for this user — no DB calls inside loop
        java.util.Map<String, CourseEnrollment> enrollmentMap = enrollmentRepository.findByUserId(userId)
                .stream().collect(Collectors.toMap(CourseEnrollment::getCourseId, e -> e, (a, b) -> a));
        return courses.stream()
                .map(c -> toSummaryDTO(c, enrollmentMap.containsKey(c.getId()) ? toProgressDTO(enrollmentMap.get(c.getId())) : null))
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
        CourseResponseDTO dto = toDetailDTO(course, progress);
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            boolean purchased = user != null && user.getPurchasedCourseIds() != null
                    && user.getPurchasedCourseIds().contains(courseId);
            dto.setPurchased(purchased);
            dto.setHasAccess(hasCourseAccess(courseId, userId));
        }
        return dto;
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
        if (!hasCourseAccess(courseId, userId)) {
            throw new AppException(ErrorCode.COURSE_REQUIRES_PLAN,
                    "Course requires BASIC plan or higher, or individual purchase");
        }
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .build();
        return toProgressDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    public CourseResponseDTO.EnrollmentProgressDTO giftEnroll(String courseId, String userId) {
        findCourse(courseId);
        // Grant permanent ownership — same as individual purchase (199k), independent of plan
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getPurchasedCourseIds() == null) {
                user.setPurchasedCourseIds(new java.util.ArrayList<>());
            }
            if (!user.getPurchasedCourseIds().contains(courseId)) {
                user.getPurchasedCourseIds().add(courseId);
                userRepository.save(user);
            }
        });
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return toProgressDTO(enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND, "Enrollment not found")));
        }
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .build();
        return toProgressDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    public CourseResponseDTO.EnrollmentProgressDTO completeLesson(String courseId, String lessonId, String userId) {
        if (!hasCourseAccess(courseId, userId)) {
            throw new AppException(ErrorCode.COURSE_REQUIRES_PLAN,
                    "Plan expired. Renew to continue learning.");
        }
        Course course = findCourse(courseId);
        if (!course.getLessonIds().contains(lessonId)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Lesson does not belong to this course: " + lessonId);
        }
        CourseEnrollment enrollment = findEnrollment(userId, courseId);
        if (!enrollment.getCompletedLessonIds().contains(lessonId)) {
            enrollment.getCompletedLessonIds().add(lessonId);
            recalcCompletion(enrollment, course);
            enrollmentRepository.save(enrollment);
        }
        return toProgressDTO(enrollment);
    }

    @Override
    public CourseResponseDTO.EnrollmentProgressDTO completeReading(String courseId, String readingId, String userId) {
        if (!hasCourseAccess(courseId, userId)) {
            throw new AppException(ErrorCode.COURSE_REQUIRES_PLAN,
                    "Plan expired. Renew to continue learning.");
        }
        Course course = findCourse(courseId);
        if (!course.getReadingIds().contains(readingId)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Reading does not belong to this course: " + readingId);
        }
        CourseEnrollment enrollment = findEnrollment(userId, courseId);
        if (!enrollment.getCompletedReadingIds().contains(readingId)) {
            enrollment.getCompletedReadingIds().add(readingId);
            recalcCompletion(enrollment, course);
            enrollmentRepository.save(enrollment);
        }
        return toProgressDTO(enrollment);
    }

    @Override
    public ExerciseResultDTO completeExercise(String courseId, String exerciseId, String userId, ExerciseSubmitRequest request) {
        if (!hasCourseAccess(courseId, userId)) {
            throw new AppException(ErrorCode.COURSE_REQUIRES_PLAN,
                    "Plan expired. Renew to continue learning.");
        }
        Course course = findCourse(courseId);
        Course.Exercise exercise = course.getExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_FAILED, "Exercise does not belong to this course: " + exerciseId));

        List<String> submitted = request.getAnswer() != null ? request.getAnswer() : List.of();
        boolean correct = submitted.equals(exercise.getItems());

        CourseEnrollment enrollment = findEnrollment(userId, courseId);
        if (correct && !enrollment.getCompletedExerciseIds().contains(exerciseId)) {
            enrollment.getCompletedExerciseIds().add(exerciseId);
            recalcCompletion(enrollment, course);
            enrollmentRepository.save(enrollment);
        }

        return ExerciseResultDTO.builder()
                .correct(correct)
                .explanation(exercise.getExplanation())
                .progress(toProgressDTO(enrollment))
                .build();
    }

    @Override
    public QuizResultDTO submitQuiz(String courseId, String userId, QuizSubmitRequest request) {
        if (!hasCourseAccess(courseId, userId)) {
            throw new AppException(ErrorCode.COURSE_REQUIRES_PLAN,
                    "Plan expired. Renew to continue learning.");
        }
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

        boolean wasCompletedBefore = enrollment.isCompleted();
        enrollment.setQuizScore(score);
        enrollment.setQuizAttempts(enrollment.getQuizAttempts() + 1);
        recalcCompletion(enrollment, course);
        enrollmentRepository.save(enrollment);
        boolean courseCompletedNow = !wasCompletedBefore && enrollment.isCompleted();

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

        // Award XP + voucher on first-time full course completion — best-effort, never fails the quiz submission
        String voucherCode = null;
        Double xpEarned = null;
        if (courseCompletedNow) {
            try {
                voucherCode = gamificationService.processCourseCompletion(userId, courseId, score);
                xpEarned = 200.0;
            } catch (Exception e) {
                log.error("Failed to process gamification for course completion: user={} course={}", userId, courseId, e);
            }
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
                .courseCompletedNow(courseCompletedNow)
                .xpEarned(xpEarned)
                .voucherCode(voucherCode)
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
    public CourseProgressStatsDTO getCourseProgressStats(String courseId, String userId) {
        Course course = findCourse(courseId);
        Set<String> lessonIdSet = new java.util.HashSet<>(course.getLessonIds());

        // Batch-fetch all sessions for this user, filter in-memory to this course's lessons —
        // avoids per-lesson DB calls in a loop
        List<PracticeSession> sessions = practiceSessionRepository.findByUserId(userId).stream()
                .filter(s -> lessonIdSet.contains(s.getLessonId()))
                .collect(Collectors.toList());

        double totalHours = sessions.stream().mapToDouble(PracticeSession::getDurationSeconds).sum() / 3600.0;
        double avgScore = sessions.stream().mapToDouble(PracticeSession::getOverallScore).average().orElse(0.0);

        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        Map<String, List<PracticeSession>> byDate = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().atZone(zone).toLocalDate().toString()));
        List<CourseProgressStatsDTO.ScorePoint> scoreOverTime = byDate.entrySet().stream()
                .map(en -> CourseProgressStatsDTO.ScorePoint.builder()
                        .date(en.getKey())
                        .avgScore(en.getValue().stream().mapToDouble(PracticeSession::getOverallScore).average().orElse(0.0))
                        .build())
                .sorted(Comparator.comparing(CourseProgressStatsDTO.ScorePoint::getDate))
                .collect(Collectors.toList());

        Map<String, List<PracticeSession>> byLesson = sessions.stream()
                .collect(Collectors.groupingBy(PracticeSession::getLessonId));
        Map<String, VoiceLesson> lessonMap = lessonRepository.findAllById(byLesson.keySet()).stream()
                .collect(Collectors.toMap(VoiceLesson::getId, l -> l));
        List<CourseProgressStatsDTO.WeakLesson> weakest = byLesson.entrySet().stream()
                .map(en -> {
                    VoiceLesson l = lessonMap.get(en.getKey());
                    double avg = en.getValue().stream().mapToDouble(PracticeSession::getOverallScore).average().orElse(0.0);
                    return CourseProgressStatsDTO.WeakLesson.builder()
                            .lessonId(en.getKey())
                            .lessonTitle(l != null ? l.getTitle() : en.getKey())
                            .avgScore(avg)
                            .attempts(en.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparingDouble(CourseProgressStatsDTO.WeakLesson::getAvgScore))
                .limit(3)
                .collect(Collectors.toList());

        return CourseProgressStatsDTO.builder()
                .totalPracticeHours(Math.round(totalHours * 10) / 10.0)
                .totalSessions(sessions.size())
                .avgScore(Math.round(avgScore * 10) / 10.0)
                .scoreOverTime(scoreOverTime)
                .weakestLessons(weakest)
                .build();
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
                    dto.setTotalCompletions(enrollmentRepository.countByCourseIdAndCompletedTrue(c.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CourseResponseDTO updatePricing(String courseId, Integer priceVnd, Integer discountPercent) {
        Course course = findCourse(courseId);
        if (priceVnd != null) {
            if (priceVnd < 0) throw new AppException(ErrorCode.VALIDATION_FAILED, "priceVnd must be >= 0");
            course.setPriceVnd(priceVnd);
        }
        if (discountPercent != null) {
            if (discountPercent < 0 || discountPercent > 100)
                throw new AppException(ErrorCode.VALIDATION_FAILED, "discountPercent must be 0-100");
            course.setDiscountPercent(discountPercent);
        }
        return toSummaryDTO(courseRepository.save(course), null);
    }

    @Override
    public CourseResponseDTO updateOutcomes(String courseId, List<String> outcomes) {
        Course course = findCourse(courseId);
        course.setOutcomes(outcomes != null ? outcomes : new ArrayList<>());
        return toSummaryDTO(courseRepository.save(course), null);
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private Course findCourse(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND, "Course not found: " + id));
    }

    /** Any paid plan (not expired) OR individually purchased course → access granted */
    public boolean hasCourseAccess(String courseId, String userId) {
        if (userId == null) return false;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));
        if (user.getPurchasedCourseIds() != null && user.getPurchasedCourseIds().contains(courseId)) {
            return true;
        }
        boolean planActive = user.getPlanExpiresAt() != null
                && user.getPlanExpiresAt().isAfter(LocalDateTime.now());
        com.mchub.enums.SubscriptionPlan plan = user.getPlan();
        boolean isPaidPlan = plan == com.mchub.enums.SubscriptionPlan.DAILY
                || plan == com.mchub.enums.SubscriptionPlan.BASIC
                || plan == com.mchub.enums.SubscriptionPlan.FULL
                || plan == com.mchub.enums.SubscriptionPlan.ANNUAL;
        return isPaidPlan && planActive;
    }

    private CourseEnrollment findEnrollment(String userId, String courseId) {
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND,
                        "Not enrolled in course: " + courseId));
    }

    private void recalcCompletion(CourseEnrollment e, Course c) {
        int total = c.getLessonIds().size() + c.getReadingIds().size() + c.getExercises().size() + 1; // +1 for quiz
        int done = e.getCompletedLessonIds().size()
                + e.getCompletedReadingIds().size()
                + e.getCompletedExerciseIds().size()
                + (e.getQuizScore() != null ? 1 : 0);
        e.setCompletionRate(Math.round((double) done / total * 1000) / 10.0);

        boolean allLessons = e.getCompletedLessonIds().containsAll(c.getLessonIds());
        boolean allReadings = e.getCompletedReadingIds().containsAll(c.getReadingIds());
        boolean allExercises = e.getCompletedExerciseIds().containsAll(
                c.getExercises().stream().map(Course.Exercise::getId).collect(Collectors.toList()));
        boolean quizPassed = e.getQuizScore() != null && e.getQuizScore() >= c.getPassingScore();
        if (allLessons && allReadings && allExercises && quizPassed && !e.isCompleted()) {
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
        target.setCaseStudyIds(req.getCaseStudyIds() != null ? req.getCaseStudyIds() : new ArrayList<>());
        target.setOutcomes(req.getOutcomes() != null ? req.getOutcomes() : new ArrayList<>());
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
        if (req.getExercises() != null) {
            target.setExercises(req.getExercises().stream()
                    .map(ex -> Course.Exercise.builder()
                            .id(ex.getId() != null ? ex.getId() : java.util.UUID.randomUUID().toString())
                            .type(ex.getType())
                            .prompt(ex.getPrompt())
                            .items(ex.getItems() != null ? ex.getItems() : new ArrayList<>())
                            .distractors(ex.getDistractors() != null ? ex.getDistractors() : new ArrayList<>())
                            .explanation(ex.getExplanation())
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
                .totalExercises(c.getExercises().size())
                .outcomes(c.getOutcomes())
                .passingScore(c.getPassingScore())
                .isActive(c.isActive())
                .createdAt(c.getCreatedAt())
                .priceVnd(c.getPriceVnd())
                .discountPercent(c.getDiscountPercent())
                .finalPriceVnd(finalPrice(c))
                .myProgress(progress)
                .build();
    }

    private int finalPrice(Course c) {
        int pct = Math.max(0, Math.min(100, c.getDiscountPercent()));
        return (int) Math.round(c.getPriceVnd() * (100 - pct) / 100.0);
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
                    .thumbnailUrl(l.getThumbnailUrl()).videoUrl(l.getVideoUrl()).build())
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

        // Exercises — shuffle items+distractors together for display, correct order not exposed
        if (!c.getExercises().isEmpty()) {
            dto.setExercises(c.getExercises().stream()
                    .map(ex -> {
                        List<String> display = new ArrayList<>(ex.getItems());
                        display.addAll(ex.getDistractors());
                        java.util.Collections.shuffle(display);
                        return CourseResponseDTO.ExerciseDTO.builder()
                                .id(ex.getId())
                                .type(ex.getType())
                                .prompt(ex.getPrompt())
                                .displayOptions(display)
                                .build();
                    })
                    .collect(Collectors.toList()));
        }

        // Case studies — summary only (id/title/videoUrl), full transcript on dedicated view endpoint
        if (!c.getCaseStudyIds().isEmpty()) {
            dto.setCaseStudies(caseStudyRepository.findAllById(c.getCaseStudyIds()).stream()
                    .map(cs -> CourseResponseDTO.CaseStudySummaryDTO.builder()
                            .id(cs.getId()).title(cs.getTitle()).videoUrl(cs.getVideoUrl()).build())
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private CourseResponseDTO.EnrollmentProgressDTO toProgressDTO(CourseEnrollment e) {
        return CourseResponseDTO.EnrollmentProgressDTO.builder()
                .enrollmentId(e.getId())
                .completedLessonIds(e.getCompletedLessonIds())
                .completedReadingIds(e.getCompletedReadingIds())
                .completedExerciseIds(e.getCompletedExerciseIds())
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
