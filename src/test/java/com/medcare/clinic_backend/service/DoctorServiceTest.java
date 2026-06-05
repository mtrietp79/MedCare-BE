package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AccountRepository;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorPhotoRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import com.medcare.clinic_backend.repository.FeedbackRepository;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.MedicalServiceRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import com.medcare.clinic_backend.repository.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorPhotoRepository doctorPhotoRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Mock
    private ServiceDetailRepository serviceDetailRepository;

    @Mock
    private MedicalServiceRepository medicalServiceRepository;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    void deleteDoctor_shouldDeleteDoctorAndRelatedDataWhenNoActiveAppointmentsRemain() {
        Doctor doctor = sampleDoctor(40, "BS.CKII. Pham Duc Huy", "ROLE_DOCTOR");
        Appointment appointment = new Appointment();
        appointment.setId(101);
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setId(201);

        when(doctorRepository.findById(40)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.countUpcomingOpenAppointmentsByDoctorId(eq(40), any())).thenReturn(0L);
        when(appointmentRepository.countUpcomingOpenFollowUpAppointmentsByDoctorId(eq(40), any())).thenReturn(0L);
        when(medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(40)).thenReturn(List.of(medicalRecord));
        when(appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(40)).thenReturn(List.of(appointment));

        doctorService.deleteDoctor(40);

        verify(medicalServiceRepository).clearAssignedDoctorByDoctorId(40);
        verify(doctorPhotoRepository).deleteByDoctorId(40);
        verify(doctorScheduleRepository).deleteByDoctorId(40);
        verify(invoiceRepository).deleteByMedicalRecordIdIn(List.of(201));
        verify(prescriptionDetailRepository).deleteByMedicalRecordIdIn(List.of(201));
        verify(serviceDetailRepository).deleteByMedicalRecordIdIn(List.of(201));
        verify(medicalRecordRepository).deleteByDoctorId(40);
        verify(feedbackRepository).deleteByDoctorId(40);
        verify(appointmentRepository).clearParentAppointmentByDoctorId(40);
        verify(appointmentRepository).deleteByDoctorId(40);
        verify(doctorRepository).delete(doctor);
        verify(doctorRepository).flush();
        verify(accountRepository).delete(doctor.getAccount());
        verify(accountRepository).flush();
    }

    @Test
    void deleteDoctor_shouldThrowConflictWhenDoctorHasActiveAppointments() {
        Doctor doctor = sampleDoctor(40, "BS.CKII. Pham Duc Huy", "ROLE_DOCTOR");

        when(doctorRepository.findById(40)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.countUpcomingOpenAppointmentsByDoctorId(eq(40), any())).thenReturn(3L);
        when(appointmentRepository.countUpcomingOpenFollowUpAppointmentsByDoctorId(eq(40), any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> doctorService.deleteDoctor(40)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("DOCTOR_DELETE_HAS_ACTIVE_APPOINTMENTS", exception.getCode());
        assertEquals(
                "Khong the xoa bac si vi dang co lich hen va lich tai kham. Vui long cho den khi het lich.",
                exception.getMessage()
        );
        verify(medicalServiceRepository, never()).clearAssignedDoctorByDoctorId(40);
        verify(doctorPhotoRepository, never()).deleteByDoctorId(40);
        verify(doctorScheduleRepository, never()).deleteByDoctorId(40);
        verify(doctorRepository, never()).delete(doctor);
        verify(accountRepository, never()).delete(doctor.getAccount());
    }

    private Doctor sampleDoctor(Integer id, String fullName, String role) {
        Account account = new Account();
        account.setId(12);
        account.setUsername("doctor40");
        account.setRole(role);

        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setFullName(fullName);
        doctor.setAccount(account);
        return doctor;
    }
}
