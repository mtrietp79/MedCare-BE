package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class DoctorScheduleService {

    private static final Set<String> ALLOWED_SHIFTS = Set.of("MORNING", "AFTERNOON", "ALL_DAY", "EVENING");

    @Autowired
    private DoctorScheduleRepository repository;

    @Autowired
    private DoctorRepository doctorRepository;

    public List<DoctorSchedule> getAllSchedules() {
        return repository.findAll();
    }

    public List<DoctorSchedule> getSchedulesByDate(LocalDate date) {
        return repository.findByWorkDate(date);
    }

    public List<DoctorSchedule> getSchedulesByDoctor(Integer doctorId) {
        return repository.findByDoctorId(doctorId);
    }

    public DoctorSchedule getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich truc ID: " + id));
    }

    public DoctorSchedule createSchedule(DoctorSchedule schedule) {
        validateSchedule(schedule);
        return repository.save(schedule);
    }

    public void deleteSchedule(Integer id) {
        if (!repository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich truc ID: " + id);
        }
        repository.deleteById(id);
    }

    private void validateSchedule(DoctorSchedule schedule) {
        if (schedule == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu lich truc khong hop le.");
        }
        if (schedule.getDoctor() == null || schedule.getDoctor().getId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich truc phai co doctorId.");
        }
        if (schedule.getWorkDate() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich truc phai co ngay lam viec.");
        }
        if (schedule.getShift() == null || schedule.getShift().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Lich truc phai co ca lam viec.");
        }
        if (!doctorRepository.existsById(schedule.getDoctor().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Khong tim thay doctorId: " + schedule.getDoctor().getId());
        }

        String normalizedShift = schedule.getShift().trim().toUpperCase();
        if (!ALLOWED_SHIFTS.contains(normalizedShift)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ca lam viec khong hop le.");
        }
        if (repository.existsByDoctorIdAndWorkDateAndShift(
                schedule.getDoctor().getId(),
                schedule.getWorkDate(),
                normalizedShift
        )) {
            throw new BusinessException(HttpStatus.CONFLICT, "Bac si da co lich truc cho ngay va ca nay.");
        }
        schedule.setShift(normalizedShift);
    }
}
