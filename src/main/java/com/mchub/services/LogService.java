package com.mchub.services;

import com.mchub.models.SystemLog;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface LogService {

    /** Register a new SSE emitter and immediately send buffered recent logs. */
    SseEmitter streamLogs();

    /** Ingest a log entry from an external source (e.g. AI service). */
    void ingestExternal(SystemLog log);

    /** Query persisted logs with optional filters. */
    List<SystemLog> getLogs(String level, String source, int limit);

    /** Scheduled cleanup of old persisted logs. */
    void cleanOldLogs();
}
