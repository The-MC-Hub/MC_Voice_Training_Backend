package com.mchub.services.impl;

import com.mchub.enums.SubscriptionPlan;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.dto.ExerciseSubmitRequest;
import com.mchub.dto.QuizSubmitRequest;
import com.mchub.enums.ExerciseType;
import com.mchub.models.Course;
import com.mchub.models.CourseEnrollment;
import com.mchub.models.PracticeSession;
import com.mchub.models.User;
import com.mchub.repositories.CaseStudyRepository;
import com.mchub.repositories.CertificateRepository;
import com.mchub.repositories.CourseEnrollmentRepository;
import com.mchub.repositories.CourseRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.ReadingGuideRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.VoiceLessonRepository;
import com.mchub.services.GamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CourseServiceImpl. Focused especially on completeLesson()/
 * completeReading() — these methods had a real bug fixed during the audit
 * (missing check that lessonId/readingId actually belongs to the course,
 * allowing progress-fraud). These tests pin that fix down as a regression
 * guard: if the ownership check is ever reverted, CPL-03/CPD-02 below fail.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseEnrollmentRepository enrollmentRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private VoiceLessonRepository lessonRepository;
    @Mock private ReadingGuideRepository readingGuideRepository;
    @Mock private UserRepository userRepository;
    @Mock private GamificationService gamificationService;
    @Mock private CaseStudyRepository caseStudyRepository;
    @Mock private PracticeSessionRepository practiceSessionRepository;

    private CourseServiceImpl courseService;

    private static final String COURSE_ID = "course-1";
    private static final String USER_ID = "user-1";
    private static final String LESSON_IN_COURSE = "lesson-A";
    private static final String LESSON_NOT_IN_COURSE = "lesson-ROGUE";
    private static final String READING_IN_COURSE = "reading-A";
    private static final String READING_NOT_IN_COURSE = "reading-ROGUE";

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(
                courseRepository, enrollmentRepository, certificateRepository,
                lessonRepository, readingGuideRepository, userRepository,
                gamificationService, caseStudyRepository, practiceSessionRepository);
    }

    private Course.CourseBuilder baseCourse() {
        return Course.builder()
                .id(COURSE_ID)
                .title("Test Course")
                .slug("test-course")
                .lessonIds(List.of(LESSON_IN_COURSE))
                .readingIds(List.of(READING_IN_COURSE))
                .passingScore(70);
    }

    private User activeSubscriberUser() {
        return User.builder()
                .id(USER_ID)
                .plan(SubscriptionPlan.BASIC)
                .planExpiresAt(LocalDateTime.now().plusDays(10))
                .build();
    }

    private CourseEnrollment.CourseEnrollmentBuilder baseEnrollment() {
        return CourseEnrollment.builder()
                .id("enrollment-1")
                .userId(USER_ID)
                .courseId(COURSE_ID);
    }

    @Nested
    @DisplayName("completeLesson — ownership validation (regression guard for fixed bug)")
    class CompleteLesson {

        @Test
        @DisplayName("marks a lesson that truly belongs to the course as completed")
        void completesLessonBelongingToCourse() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            CourseEnrollment enrollment = baseEnrollment().build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

            courseService.completeLesson(COURSE_ID, LESSON_IN_COURSE, USER_ID);

            assertThat(enrollment.getCompletedLessonIds()).contains(LESSON_IN_COURSE);
            verify(enrollmentRepository).save(enrollment);
        }

        @Test
        @DisplayName("REGRESSION GUARD: rejects a lessonId that does not belong to the course (progress-fraud bug, fixed)")
        void rejectsLessonNotBelongingToCourse() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));

            assertThatThrownBy(() -> courseService.completeLesson(COURSE_ID, LESSON_NOT_IN_COURSE, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("is idempotent: completing the same lesson twice does not duplicate or re-save")
        void idempotentOnRepeatedCompletion() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            CourseEnrollment enrollment = baseEnrollment()
                    .completedLessonIds(new java.util.ArrayList<>(List.of(LESSON_IN_COURSE)))
                    .build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));

            courseService.completeLesson(COURSE_ID, LESSON_IN_COURSE, USER_ID);

            assertThat(enrollment.getCompletedLessonIds()).containsExactly(LESSON_IN_COURSE);
            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws COURSE_REQUIRES_PLAN when the user's plan has expired")
        void rejectsWhenPlanExpired() {
            User expiredUser = User.builder()
                    .id(USER_ID)
                    .plan(SubscriptionPlan.BASIC)
                    .planExpiresAt(LocalDateTime.now().minusDays(1))
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(expiredUser));

            assertThatThrownBy(() -> courseService.completeLesson(COURSE_ID, LESSON_IN_COURSE, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COURSE_REQUIRES_PLAN);
        }

        @Test
        @DisplayName("throws COURSE_NOT_FOUND when courseId does not exist")
        void throwsWhenCourseMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courseService.completeLesson(COURSE_ID, LESSON_IN_COURSE, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("completeReading — ownership validation (regression guard for fixed bug, symmetric to completeLesson)")
    class CompleteReading {

        @Test
        @DisplayName("marks a reading that truly belongs to the course as completed")
        void completesReadingBelongingToCourse() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            CourseEnrollment enrollment = baseEnrollment().build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

            courseService.completeReading(COURSE_ID, READING_IN_COURSE, USER_ID);

            assertThat(enrollment.getCompletedReadingIds()).contains(READING_IN_COURSE);
        }

        @Test
        @DisplayName("REGRESSION GUARD: rejects a readingId that does not belong to the course")
        void rejectsReadingNotBelongingToCourse() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));

            assertThatThrownBy(() -> courseService.completeReading(COURSE_ID, READING_NOT_IN_COURSE, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);

            verify(enrollmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("hasCourseAccess")
    class HasCourseAccess {

        @Test
        @DisplayName("true when user purchased the course individually, regardless of plan")
        void trueWhenPurchasedIndividually() {
            User user = User.builder()
                    .id(USER_ID)
                    .plan(SubscriptionPlan.FREE)
                    .purchasedCourseIds(List.of(COURSE_ID))
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThat(courseService.hasCourseAccess(COURSE_ID, USER_ID)).isTrue();
        }

        @Test
        @DisplayName("true when user has an active paid plan")
        void trueWhenPlanActive() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));

            assertThat(courseService.hasCourseAccess(COURSE_ID, USER_ID)).isTrue();
        }

        @Test
        @DisplayName("false when plan has expired and course was not purchased")
        void falseWhenPlanExpired() {
            User user = User.builder()
                    .id(USER_ID)
                    .plan(SubscriptionPlan.BASIC)
                    .planExpiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThat(courseService.hasCourseAccess(COURSE_ID, USER_ID)).isFalse();
        }

        @Test
        @DisplayName("false when userId is null — short-circuits without a DB call")
        void falseWhenUserIdNull() {
            assertThat(courseService.hasCourseAccess(COURSE_ID, null)).isFalse();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("false for FREE plan with no individual purchase")
        void falseForFreePlan() {
            User user = User.builder().id(USER_ID).plan(SubscriptionPlan.FREE).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThat(courseService.hasCourseAccess(COURSE_ID, USER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("enroll")
    class Enroll {

        @Test
        @DisplayName("throws COURSE_ALREADY_ENROLLED when the user already has an enrollment")
        void throwsWhenAlreadyEnrolled() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            when(enrollmentRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(true);

            assertThatThrownBy(() -> courseService.enroll(COURSE_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COURSE_ALREADY_ENROLLED);
        }

        @Test
        @DisplayName("throws COURSE_REQUIRES_PLAN when user has no access (FREE, no purchase)")
        void throwsWhenNoAccess() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            when(enrollmentRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).plan(SubscriptionPlan.FREE).build()));

            assertThatThrownBy(() -> courseService.enroll(COURSE_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COURSE_REQUIRES_PLAN);
        }

        @Test
        @DisplayName("creates a new enrollment when access is granted and not already enrolled")
        void createsEnrollmentWhenEligible() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            when(enrollmentRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = courseService.enroll(COURSE_ID, USER_ID);

            assertThat(result).isNotNull();
            verify(enrollmentRepository).save(any(CourseEnrollment.class));
        }
    }

    @Nested
    @DisplayName("completeExercise")
    class CompleteExercise {

        private static final String EXERCISE_ID = "ex-1";

        private Course courseWithExercise() {
            return baseCourse()
                    .exercises(List.of(Course.Exercise.builder()
                            .id(EXERCISE_ID)
                            .type(ExerciseType.SENTENCE_ORDER)
                            .items(List.of("A", "B", "C"))
                            .explanation("Order matters")
                            .build()))
                    .build();
        }

        @Test
        @DisplayName("marks exercise complete and returns correct=true when answer matches items in order")
        void marksCompleteWhenAnswerCorrect() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseWithExercise()));
            CourseEnrollment enrollment = baseEnrollment().build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

            ExerciseSubmitRequest request = new ExerciseSubmitRequest();
            request.setAnswer(List.of("A", "B", "C"));

            var result = courseService.completeExercise(COURSE_ID, EXERCISE_ID, USER_ID, request);

            assertThat(result.isCorrect()).isTrue();
            assertThat(enrollment.getCompletedExerciseIds()).contains(EXERCISE_ID);
        }

        @Test
        @DisplayName("returns correct=false and does not mark complete when answer order is wrong")
        void doesNotMarkCompleteWhenAnswerWrong() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseWithExercise()));
            CourseEnrollment enrollment = baseEnrollment().build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));

            ExerciseSubmitRequest request = new ExerciseSubmitRequest();
            request.setAnswer(List.of("C", "B", "A"));

            var result = courseService.completeExercise(COURSE_ID, EXERCISE_ID, USER_ID, request);

            assertThat(result.isCorrect()).isFalse();
            assertThat(enrollment.getCompletedExerciseIds()).doesNotContain(EXERCISE_ID);
            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws VALIDATION_FAILED when exerciseId does not belong to the course")
        void rejectsExerciseNotBelongingToCourse() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseWithExercise()));

            ExerciseSubmitRequest request = new ExerciseSubmitRequest();
            request.setAnswer(List.of("A"));

            assertThatThrownBy(() -> courseService.completeExercise(COURSE_ID, "ex-ROGUE", USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("submitQuiz — gamification trigger on first-time course completion")
    class SubmitQuizGamification {

        private Course courseAllDoneExceptQuiz() {
            return baseCourse()
                    .quizQuestions(List.of(Course.QuizQuestion.builder()
                            .question("Q1").options(List.of("a", "b")).correctIndex(0).build()))
                    .build();
        }

        @Test
        @DisplayName("calls gamificationService.processCourseCompletion exactly once when course becomes fully completed")
        void triggersGamificationOnFirstFullCompletion() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            Course course = courseAllDoneExceptQuiz();
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            CourseEnrollment enrollment = baseEnrollment()
                    .completedLessonIds(new java.util.ArrayList<>(List.of(LESSON_IN_COURSE)))
                    .completedReadingIds(new java.util.ArrayList<>(List.of(READING_IN_COURSE)))
                    .completed(false)
                    .build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(certificateRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(false);
            when(certificateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(gamificationService.processCourseCompletion(USER_ID, COURSE_ID, 100)).thenReturn("COURSE30-ABC");

            QuizSubmitRequest request = new QuizSubmitRequest();
            request.setAnswers(List.of(0));

            var result = courseService.submitQuiz(COURSE_ID, USER_ID, request);

            assertThat(result.isCourseCompletedNow()).isTrue();
            assertThat(result.getVoucherCode()).isEqualTo("COURSE30-ABC");
            assertThat(result.getXpEarned()).isEqualTo(200.0);
            verify(gamificationService).processCourseCompletion(USER_ID, COURSE_ID, 100);
        }

        @Test
        @DisplayName("does NOT call gamificationService again on quiz retake after course already completed")
        void doesNotRetriggerOnRetake() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            Course course = courseAllDoneExceptQuiz();
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            CourseEnrollment enrollment = baseEnrollment()
                    .completedLessonIds(new java.util.ArrayList<>(List.of(LESSON_IN_COURSE)))
                    .completedReadingIds(new java.util.ArrayList<>(List.of(READING_IN_COURSE)))
                    .quizScore(100)
                    .completed(true)
                    .build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(certificateRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(true);

            QuizSubmitRequest request = new QuizSubmitRequest();
            request.setAnswers(List.of(0));

            var result = courseService.submitQuiz(COURSE_ID, USER_ID, request);

            assertThat(result.isCourseCompletedNow()).isFalse();
            verify(gamificationService, never()).processCourseCompletion(any(), any(), anyInt());
        }

        @Test
        @DisplayName("does not mark course completed and does not call gamification when quiz fails")
        void doesNotTriggerWhenQuizFails() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeSubscriberUser()));
            Course course = courseAllDoneExceptQuiz();
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
            CourseEnrollment enrollment = baseEnrollment()
                    .completedLessonIds(new java.util.ArrayList<>(List.of(LESSON_IN_COURSE)))
                    .completedReadingIds(new java.util.ArrayList<>(List.of(READING_IN_COURSE)))
                    .build();
            when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));
            when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

            QuizSubmitRequest request = new QuizSubmitRequest();
            request.setAnswers(List.of(1)); // wrong answer, index 1 != correctIndex 0

            var result = courseService.submitQuiz(COURSE_ID, USER_ID, request);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.isCourseCompletedNow()).isFalse();
            verify(gamificationService, never()).processCourseCompletion(any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("getCourseProgressStats")
    class GetCourseProgressStats {

        @Test
        @DisplayName("aggregates hours/sessions/avgScore only from sessions belonging to the course's lessons")
        void aggregatesOnlyCourseLessons() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            PracticeSession inCourse1 = PracticeSession.builder()
                    .id("s1").lessonId(LESSON_IN_COURSE).userId(USER_ID)
                    .overallScore(80.0).durationSeconds(120).createdAt(java.time.Instant.now()).build();
            PracticeSession inCourse2 = PracticeSession.builder()
                    .id("s2").lessonId(LESSON_IN_COURSE).userId(USER_ID)
                    .overallScore(90.0).durationSeconds(180).createdAt(java.time.Instant.now()).build();
            PracticeSession otherLesson = PracticeSession.builder()
                    .id("s3").lessonId("lesson-OUTSIDE-COURSE").userId(USER_ID)
                    .overallScore(10.0).durationSeconds(999).createdAt(java.time.Instant.now()).build();
            when(practiceSessionRepository.findByUserId(USER_ID)).thenReturn(List.of(inCourse1, inCourse2, otherLesson));
            when(lessonRepository.findAllById(any())).thenReturn(List.of());

            var stats = courseService.getCourseProgressStats(COURSE_ID, USER_ID);

            assertThat(stats.getTotalSessions()).isEqualTo(2);
            assertThat(stats.getAvgScore()).isEqualTo(85.0);
        }

        @Test
        @DisplayName("returns zeroed stats when user has no sessions for this course")
        void returnsZeroedStatsWhenNoSessions() {
            when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(baseCourse().build()));
            when(practiceSessionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            var stats = courseService.getCourseProgressStats(COURSE_ID, USER_ID);

            assertThat(stats.getTotalSessions()).isZero();
            assertThat(stats.getAvgScore()).isZero();
            assertThat(stats.getWeakestLessons()).isEmpty();
        }
    }
}
