package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.SystemLog;
import com.mchub.services.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class LogController {

    private final LogService logService;

    /** SSE stream — browser connects here, receives live log events */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return logService.streamLogs();
    }

    /** Last N logs with optional ?level=ERROR&source=AI filters */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemLog>>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(ApiResponse.success(logService.getLogs(level, source, limit)));
    }

    /** AI service pushes logs here */
    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@RequestBody SystemLog log) {
        logService.ingestExternal(log);
        return ResponseEntity.ok().build();
    }
}
