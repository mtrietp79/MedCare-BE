package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
    // Chỉ cần để trống thế này, Spring Data JPA sẽ tự động lo hết các lệnh CRUD cơ bản!
}