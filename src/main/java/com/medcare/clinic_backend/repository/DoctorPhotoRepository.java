package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.DoctorPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorPhotoRepository extends JpaRepository<DoctorPhoto, Integer> {

    Optional<DoctorPhoto> findByDoctorId(Integer doctorId);

    @Query("select p.id from DoctorPhoto p where p.doctor.id = :doctorId")
    Optional<Integer> findIdByDoctorId(Integer doctorId);

    boolean existsByDoctorId(Integer doctorId);

    void deleteByDoctorId(Integer doctorId);
}
