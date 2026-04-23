# Clinic Backend API for Frontend

## 1) Base setup

- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- Auth header for protected APIs:
  `Authorization: Bearer <accessToken>`

## 2) Security rules

- Public APIs:
  - `/api/auth/**`
  - `/api/payment/vnpay-return`
  - `/swagger-ui/**`
  - `/v3/api-docs/**`
- All remaining APIs require JWT.

---

## 3) Auth APIs

### `POST /api/auth/register`

- Purpose: patient register account directly, no OTP at register time.
- Allowed username:
  - Gmail
  - Phone number

- Body:

```json
{
  "username": "patient01@gmail.com",
  "password": "123456"
}
```

- Example with phone:

```json
{
  "username": "0912345678",
  "password": "123456"
}
```

- Success response:

```json
"Dang ky thanh cong!"
```

- Notes:
  - Backend auto-creates linked `Patient` profile.
  - If register by Gmail, initial patient email is prefilled.
  - If register by phone, initial patient phone is prefilled.

### `POST /api/auth/login`

- Body:

```json
{
  "username": "patient01@gmail.com",
  "password": "123456"
}
```

- Success response:

```json
{
  "accessToken": "jwt_token_here",
  "tokenType": "Bearer ",
  "username": "patient01@gmail.com",
  "role": "ROLE_PATIENT",
  "profileCompleted": false
}
```

- FE rule:
  - If `role = ROLE_PATIENT` and `profileCompleted = false`, redirect user to first-time profile form.

### `GET /api/auth/me`

- Role: authenticated user

- Success response example:

```json
{
  "username": "patient01@gmail.com",
  "role": "ROLE_PATIENT",
  "profileCompleted": true
}
```

### `POST /api/auth/google`

- Body:

```json
{
  "token": "google_id_token"
}
```

- Success response shape is the same as `/api/auth/login`.

### `POST /api/auth/facebook`

- Body:

```json
{
  "token": "facebook_access_token"
}
```

- Success response shape is the same as `/api/auth/login`.

### `POST /api/auth/forgot-password`

- Purpose: send OTP only when user wants to reset password.
- Body:

```json
{
  "username": "user@gmail.com"
}
```

- Gmail account response example:

```json
{
  "message": "OTP da duoc gui den Gmail cua ban.",
  "channel": "EMAIL"
}
```

- Phone account response in current project mode:

```json
{
  "message": "Che do do an: chua gui SMS that. OTP duoc tra ve de test.",
  "channel": "PHONE_DEV",
  "otp": "123456"
}
```

- Notes:
  - Gmail: OTP is sent to email for real.
  - Phone number: current project keeps dev mode, OTP is returned in response and logged in backend console for testing.

### `POST /api/auth/reset-password`

- Body:

```json
{
  "username": "user@gmail.com",
  "otp": "123456",
  "newPassword": "new_secret"
}
```

- Success response:

```json
"Dat lai mat khau thanh cong."
```

### `POST /api/auth/register-doctor`

- Role: `ROLE_ADMIN`

- Body:

```json
{
  "username": "doctor.a",
  "password": "Doctor@123"
}
```

---

## 4) Patient first-time profile flow

### FE flow after patient login

1. Call `/api/auth/login`.
2. If response has `profileCompleted = false`, redirect to profile form page.
3. Call `GET /api/patients/me` to get current patient profile.
4. Submit form to `PUT /api/patients/me`.
5. Only after profile is complete should FE allow booking UI.

### Required fields for completed patient profile

- `fullName`
- `dateOfBirth`
- `phone`
- `gender`
- `nationalId`
- `address`

### Optional field

- `email`
  - If patient registered by Gmail, backend usually already has it.
  - If patient registered by phone, patient may leave it blank or fill it later.

### Validation rules

- `gender` accepts:
  - `MALE`
  - `FEMALE`
  - `OTHER`
- `nationalId` must be exactly `12` digits.
- `phone` must be valid VN phone format.

---

## 5) Patient APIs

### `GET /api/patients/me`

- Role: `ROLE_PATIENT`

- Example response:

```json
{
  "id": 7,
  "fullName": "patient01@gmail.com",
  "dateOfBirth": null,
  "phone": null,
  "email": "patient01@gmail.com",
  "gender": null,
  "nationalId": null,
  "address": null,
  "profileCompleted": false,
  "account": {
    "id": 15,
    "username": "patient01@gmail.com",
    "role": "ROLE_PATIENT"
  }
}
```

### `PUT /api/patients/me`

- Role: `ROLE_PATIENT`
- Purpose: first-time profile completion and later profile update.

- Example request:

```json
{
  "fullName": "Nguyen Van A",
  "dateOfBirth": "2003-09-12",
  "phone": "0912345678",
  "email": "patient01@gmail.com",
  "gender": "MALE",
  "nationalId": "012345678901",
  "address": "123 Le Loi, Quan 1, TP HCM"
}
```

- Success response:
  - returns updated `Patient`
  - backend sets `profileCompleted = true` if all required fields are valid

### `GET /api/patients`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`
- Behavior:
  - `ROLE_ADMIN`: gets all patients
  - `ROLE_DOCTOR`: gets only patients who already booked with that doctor

### `GET /api/patients/{id}`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`
- Behavior:
  - `ROLE_ADMIN`: view any patient
  - `ROLE_DOCTOR`: only view patient who has appointment with current doctor

### `POST /api/patients`

- Role: `ROLE_ADMIN`

### `PUT /api/patients/{id}`

- Role: `ROLE_ADMIN`

### `DELETE /api/patients/{id}`

- Role: `ROLE_ADMIN`

---

## 6) Appointment APIs

### `GET /api/appointments`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`
- Behavior:
  - `ROLE_PATIENT`: returns booking history of current patient
  - `ROLE_DOCTOR`: returns appointments of current doctor

### `GET /api/appointments/{id}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

### `GET /api/appointments/doctor/{doctorId}/slots?date=YYYY-MM-DD`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

- Example:

`/api/appointments/doctor/5/slots?date=2026-04-23`

- Response item example:

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
  - If `full = true`: slot is full
  - If `disabled = true`: disable click

### `POST /api/appointments`

- Role: `ROLE_PATIENT`
- Important:
  - patient is auto-resolved from current token
  - only allowed when patient profile is completed

- Example request:

```json
{
  "specialty": { "id": 1 },
  "doctor": { "id": 5 },
  "appointmentDate": "2026-04-23T08:00:00",
  "symptoms": "Ho, sot"
}
```

- Example success response:

```json
{
  "id": 21,
  "appointmentCode": "PKB-1761234567890",
  "patient": {
    "id": 7
  },
  "specialty": {
    "id": 1,
    "name": "Noi tong quat"
  },
  "doctor": {
    "id": 5,
    "fullName": "Bac si Nguyen Van A"
  },
  "appointmentDate": "2026-04-23T08:00:00",
  "status": "PENDING",
  "symptoms": "Ho, sot",
  "consultationFee": 300000.0,
  "paymentStatus": "UNPAID",
  "notes": null
}
```

- Booking ticket behavior:
  - backend generates `appointmentCode` as booking ticket code
  - if patient has email, booking ticket info is sent to that email
  - `appointmentCode` is also available in booking history (`GET /api/appointments`)

### `PUT /api/appointments/{id}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

### `DELETE /api/appointments/{id}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

---

## 7) Doctor APIs

### `GET /api/doctors`

- Role: `ROLE_ADMIN` / `ROLE_DOCTOR` / `ROLE_PATIENT`

### `GET /api/doctors/{id}`

- Role: `ROLE_ADMIN` / `ROLE_DOCTOR` / `ROLE_PATIENT`

### `POST /api/doctors`

- Role: `ROLE_ADMIN`

- Example:

```json
{
  "fullName": "Bac si Nguyen Van A",
  "email": "doctor.a@medcare.vn",
  "phone": "0901234567",
  "price": 300000,
  "specialty": { "id": 1 },
  "account": {
    "username": "doctor.a",
    "password": "Doctor@123"
  }
}
```

### `PUT /api/doctors/{id}`

- Role: `ROLE_ADMIN`

### `DELETE /api/doctors/{id}`

- Role: `ROLE_ADMIN`

---

## 8) Doctor Schedule APIs

### `GET /api/doctor-schedules`

### `GET /api/doctor-schedules/filter?date=YYYY-MM-DD`

### `POST /api/doctor-schedules`

- Role: `ROLE_DOCTOR`

### `DELETE /api/doctor-schedules/{id}`

- Role: `ROLE_DOCTOR`

---

## 9) Specialty APIs

### `GET /api/specialties`

### `GET /api/specialties/{id}`

### `POST /api/specialties`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `PUT /api/specialties/{id}`

### `DELETE /api/specialties/{id}`

---

## 10) Medicine APIs

### `GET /api/medicines`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `GET /api/medicines/{id}`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `POST /api/medicines`

- Role: `ROLE_ADMIN`

### `PUT /api/medicines/{id}`

- Role: `ROLE_ADMIN`

### `DELETE /api/medicines/{id}`

- Role: `ROLE_ADMIN`

---

## 11) Medical Record APIs

### `GET /api/medical-records`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `GET /api/medical-records/{id}`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `GET /api/medical-records/patient/{patientId}`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `POST /api/medical-records`

- Role: `ROLE_DOCTOR`

### `PUT /api/medical-records/{id}`

- Role: `ROLE_DOCTOR`

### `DELETE /api/medical-records/{id}`

- Role: `ROLE_ADMIN`

---

## 12) Prescription Detail APIs

### `GET /api/prescription-details/record/{recordId}`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `POST /api/prescription-details`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

---

## 13) Medical Service APIs

### `GET /api/medical-services`

### `POST /api/medical-services`

---

## 14) Invoice APIs

### `GET /api/invoices`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `GET /api/invoices/record/{recordId}`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `PUT /api/invoices/{id}/pay`

- Role: `ROLE_ADMIN`

---

## 15) Feedback APIs

### `GET /api/feedbacks`

- Role: `ROLE_ADMIN`

### `GET /api/feedbacks/doctor/{doctorId}`

### `POST /api/feedbacks`

- Role: `ROLE_PATIENT`

### `DELETE /api/feedbacks/{id}`

- Role: `ROLE_ADMIN`

---

## 16) Dashboard APIs

### `GET /api/dashboard/summary`

- Role: `ROLE_ADMIN`

### `GET /api/dashboard/recent-appointments`

- Role: `ROLE_ADMIN`

### `GET /api/dashboard/revenue-chart`

- Role: `ROLE_ADMIN`

---

## 17) Payment APIs

### `GET /api/payment/create-url?amount=<amount>&appointmentId=<id>`

- Creates VNPay checkout URL

### `GET /api/payment/vnpay-return?...&appointmentId=<id>`

- Public VNPay callback

---

## 18) Test security APIs

### `GET /api/test/all`

### `GET /api/test/patient`

- Role: `ROLE_PATIENT`

### `GET /api/test/doctor`

- Role: `ROLE_DOCTOR`

---

## 19) Suggested FE flow

### Patient register and first login

1. Call `POST /api/auth/register`.
2. Call `POST /api/auth/login`.
3. If `profileCompleted = false`, redirect to profile form page.
4. Call `PUT /api/patients/me` to complete patient profile.
5. After success, allow booking flow.

### Forgot password

1. Call `POST /api/auth/forgot-password`.
2. If Gmail account: tell user to check email.
3. If phone account in current project mode: read `otp` from response.
4. Call `POST /api/auth/reset-password`.

### Booking flow

1. Call `GET /api/doctors` or doctor detail APIs.
2. Call `GET /api/appointments/doctor/{doctorId}/slots?date=...`.
3. Call `POST /api/appointments`.
4. Show `appointmentCode` as booking ticket code.
5. Show booking history from `GET /api/appointments`.
