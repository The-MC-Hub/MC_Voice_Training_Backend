package com.mchub.controllers.admin;

import com.mchub.dto.CourseResponseDTO;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCourseController.class)
@ContextConfiguration(classes = {AdminCourseController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminCourseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CourseService courseService;

    @Nested
    @DisplayName("GET /api/v1/admin/courses, /{id}")
    class Read {

        @Test
        @DisplayName("listAll delegates to getAllCoursesAdmin")
        void listsAll() throws Exception {
            when(courseService.getAllCoursesAdmin()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/courses")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("getOne passes null userId (admin view — no per-user enrollment context)")
        void getsOneWithNullUserId() throws Exception {
            when(courseService.getCourseDetail("course-1", null)).thenReturn(new CourseResponseDTO());

            mockMvc.perform(get("/api/v1/admin/courses/{id}", "course-1")).andExpect(status().isOk());

            verify(courseService).getCourseDetail("course-1", null);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/courses")
    class Create {

        @Test
        @DisplayName("400 when title is blank — bean validation")
        void rejectsBlankTitle() throws Exception {
            mockMvc.perform(post("/api/v1/admin/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\",\"slug\":\"x\",\"type\":\"WEDDING_MC\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when type is missing — bean validation")
        void rejectsMissingType() throws Exception {
            mockMvc.perform(post("/api/v1/admin/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Course\",\"slug\":\"course\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK with valid payload")
        void createsWithValidPayload() throws Exception {
            when(courseService.createCourse(any())).thenReturn(new CourseResponseDTO());

            mockMvc.perform(post("/api/v1/admin/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Course\",\"slug\":\"course\",\"type\":\"WEDDING_MC\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/courses/{id}/pricing")
    class UpdatePricing {

        @Test
        @DisplayName("delegates optional priceVnd/discountPercent params")
        void delegatesOptionalParams() throws Exception {
            when(courseService.updatePricing("course-1", 199000, 20)).thenReturn(new CourseResponseDTO());

            mockMvc.perform(patch("/api/v1/admin/courses/{id}/pricing", "course-1")
                            .param("priceVnd", "199000").param("discountPercent", "20"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("allows omitting both params (null passthrough)")
        void allowsOmittingParams() throws Exception {
            when(courseService.updatePricing("course-1", null, null)).thenReturn(new CourseResponseDTO());

            mockMvc.perform(patch("/api/v1/admin/courses/{id}/pricing", "course-1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/courses/{id}")
    class Delete {

        @Test
        @DisplayName("200 OK, delegates to deleteCourse")
        void deletesCourse() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/courses/{id}", "course-1")).andExpect(status().isOk());

            verify(courseService).deleteCourse("course-1");
        }
    }
}
