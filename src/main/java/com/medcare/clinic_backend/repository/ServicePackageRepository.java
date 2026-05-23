package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Integer> {
    List<ServicePackage> findByIsActiveTrueOrderByIdDesc();
}
