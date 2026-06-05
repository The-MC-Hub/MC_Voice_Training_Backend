package com.mchub.services.impl;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mchub.config.LogAppender;
import com.mchub.models.SystemLog;
import com.mchub.repositories.SystemLogRepository;
import com.mchub.services.LogService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5 min, client reconnects
    private static final int  SHORT_LOGGER = 30;

    private final SystemLogRepository logRepository;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        // Persist + broadcast every Java log event captured by LogAppender
        LogAppender.addListener(this::handleJavaLog);
    }

    // ── Java log handler ─────────────────────────────────────────────────────

    @Async
    public void handleJavaLog(ILoggingEvent event) {
        String logger = event.getLoggerName();
        if (logger.length() > SHORT_LOGGER) {
            int dot = logger.lastIndexOf('.');
            logger = dot >= 0 ? "…" + logger.substring(dot) : logger.substring(logger.length() - SHORT_LOGGER);
        }

        SystemLog entry = SystemLog.builder()
            .level(event.getLevel().toString())
            .logger(logger)
            .message(event.getFormattedMessage())
            .source("JAVA")
            .thread(event.getThreadName())
            .timestamp(Instant.ofEpochMilli(event.getTimeStamp()))
            .build();

        try { logRepository.save(entry); } catch (Exception ignored) {}
        broadcast(entry);
    }

    // ── SSE ──────────────────────────────────────────────────────────────────

    @Override
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e       -> emitters.remove(emitter));

        // Send last 100 persisted logs immediately on connect
        try {
            List<SystemLog> recent = logRepository.findTop200ByOrderByTimestampDesc();
            for (int i = recent.size() - 1; i >= 0; i--) {
                emitter.send(SseEmitter.event().name("log").data(recent.get(i)));
            }
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    private void broadcast(SystemLog entry) {
        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("log").data(entry));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    // ── External ingest (AI service) ─────────────────────────────────────────

    @Override
    public void ingestExternal(SystemLog logEntry) {
        logEntry.setSource("AI");
        if (logEntry.getTimestamp() == null) logEntry.setTimestamp(Instant.now());
        try { logRepository.save(logEntry); } catch (Exception ignored) {}
        broadcast(logEntry);
    }

    // ── Query ────────────────────────────────────────────────────────────────

    @Override
    public List<SystemLog> getLogs(String level, String source, int limit) {
        if (level != null && source != null)
            return logRepository.findTop200ByLevelAndSourceOrderByTimestampDesc(level.toUpperCase(), source.toUpperCase());
        if (level != null)
            return logRepository.findTop200ByLevelOrderByTimestampDesc(level.toUpperCase());
        if (source != null)
            return logRepository.findTop200BySourceOrderByTimestampDesc(source.toUpperCase());
        return logRepository.findTop200ByOrderByTimestampDesc();
    }

    // ── TTL cleanup scheduler — belt-and-suspenders (MongoDB TTL index is primary) ─

    @Scheduled(cron = "0 0 3 * * *") // 03:00 every day
    public void cleanOldLogs() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        logRepository.deleteByTimestampBefore(cutoff);
        log.info("System logs older than 7 days purged");
    }
}
