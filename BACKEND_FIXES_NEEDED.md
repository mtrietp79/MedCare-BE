# 🚨 CRITICAL: Backend Issue Found - 500 Error on Follow-up Slots

**Date:** 2026-06-07
**Severity:** CRITICAL 🔴
**Component:** Follow-up Appointment Scheduling Flow
**Status:** BLOCKING - Cannot schedule follow-ups

---

## Issue Summary

The frontend is unable to schedule follow-up appointments because the backend endpoint `/api/doctor/follow-up-slots` is returning **HTTP 500** error when called with a date parameter.

---

## Error Details

### Request
```
GET /api/doctor/follow-up-slots?date=2026-06-14
```

### Current Response
```
Status: 500 Internal Server Error
```

### Error Message (from FE console)
```
Failed to load resource: the server responded with a status of 500 ()
```

---

## What Frontend is Sending

```typescript
// From: src/services/doctorAppointmentService.ts line 489-495
async getFollowUpSlots(date: string): Promise<AppointmentSlot[]> {
  const normalizedDate = normalizeDateToIsoDate(date)  // Converts to yyyy-MM-dd format
  const { data } = await doctorApiClient.get('/doctor/follow-up-slots', {
    params: { date: normalizedDate },  // e.g., { date: "2026-06-14" }
  })
  return normalizeListResponse<unknown>(data)
    .map((item) => normalizeAppointmentSlot(item))
    .filter((item): item is AppointmentSlot => item !== null)
}
```

---

## Expected Response

### Success Response (Status 200)
```json
[
  {
    "startTime": "2026-06-14T08:00:00",
    "endTime": "2026-06-14T08:30:00",
    "shift": "morning",
    "maxPatients": 5,
    "bookedPatients": 2,
    "full": false,
    "disabled": false,
    "disabledReason": null
  },
  {
    "startTime": "2026-06-14T08:30:00",
    "endTime": "2026-06-14T09:00:00",
    "shift": "morning",
    "maxPatients": 5,
    "bookedPatients": 4,
    "full": false,
    "disabled": false,
    "disabledReason": null
  },
  {
    "startTime": "2026-06-14T09:00:00",
    "endTime": "2026-06-14T09:30:00",
    "shift": "morning",
    "maxPatients": 5,
    "bookedPatients": 5,
    "full": true,
    "disabled": true,
    "disabledReason": "Slot is full"
  }
]
```

---

## Impact

- **Doctor UI:** Cannot click on date in follow-up scheduling modal → no time slots load → shows error message
- **Patient UI:** Cannot see follow-up appointments scheduled by doctor (no new follow-ups created)
- **System:** Complete follow-up appointment flow is blocked

---

## Next Steps for Backend Team

1. **Debug the endpoint**
   - Check `/api/doctor/follow-up-slots` handler in backend code
   - Verify query parameter parsing for `date`
   - Check database queries for fetching available slots
   - Look for null pointer exceptions, database connection issues, or invalid SQL

2. **Test the endpoint**
   - Use Postman/curl to test:
     ```bash
     curl "http://localhost:8080/api/doctor/follow-up-slots?date=2026-06-14"
     ```
   - Should return status 200 with array of AppointmentSlot objects

3. **Return proper response**
   - Should return array (can be empty if no slots available)
   - Each slot should have all required fields
   - Status must be 200

4. **Verify doctor authentication**
   - Ensure current doctor ID is properly extracted from JWT token
   - Filter follow-up slots for current doctor only

---

## Additional Notes

- **Frontend is working correctly:** All other endpoints (complete appointment, get appointments) are implemented and ready
- **Payload formats are correct:** Complete appointment payloads follow the agreed contract (no `appointment_time`, correct `followUpDate`/`followUpTime` formats)
- **UI is ready:** Doctor page has full UI for scheduling follow-ups, just waiting for this endpoint to work

---

## Contact

**Frontend Dev:** Ready to provide network traces, browser console logs, or additional debugging info if needed.

---

**Document Version:** 1.0
**Generated:** 2026-06-07 10:15 UTC+7
**Priority Level:** P0 - Blocks user stories
