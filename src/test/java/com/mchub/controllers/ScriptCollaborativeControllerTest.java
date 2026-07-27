package com.mchub.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.ScriptDocument;
import com.mchub.models.ScriptRevision;
import com.mchub.repositories.ScriptDocumentRepository;
import com.mchub.repositories.ScriptRevisionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ScriptCollaborativeController.class)
@ContextConfiguration(classes = {ScriptCollaborativeController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class ScriptCollaborativeControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ScriptDocumentRepository scriptRepository;
  @MockBean private ScriptRevisionRepository revisionRepository;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "user_101", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  @Test
  void getScriptByBooking_ExistingScript() throws Exception {
    ScriptDocument doc = new ScriptDocument();
    doc.setBookingId("book_100");
    doc.setTitle("Event Script #book_100");

    when(scriptRepository.findByBookingId("book_100")).thenReturn(Optional.of(doc));

    mockMvc
        .perform(get("/api/v1/scripts/booking/book_100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data.title").value("Event Script #book_100"));
  }

  @Test
  void addAnnotation_Success() throws Exception {
    ScriptDocument doc = new ScriptDocument();
    doc.setBookingId("book_100");

    ScriptDocument.Annotation annotation = new ScriptDocument.Annotation(15, "mc_101", "Nhat giong o day", "PAUSE");

    when(scriptRepository.findByBookingId("book_100")).thenReturn(Optional.of(doc));
    when(scriptRepository.save(any(ScriptDocument.class))).thenReturn(doc);

    mockMvc
        .perform(
            post("/api/v1/scripts/booking/book_100/annotations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(annotation)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void getRevisions_Success() throws Exception {
    ScriptRevision rev = new ScriptRevision("script_1", "book_100", "user_101", "Kich ban v1", 1);
    when(revisionRepository.findByBookingIdOrderByRevisionNumberDesc("book_100"))
        .thenReturn(List.of(rev));

    mockMvc
        .perform(get("/api/v1/scripts/booking/book_100/revisions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data[0].revisionNumber").value(1));
  }
}
