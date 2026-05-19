package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecialtyService {

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public List<Specialty> getAllSpecialties() {
        return specialtyRepository.findAll().stream()
                .map(this::enrichDoctorCount)
                .toList();
    }

    public Specialty createSpecialty(Specialty specialty) {
        validateSpecialty(specialty);
        return enrichDoctorCount(specialtyRepository.save(specialty));
    }

    public Specialty getSpecialtyById(Integer id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay chuyen khoa ID: " + id));
        return enrichDoctorCount(specialty);
    }

    public Specialty updateSpecialty(Integer id, Specialty specialtyDetails) {
        Specialty specialty = getSpecialtyById(id);
        validateSpecialty(specialtyDetails);
        specialty.setName(specialtyDetails.getName().trim());
        specialty.setDescription(specialtyDetails.getDescription());
        return enrichDoctorCount(specialtyRepository.save(specialty));
    }

    public void deleteSpecialty(Integer id) {
        if (!specialtyRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Khong tim thay chuyen khoa ID: " + id);
        }
        specialtyRepository.deleteById(id);
    }

    private void validateSpecialty(Specialty specialty) {
        if (specialty == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Du lieu chuyen khoa khong hop le.");
        }
        if (specialty.getName() == null || specialty.getName().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Ten chuyen khoa khong duoc de trong.");
        }
    }

    private Specialty enrichDoctorCount(Specialty specialty) {
        if (specialty == null) {
            return null;
        }
        if (specialty.getId() == null) {
            specialty.setTotalDoctors(0L);
            specialty.setDoctorCount(0L);
            return specialty;
        }
        long totalDoctors = doctorRepository.countBySpecialty_Id(specialty.getId());
        specialty.setTotalDoctors(totalDoctors);
        specialty.setDoctorCount(totalDoctors);
        return specialty;
    }
}
