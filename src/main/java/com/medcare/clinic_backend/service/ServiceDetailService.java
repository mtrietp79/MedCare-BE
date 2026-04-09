package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.ServiceDetail;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServiceDetailService {
    @Autowired
    private ServiceDetailRepository repository;

    public List<ServiceDetail> getByRecordId(Integer recordId) {
        return repository.findByMedicalRecordId(recordId);
    }

    public ServiceDetail addService(ServiceDetail detail) {
        return repository.save(detail);
    }
}