package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.ServicePackageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePackageItemRepository extends JpaRepository<ServicePackageItem, Integer> {
    List<ServicePackageItem> findByServicePackageId(Integer servicePackageId);
}
