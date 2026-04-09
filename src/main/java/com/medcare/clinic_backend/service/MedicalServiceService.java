package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.repository.MedicalServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MedicalServiceService {
    @Autowired
    private MedicalServiceRepository repository;

    public List<MedicalService> getAll() {
        return repository.findAll();
    }

    public MedicalService create(MedicalService medicalService) {
        return repository.save(medicalService);
    }
}