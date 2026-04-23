package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Integer> {

    List<MedicalRecord> findByPatientIdOrderByExaminationDateDesc(Integer patientId);

    List<MedicalRecord> findByDoctorIdOrderByExaminationDateDesc(Integer doctorId);

    Optional<MedicalRecord> findByIdAndDoctorId(Integer id, Integer doctorId);

    List<MedicalRecord> findByPatientIdAndDoctorIdOrderByExaminationDateDesc(Integer patientId, Integer doctorId);

    boolean existsByAppointmentId(Integer appointmentId);
}
