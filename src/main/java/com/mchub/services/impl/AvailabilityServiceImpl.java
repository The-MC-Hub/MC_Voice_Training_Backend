package com.mchub.services.impl;

import com.mchub.models.Schedule;
import com.mchub.repositories.ScheduleRepository;
import com.mchub.services.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final ScheduleRepository scheduleRepository;

    @Override
    public Schedule createSchedule(String mcId, Schedule schedule) {
        schedule.setMc(mcId);
        return scheduleRepository.save(schedule);
    }

    @Override
    public List<Schedule> getSchedules(String mcId) {
        return scheduleRepository.findByMc(mcId);
    }

    @Override
    public void deleteSchedule(String id, String mcId) {
        scheduleRepository.deleteByIdAndMc(id, mcId);
    }
}
