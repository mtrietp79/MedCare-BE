package com.medcare.clinic_backend.dto.servicepackage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicServicePackageDetailResponse {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Integer durationMinutes;
    private String imageUrl;
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Integer id;
        private String name;
        private Double price;
    }
}
