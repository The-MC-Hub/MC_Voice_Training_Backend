package com.mchub.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.QuickReply;
import com.mchub.repositories.QuickReplyRepository;
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

@WebMvcTest(controllers = QuickReplyController.class)
@ContextConfiguration(classes = {QuickReplyController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class QuickReplyControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private QuickReplyRepository quickReplyRepository;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "mc_101", null, List.of(new SimpleGrantedAuthority("ROLE_MC"))));
  }

  @Test
  void getMyQuickReplies_Success() throws Exception {
    QuickReply reply = new QuickReply("mc_101", "Greeting", "Xin chao!", 1);
    when(quickReplyRepository.findByMcUserIdOrderByDisplayOrderAsc("mc_101"))
        .thenReturn(List.of(reply));

    mockMvc
        .perform(get("/api/v1/mcs/quick-replies"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data[0].title").value("Greeting"));
  }

  @Test
  void createQuickReply_Success() throws Exception {
    QuickReply request = new QuickReply("mc_101", "Quote", "Bao gia show", 1);
    when(quickReplyRepository.save(any(QuickReply.class))).thenReturn(request);

    mockMvc
        .perform(
            post("/api/v1/mcs/quick-replies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data.title").value("Quote"));
  }

  @Test
  void deleteQuickReply_Success() throws Exception {
    QuickReply reply = new QuickReply("mc_101", "Quote", "Bao gia show", 1);
    reply.setId("reply_1");
    when(quickReplyRepository.findById("reply_1")).thenReturn(Optional.of(reply));

    mockMvc
        .perform(delete("/api/v1/mcs/quick-replies/reply_1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    verify(quickReplyRepository).delete(reply);
  }
}
