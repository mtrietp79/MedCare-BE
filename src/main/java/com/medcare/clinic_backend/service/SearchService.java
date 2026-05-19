package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.SearchResponse;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private static final int MAX_RESULTS_PER_GROUP = 20;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    public SearchResponse search(String rawQuery) {
        String normalizedQuery = normalizeText(rawQuery);
        if (normalizedQuery == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vui long nhap chuyen khoa hoac bac si.");
        }

        List<Doctor> matchedDoctors = doctorRepository
                .findByFullNameContainingIgnoreCaseOrSpecialty_NameContainingIgnoreCase(normalizedQuery, normalizedQuery);
        List<Specialty> matchedSpecialties = specialtyRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(normalizedQuery, normalizedQuery);

        if (matchedDoctors.isEmpty() && matchedSpecialties.isEmpty()) {
            String foldedQuery = foldVietnameseText(normalizedQuery);
            matchedDoctors = doctorRepository.findAll().stream()
                    .filter(doctor -> containsFolded(doctor == null ? null : doctor.getFullName(), foldedQuery)
                            || containsFolded(doctor == null || doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName(), foldedQuery))
                    .toList();
            matchedSpecialties = specialtyRepository.findAll().stream()
                    .filter(specialty -> containsFolded(specialty == null ? null : specialty.getName(), foldedQuery)
                            || containsFolded(specialty == null ? null : specialty.getDescription(), foldedQuery))
                    .toList();
        }

        return new SearchResponse(
                normalizedQuery,
                mapDoctors(matchedDoctors),
                mapSpecialties(matchedSpecialties)
        );
    }

    private List<SearchResponse.SearchDoctorItem> mapDoctors(List<Doctor> doctors) {
        if (doctors == null || doctors.isEmpty()) {
            return List.of();
        }

        Map<Integer, SearchResponse.SearchDoctorItem> deduplicated = new LinkedHashMap<>();
        for (Doctor doctor : doctors) {
            if (doctor == null || doctor.getId() == null) {
                continue;
            }
            if (deduplicated.size() >= MAX_RESULTS_PER_GROUP) {
                break;
            }
            Integer specialtyId = doctor.getSpecialty() == null ? null : doctor.getSpecialty().getId();
            String specialtyName = doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName();
            deduplicated.putIfAbsent(
                    doctor.getId(),
                    new SearchResponse.SearchDoctorItem(
                            doctor.getId(),
                            doctor.getFullName(),
                            specialtyId,
                            specialtyName,
                            doctor.getRating(),
                            doctor.getExperienceYears(),
                            doctor.getPrice()
                    )
            );
        }
        return new ArrayList<>(deduplicated.values());
    }

    private List<SearchResponse.SearchSpecialtyItem> mapSpecialties(List<Specialty> specialties) {
        if (specialties == null || specialties.isEmpty()) {
            return List.of();
        }

        Map<Integer, SearchResponse.SearchSpecialtyItem> deduplicated = new LinkedHashMap<>();
        for (Specialty specialty : specialties) {
            if (specialty == null || specialty.getId() == null) {
                continue;
            }
            if (deduplicated.size() >= MAX_RESULTS_PER_GROUP) {
                break;
            }
            long totalDoctors = doctorRepository.countBySpecialty_Id(specialty.getId());
            deduplicated.putIfAbsent(
                    specialty.getId(),
                    new SearchResponse.SearchSpecialtyItem(
                            specialty.getId(),
                            specialty.getName(),
                            specialty.getDescription(),
                            totalDoctors,
                            totalDoctors
                    )
            );
        }
        return new ArrayList<>(deduplicated.values());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean containsFolded(String source, String foldedQuery) {
        String foldedSource = foldVietnameseText(source);
        return foldedSource != null && foldedQuery != null && foldedSource.contains(foldedQuery);
    }

    private String foldVietnameseText(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        String withoutAccent = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccent
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase();
    }
}
