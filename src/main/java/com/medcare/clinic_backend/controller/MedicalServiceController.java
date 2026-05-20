package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.MedicalService;
import com.medcare.clinic_backend.entity.MedicalServicePhoto;
import com.medcare.clinic_backend.service.MedicalServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/medical-services")
public class MedicalServiceController {
    @Autowired
    private MedicalServiceService service;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<MedicalService> getActive(@RequestParam(required = false) Integer specialtyId) {
        return service.getActiveServices(specialtyId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public MedicalService getById(@PathVariable Integer id) {
        return service.getActiveByIdForBooking(id);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<MedicalService> getAllForAdmin(@RequestParam(required = false) Integer specialtyId) {
        return service.getAllForAdmin(specialtyId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MedicalService create(@RequestBody MedicalService medicalService) {
        return service.create(medicalService);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MedicalService update(@PathVariable Integer id, @RequestBody MedicalService medicalService) {
        return service.update(id, medicalService);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Integer id) {
        MedicalServicePhoto photo = service.getPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFileName() + "\"")
                .body(photo.getData());
    }

    @PutMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MedicalService uploadPhoto(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return service.uploadPhoto(id, file);
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void deletePhoto(@PathVariable Integer id) {
        service.deletePhoto(id);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MedicalService setActive(@PathVariable Integer id, @RequestParam boolean active) {
        return service.setActive(id, active);
    }
}
