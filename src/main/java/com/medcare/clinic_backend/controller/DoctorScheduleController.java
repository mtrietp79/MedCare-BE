package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.service.DoctorScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/doctor-schedules")
public class DoctorScheduleController {

    @Autowired
    private DoctorScheduleService service;

    @GetMapping
    public List<DoctorSchedule> getAll() {
        return service.getAllSchedules();
    }

    // API: Lấy lịch trực theo ngày (truyền tham số ?date=2026-04-21)
    @GetMapping("/filter")
    public List<DoctorSchedule> getByDate(@RequestParam String date) {
        return service.getSchedulesByDate(LocalDate.parse(date));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public DoctorSchedule create(@RequestBody DoctorSchedule schedule) {
        return service.createSchedule(schedule);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public void delete(@PathVariable Integer id) {
        service.deleteSchedule(id);
    }
}