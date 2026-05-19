package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    public List<DoctorSchedule> getSchedulesByDoctor(Integer doctorId) {
        return repository.findByDoctorId(doctorId);
    }

    public DoctorSchedule getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay lich truc ID: " + id));
    }

    public DoctorSchedule createSchedule(DoctorSchedule schedule) {
        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "He thong da bo quan ly lich truc rieng theo bac si. Bac si mac dinh lam viec tat ca khung gio moi ngay."
        );
    }

    public void deleteSchedule(Integer id) {
        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "He thong da bo quan ly lich truc rieng theo bac si. Khong can xoa lich truc."
        );
    }
}
