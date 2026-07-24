package com.mchub.services;

import com.mchub.models.Schedule;

import java.util.List;

public interface AvailabilityService {

    Schedule createSchedule(String mcId, Schedule schedule);

    List<Schedule> getSchedules(String mcId);

    void deleteSchedule(String id, String mcId);
}
