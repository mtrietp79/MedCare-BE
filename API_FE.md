# Clinic Backend API for Frontend

> Status/payment mini contract for FE:
> `API_FE_STATUS_CONTRACT.md`

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

## 2.1) Admin Hotfix 2026-06-01

- `GET /api/admin/dashboard/**` and `GET /api/dashboard/**`:
  - now requires `ROLE_ADMIN` (no longer returns fake zero data for non-admin).

- Service package management (`/api/admin/service-packages`):
  - response now includes package booking counters:
    - `totalBooked`
    - `totalCompleted`
    - `totalPaid`
    - `totalPending`
  - response also includes:
    - `itemCount`
    - `status` = `ACTIVE | INACTIVE`
    - `statusDisplay`
    - `hasBookings`
    - `canDelete`
  - admin list query params:
    - `keyword` or `q` or `search`
    - `active=true|false`
    - `status=ACTIVE|INACTIVE`
    - `configured=true|false`
  - new endpoints:
    - `GET /api/admin/service-packages/summary`
    - `GET /api/admin/service-packages/{id}`
    - `PATCH /api/admin/service-packages/{id}/active?active=true|false`
  - `DELETE /api/admin/service-packages/{id}`:
    - now returns `{ "message": "Da xoa goi dich vu." }`
    - backend rejects delete if package already has bookings

- Service package booking admin path aliases:
  - old: `/api/admin/service-package-bookings`
  - new alias: `/api/admin/service-packages/bookings`

- Patient service package booking path aliases:
  - old: `/api/patient/service-package-bookings`
  - new alias: `/api/patient/service-packages/bookings`

- Admin medicine create/update/quantity now return `AdminMedicineResponse` shape (same as medicine list page), not raw `Medicine` entity.

- Website feedback admin response now includes:
  - `statusDisplay`
  - `canApprove`
  - `canHide`
  - `canUnhide`
  - `canDelete`
  - `visibleOnHomepage`

- Website feedback action aliases added:
  - unhide/show/publish:
    - `PUT|PATCH|POST /api/admin/website-feedbacks/{id}/unhide`
    - `PUT|PATCH|POST /api/admin/website-feedbacks/{id}/show`
    - `PUT|PATCH|POST /api/admin/website-feedbacks/{id}/publish`
  - hide aliases:
    - `PUT|PATCH|POST /api/admin/website-feedbacks/{id}/archive`
    - `PUT|PATCH|POST /api/admin/website-feedbacks/{id}/reject`
  - delete alias:
    - `PUT|PATCH|POST /api/admin/website-feedbacks/{id}/destroy`

- Website feedback admin action messages:
  - approve -> `Da duyet feedback.`
  - hide -> `Da an feedback.`
  - unhide -> `Da bo an feedback.`
  - delete -> `Da xoa feedback.`

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
  "displayName": "Nguyen Van A",
  "role": "ROLE_PATIENT",
  "profileCompleted": false
}
```

- FE rule:
  - Do not redirect or show the patient profile form immediately after login.
  - `profileCompleted` is only a status flag. Use it on `/patient/profile` and inside the booking patient-information step.

### `GET /api/auth/me`

- Role: authenticated user

- Success response example:

```json
{
  "username": "patient01@gmail.com",
  "displayName": "Nguyen Van A",
  "role": "ROLE_PATIENT",
  "profileCompleted": true
}
```

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
  "message": "Che do do an: chua gui SMS that. OTP khong tra ve response.",
  "channel": "PHONE_DEV"
}
```

- Notes:
  - Gmail: OTP is sent to email for real.
  - Phone number: in dev mode, backend can return OTP only when `OTP_EXPOSE_DEV_OTP=true`.

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

## 4) Patient profile completion flow

### FE flow after patient login

1. Call `/api/auth/login`.
2. Store auth data as usual.
3. Do not redirect to profile form and do not open a profile-completion modal on home.
4. If `role = ROLE_PATIENT` and `profileCompleted = false`, keep the user on the intended page.

### Allowed places to show the incomplete-profile form

1. `/patient/profile`: user opens this page voluntarily. Call `GET /api/patients/me`, render the existing form, and submit to `PUT /api/patients/me`.
2. Booking flow: when the user reaches the patient-information step, call `GET /api/patients/me`. If `profileCompleted = false`, show the same form inside that booking step and submit to `PUT /api/patients/me`. After success, continue booking.

Do not show this form on home or as a global login guard.

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

- Public.

- Example:

`/api/appointments/doctor/5/slots?date=2026-04-23`

Also accepts non-padded dates like `23/5/2026`.

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

### `GET /api/appointments/medical-service/{serviceId}/slots?date=YYYY-MM-DD`

- Public.
- Purpose: slot status when user books directly from a service package.
- If the service package has `assignedDoctor`, backend checks that doctor only.
- If the service package has no assigned doctor, backend checks all doctors in the service specialty and marks a slot available when at least one doctor still has capacity.

Example:

`/api/appointments/medical-service/3/slots?date=2026-04-23`

Response item shape is the same as doctor slot API.

### `POST /api/appointments`

- Role: `ROLE_PATIENT`
- Important:
  - patient is auto-resolved from current token
  - only allowed when patient profile is completed
  - if profile is incomplete, backend returns:

```json
{
  "message": "Vui long cap nhat day du ho so ca nhan truoc khi dat lich.",
  "code": "PROFILE_INCOMPLETE"
}
```

FE should handle `PROFILE_INCOMPLETE` only inside the booking flow by returning the user to the patient-information step and showing the profile form there.

- Example request:

```json
{
  "specialty": { "id": 1 },
  "medicalService": { "id": 3 },
  "doctor": { "id": 5 },
  "appointmentDate": "2026-04-23T08:00:00",
  "symptoms": "Ho, sot"
}
```

Booking from services page can omit doctor:

```json
{
  "medicalService": { "id": 3 },
  "appointmentDate": "2026-04-23T08:00:00",
  "symptoms": "Can tu van goi dich vu"
}
```

- `medicalService` is optional.
- If omitted/null, appointment is a normal consultation and FE should display service as `Kham benh`.
- If provided, backend validates that the service is active and belongs to the selected specialty.
- If booking from the services page, FE can send `medicalService.id` and `appointmentDate`; `doctor` can be omitted.
- If the service has `assignedDoctor`, backend uses that doctor and rejects a different requested doctor.
- If the service has no assigned doctor, backend randomly selects an available doctor in the service specialty for the selected slot.
- When `medicalService` is selected, `consultationFee` is set from the service package price.

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
  "medicalService": {
    "id": 3,
    "name": "Dich vu xet nghiem mau",
    "price": 450000.0,
    "active": true,
    "assignedDoctor": {
      "id": 5,
      "fullName": "Bac si Nguyen Van A"
    }
  },
  "appointmentDate": "2026-04-23T08:00:00",
  "status": "PENDING",
  "symptoms": "Ho, sot",
  "consultationFee": 450000.0,
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
- Patient chi duoc cap nhat de huy lich (`status = CANCELLED`), khong duoc doi ngay gio/bac si.

### `PATCH /api/appointments/{id}/cancel`

- Role: `ROLE_PATIENT`
- Dung endpoint nay cho nut `Huy lich kham` o FE patient.

### `DELETE /api/appointments/{id}`

- Role: `ROLE_DOCTOR` or `ROLE_PATIENT`

---

## 7) Doctor APIs

### `GET /api/doctors`

- Role: `ROLE_ADMIN` / `ROLE_DOCTOR` / `ROLE_PATIENT`
- Query optional:
  - `specialtyId`
  - `name`: search by doctor full name, case-insensitive
- Response bo sung cac truong de FE filter an toan:
  - `fullName`, `name`
  - `email`, `phone`
  - `photoId`, `photoUrl`, `imageUrl`
  - `specialtyName`, `specialization`
  - `username`
  - van giu object `specialty` va `account`

Example:

`GET /api/doctors?name=nguyen`

Response item example:

```json
{
  "id": 5,
  "fullName": "Bac si Nguyen Van A",
  "name": "Bac si Nguyen Van A",
  "email": "doctor.a@medcare.vn",
  "phone": "0901234567",
  "price": 300000,
  "rating": 4.8,
  "experienceYears": 8,
  "experience": 8,
  "photoId": 2,
  "photoUrl": "/api/doctors/5/photo",
  "imageUrl": "/api/doctors/5/photo",
  "specialtyId": 1,
  "specialtyName": "Noi tong quat",
  "specialization": "Noi tong quat"
}
```

FE rule:
- On `/doctors`, add a search input for doctor name.
- Debounce the input and call `GET /api/doctors?name=<keyword>`.
- If specialty filter is also active, call `GET /api/doctors?specialtyId=<id>&name=<keyword>`.
- Use `photoUrl` or `imageUrl` for doctor avatar. If null, show default avatar.

### `GET /api/doctors/{id}`

- Role: `ROLE_ADMIN` / `ROLE_DOCTOR` / `ROLE_PATIENT`

### `GET /api/doctors/me`

- Role: `ROLE_DOCTOR`
- Returns current doctor's profile, including optional `photoUrl`.

### `PUT /api/doctors/me/photo`

- Role: `ROLE_DOCTOR`
- Purpose: optional upload/update doctor personal photo in doctor's own profile.
- Content-Type: `multipart/form-data`
- Form field: `file`
- Supported file types: JPEG, PNG, WEBP
- Max size: 2MB
- Success response: updated `DoctorResponse` with `photoUrl`.

### `DELETE /api/doctors/me/photo`

- Role: `ROLE_DOCTOR`
- Purpose: remove current doctor's personal photo.

### `GET /api/doctors/{id}/photo`

- Public image endpoint for rendering doctor photo in `<img>`.
- Returns raw image bytes with image content type.

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
- Admin can set/update personal doctor consultation price with field `price`.

### `PUT /api/doctors/{id}/photo`

- Role: `ROLE_ADMIN`
- Content-Type: `multipart/form-data`
- Form field: `file`
- Upload/update photo for a doctor.

### `DELETE /api/doctors/{id}/photo`

- Role: `ROLE_ADMIN`
- Remove photo for a doctor.

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
- Query optional:
  - `keyword`: search theo `name` hoặc `medicineCategory`
  - `category`: lọc theo `medicineCategory` (exact match, không phân biệt hoa thường)
- Response item cần dùng:
  - `id`, `name`, `medicineCategory`, `unit`, `quantity`, `price`, `status`, `dosage`, `description`
- Ghi chú hotfix 2026-06-01:
  - Endpoint đã xử lý an toàn với dữ liệu DB cũ bị lệch kiểu (`bytea`) để tránh 500.
  - FE phải đọc danh mục thuốc từ `medicineCategory`, không dùng tạm `unit` để phân loại.

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

### `GET /api/medical-records/my`

- Role: `ROLE_PATIENT`
- Lay danh sach ho so benh an cua chinh patient dang dang nhap.

### `GET /api/medical-records/my/{id}`

- Role: `ROLE_PATIENT`
- Lay chi tiet 1 ho so benh an cua chinh patient dang dang nhap.

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

- Public.
- Purpose: service package list for `services` tab/page and home ads.
- Query optional:
  - `specialtyId`: only active services of selected specialty.
  - `q` or `search`: search active service packages by name, case-insensitive.

Example:

`GET /api/medical-services?specialtyId=1`

Search example:

`GET /api/medical-services?q=xet%20nghiem`

Response item example:

```json
{
  "id": 3,
  "name": "Dich vu xet nghiem mau",
  "description": "Goi xet nghiem cong thuc mau co ban.",
  "price": 450000.0,
  "imageUrl": "/api/medical-services/3/photo",
  "active": true,
  "advertised": false,
  "specialty": {
    "id": 1,
    "name": "Noi tong quat"
  },
  "assignedDoctor": {
    "id": 5,
    "fullName": "Bac si Nguyen Van A",
    "price": 300000,
    "specialty": {
      "id": 1,
      "name": "Noi tong quat"
    }
  },
  "prescriptionItems": [
    {
      "id": 10,
      "medicine": {
        "id": 2,
        "name": "Paracetamol"
      },
      "quantity": 10,
      "dosage": "Sang 1 vien, toi 1 vien sau an"
    }
  ]
}
```

Notes:
- `imageUrl` is returned by backend when the service has a photo in database.
- `assignedDoctor` can be null. Null means backend will randomly choose an available doctor in the service specialty at booking time.
- `prescriptionItems` can be empty. Empty means doctor will prescribe after examination.
- FE services page: add a search input and call `GET /api/medical-services?q=<keyword>`. If a specialty filter is active, call `GET /api/medical-services?specialtyId=<id>&q=<keyword>`.
- FE booking flow: remove service package selection from the specialty step. Service packages are booked from the services page/detail page.

### `GET /api/medical-services/{id}`

- Public.

### `GET /api/medical-services/admin`

- Role: `ROLE_ADMIN`
- Returns all service packages, including inactive/stopped packages.
- Query optional:
  - `specialtyId`
  - `q` or `search`: search all service packages by name, including inactive packages.

### `POST /api/medical-services`

- Role: `ROLE_ADMIN`

Example request:

```json
{
  "name": "Dich vu kham tong quat",
  "description": "Kham tong quat theo chuyen khoa noi.",
  "price": 600000,
  "active": true,
  "specialty": { "id": 1 },
  "assignedDoctor": { "id": 5 },
  "prescriptionItems": []
}
```

You may send `assignedDoctorId: 5` instead of nested `assignedDoctor`.

Example with predefined medicines:

```json
{
  "name": "Dich vu xet nghiem mau",
  "description": "Goi xet nghiem va tu van sau ket qua.",
  "price": 450000,
  "specialty": { "id": 1 },
  "assignedDoctor": null,
  "prescriptionItems": [
    {
      "medicine": { "id": 2 },
      "quantity": 10,
      "dosage": "Sang 1 vien sau an"
    }
  ]
}
```

### `PUT /api/medical-services/{id}`

- Role: `ROLE_ADMIN`
- Full update service package.

### `PATCH /api/medical-services/{id}/active?active=false`

- Role: `ROLE_ADMIN`
- Stop or reactivate service package without deleting historical appointments.

### `PUT /api/medical-services/{id}/photo`

- Role: `ROLE_ADMIN`
- Content-Type: `multipart/form-data`
- Form field: `file`
- Supported file types: JPEG, PNG, WEBP
- Max size: 2MB
- Stores image bytes in database, same style as doctor photo.
- Success response: updated service package with `imageUrl`.

### `GET /api/medical-services/{id}/photo`

- Public image endpoint for rendering service photo in `<img>`.
- Returns raw image bytes with image content type.

### `DELETE /api/medical-services/{id}/photo`

- Role: `ROLE_ADMIN`
- Remove photo for a service package.

FE page suggestions:
- Add nav tab `services` beside home/doctor/booking.
- Home service ad can show several active items from `GET /api/medical-services`.
- Booking appointment info should display `appointment.medicalService.name`; if null display `Kham benh`.

---

## 14) Invoice APIs

### `GET /api/invoices`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `GET /api/invoices/record/{recordId}`

- Role: `ROLE_ADMIN` or `ROLE_DOCTOR`

### `GET /api/invoices/my`

- Role: `ROLE_PATIENT`
- Query optional:
  - `status`
  - `keyword`

### `GET /api/invoices/my/{id}`

- Role: `ROLE_PATIENT`
- Lay chi tiet hoa don cua chinh patient.

### `GET /api/invoices/my/record/{recordId}`

- Role: `ROLE_PATIENT`
- Lay hoa don theo ho so benh an cua chinh patient.

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

### `GET /api/payment/create-url?appointmentId=<id>`

- Role: `ROLE_PATIENT`
- Creates VNPay checkout URL.
- Amount is resolved from backend by appointment consultation fee (client cannot override).
- Chi patient so huu lich hen moi tao duoc link thanh toan.

### `GET /api/payment/create-invoice-url?invoiceId=<id>`

- Role: `ROLE_PATIENT`
- Purpose: patient thanh toan hoa don phat sinh sau kham qua VNPay.
- Only the owner patient can tao link thanh toan hoa don.
- Backend returns payment URL string:

```json
"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."
```

- FE rule:
  - Không hiển thị nút `Thanh toán tại phòng khám`.
  - Hiển thị nút `Thanh toán VNPay` cho hóa đơn có `canPayOnline = true`.
  - On click, call this endpoint và redirect sang VNPay URL backend trả về.
  - Ẩn hoặc disable nút thanh toán khi hóa đơn đã `PAID`.

### `GET /api/payment/vnpay-return?...&appointmentId=<id>`

- Public VNPay callback

### `GET /api/payment/vnpay-return?...&invoiceId=<id>`

- Public VNPay callback cho hoa don sau kham.

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
3. Do not redirect to profile form and do not show a global profile modal on home.
4. Let the user update the profile voluntarily at `/patient/profile`, or require it only inside the booking patient-information step.

### Forgot password

1. Call `POST /api/auth/forgot-password`.
2. If Gmail account: tell user to check email.
3. If phone account in current project mode: read `otp` from response.
4. Call `POST /api/auth/reset-password`.

### Booking flow

1. Call `GET /api/doctors` or doctor detail APIs.
2. Call `GET /api/appointments/doctor/{doctorId}/slots?date=...`.
3. At the patient-information step, call `GET /api/patients/me`.
4. If `profileCompleted = false`, show the profile form in this step and submit `PUT /api/patients/me`.
5. After profile is complete, call `POST /api/appointments`.
6. Show `appointmentCode` as booking ticket code.
7. Show booking history from `GET /api/appointments`.
