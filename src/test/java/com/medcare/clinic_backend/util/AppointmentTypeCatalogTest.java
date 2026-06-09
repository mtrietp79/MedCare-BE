package com.medcare.clinic_backend.util;

import com.medcare.clinic_backend.entity.Appointment;
import com.medcare.clinic_backend.entity.Patient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentTypeCatalogTest {

    @Test
    void resolve_shouldReturnExaminationForPatientBooking() {
        Appointment appointment = new Appointment();
        appointment.setAppointmentType("Kh\u00e1m b\u1ec7nh");

        AppointmentTypeCatalog.ResolvedType resolved = AppointmentTypeCatalog.resolve(appointment);

        assertEquals(AppointmentTypeCatalog.CODE_EXAMINATION, resolved.code());
        assertEquals(AppointmentTypeCatalog.LABEL_EXAMINATION, resolved.label());
        assertFalse(resolved.reExamination());
    }

    @Test
    void resolve_shouldReturnReExaminationFromParentAppointmentId() {
        Appointment appointment = new Appointment();
        appointment.setAppointmentType("Kh\u00e1m b\u1ec7nh");
        appointment.setParentAppointmentKey(12);

        AppointmentTypeCatalog.ResolvedType resolved = AppointmentTypeCatalog.resolve(appointment);

        assertEquals(AppointmentTypeCatalog.CODE_RE_EXAMINATION, resolved.code());
        assertEquals(AppointmentTypeCatalog.LABEL_RE_EXAMINATION, resolved.label());
        assertTrue(resolved.reExamination());
        assertEquals(12, resolved.originalAppointmentId());
    }

    @Test
    void resolve_shouldReturnReExaminationFromStoredLabel() {
        Appointment appointment = new Appointment();
        appointment.setAppointmentType("T\u00e1i kh\u00e1m");
        appointment.setFollowUpNote("theo doi ket qua");

        AppointmentTypeCatalog.ResolvedType resolved = AppointmentTypeCatalog.resolve(appointment);

        assertEquals(AppointmentTypeCatalog.CODE_RE_EXAMINATION, resolved.code());
        assertTrue(resolved.reExamination());
    }

    @Test
    void resolve_shouldReturnServicePackageWhenLinked() {
        Appointment appointment = new Appointment();
        appointment.setPatient(new Patient());
        com.medcare.clinic_backend.entity.ServicePackage servicePackage =
                new com.medcare.clinic_backend.entity.ServicePackage();
        servicePackage.setId(5);
        appointment.setServicePackage(servicePackage);

        AppointmentTypeCatalog.ResolvedType resolved = AppointmentTypeCatalog.resolve(appointment);

        assertEquals(AppointmentTypeCatalog.CODE_SERVICE_PACKAGE, resolved.code());
        assertEquals(AppointmentTypeCatalog.LABEL_SERVICE_PACKAGE, resolved.label());
    }
}
