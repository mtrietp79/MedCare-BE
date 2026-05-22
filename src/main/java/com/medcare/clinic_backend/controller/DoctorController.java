package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorPhoto;
import com.medcare.clinic_backend.dto.DoctorResponse;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<DoctorResponse> getAll(
            @RequestParam(required = false) Integer specialtyId,
            @RequestParam(required = false) String name
    ) {
        return doctorService.getAllDoctorResponses(specialtyId, name);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public DoctorResponse getById(@PathVariable Integer id) {
        return doctorService.getDoctorResponseById(id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public DoctorResponse getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return doctorService.getDoctorResponseByAccountUsername(authentication.getName());
    }

    @PutMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public DoctorResponse uploadMyPhoto(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        return doctorService.uploadOwnPhoto(authentication.getName(), file);
    }

    @DeleteMapping("/me/photo")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public void deleteMyPhoto() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Khong xac dinh duoc nguoi dung hien tai.");
        }
        doctorService.deleteOwnPhoto(authentication.getName());
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Integer id) {
        DoctorPhoto photo = doctorService.getDoctorPhoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFileName() + "\"")
                .body(photo.getData());
    }

    @PutMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DoctorResponse uploadPhoto(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return doctorService.uploadDoctorPhoto(id, file);
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void deletePhoto(@PathVariable Integer id) {
        doctorService.deleteDoctorPhoto(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DoctorResponse create(@RequestBody Doctor doctor) {
        return doctorService.toDoctorResponse(doctorService.createDoctor(doctor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DoctorResponse update(@PathVariable Integer id, @RequestBody Doctor doctor) {
        return doctorService.toDoctorResponse(doctorService.updateDoctor(id, doctor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Integer id) {
        doctorService.deleteDoctor(id);
    }
}
