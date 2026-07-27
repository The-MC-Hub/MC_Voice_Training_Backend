package com.mchub.services.impl;

import com.mchub.services.SystemHealthService;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemHealthServiceImpl implements SystemHealthService {

  private final MongoTemplate mongoTemplate;

  @Override
  public Map<String, Object> getSystemHealth() {
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

    long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
    long heapMax = memoryBean.getHeapMemoryUsage().getMax();
    long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

    double memoryUsagePercent = (heapMax > 0) ? Math.round((double) heapUsed / heapMax * 1000.0) / 10.0 : 0.0;

    Map<String, Object> memoryStats = new LinkedHashMap<>();
    memoryStats.put("heapUsedMb", heapUsed / (1024 * 1024));
    memoryStats.put("heapMaxMb", heapMax / (1024 * 1024));
    memoryStats.put("nonHeapUsedMb", nonHeapUsed / (1024 * 1024));
    memoryStats.put("usagePercent", memoryUsagePercent);

    Map<String, Object> threadStats = new LinkedHashMap<>();
    threadStats.put("threadCount", threadBean.getThreadCount());
    threadStats.put("peakThreadCount", threadBean.getPeakThreadCount());
    threadStats.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());

    Map<String, Object> osStats = new LinkedHashMap<>();
    osStats.put("osArch", osBean.getArch());
    osStats.put("osName", osBean.getName());
    osStats.put("availableProcessors", osBean.getAvailableProcessors());
    osStats.put("systemLoadAverage", osBean.getSystemLoadAverage());
    osStats.put("uptimeHours", Math.round((uptimeMs / (1000.0 * 3600.0)) * 10.0) / 10.0);
    osStats.put("javaVersion", System.getProperty("java.version"));

    // DB Ping
    String dbStatus = "UP";
    try {
      mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
    } catch (Exception e) {
      dbStatus = "DOWN: " + e.getMessage();
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "UP");
    result.put("dbStatus", dbStatus);
    result.put("memory", memoryStats);
    result.put("threads", threadStats);
    result.put("system", osStats);
    return result;
  }
}
