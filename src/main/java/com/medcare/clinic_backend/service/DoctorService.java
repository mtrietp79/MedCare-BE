package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Doctor getDoctorById(Integer id) {
        return doctorRepository.findById(id).orElse(null);
    }

    public Doctor createDoctor(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    public Doctor updateDoctor(Integer id, Doctor doctorDetails) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor != null) {
            doctor.setFullName(doctorDetails.getFullName());
            doctor.setEmail(doctorDetails.getEmail());
            doctor.setPhone(doctorDetails.getPhone());
            doctor.setPrice(doctorDetails.getPrice());
            doctor.setSpecialty(doctorDetails.getSpecialty()); // Cập nhật chuyên khoa
            return doctorRepository.save(doctor);
        }
        return null;
    }

    public void deleteDoctor(Integer id) {
        doctorRepository.deleteById(id);
    }
}