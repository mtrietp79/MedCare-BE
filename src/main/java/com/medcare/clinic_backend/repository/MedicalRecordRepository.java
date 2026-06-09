package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Integer> {

    List<MedicalRecord> findByPatientIdOrderByExaminationDateDesc(Integer patientId);

    List<MedicalRecord> findByDoctorIdOrderByExaminationDateDesc(Integer doctorId);

    Optional<MedicalRecord> findByIdAndDoctorId(Integer id, Integer doctorId);

    Optional<MedicalRecord> findByIdAndPatientId(Integer id, Integer patientId);

    List<MedicalRecord> findByPatientIdAndDoctorIdOrderByExaminationDateDesc(Integer patientId, Integer doctorId);

    long countByDoctorId(Integer doctorId);

    @Query("""
            SELECT COUNT(mr)
            FROM MedicalRecord mr
            WHERE mr.doctor.specialty.id = :specialtyId
            """)
    long countByDoctorSpecialtyId(@Param("specialtyId") Integer specialtyId);

    long countByPatientId(Integer patientId);

    void deleteByDoctorId(Integer doctorId);

    boolean existsByAppointmentId(Integer appointmentId);

    Optional<MedicalRecord> findByAppointmentIdAndPatientId(Integer appointmentId, Integer patientId);

    boolean existsByMedicalRecordCode(String medicalRecordCode);

    @Query("""
            select mr.id, mr.medicalRecordCode, mr.createdAt, a.id, a.appointmentCode, a.appointmentDate, a.appointmentType, a.status,
                   mr.examinationDate, mr.diagnosis, d.id, d.fullName, s.name
            from MedicalRecord mr
            join mr.appointment a
            join mr.doctor d
            left join d.specialty s
            where mr.patient.id = :patientId
            order by mr.examinationDate desc, mr.id desc
            """)
    List<Object[]> findPatientRecordSummaryRows(@Param("patientId") Integer patientId);

    @Query("""
            select mr.id, a.id, a.appointmentCode, a.appointmentDate, a.appointmentType, a.status, a.symptoms, a.notes,
                   mr.examinationDate, mr.diagnosis, mr.doctorAdvice, mr.treatmentPlan, mr.prescription,
                   d.id, d.fullName, d.phone, d.email, s.name,
                   f.id, f.appointmentCode, f.appointmentDate, f.status, f.appointmentType, f.notes
            from MedicalRecord mr
            join mr.appointment a
            join mr.doctor d
            left join d.specialty s
            left join mr.followUpAppointment f
            where mr.id = :recordId and mr.patient.id = :patientId
            """)
    Optional<Object[]> findPatientRecordDetailRowByRecordId(
            @Param("recordId") Integer recordId,
            @Param("patientId") Integer patientId
    );

    @Query("""
            select mr.id, a.id, a.appointmentCode, a.appointmentDate, a.appointmentType, a.status, a.symptoms, a.notes,
                   mr.examinationDate, mr.diagnosis, mr.doctorAdvice, mr.treatmentPlan, mr.prescription,
                   d.id, d.fullName, d.phone, d.email, s.name,
                   f.id, f.appointmentCode, f.appointmentDate, f.status, f.appointmentType, f.notes
            from MedicalRecord mr
            join mr.appointment a
            join mr.doctor d
            left join d.specialty s
            left join mr.followUpAppointment f
            where a.id = :appointmentId and mr.patient.id = :patientId
            """)
    Optional<Object[]> findPatientRecordDetailRowByAppointmentId(
            @Param("appointmentId") Integer appointmentId,
            @Param("patientId") Integer patientId
    );

    List<MedicalRecord> findTop5ByPatientIdOrderByExaminationDateDesc(Integer patientId);
}
