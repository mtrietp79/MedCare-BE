# Clinic Backend API for Frontend

## 1) Base setup

- Base URL: `http://localhost:8080`
- Content-Type (JSON APIs): `application/json`
- Auth header (for protected APIs):  
  `Authorization: Bearer <accessToken>`

## 2) Security rules

- Public (no token):
  - `/api/auth/**`
  - `/api/payment/vnpay-return`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`
- All other endpoints require JWT token.

---

## 3) Auth APIs

### `POST /api/auth/register`

- Body:

```json
{
  "username": "patient01",
  "password": "123456",
  "role": "ROLE_PATIENT"
}
```

- Note: backend currently sets default role to `ROLE_PATIENT`.

### `POST /api/auth/login`

- Body:

```json
{
  "username": "patient01",
  "password": "123456"
}
```

- Response:

```json
{
  "accessToken": "jwt_token_here",
  "tokenType": "Bearer "
}
```

### `POST /api/auth/google`

- Body:

```json
{
  "token": "google_id_token"
}
```

### `POST /api/auth/facebook`

- Body:

```json
{
  "token": "facebook_access_token"
}
```

### `POST /api/auth/forgot-password`

- Body:

```json
{
  "email": "user@mail.com"
}
```

### `POST /api/auth/reset-password`

- Body:

```json
{
  "email": "user@mail.com",
  "otp": "123456",
  "newPassword": "new_secret"
}
```

---

## 4) Appointment APIs

### `GET /api/appointments`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

### `GET /api/appointments/{id}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

### `GET /api/appointments/doctor/{doctorId}/slots?date=YYYY-MM-DD`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`
- Example: `/api/appointments/doctor/5/slots?date=2026-04-23`
- Response item:

```json
{
  "startTime": "2026-04-23T08:00:00",
  "endTime": "2026-04-23T09:00:00",
  "shift": "MORNING",
  "maxPatients": 5,
  "bookedPatients": 5,
  "full": true,
  "disabled": true
}
```

- FE rule:
  - If `full = true`: slot is full, paint black.
  - If `disabled = true`: disable click.

### `POST /api/appointments`

- Role: `ROLE_PATIENT`
- Patient is auto-resolved from logged-in account.
- Core fields FE should send in body:
  - `specialty.id` (required if no doctor)
  - `doctor.id` (optional if user chooses specific doctor)
  - `appointmentDate` (must be one of supported slot start times)
  - `symptoms` (optional)

- Example body:

```json
{
  "specialty": { "id": 1 },
  "doctor": { "id": 5 },
  "appointmentDate": "2026-04-23T08:00:00",
  "symptoms": "Ho, sot"
}
```

### `PUT /api/appointments/{id}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

### `DELETE /api/appointments/{id}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

---

## 5) Doctor APIs

### `GET /api/doctors`

### `GET /api/doctors/{id}`

### `POST /api/doctors`

### `PUT /api/doctors/{id}`

### `DELETE /api/doctors/{id}`

---

## 6) Doctor Schedule APIs

### `GET /api/doctor-schedules`

### `GET /api/doctor-schedules/filter?date=YYYY-MM-DD`

### `POST /api/doctor-schedules`

- Role: `ROLE_DOCTOR`

### `DELETE /api/doctor-schedules/{id}`

- Role: `ROLE_DOCTOR`

---

## 7) Patient APIs

### `GET /api/patients`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `GET /api/patients/{id}`

### `POST /api/patients`

### `PUT /api/patients/{id}`

### `DELETE /api/patients/{id}`

---

## 8) Specialty APIs

### `GET /api/specialties`

### `GET /api/specialties/{id}`

### `POST /api/specialties`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `PUT /api/specialties/{id}`

### `DELETE /api/specialties/{id}`

---

## 9) Medicine APIs

### `GET /api/medicines`

### `GET /api/medicines/{id}`

### `POST /api/medicines`

- Role: `ROLE_ADMIN`

### `PUT /api/medicines/{id}`

- Role: `ROLE_ADMIN`

### `DELETE /api/medicines/{id}`

- Role: `ROLE_ADMIN`

---

## 10) Medical Record APIs

### `GET /api/medical-records`

### `GET /api/medical-records/{id}`

### `GET /api/medical-records/patient/{patientId}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

### `POST /api/medical-records`

- Role: `ROLE_DOCTOR`

### `PUT /api/medical-records/{id}`

- Role: `ROLE_DOCTOR`

### `DELETE /api/medical-records/{id}`

---

## 11) Prescription Detail APIs

### `GET /api/prescription-details/record/{recordId}`

### `POST /api/prescription-details`

- Role: `ROLE_DOCTOR`

---

## 12) Medical Service APIs

### `GET /api/medical-services`

### `POST /api/medical-services`

---

## 13) Invoice APIs

### `GET /api/invoices`

### `GET /api/invoices/record/{recordId}`

### `PUT /api/invoices/{id}/pay`

---

## 14) Feedback APIs

### `GET /api/feedbacks`

- Role: `ROLE_ADMIN`

### `GET /api/feedbacks/doctor/{doctorId}`

### `POST /api/feedbacks`

- Role: `ROLE_PATIENT`

### `DELETE /api/feedbacks/{id}`

- Role: `ROLE_ADMIN`

---

## 15) Dashboard APIs

### `GET /api/dashboard/summary`

- Role: `ROLE_ADMIN`

### `GET /api/dashboard/recent-appointments`

- Role: `ROLE_ADMIN`

### `GET /api/dashboard/revenue-chart`

- Role: `ROLE_ADMIN`

---

## 16) Payment APIs

### `GET /api/payment/create-url?amount=<amount>&appointmentId=<id>`

- Creates VNPay checkout URL.

### `GET /api/payment/vnpay-return?...&appointmentId=<id>`

- Public callback from VNPay.

---

## 17) Test security APIs

### `GET /api/test/all`

### `GET /api/test/patient`

- Role: `ROLE_PATIENT`

### `GET /api/test/doctor`

- Role: `ROLE_DOCTOR`

---

## 18) Suggested FE flow

1. Login via `/api/auth/login` -> store `accessToken`.
2. Send token in `Authorization` header for protected APIs.
3. Booking screen:
   - call slot API `/api/appointments/doctor/{doctorId}/slots?date=...`
   - disable slot when `disabled = true`
   - paint full slot when `full = true`
4. Create appointment via `POST /api/appointments`.

