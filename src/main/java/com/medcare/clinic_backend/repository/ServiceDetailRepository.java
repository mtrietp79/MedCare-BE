package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.ServiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceDetailRepository extends JpaRepository<ServiceDetail, Integer> {
    List<ServiceDetail> findByMedicalRecordId(Integer recordId);

    List<ServiceDetail> findByMedicalRecordIdIn(List<Integer> recordIds);
}
