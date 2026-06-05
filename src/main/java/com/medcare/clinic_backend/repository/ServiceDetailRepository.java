package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.ServiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceDetailRepository extends JpaRepository<ServiceDetail, Integer> {
    List<ServiceDetail> findByMedicalRecordId(Integer recordId);

    List<ServiceDetail> findByMedicalRecordIdIn(List<Integer> recordIds);

    void deleteByMedicalRecordIdIn(List<Integer> recordIds);

    @Query("""
            select ms.id, ms.name, sd.quantity, sd.result, ms.price
            from ServiceDetail sd
            join sd.medicalService ms
            join sd.medicalRecord mr
            where mr.id = :recordId and mr.patient.id = :patientId
            order by sd.id asc
            """)
    List<Object[]> findPatientServiceRowsByRecordId(
            @Param("recordId") Integer recordId,
            @Param("patientId") Integer patientId
    );
}
