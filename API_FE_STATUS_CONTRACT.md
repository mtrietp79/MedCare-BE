# MedCare Mini API Contract (Status & Payment)

Last updated: `2026-06-05`  
Owner: Backend team  
Scope: FE pages that render appointment / payment / invoice / medical-record statuses.

## 1) Global status dictionary (single source of truth)

### 1.1 Appointment status (code -> label)

| Code | Label (VI) | Notes |
|---|---|---|
| `PENDING_PAYMENT` | `ChÆ°a khÃ¡m` | Chá» thanh toÃ¡n lá»‹ch khÃ¡m ban Ä‘áº§u |
| `PENDING` | `ChÆ°a khÃ¡m` | Chá» khÃ¡m |
| `CONFIRMED` | `ChÆ°a khÃ¡m` | ÄÃ£ thanh toÃ¡n, Ä‘ang chá» khÃ¡m |
| `COMPLETED` | `ÄÃ£ khÃ¡m` | ÄÃ£ hoÃ n táº¥t khÃ¡m |
| `CANCELLED` | `ÄÃ£ há»§y` | ÄÃ£ há»§y lá»‹ch |

### 1.2 Appointment paymentStatus (code -> label)

| Code | Label (VI) | Notes |
|---|---|---|
| `UNPAID` | `ChÆ°a thanh toÃ¡n` | ChÆ°a thanh toÃ¡n |
| `PAID` | `ÄÃ£ thanh toÃ¡n` | ÄÃ£ thanh toÃ¡n |
| `PAID_ONLINE` | `ÄÃ£ thanh toÃ¡n` | VNPay thÃ nh cÃ´ng |
| `FAILED` | `Thanh toÃ¡n tháº¥t báº¡i` | Thanh toÃ¡n lá»—i |
| `CANCELLED` | `ÄÃ£ há»§y` | Giao dá»‹ch/lá»‹ch bá»‹ há»§y |

### 1.3 Invoice status (code -> label)

| Code | Label (VI) |
|---|---|
| `UNPAID` / `PENDING` | `ChÆ°a thanh toÃ¡n` |
| `PAID` | `ÄÃ£ thanh toÃ¡n` |
| `FAILED` | `Thanh toÃ¡n tháº¥t báº¡i` |
| `CANCELLED` | `ÄÃ£ há»§y` |

### 1.4 Appointment type

| Field | Value |
|---|---|
| `appointmentType` | `KhÃ¡m bá»‡nh` \| `TÃ¡i khÃ¡m` |

---

## 2) Mini contract theo mÃ n FE

## 2.1 Patient - Lá»‹ch háº¹n cá»§a tÃ´i

Endpoint: `GET /api/appointments` (ROLE_PATIENT)

> Response hiá»‡n lÃ  `Appointment` entity. FE chá»‰ Ä‘á»c cÃ¡c field Ä‘Ã£ liá»‡t kÃª dÆ°á»›i Ä‘Ã¢y.

| Field | Type | Báº¯t buá»™c | FE dÃ¹ng Ä‘á»ƒ |
|---|---|---|---|
| `id` | number | Yes | Link chi tiáº¿t lá»‹ch |
| `appointmentCode` | string | Yes | MÃ£ lá»‹ch |
| `appointmentDate` | datetime | Yes | Hiá»ƒn thá»‹ ngÃ y + giá» |
| `appointmentType` | string | Yes | Badge loáº¡i khÃ¡m |
| `status` | string (code) | Yes | Logic nghiá»‡p vá»¥ ná»™i bá»™ |
| `statusDisplay` | string | Yes | Hiá»ƒn thá»‹ tráº¡ng thÃ¡i khÃ¡m |
| `paymentStatus` | string (code) | Yes | Logic nÃºt thanh toÃ¡n |
| `paymentStatusDisplay` | string | Yes | Hiá»ƒn thá»‹ tráº¡ng thÃ¡i thanh toÃ¡n |
| `consultationFee` | number | Yes | PhÃ­ khÃ¡m |
| `symptoms` | string | No | Triá»‡u chá»©ng bá»‡nh nhÃ¢n nháº­p |
| `followUpNote` | string | No | Ghi chÃº tÃ¡i khÃ¡m |

Render time:
- `appointmentDateText = format(appointmentDate, "yyyy-MM-dd")`
- `appointmentTimeText = format(appointmentDate, "HH:mm")`

---

## 2.2 Patient - Chi tiáº¿t lá»‹ch háº¹n

Endpoint: `GET /api/appointments/{id}` (ROLE_PATIENT)

| Field | Type | Báº¯t buá»™c | FE dÃ¹ng Ä‘á»ƒ |
|---|---|---|---|
| `id`, `appointmentCode`, `appointmentDate` | mixed | Yes | Header |
| `statusDisplay` | string | Yes | Tráº¡ng thÃ¡i khÃ¡m |
| `paymentStatusDisplay` | string | Yes | Tráº¡ng thÃ¡i thanh toÃ¡n |
| `doctorName`, `specialtyName`, `serviceName` | string | No | ThÃ´ng tin bÃ¡c sÄ©/dá»‹ch vá»¥ |
| `notes`, `symptoms`, `followUpNote` | string | No | Ná»™i dung khÃ¡m |
| `parentAppointmentId` | number | No | Id lá»‹ch gá»‘c náº¿u Ä‘Ã¢y lÃ  lá»‹ch tÃ¡i khÃ¡m; khÃ´ng dá»±a vÃ o `parentAppointment` object |

---

## 2.3 Patient - Thanh toÃ¡n lá»‹ch khÃ¡m ban Ä‘áº§u

1) Táº¡o URL VNPay  
Endpoint: `GET /api/payment/create-url?appointmentId={id}`

2) VNPay callback  
Endpoint: `GET /api/payment/vnpay-return?...&appointmentId={id}`  
Náº¿u cáº¥u hÃ¬nh FE return URL, backend redirect kÃ¨m query:
- `status=SUCCESS|FAILED`
- `responseCode=<VNPay code>`
- `message=<message>`

3) BiÃªn nháº­n  
Endpoint: `GET /api/payment/appointment-receipt?appointmentId={id}`

| Field | Type | FE dÃ¹ng Ä‘á»ƒ |
|---|---|---|
| `booking.appointmentStatus` | string (label VI) | Hiá»ƒn thá»‹ tráº¡ng thÃ¡i khÃ¡m |
| `booking.paymentStatus` | string (label VI) | Hiá»ƒn thá»‹ tráº¡ng thÃ¡i thanh toÃ¡n |
| `payment.amount`, `payment.paidAt` | mixed | BiÃªn nháº­n |

---

## 2.4 Patient - HÃ³a Ä‘Æ¡n sau khÃ¡m

Endpoint:
- `GET /api/invoices/my`
- `GET /api/invoices/my/{id}`

| Field | Type | Báº¯t buá»™c | FE dÃ¹ng Ä‘á»ƒ |
|---|---|---|---|
| `id`, `invoiceCode` | mixed | Yes | Nháº­n diá»‡n hÃ³a Ä‘Æ¡n |
| `status` | string (code) | Yes | Map sang label thanh toÃ¡n |
| `canPayOnline` | boolean | Yes | Báº­t/táº¯t nÃºt thanh toÃ¡n |
| `medicineFee`, `serviceFee`, `totalAmount` | number | Yes | Tá»•ng tiá»n |
| `createdAt` | datetime | Yes | Thá»i Ä‘iá»ƒm táº¡o |

---

## 2.5 Patient - Bá»‡nh Ã¡n

Endpoint:
- `GET /api/medical-records/my`
- `GET /api/medical-records/my/{id}`

List fields:

| Field | Type | FE dÃ¹ng Ä‘á»ƒ |
|---|---|---|
| `recordCreatedAt` (`createdAt` alias) | datetime | Hiá»ƒn thá»‹ ngÃ y táº¡o bá»‡nh Ã¡n |
| `appointmentType` | string | Badge `KhÃ¡m bá»‡nh` / `TÃ¡i khÃ¡m` |
| `appointmentStatus` | string (code) | Logic ná»™i bá»™ |
| `appointmentStatusDisplay` | string | Hiá»ƒn thá»‹ tráº¡ng thÃ¡i khÃ¡m |
| `invoice.status` | string (code) | Map label hÃ³a Ä‘Æ¡n |

Detail fields:

| Field | Type | FE dÃ¹ng Ä‘á»ƒ |
|---|---|---|
| `recordCreatedAt` (`createdAt` alias) | datetime | Hiá»ƒn thá»‹ ngÃ y táº¡o bá»‡nh Ã¡n |
| `appointment.type` | string | Loáº¡i khÃ¡m |
| `appointment.statusDisplay` | string | Tráº¡ng thÃ¡i khÃ¡m |
| `invoice.status` | string (code) | Tráº¡ng thÃ¡i thanh toÃ¡n hÃ³a Ä‘Æ¡n |
| `invoice.canPayOnline` | boolean | NÃºt thanh toÃ¡n hÃ³a Ä‘Æ¡n |
| `followUpAppointment.statusDisplay` | string | Tráº¡ng thÃ¡i lá»‹ch tÃ¡i khÃ¡m |

---

## 2.6 Doctor - Danh sach lich hen

Endpoint: `GET /api/doctor/appointments`

> O API doctor, `status` va `paymentStatus` la label tieng Viet. Dung `typeCode`/`appointmentTypeCode` de phan luong logic, khong suy doan tu label.

| Field | Type | Bat buoc | FE dung de |
|---|---|---|---|
| `id`, `patientName`, `appointmentDate`, `appointmentTime` | mixed | Yes | Danh sach |
| `type` (`appointmentType` alias) | string | Yes | Label loai kham |
| `typeCode` (`appointmentTypeCode` alias) | string | Yes | Logic: `NEW_EXAM` / `FOLLOW_UP` |
| `followUp` | boolean | Yes | Logic UI nhanh cho lich tai kham |
| `status` | string (label) | Yes | Trang thai kham |
| `paymentStatus` | string (label) | Yes | Trang thai thanh toan |
| `consultationFee` | number | Yes | Phi kham |
| `canExamine` | boolean | Yes | Bat nut kham |
| `followUpNote`, `parentAppointmentId` | optional | No | Context tai kham |

---

## 2.7 Doctor - Chi tiet lich hen

Endpoint: `GET /api/doctor/appointments/{id}`

| Field | Type | FE dung de |
|---|---|---|
| `type` (`appointmentType` alias) | string | Label loai kham |
| `typeCode` (`appointmentTypeCode` alias) | string | Logic: `NEW_EXAM` / `FOLLOW_UP` |
| `followUp`, `parentAppointmentId` | mixed | Phan nhanh lich tai kham va lien ket lich goc |
| `status` | string (label) | Trang thai kham |
| `paymentStatus` | string (label) | Trang thai thanh toan |
| `consultationFee` | number | Phi kham |
| `followUpNote` | string | Ghi chu tai kham do bac si tao |
| `symptoms`, `note` | string | Noi dung kham; voi lich tai kham, uu tien `symptoms`, `note` co the null |

---

## 2.8 Doctor - HoÃ n táº¥t khÃ¡m

Endpoint: `POST /api/doctor/appointments/{appointmentId}/complete`

| Field | Type | FE dÃ¹ng Ä‘á»ƒ |
|---|---|---|
| `status` | string (label) | Káº¿t quáº£ lá»‹ch hiá»‡n táº¡i (`ÄÃ£ khÃ¡m`) |
| `invoice.status` | string (label) | Tráº¡ng thÃ¡i hÃ³a Ä‘Æ¡n sau khÃ¡m |
| `followUpAppointment.status` | string (label) | Tráº¡ng thÃ¡i lá»‹ch tÃ¡i khÃ¡m |
| `followUpAppointment.paymentStatus` | string (label) | Thanh toÃ¡n lá»‹ch tÃ¡i khÃ¡m |

---

## 2.9 Doctor - Tao lich tai kham thu cong

Endpoint: `POST /api/doctor/medical-records/{recordId}/follow-up`

Request canonical:

```json
{
  "followUpDate": "yyyy-MM-dd",
  "followUpTime": "HH:mm",
  "note": "string?"
}
```

Alias tam thoi BE van chap nhan:
- `date` -> `followUpDate`
- `time` -> `followUpTime`
- `appointmentDate` -> `followUpDate`
- `appointmentTime` -> `followUpTime`
- `followUpNote` -> `note`

Neu reject request, endpoint nay tra `400` theo format:

```json
{
  "message": "...",
  "code": "FOLLOW_UP_VALIDATION_ERROR",
  "fieldErrors": {
    "followUpDate": "...",
    "followUpTime": "..."
  }
}
```

`fieldErrors` co the rong voi cac loi business khong gan truc tiep vao field, vi du:
- medical record khong ton tai
- medical record khong thuoc doctor hien tai
- da ton tai lich tai kham cho record

| Field | Type | FE dung de |
|---|---|---|
| `id` | number | Ma lich tai kham |
| `appointmentDate`, `appointmentTime` | date/time | Hien thi lich tai kham |
| `type` | string | `Tai kham` |
| `status` | string (label) | Trang thai kham |
| `paymentStatus` | string (label) | Trang thai thanh toan |
| `consultationFee` | number | Phi tai kham (50%) |

---

## 2.10 Doctor - Há»“ sÆ¡ bá»‡nh Ã¡n bá»‡nh nhÃ¢n

Endpoint: `GET /api/doctor/medical-records/patients/{patientId}`

| Field | Type | FE dung de |
|---|---|---|
| `records[].recordId` | number | Ma benh an noi bo |
| `records[].recordCreatedAt` (`records[].createdAt` alias) | datetime | Hien thi ngay tao benh an |
| `records[].examDate` | date | Ngay kham |
| `records[].type` (`records[].appointmentType` alias) | string | Label loai kham cua benh an |
| `records[].typeCode` (`records[].appointmentTypeCode` alias) | string | Logic: `NEW_EXAM` / `FOLLOW_UP` |
| `records[].symptoms`, `records[].diagnosis`, `records[].doctorAdvice` | string | Noi dung benh an |
| `records[].followUpAppointment` | object? | Neu co object nay thi an CTA `Tao lich tai kham` va hien thong tin lich da tao |
| `records[].followUpAppointment.appointmentId`, `appointmentCode`, `appointmentDateTime` | mixed | Thong tin lich tai kham |
| `records[].followUpAppointment.type`, `typeCode`, `status`, `statusDisplay`, `paymentStatus` | mixed | Badge/trang thai/logic lich tai kham |
| `records[].followUpAppointment.note` | string | Ghi chu tai kham |
---

## 2.11 Doctor - Tong quan benh an va danh sach benh nhan

Endpoints:
- `GET /api/doctor/medical-records/summary`
- `GET /api/doctor/medical-records/patients`

Summary fields:

| Field | Type | FE dung de |
|---|---|---|
| `totalPatients` | number | Tong so benh nhan da co benh an voi bac si hien tai |
| `newPatients` | number | So benh nhan co it nhat 1 lich kham thuong khong bi huy |
| `followUpPatients` | number | So benh nhan co it nhat 1 lich tai kham khong bi huy |

Patient list fields:

| Field | Type | FE dung de |
|---|---|---|
| `newExamCount` | number | So lan kham thuong cua benh nhan voi bac si hien tai |
| `followUpCount` | number | So lan tai kham cua benh nhan voi bac si hien tai |
| `totalVisitCount` | number | Tong so lan kham = `newExamCount + followUpCount` |
| `visitCount` | number | Alias backward-compatible cua `totalVisitCount` |
| `latestVisitDate` | date | Ngay lich kham gan nhat; co the la lich tai kham sap toi |

---

## 2.12 Admin - Lich hen tong

Endpoint: `GET /api/admin/appointments`

Response lÃ  `Appointment` entity giá»‘ng patient list, dÃ¹ng:
- `statusDisplay`
- `paymentStatusDisplay`
- `appointmentType`
- `appointmentDate`

---

## 3) FE rules báº¯t buá»™c (Ä‘á»ƒ háº¿t lá»—i status)

1. KhÃ´ng hiá»ƒn thá»‹ raw code (`PENDING`, `PAID_ONLINE`, ...) náº¿u API Ä‘Ã£ tráº£ field display/label.
2. Vá»›i `Appointment` entity, Æ°u tiÃªn:
   - `statusDisplay`
   - `paymentStatusDisplay`
3. NÃºt thanh toÃ¡n lá»‹ch khÃ¡m ban Ä‘áº§u:
   - áº¨n/disable náº¿u `paymentStatus in [PAID, PAID_ONLINE]` hoáº·c `paymentStatusDisplay = "ÄÃ£ thanh toÃ¡n"`.
4. NÃºt thanh toÃ¡n hÃ³a Ä‘Æ¡n sau khÃ¡m:
   - Chá»‰ báº­t khi `canPayOnline = true`.
5. Khi parse ngÃ y giá» tá»« `appointmentDate` (timestamp):
   - Date: `yyyy-MM-dd`
   - Time: `HH:mm`

---

## 4) Change log (status contract)

| Date | Change |
|---|---|
| 2026-06-01 | VNPay success cá»§a lá»‹ch háº¹n nÃ¢ng `status` tá»« `PENDING/PENDING_PAYMENT` lÃªn `CONFIRMED` (khÃ´ng cÃ²n treo pending). |
| 2026-06-01 | `Appointment` bá»• sung label chuáº©n: `statusDisplay`, `paymentStatusDisplay`. |
| 2026-06-01 | Label máº·c Ä‘á»‹nh tráº¡ng thÃ¡i khÃ¡m thá»‘ng nháº¥t vá» `ChÆ°a khÃ¡m` (thay cho `Chá» khÃ¡m` á»Ÿ cÃ¡c API chÃ­nh). |
| 2026-06-01 | Bá»• sung `recordCreatedAt` cho API há»“ sÆ¡ bá»‡nh Ã¡n patient vÃ  doctor Ä‘á»ƒ FE hiá»ƒn thá»‹ ngÃ y táº¡o á»•n Ä‘á»‹nh. |
| 2026-06-01 | ThÃªm alias backward-compatible `createdAt` cho dá»¯ liá»‡u há»“ sÆ¡ bá»‡nh Ã¡n Ä‘á»ƒ FE cÅ© khÃ´ng cáº§n Ä‘á»•i key. |
| 2026-06-01 | Dashboard admin yÃªu cáº§u ROLE_ADMIN; gÃ³i dá»‹ch vá»¥ bá»• sung bá»™ Ä‘áº¿m Ä‘Ã£ Ä‘áº·t/hoÃ n thÃ nh/thanh toÃ¡n/chá»; feedback bá»• sung tráº¡ng thÃ¡i hiá»ƒn thá»‹ vÃ  action aliases. |
| 2026-06-02 | Website feedback admin bo sung `canUnhide`, `visibleOnHomepage`; action response tach rieng `Da duyet feedback.`, `Da an feedback.`, `Da bo an feedback.`, `Da xoa feedback.` de FE show toast dung thao tac. |
| 2026-06-03 | Admin service package management bo sung `statusDisplay`, `itemCount`, `hasBookings`, `canDelete`, query filter `active/status/configured`, endpoint `summary`, `detail`, `toggle active`; FE co the lam man quan ly goi dich vu rieng thay vi chi co man booking. |
| 2026-06-05 | Doctor medical-record overview/list them `newExamCount`, `followUpCount`, `totalVisitCount`; `visitCount` duoc giu lam alias cu. So lieu va ngay gan nhat tinh theo lich kham khong bi huy, bao gom lich tai kham sap toi. |

---

## 5) Quy trÃ¬nh cáº­p nháº­t FE/BE

Má»—i láº§n BE thÃªm/sá»­a field liÃªn quan tráº¡ng thÃ¡i hoáº·c payment:
1. Cáº­p nháº­t file `API_FE_STATUS_CONTRACT.md`.
2. ThÃªm 1 dÃ²ng vÃ o `Change log`.
3. Gá»­i FE changelog + endpoint bá»‹ áº£nh hÆ°á»Ÿng + sample JSON má»›i.

