package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.service.DoctorScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/doctor-schedules", "/api/schedules"})
public class DoctorScheduleController {

    @Autowired
    private DoctorScheduleService service;

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<DoctorSchedule> getAll() {
        return service.getAllSchedules();
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public List<DoctorSchedule> getByDate(@RequestParam String date) {
        return service.getSchedulesByDate(LocalDate.parse(date));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public List<DoctorSchedule> getMySchedules() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
        return service.getSchedulesByDoctor(currentDoctor.getId());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public DoctorSchedule create(@RequestBody DoctorSchedule schedule) {
        if (schedule == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu lich truc khong hop le.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            schedule.setDoctor(currentDoctor);
        } else if (schedule.getDoctor() == null || schedule.getDoctor().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Admin phai chi dinh doctorId khi tao lich truc.");
        }

        return service.createSchedule(schedule);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DOCTOR', 'ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_DOCTOR") && !hasAuthority(authentication, "ROLE_ADMIN")) {
            Doctor currentDoctor = getCurrentDoctorOrThrow(authentication);
            DoctorSchedule schedule = service.getById(id);
            if (schedule.getDoctor() == null || !currentDoctor.getId().equals(schedule.getDoctor().getId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "Ban khong co quyen xoa lich truc cua bac si khac.");
            }
        }
        service.deleteSchedule(id);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Doctor getCurrentDoctorOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }

        return doctorRepository.findByAccount_Username(authentication.getName())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "Tai khoan doctor chua duoc lien ket voi ho so bac si."
                ));
    }
}
