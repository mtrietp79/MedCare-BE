package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class DoctorScheduleService {

    @Autowired
    private DoctorScheduleRepository repository;

    public List<DoctorSchedule> getAllSchedules() {
        return repository.findAll();
    }

    public List<DoctorSchedule> getSchedulesByDate(LocalDate date) {
        return repository.findByWorkDate(date);
    }

    public DoctorSchedule createSchedule(DoctorSchedule schedule) {
        return repository.save(schedule);
    }

    public void deleteSchedule(Integer id) {
        repository.deleteById(id);
    }
}