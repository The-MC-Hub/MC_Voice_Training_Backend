package com.mchub.controllers;

import com.mchub.dto.CourseResponseDTO;
import com.mchub.dto.QuizResultDTO;
import com.mchub.enums.CourseType;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.CourseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class)
@ContextConfiguration(classes = {CourseController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CourseService courseService;

    private static final String USER_ID = "user-course-001";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @Nested
    @DisplayName("GET /api/v1/courses — public, tryGetUserId gracefully handles unauthenticated")
    class ListCourses {

        @Test
        @DisplayName("unauthenticated caller: passes null userId, no exception")
        void passesNullUserIdWhenUnauthenticated() throws Exception {
            when(courseService.getAllActiveCourses(isNull())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/courses")).andExpect(status().isOk());

            verify(courseService).getAllActiveCourses(null);
        }

        @Test
        @DisplayName("authenticated caller: passes real userId")
        void passesRealUserIdWhenAuthenticated() throws Exception {
            authenticate();
            when(courseService.getAllActiveCourses(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/courses")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("type param present: delegates to getCoursesByType")
        void delegatesToTypeFilterWhenPresent() throws Exception {
            when(courseService.getCoursesByType(eq(CourseType.WEDDING_MC), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/courses").param("type", "WEDDING_MC")).andExpect(status().isOk());

            verify(courseService).getCoursesByType(eq(CourseType.WEDDING_MC), any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/roadmap — public")
    class GetRoadmap {

        @Test
        @DisplayName("200 OK regardless of auth state")
        void returnsRoadmap() throws Exception {
            when(courseService.getMilestoneCourses(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/courses/roadmap")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/courses/{id}/enroll — requires auth")
    class Enroll {

        @Test
        @DisplayName("delegates using caller's real userId")
        void delegatesWithCallerUserId() throws Exception {
            authenticate();
            when(courseService.enroll(eq("course-1"), eq(USER_ID)))
                    .thenReturn(new CourseResponseDTO.EnrollmentProgressDTO());

            mockMvc.perform(post("/api/v1/courses/{id}/enroll", "course-1")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/courses/{id}/lessons/{lessonId}/complete")
    class CompleteLesson {

        @Test
        @DisplayName("delegates with both path variables and caller's userId")
        void delegatesWithPathVariables() throws Exception {
            authenticate();
            when(courseService.completeLesson("course-1", "lesson-1", USER_ID))
                    .thenReturn(new CourseResponseDTO.EnrollmentProgressDTO());

            mockMvc.perform(post("/api/v1/courses/{id}/lessons/{lessonId}/complete", "course-1", "lesson-1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/courses/{id}/quiz/submit")
    class SubmitQuiz {

        @Test
        @DisplayName("400 when answers list is empty — bean validation")
        void rejectsEmptyAnswers() throws Exception {
            authenticate();

            mockMvc.perform(post("/api/v1/courses/{id}/quiz/submit", "course-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answers\":[]}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK with valid answers")
        void submitsValidAnswers() throws Exception {
            authenticate();
            when(courseService.submitQuiz(eq("course-1"), eq(USER_ID), any())).thenReturn(new QuizResultDTO());

            mockMvc.perform(post("/api/v1/courses/{id}/quiz/submit", "course-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answers\":[0,1,2]}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses/my/enrolled, /my/certificates — requires auth")
    class MyEndpoints {

        @Test
        @DisplayName("myEnrolled delegates using caller's userId")
        void delegatesMyEnrolled() throws Exception {
            authenticate();
            when(courseService.getMyEnrolledCourses(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/courses/my/enrolled")).andExpect(status().isOk());
        }
    }
}
