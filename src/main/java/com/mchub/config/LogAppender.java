package com.mchub.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Logback appender that buffers recent log events in-memory and
 * notifies registered listeners (LogService SSE emitters).
 */
public class LogAppender extends AppenderBase<ILoggingEvent> {

    private static final int BUFFER_SIZE = 500;

    // Singleton state — one appender per JVM
    private static final BlockingQueue<ILoggingEvent> buffer = new ArrayBlockingQueue<>(BUFFER_SIZE);
    private static final CopyOnWriteArrayList<Consumer<ILoggingEvent>> listeners = new CopyOnWriteArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        if (buffer.remainingCapacity() == 0) buffer.poll();
        buffer.offer(event);
        listeners.forEach(l -> l.accept(event));
    }

    public static BlockingQueue<ILoggingEvent> getBuffer() { return buffer; }

    public static void addListener(Consumer<ILoggingEvent> listener) { listeners.add(listener); }

    public static void removeListener(Consumer<ILoggingEvent> listener) { listeners.remove(listener); }
}
