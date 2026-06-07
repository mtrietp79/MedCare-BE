package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.dto.SlotAvailabilityDto;
import com.medcare.clinic_backend.dto.doctor.DoctorAppointmentDetailResponse;
import com.medcare.clinic_backend.dto.doctor.CompleteAppointmentRequest;
import com.medcare.clinic_backend.dto.doctor.CompleteAppointmentResponse;
import com.medcare.clinic_backend.dto.doctor.DoctorMedicalRecordPatientItemResponse;
import com.medcare.clinic_backend.dto.doctor.DoctorMedicalRecordsSummaryResponse;
import com.medcare.clinic_backend.dto.doctor.DoctorPatientMedicalRecordsResponse;
import com.medcare.clinic_backend.dto.doctor.DoctorScheduleDayAppointmentResponse;
import com.medcare.clinic_backend.dto.doctor.CreateFollowUpRequest;
import com.medcare.clinic_backend.dto.doctor.CreateFollowUpResponse;
import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Doctor;
import com.medcare.clinic_backend.entity.DoctorSchedule;
import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.entity.Patient;
import com.medcare.clinic_backend.entity.PrescriptionDetail;
import com.medcare.clinic_backend.entity.ServiceDetail;
import com.medcare.clinic_backend.entity.Specialty;
import com.medcare.clinic_backend.exception.BusinessException;
import com.medcare.clinic_backend.repository.AppointmentRepository;
import com.medcare.clinic_backend.repository.DoctorRepository;
import com.medcare.clinic_backend.repository.DoctorScheduleRepository;
import com.medcare.clinic_backend.repository.MedicalRecordRepository;
import com.medcare.clinic_backend.repository.PatientRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorPortalServiceTest {

    private static final String USERNAME = "doctor1";

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Mock
    private ServiceDetailRepository serviceDetailRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private DoctorPortalService doctorPortalService;

    @Test
    void createFollowUp_shouldBeTransactionalToSupportLazyLoadedMedicalRecordRelations() throws Exception {
        Method method = DoctorPortalService.class.getMethod(
                "createFollowUp",
                String.class,
                Integer.class,
                CreateFollowUpRequest.class
        );

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    void completeAppointment_shouldAcceptCompatibilityFollowUpFieldsAndCreateFollowUp() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment appointment = buildSourceAppointment(currentDoctor);
        appointment.setId(401);
        appointment.setStatus("PENDING");
        appointment.setAppointmentDate(LocalDateTime.now().minusHours(2));

        Invoice invoice = new Invoice();
        invoice.setId(801);
        invoice.setStatus("UNPAID");
        invoice.setConsultationFee(200000.0);
        invoice.setMedicineFee(0.0);
        invoice.setServiceFee(0.0);
        invoice.setTotalAmount(200000.0);

        when(appointmentRepository.findByIdAndDoctorId(401, currentDoctor.getId())).thenReturn(Optional.of(appointment));
        when(medicalRecordRepository.existsByAppointmentId(401)).thenReturn(false);
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> {
            MedicalRecord saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(601);
            }
            return saved;
        });
        when(medicalRecordRepository.saveAndFlush(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceService.createInvoiceFromRecord(any(MedicalRecord.class))).thenReturn(invoice);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(901);
            }
            return saved;
        });

        CompleteAppointmentResponse response = doctorPortalService.completeAppointment(
                USERNAME,
                401,
                buildCompleteRequest()
        );

        assertEquals(401, response.getAppointmentId());
        assertNotNull(response.getFollowUpAppointment());
        assertEquals(901, response.getFollowUpAppointment().getId());
        assertEquals(LocalDate.of(2026, 6, 14), response.getFollowUpAppointment().getAppointmentDate());
        assertEquals(LocalTime.of(8, 0), response.getFollowUpAppointment().getAppointmentTime());
        assertEquals("Tai kham sau 1 tuan", response.getFollowUpAppointment().getNote());
    }

    @Test
    void getFollowUpSlots_shouldDisableShiftsOutsideDoctorSchedule() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        LocalDate followUpDate = LocalDate.now().plusDays(5);
        when(doctorScheduleRepository.countByDoctorId(currentDoctor.getId())).thenReturn(1L);
        when(doctorScheduleRepository.findByDoctorIdAndWorkDate(currentDoctor.getId(), followUpDate))
                .thenReturn(List.of(buildSchedule(currentDoctor, followUpDate, "MORNING")));
        when(appointmentRepository.countByDoctorInSlot(eq(currentDoctor.getId()), any(), any())).thenReturn(0L);

        List<SlotAvailabilityDto> slots = doctorPortalService.getFollowUpSlots(USERNAME, followUpDate);

        assertTrue(slots.stream().anyMatch(slot ->
                "MORNING".equals(slot.shift()) && !slot.disabled()
        ));
        assertTrue(slots.stream().anyMatch(slot ->
                "AFTERNOON".equals(slot.shift())
                        && slot.disabled()
                        && "SHIFT_UNAVAILABLE".equals(slot.disabledReason())
        ));
    }

    @Test
    void createFollowUp_shouldRejectWhenRecordNotFound() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);
        when(medicalRecordRepository.findById(101)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> doctorPortalService.createFollowUp(USERNAME, 101, buildRequest("2026-06-10", "09:00"))
        );

        assertEquals("Khong tim thay benh an.", ex.getMessage());
        assertEquals("FOLLOW_UP_VALIDATION_ERROR", ex.getCode());
        assertEquals(0, ex.getFieldErrors().size());
    }

    @Test
    void createFollowUp_shouldRejectWhenRecordBelongsToDifferentDoctor() {
        Doctor currentDoctor = buildDoctor(7);
        Doctor otherDoctor = buildDoctor(8);
        stubCurrentDoctor(currentDoctor);

        MedicalRecord record = buildRecord(otherDoctor, buildSourceAppointment(otherDoctor));
        when(medicalRecordRepository.findById(102)).thenReturn(Optional.of(record));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> doctorPortalService.createFollowUp(USERNAME, 102, buildRequest("2026-06-10", "09:00"))
        );

        assertEquals("Benh an nay khong thuoc bac si hien tai.", ex.getMessage());
        assertEquals("FOLLOW_UP_VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void createFollowUp_shouldCreateWithoutCheckingDoctorSchedule() {
        Doctor currentDoctor = buildDoctor(7);
        LocalDate followUpDate = LocalDate.now().plusDays(2);
        stubCurrentDoctor(currentDoctor);

        Appointment sourceAppointment = buildSourceAppointment(currentDoctor);
        MedicalRecord record = buildRecord(currentDoctor, sourceAppointment);
        when(medicalRecordRepository.findById(103)).thenReturn(Optional.of(record));
        when(appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())).thenReturn(false);
        when(appointmentRepository.countActiveByDoctorIdAndAppointmentDate(
                currentDoctor.getId(),
                LocalDateTime.of(followUpDate, LocalTime.of(9, 0))
        )).thenReturn(0L);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(503);
            return saved;
        });

        CreateFollowUpResponse response = doctorPortalService.createFollowUp(
                USERNAME,
                103,
                buildRequest(followUpDate.toString(), "09:00")
        );

        assertEquals(503, response.getId());
        assertEquals(followUpDate, response.getAppointmentDate());
    }

    @Test
    void createFollowUp_shouldAllowDoctorWithoutAnyConfiguredSchedule() {
        Doctor currentDoctor = buildDoctor(7);
        LocalDate followUpDate = LocalDate.now().plusDays(2);
        stubCurrentDoctor(currentDoctor);

        Appointment sourceAppointment = buildSourceAppointment(currentDoctor);
        MedicalRecord record = buildRecord(currentDoctor, sourceAppointment);
        when(medicalRecordRepository.findById(1030)).thenReturn(Optional.of(record));
        when(appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())).thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(503);
            return saved;
        });
        CreateFollowUpResponse response = doctorPortalService.createFollowUp(
                USERNAME,
                1030,
                buildRequest(followUpDate.toString(), "09:00")
        );

        assertEquals(503, response.getId());
        assertEquals(followUpDate, response.getAppointmentDate());
    }

    @Test
    void createFollowUp_shouldRejectWhenDoctorHasActiveAppointmentAtSameDateTime() {
        Doctor currentDoctor = buildDoctor(7);
        LocalDate followUpDate = LocalDate.now().plusDays(3);
        stubCurrentDoctor(currentDoctor);

        Appointment sourceAppointment = buildSourceAppointment(currentDoctor);
        MedicalRecord record = buildRecord(currentDoctor, sourceAppointment);
        when(medicalRecordRepository.findById(104)).thenReturn(Optional.of(record));
        when(appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())).thenReturn(false);
        when(appointmentRepository.countActiveByDoctorIdAndAppointmentDate(
                currentDoctor.getId(),
                LocalDateTime.of(followUpDate, LocalTime.of(9, 0))
        )).thenReturn(1L);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> doctorPortalService.createFollowUp(
                        USERNAME,
                        104,
                        buildRequest(followUpDate.toString(), "09:00")
                )
        );

        assertEquals("Bac si da co lich hen tai dung thoi diem tai kham nay.", ex.getMessage());
        assertEquals(
                "Bac si da co lich hen tai ngay gio nay.",
                ex.getFieldErrors().get("followUpTime")
        );
    }

    @Test
    void createFollowUp_shouldRejectWhenTimeIsInPast() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment sourceAppointment = buildSourceAppointment(currentDoctor);
        MedicalRecord record = buildRecord(currentDoctor, sourceAppointment);
        when(medicalRecordRepository.findById(105)).thenReturn(Optional.of(record));
        when(appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())).thenReturn(false);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> doctorPortalService.createFollowUp(
                        USERNAME,
                        105,
                        buildRequest(yesterday.toString(), "09:00")
                )
        );

        assertEquals("Thoi gian tai kham khong duoc o qua khu.", ex.getMessage());
        assertEquals("Thoi gian tai kham khong duoc o qua khu.", ex.getFieldErrors().get("followUpTime"));
    }

    @Test
    void createFollowUp_shouldRejectWhenRecordAlreadyHasFollowUp() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment sourceAppointment = buildSourceAppointment(currentDoctor);
        MedicalRecord record = buildRecord(currentDoctor, sourceAppointment);
        record.setFollowUpAppointment(new Appointment());
        when(medicalRecordRepository.findById(106)).thenReturn(Optional.of(record));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> doctorPortalService.createFollowUp(USERNAME, 106, buildRequest("2026-06-10", "09:00"))
        );

        assertEquals("Benh an nay da ton tai lich tai kham.", ex.getMessage());
        assertEquals("FOLLOW_UP_VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void createFollowUp_shouldCreateCanonicalResponse() {
        Doctor currentDoctor = buildDoctor(7);
        LocalDate followUpDate = LocalDate.now().plusDays(4);
        stubCurrentDoctor(currentDoctor);

        Appointment sourceAppointment = buildSourceAppointment(currentDoctor);
        MedicalRecord record = buildRecord(currentDoctor, sourceAppointment);
        when(medicalRecordRepository.findById(107)).thenReturn(Optional.of(record));
        when(appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())).thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(501);
            return saved;
        });
        CreateFollowUpResponse response = doctorPortalService.createFollowUp(
                USERNAME,
                107,
                buildRequest(followUpDate.toString(), "09:00")
        );

        assertNotNull(record.getFollowUpAppointment());
        assertEquals(501, response.getId());
        assertEquals(followUpDate, response.getAppointmentDate());
        assertEquals("T\u00e1i kh\u00e1m", response.getType());
        assertEquals("Ch\u01b0a kh\u00e1m", response.getStatus());
        assertEquals("Ch\u01b0a thanh to\u00e1n", response.getPaymentStatus());
        assertEquals(100000.0, response.getConsultationFee());
        verify(medicalRecordRepository).saveAndFlush(record);
    }

    @Test
    void getDaySchedule_shouldClassifyFollowUpUsingParentAppointmentContext() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment parentAppointment = buildSourceAppointment(currentDoctor);
        parentAppointment.setId(1);

        Appointment followUpAppointment = buildFollowUpAppointment(currentDoctor, parentAppointment);
        followUpAppointment.setAppointmentType(null);

        LocalDate date = followUpAppointment.getAppointmentDate().toLocalDate();
        when(appointmentRepository.findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(
                eq(currentDoctor.getId()),
                eq(date.atStartOfDay()),
                eq(date.plusDays(1).atStartOfDay())
        )).thenReturn(List.of(followUpAppointment));

        List<DoctorScheduleDayAppointmentResponse> response = doctorPortalService.getDaySchedule(USERNAME, date, "morning");

        assertEquals(1, response.size());
        assertEquals("T\u00e1i kh\u00e1m", response.get(0).getType());
        assertEquals("FOLLOW_UP", response.get(0).getTypeCode());
        assertTrue(response.get(0).isFollowUp());
        assertEquals(1, response.get(0).getParentAppointmentId());
    }

    @Test
    void getAppointmentDetail_shouldSeparateFollowUpNoteFromSymptoms() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment parentAppointment = buildSourceAppointment(currentDoctor);
        parentAppointment.setId(1);
        parentAppointment.setSymptoms("mat ngu nhieu dem");

        Appointment followUpAppointment = buildFollowUpAppointment(currentDoctor, parentAppointment);
        followUpAppointment.setNotes("an sang truoc 7h sang");
        followUpAppointment.setSymptoms(null);

        when(appointmentRepository.findByIdAndDoctorId(2, currentDoctor.getId())).thenReturn(Optional.of(followUpAppointment));
        when(appointmentRepository.findById(1)).thenReturn(Optional.of(parentAppointment));

        DoctorAppointmentDetailResponse response = doctorPortalService.getAppointmentDetail(USERNAME, 2);

        assertEquals("T\u00e1i kh\u00e1m", response.getType());
        assertEquals("FOLLOW_UP", response.getTypeCode());
        assertTrue(response.isFollowUp());
        assertEquals("an sang truoc 7h sang", response.getFollowUpNote());
        assertEquals("mat ngu nhieu dem", response.getSymptoms());
        assertEquals(null, response.getNote());
        assertEquals(1, response.getParentAppointmentId());
    }

    @Test
    void getAppointmentDetail_shouldExposeCanExamineFlag() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment appointment = buildSourceAppointment(currentDoctor);
        appointment.setId(21);
        appointment.setStatus("PENDING");

        when(appointmentRepository.findByIdAndDoctorId(21, currentDoctor.getId())).thenReturn(Optional.of(appointment));
        when(medicalRecordRepository.existsByAppointmentId(21)).thenReturn(false);

        DoctorAppointmentDetailResponse response = doctorPortalService.getAppointmentDetail(USERNAME, 21);

        assertTrue(response.isCanExamine());
    }

    @Test
    void getPatientMedicalRecords_shouldExposeFollowUpAppointmentToHideCreateButton() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Patient patient = new Patient();
        patient.setId(15);
        patient.setFullName("Pham Minh Triet");

        Appointment parentAppointment = buildSourceAppointment(currentDoctor);
        parentAppointment.setId(1);
        parentAppointment.setPatient(patient);
        parentAppointment.setSymptoms("mat ngu nhieu dem");

        Appointment followUpAppointment = buildFollowUpAppointment(currentDoctor, parentAppointment);
        followUpAppointment.setPatient(patient);

        MedicalRecord record = buildRecord(currentDoctor, parentAppointment);
        record.setId(501);
        record.setPatient(patient);
        record.setExaminationDate(LocalDate.now().minusDays(1));
        record.setDiagnosis("roi loan than kinh");
        record.setDoctorAdvice("theo doi them");
        record.setFollowUpAppointment(followUpAppointment);

        when(medicalRecordRepository.findByPatientIdAndDoctorIdOrderByExaminationDateDesc(patient.getId(), currentDoctor.getId()))
                .thenReturn(List.of(record));
        when(prescriptionDetailRepository.findByMedicalRecordIdIn(List.of(501))).thenReturn(List.<PrescriptionDetail>of());
        when(serviceDetailRepository.findByMedicalRecordIdIn(List.of(501))).thenReturn(List.<ServiceDetail>of());

        DoctorPatientMedicalRecordsResponse response = doctorPortalService.getPatientMedicalRecords(USERNAME, patient.getId());

        assertEquals(1, response.getRecords().size());
        DoctorPatientMedicalRecordsResponse.RecordItem recordItem = response.getRecords().get(0);
        assertEquals("Kh\u00e1m b\u1ec7nh", recordItem.getType());
        assertEquals("NEW_EXAM", recordItem.getTypeCode());
        assertNotNull(recordItem.getFollowUpAppointment());
        assertEquals(2, recordItem.getFollowUpAppointment().getAppointmentId());
        assertEquals("T\u00e1i kh\u00e1m", recordItem.getFollowUpAppointment().getType());
        assertEquals("FOLLOW_UP", recordItem.getFollowUpAppointment().getTypeCode());
        assertEquals("an sang truoc 7h sang", recordItem.getFollowUpAppointment().getNote());
        assertTrue(recordItem.getFollowUpAppointment().isFollowUp());
        assertEquals(2, recordItem.getFollowUpAppointmentId());
    }

    @Test
    void getPatientMedicalRecords_shouldResolveAppointmentFromRawKeyWhenRelationUnavailable() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Patient patient = new Patient();
        patient.setId(15);
        patient.setFullName("Pham Minh Triet");

        Appointment appointment = buildSourceAppointment(currentDoctor);
        appointment.setId(301);
        appointment.setPatient(patient);
        appointment.setSymptoms("mat ngu nhieu dem");

        MedicalRecord record = buildRecord(currentDoctor, null);
        record.setId(701);
        record.setPatient(patient);
        record.setExaminationDate(LocalDate.now().minusDays(1));
        record.setDiagnosis("roi loan than kinh");
        record.setDoctorAdvice("theo doi them");
        record.setAppointmentKey(301);

        when(medicalRecordRepository.findByPatientIdAndDoctorIdOrderByExaminationDateDesc(patient.getId(), currentDoctor.getId()))
                .thenReturn(List.of(record));
        when(appointmentRepository.findById(301)).thenReturn(Optional.of(appointment));
        when(prescriptionDetailRepository.findByMedicalRecordIdIn(List.of(701))).thenReturn(List.of());
        when(serviceDetailRepository.findByMedicalRecordIdIn(List.of(701))).thenReturn(List.of());

        DoctorPatientMedicalRecordsResponse response = doctorPortalService.getPatientMedicalRecords(USERNAME, patient.getId());

        assertEquals(1, response.getRecords().size());
        assertEquals(301, response.getRecords().get(0).getAppointmentId());
        assertEquals("NEW_EXAM", response.getRecords().get(0).getTypeCode());
        assertEquals("mat ngu nhieu dem", response.getRecords().get(0).getSymptoms());
    }

    @Test
    void getPatientMedicalRecords_shouldReturnEmptyPayloadWhenDoctorOwnsPatientButHasNoRecords() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Patient patient = new Patient();
        patient.setId(15);
        patient.setFullName("Pham Minh Triet");
        patient.setPhone("0909123456");

        when(medicalRecordRepository.findByPatientIdAndDoctorIdOrderByExaminationDateDesc(15, currentDoctor.getId()))
                .thenReturn(List.of());
        when(appointmentRepository.existsByDoctorIdAndPatientId(currentDoctor.getId(), 15)).thenReturn(true);
        when(patientRepository.findById(15)).thenReturn(Optional.of(patient));

        DoctorPatientMedicalRecordsResponse response = doctorPortalService.getPatientMedicalRecords(USERNAME, 15);

        assertNotNull(response.getPatient());
        assertEquals(15, response.getPatient().getId());
        assertEquals("Pham Minh Triet", response.getPatient().getFullName());
        assertNotNull(response.getRecords());
        assertTrue(response.getRecords().isEmpty());
    }

    @Test
    void createFollowUp_shouldIgnoreBrokenFollowUpReferenceAndCreateNewAppointment() {
        Doctor currentDoctor = buildDoctor(7);
        LocalDate followUpDate = LocalDate.now().plusDays(5);
        stubCurrentDoctor(currentDoctor);

        Appointment sourceAppointment = buildSourceAppointment(currentDoctor);
        sourceAppointment.setId(901);

        MedicalRecord record = buildRecord(currentDoctor, sourceAppointment);
        record.setFollowUpAppointmentKey(999);

        when(medicalRecordRepository.findById(109)).thenReturn(Optional.of(record));
        when(appointmentRepository.findById(999)).thenReturn(Optional.empty());
        when(appointmentRepository.existsByParentAppointmentId(sourceAppointment.getId())).thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(777);
            return saved;
        });

        CreateFollowUpResponse response = doctorPortalService.createFollowUp(
                USERNAME,
                109,
                buildRequest(followUpDate.toString(), "08:00")
        );

        assertEquals(777, response.getId());
        assertEquals(followUpDate, response.getAppointmentDate());
        verify(medicalRecordRepository).saveAndFlush(record);
    }

    @Test
    void createFollowUp_shouldReturnValidationErrorWhenSourceAppointmentIsBroken() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        MedicalRecord record = buildRecord(currentDoctor, null);
        record.setAppointmentKey(901);

        when(medicalRecordRepository.findById(110)).thenReturn(Optional.of(record));
        when(appointmentRepository.findById(901)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> doctorPortalService.createFollowUp(USERNAME, 110, buildRequest("2026-06-14", "08:00"))
        );

        assertEquals("Benh an khong hop le de tao lich tai kham.", ex.getMessage());
        assertEquals("FOLLOW_UP_VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void getMedicalRecordSummary_shouldCountNewAndFollowUpPatientsSeparately() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment initialAppointment = buildSourceAppointment(currentDoctor);
        initialAppointment.setAppointmentDate(LocalDate.now().minusDays(2).atTime(8, 0));

        MedicalRecord record = buildRecord(currentDoctor, initialAppointment);
        record.setPatient(initialAppointment.getPatient());
        record.setExaminationDate(initialAppointment.getAppointmentDate().toLocalDate());

        Appointment followUpAppointment = buildFollowUpAppointment(currentDoctor, initialAppointment);

        when(medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(currentDoctor.getId()))
                .thenReturn(List.of(record));
        when(appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(currentDoctor.getId()))
                .thenReturn(List.of(followUpAppointment, initialAppointment));

        DoctorMedicalRecordsSummaryResponse response = doctorPortalService.getMedicalRecordSummary(USERNAME);

        assertEquals(1, response.getTotalPatients());
        assertEquals(1, response.getNewPatients());
        assertEquals(1, response.getFollowUpPatients());
    }

    @Test
    void getMedicalRecordPatients_shouldReturnSeparateNewExamAndFollowUpCounts() {
        Doctor currentDoctor = buildDoctor(7);
        stubCurrentDoctor(currentDoctor);

        Appointment initialAppointment = buildSourceAppointment(currentDoctor);
        initialAppointment.setAppointmentDate(LocalDate.now().minusDays(2).atTime(8, 0));

        MedicalRecord record = buildRecord(currentDoctor, initialAppointment);
        record.setPatient(initialAppointment.getPatient());
        record.setExaminationDate(initialAppointment.getAppointmentDate().toLocalDate());

        Appointment followUpAppointment = buildFollowUpAppointment(currentDoctor, initialAppointment);
        LocalDate expectedLatestVisitDate = followUpAppointment.getAppointmentDate().toLocalDate();

        when(medicalRecordRepository.findByDoctorIdOrderByExaminationDateDesc(currentDoctor.getId()))
                .thenReturn(List.of(record));
        when(appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(currentDoctor.getId()))
                .thenReturn(List.of(followUpAppointment, initialAppointment));

        List<DoctorMedicalRecordPatientItemResponse> response =
                doctorPortalService.getMedicalRecordPatients(USERNAME, null);

        assertEquals(1, response.size());
        DoctorMedicalRecordPatientItemResponse patient = response.get(0);
        assertEquals(1, patient.getNewExamCount());
        assertEquals(1, patient.getFollowUpCount());
        assertEquals(2, patient.getTotalVisitCount());
        assertEquals(2, patient.getVisitCount());
        assertEquals(expectedLatestVisitDate, patient.getLatestVisitDate());
    }

    private void stubCurrentDoctor(Doctor doctor) {
        when(doctorRepository.findByAccount_Username(USERNAME)).thenReturn(Optional.of(doctor));
    }

    private Doctor buildDoctor(int id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setPrice(BigDecimal.valueOf(200000));
        doctor.setIsActive(true);
        return doctor;
    }

    private Appointment buildSourceAppointment(Doctor doctor) {
        Appointment appointment = new Appointment();
        appointment.setId(301);
        appointment.setDoctor(doctor);
        Patient patient = new Patient();
        patient.setId(10);
        patient.setFullName("Pham Minh Triet");
        appointment.setPatient(patient);
        Specialty specialty = new Specialty();
        specialty.setId(20);
        specialty.setName("Noi tong quat");
        appointment.setSpecialty(specialty);
        appointment.setAppointmentDate(LocalDateTime.now().minusDays(1));
        appointment.setStatus("COMPLETED");
        appointment.setAppointmentType("Kh\u00e1m b\u1ec7nh");
        return appointment;
    }

    private Appointment buildFollowUpAppointment(Doctor doctor, Appointment parentAppointment) {
        Appointment appointment = new Appointment();
        appointment.setId(2);
        appointment.setDoctor(doctor);
        appointment.setPatient(parentAppointment.getPatient());
        appointment.setSpecialty(parentAppointment.getSpecialty());
        appointment.setAppointmentDate(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 0)));
        appointment.setStatus("PENDING");
        appointment.setPaymentStatus("UNPAID");
        appointment.setAppointmentType("T\u00e1i kh\u00e1m");
        appointment.setParentAppointment(parentAppointment);
        appointment.setFollowUpNote("an sang truoc 7h sang");
        return appointment;
    }

    private MedicalRecord buildRecord(Doctor doctor, Appointment appointment) {
        MedicalRecord record = new MedicalRecord();
        record.setId(201);
        record.setDoctor(doctor);
        record.setAppointment(appointment);
        return record;
    }

    private DoctorSchedule buildSchedule(Doctor doctor, LocalDate date, String shift) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setWorkDate(date);
        schedule.setShift(shift);
        return schedule;
    }

    private CreateFollowUpRequest buildRequest(String date, String time) {
        CreateFollowUpRequest request = new CreateFollowUpRequest();
        request.setFollowUpDate(date);
        request.setFollowUpTime(time);
        request.setNote("Tai kham");
        return request;
    }

    private CompleteAppointmentRequest buildCompleteRequest() {
        CompleteAppointmentRequest request = new CompleteAppointmentRequest();
        request.setDiagnosis("Cam cum");
        request.setNeedFollowUp(true);
        request.setFollowUpDate("14-06-2026");
        request.setFollowUpTime("08:00:00");
        request.setFollowUpNote("Tai kham sau 1 tuan");
        return request;
    }
}
