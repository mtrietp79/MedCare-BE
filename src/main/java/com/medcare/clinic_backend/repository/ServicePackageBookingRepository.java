package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.ServicePackageBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicePackageBookingRepository extends JpaRepository<ServicePackageBooking, Integer> {

    List<ServicePackageBooking> findByPatientIdOrderByCreatedAtDesc(Integer patientId);

    Optional<ServicePackageBooking> findByIdAndPatientId(Integer id, Integer patientId);

    List<ServicePackageBooking> findAllByOrderByCreatedAtDesc();

    boolean existsByServicePackage_Id(Integer servicePackageId);
}
