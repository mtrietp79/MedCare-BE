package com.medcare.clinic_backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SearchResponse {
    private String query;
    private List<SearchDoctorItem> doctors = new ArrayList<>();
    private List<SearchSpecialtyItem> specialties = new ArrayList<>();

    public SearchResponse() {
    }

    public SearchResponse(String query, List<SearchDoctorItem> doctors, List<SearchSpecialtyItem> specialties) {
        this.query = query;
        this.doctors = doctors;
        this.specialties = specialties;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<SearchDoctorItem> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<SearchDoctorItem> doctors) {
        this.doctors = doctors;
    }

    public List<SearchSpecialtyItem> getSpecialties() {
        return specialties;
    }

    public void setSpecialties(List<SearchSpecialtyItem> specialties) {
        this.specialties = specialties;
    }

    public static class SearchDoctorItem {
        private Integer id;
        private String fullName;
        private Integer specialtyId;
        private String specialtyName;
        private Double rating;
        private Integer experienceYears;
        private BigDecimal price;

        public SearchDoctorItem() {
        }

        public SearchDoctorItem(
                Integer id,
                String fullName,
                Integer specialtyId,
                String specialtyName,
                Double rating,
                Integer experienceYears,
                BigDecimal price
        ) {
            this.id = id;
            this.fullName = fullName;
            this.specialtyId = specialtyId;
            this.specialtyName = specialtyName;
            this.rating = rating;
            this.experienceYears = experienceYears;
            this.price = price;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public Integer getSpecialtyId() {
            return specialtyId;
        }

        public void setSpecialtyId(Integer specialtyId) {
            this.specialtyId = specialtyId;
        }

        public String getSpecialtyName() {
            return specialtyName;
        }

        public void setSpecialtyName(String specialtyName) {
            this.specialtyName = specialtyName;
        }

        public Double getRating() {
            return rating;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }

        public Integer getExperienceYears() {
            return experienceYears;
        }

        public void setExperienceYears(Integer experienceYears) {
            this.experienceYears = experienceYears;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

    public static class SearchSpecialtyItem {
        private Integer id;
        private String name;
        private String description;
        private Long totalDoctors;
        private Long doctorCount;

        public SearchSpecialtyItem() {
        }

        public SearchSpecialtyItem(Integer id, String name, String description, Long totalDoctors, Long doctorCount) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.totalDoctors = totalDoctors;
            this.doctorCount = doctorCount;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Long getTotalDoctors() {
            return totalDoctors;
        }

        public void setTotalDoctors(Long totalDoctors) {
            this.totalDoctors = totalDoctors;
        }

        public Long getDoctorCount() {
            return doctorCount;
        }

        public void setDoctorCount(Long doctorCount) {
            this.doctorCount = doctorCount;
        }
    }
}
