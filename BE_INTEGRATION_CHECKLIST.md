# 📋 Backend Integration Checklist - Follow-up Appointment Flow

**Date:** 2026-06-07  
**Status:** 🔴 Testing in progress - 500 errors encountered  
**Frontend Version:** React 19 + TypeScript + Vite

---

## 🐛 Issues Found

### Issue #1: 500 Error on Follow-up Time Slot Loading
- **Endpoint:** GET `/api/doctor/schedule/{doctorId}/available-slots` or similar
- **Trigger:** When user opens follow-up appointment scheduling modal
- **Error:** `Status 500`
- **Message:** "Dạ vậy rõi lợi be thong. Vui lòng thử lại sau." (Backend error message)
- **Impact:** Cannot select follow-up date/time

---

## ✅ Completed Frontend Changes

### 1. **Complete Appointment Endpoint**
- **Endpoint:** `POST /api/doctor/appointments/{appointmentId}/complete`
- **Payload Structure:**
```json
{
  "symptoms": "string",
  "diagnosis": "string",
  "doctorAdvice": "string",
  "medicineItems": [
    {
      "medicineId": "string",
      "quantity": number,
      "usage": "string"
    }
  ],
  "serviceItems": [
    {
      "serviceId": "string",
      "description": "string"
    }
  ],
  "followUp": {
    "needFollowUp": boolean,
    "followUpDate": "yyyy-MM-dd",  // ISO date format (no appointment_time)
    "followUpTime": "HH:mm",       // 24-hour format
    "note": "string"
  }
}
```

### 2. **Response Expected Structure**
```json
{
  "success": true,
  "message": "string",
  "completeResult": {
    "appointmentId": "string",
    "completedAt": "ISO8601 datetime",
    "invoice": {
      "id": "string",
      "appointmentId": "string",
      "consultationFee": number,      // 50% of original fee if follow-up
      "totalAmount": number,
      "paymentStatus": "PENDING|COMPLETED|FAILED",
      "paidAt": "ISO8601 datetime or null"
    },
    "followUpAppointment": {  // null if no follow-up
      "id": "string",
      "appointmentId": "string",
      "appointmentTime": "ISO8601 datetime",
      "appointmentType": "Tái khám",  // Use appointmentType, not type
      "doctorId": "string",
      "patientId": "string",
      "status": "SCHEDULED",
      "consultationFee": number,      // 50% of original fee
      "parentAppointmentId": "string",
      "followUpNote": "string",
      "createdAt": "ISO8601 datetime"
    }
  }
}
```

### 3. **Frontend Rules Implemented**
- ✅ Never send `appointment_time` in completion payload
- ✅ Use `appointmentType` (not `type`) for follow-up display
- ✅ Follow-up date formatted as `yyyy-MM-dd` (e.g., "2026-06-14")
- ✅ Follow-up time formatted as `HH:mm` (e.g., "08:00")
- ✅ Show toast notification when `followUpAppointment` is not null
- ✅ Reload GET `/api/doctor/appointments` after completion
- ✅ Display consultationFee (50% for follow-ups)
- ✅ Add follow-up appointment to doctor's appointment list immediately

---

## 🔧 Endpoints to Check/Fix

### Priority 1 (Critical)
1. **POST `/api/doctor/appointments/{appointmentId}/complete`**
    - Verify payload structure matches above
    - Ensure `followUpAppointment` response is populated when follow-up is created
    - Check invoice `consultationFee` calculation (50% for follow-ups)

2. **GET `/api/doctor/follow-up-slots`** ⚠️ **Currently returning 500 ERROR**
    - **Query Parameters:** `date` (ISO format: yyyy-MM-dd, e.g., "2026-06-14")
    - **Response Expected:** Array of `AppointmentSlot` objects
    - **AppointmentSlot Structure:**
      ```json
      {
        "startTime": "2026-06-14T08:00:00",  // ISO8601 datetime
        "endTime": "2026-06-14T08:30:00",    // ISO8601 datetime
        "shift": "morning",                   // e.g., "morning", "afternoon"
        "maxPatients": 5,
        "bookedPatients": 2,
        "full": false,
        "disabled": false,
        "disabledReason": null
      }
      ```
    - **Current Frontend Call:**
      ```typescript
      GET /doctor/follow-up-slots?date=2026-06-14
      ```
    - **Status:** Throwing 500 error when doctor tries to schedule a follow-up
    - **Priority:** CRITICAL - Blocks follow-up appointment scheduling UI

### Priority 2 (Important)
3. **GET `/api/doctor/appointments`**
    - Should return updated list including newly created follow-ups
    - Verify `appointmentType` field is populated

4. **GET `/api/patient/appointments`**
    - Returns patient's appointments including follow-ups
    - Verify follow-up appointments have `appointmentType: "Tái khám"` or similar

5. **GET `/api/patient/medical-records/{recordId}`**
    - Should include `followUp` object in response
    - Used to display follow-up details on patient medical record page

---

## 📝 Frontend Files Modified

1. `src/pages/doctor/DoctorAppointmentsPage.tsx`
    - Complete appointment flow with follow-up support
    - Shows consultationFee and paymentStatus columns
    - Toast notification for follow-up creation

2. `src/services/doctorAppointmentService.ts`
    - CompleteAppointmentPayload with followUp structure
    - Payload normalizer to ensure date/time formats

3. `src/services/api.ts`
    - Added `patientApi.getMyAppointments()`
    - Appointment list normalizer

4. `src/pages/patient/PatientAppointmentsPage.tsx`
    - Shows follow-ups with label "Lịch tái khám do bác sĩ hẹn"

5. `src/lib/appointment-type.ts`
    - Helper to get appointment type label (prefer `appointmentType`)

6. `src/pages/patient/PatientMedicalRecordsPage.tsx`
    - Displays appointment type (Khám bệnh / Tái khám)

---

## 🧪 Testing Scenarios

### Scenario 1: Complete with Follow-up
1. Doctor logs in → View appointments
2. Click "Hoàn tất" on an appointment
3. Fill symptoms, diagnosis, advice
4. **Toggle "Cần tái khám" ON**
5. Select follow-up date (e.g., 2026-06-14) and time (e.g., 08:00)
6. Add follow-up note
7. Submit → Expect toast: "Tái khám đã được lên lịch"
8. Verify GET `/api/doctor/appointments` returns follow-up in list
9. Check invoice shows 50% consultation fee

### Scenario 2: Complete without Follow-up
1. Same as Scenario 1, but toggle "Cần tái khám" OFF
2. Submit → No follow-up should be created
3. Invoice should show full consultation fee (or appropriate fee)

### Scenario 3: Patient View
1. Patient logs in → My Appointments
2. Should see completed appointment + newly scheduled follow-up
3. Follow-up should show label "Lịch tái khám do bác sĩ hẹn"

### Scenario 4: Medical Records
1. Patient → Medical Records
2. Completed visit shows appointment type "Khám bệnh"
3. Follow-up visit shows appointment type "Tái khám"
4. Click on follow-up visit → Show follow-up details (date, time, doctor notes)

---

## 🚨 Current Blocker

**500 Error on Available Time Slots**
- Endpoint: Likely `GET /api/doctor/schedule/...`
- Need backend to debug and return proper response
- Frontend is sending correct parameters but backend is throwing error

---

## 📞 Next Steps

1. **Backend Team:** Fix the 500 error on available time slots endpoint
2. **Backend Team:** Test complete appointment endpoint with sample payloads
3. **Both Teams:** Coordinate on exact response structure for invoice/followUpAppointment
4. **Frontend Team:** Run full scenario tests once backend is stable

---

## 📎 Reference Payloads

### Complete Appointment Request (with follow-up)
```json
{
  "symptoms": "Ho, sốt, đau cơ",
  "diagnosis": "Cảm cúm",
  "doctorAdvice": "Nghỉ ngơi, uống nhiều nước",
  "medicineItems": [
    {
      "medicineId": "med_123",
      "quantity": 1,
      "usage": "Uống 1 viên 3 lần/ngày"
    }
  ],
  "serviceItems": [],
  "followUp": {
    "needFollowUp": true,
    "followUpDate": "2026-06-14",
    "followUpTime": "08:00",
    "note": "Tái khám lại để kiểm tra tiến triển"
  }
}
```

### Complete Appointment Request (without follow-up)
```json
{
  "symptoms": "Kiểm tra sức khỏe định kỳ",
  "diagnosis": "Bình thường",
  "doctorAdvice": "Tiếp tục tập thể dục và ăn uống lành mạnh",
  "medicineItems": [],
  "serviceItems": [],
  "followUp": {
    "needFollowUp": false
  }
}
```

---

**Document Version:** 1.0  
**Last Updated:** 2026-06-07 10:00 UTC+7
