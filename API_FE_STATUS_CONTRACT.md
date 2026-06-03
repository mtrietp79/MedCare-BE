# MedCare Mini API Contract (Status & Payment)

Last updated: `2026-06-01`  
Owner: Backend team  
Scope: FE pages that render appointment / payment / invoice / medical-record statuses.

## 1) Global status dictionary (single source of truth)

### 1.1 Appointment status (code -> label)

| Code | Label (VI) | Notes |
|---|---|---|
| `PENDING_PAYMENT` | `Chưa khám` | Chờ thanh toán lịch khám ban đầu |
| `PENDING` | `Chưa khám` | Chờ khám |
| `CONFIRMED` | `Chưa khám` | Đã thanh toán, đang chờ khám |
| `COMPLETED` | `Đã khám` | Đã hoàn tất khám |
| `CANCELLED` | `Đã hủy` | Đã hủy lịch |

### 1.2 Appointment paymentStatus (code -> label)

| Code | Label (VI) | Notes |
|---|---|---|
| `UNPAID` | `Chưa thanh toán` | Chưa thanh toán |
| `PAID` | `Đã thanh toán` | Đã thanh toán |
| `PAID_ONLINE` | `Đã thanh toán` | VNPay thành công |
| `FAILED` | `Thanh toán thất bại` | Thanh toán lỗi |
| `CANCELLED` | `Đã hủy` | Giao dịch/lịch bị hủy |

### 1.3 Invoice status (code -> label)

| Code | Label (VI) |
|---|---|
| `UNPAID` / `PENDING` | `Chưa thanh toán` |
| `PAID` | `Đã thanh toán` |
| `FAILED` | `Thanh toán thất bại` |
| `CANCELLED` | `Đã hủy` |

### 1.4 Appointment type

| Field | Value |
|---|---|
| `appointmentType` | `Khám bệnh` \| `Tái khám` |

---

## 2) Mini contract theo màn FE

## 2.1 Patient - Lịch hẹn của tôi

Endpoint: `GET /api/appointments` (ROLE_PATIENT)

> Response hiện là `Appointment` entity. FE chỉ đọc các field đã liệt kê dưới đây.

| Field | Type | Bắt buộc | FE dùng để |
|---|---|---|---|
| `id` | number | Yes | Link chi tiết lịch |
| `appointmentCode` | string | Yes | Mã lịch |
| `appointmentDate` | datetime | Yes | Hiển thị ngày + giờ |
| `appointmentType` | string | Yes | Badge loại khám |
| `status` | string (code) | Yes | Logic nghiệp vụ nội bộ |
| `statusDisplay` | string | Yes | Hiển thị trạng thái khám |
| `paymentStatus` | string (code) | Yes | Logic nút thanh toán |
| `paymentStatusDisplay` | string | Yes | Hiển thị trạng thái thanh toán |
| `consultationFee` | number | Yes | Phí khám |
| `symptoms` | string | No | Triệu chứng bệnh nhân nhập |
| `followUpNote` | string | No | Ghi chú tái khám |

Render time:
- `appointmentDateText = format(appointmentDate, "yyyy-MM-dd")`
- `appointmentTimeText = format(appointmentDate, "HH:mm")`

---

## 2.2 Patient - Chi tiết lịch hẹn

Endpoint: `GET /api/appointments/{id}` (ROLE_PATIENT)

| Field | Type | Bắt buộc | FE dùng để |
|---|---|---|---|
| `id`, `appointmentCode`, `appointmentDate` | mixed | Yes | Header |
| `statusDisplay` | string | Yes | Trạng thái khám |
| `paymentStatusDisplay` | string | Yes | Trạng thái thanh toán |
| `doctorName`, `specialtyName`, `serviceName` | string | No | Thông tin bác sĩ/dịch vụ |
| `notes`, `symptoms`, `followUpNote` | string | No | Nội dung khám |

---

## 2.3 Patient - Thanh toán lịch khám ban đầu

1) Tạo URL VNPay  
Endpoint: `GET /api/payment/create-url?appointmentId={id}`

2) VNPay callback  
Endpoint: `GET /api/payment/vnpay-return?...&appointmentId={id}`  
Nếu cấu hình FE return URL, backend redirect kèm query:
- `status=SUCCESS|FAILED`
- `responseCode=<VNPay code>`
- `message=<message>`

3) Biên nhận  
Endpoint: `GET /api/payment/appointment-receipt?appointmentId={id}`

| Field | Type | FE dùng để |
|---|---|---|
| `booking.appointmentStatus` | string (label VI) | Hiển thị trạng thái khám |
| `booking.paymentStatus` | string (label VI) | Hiển thị trạng thái thanh toán |
| `payment.amount`, `payment.paidAt` | mixed | Biên nhận |

---

## 2.4 Patient - Hóa đơn sau khám

Endpoint:
- `GET /api/invoices/my`
- `GET /api/invoices/my/{id}`

| Field | Type | Bắt buộc | FE dùng để |
|---|---|---|---|
| `id`, `invoiceCode` | mixed | Yes | Nhận diện hóa đơn |
| `status` | string (code) | Yes | Map sang label thanh toán |
| `canPayOnline` | boolean | Yes | Bật/tắt nút thanh toán |
| `medicineFee`, `serviceFee`, `totalAmount` | number | Yes | Tổng tiền |
| `createdAt` | datetime | Yes | Thời điểm tạo |

---

## 2.5 Patient - Bệnh án

Endpoint:
- `GET /api/medical-records/my`
- `GET /api/medical-records/my/{id}`

List fields:

| Field | Type | FE dùng để |
|---|---|---|
| `recordCreatedAt` (`createdAt` alias) | datetime | Hiển thị ngày tạo bệnh án |
| `appointmentType` | string | Badge `Khám bệnh` / `Tái khám` |
| `appointmentStatus` | string (code) | Logic nội bộ |
| `appointmentStatusDisplay` | string | Hiển thị trạng thái khám |
| `invoice.status` | string (code) | Map label hóa đơn |

Detail fields:

| Field | Type | FE dùng để |
|---|---|---|
| `recordCreatedAt` (`createdAt` alias) | datetime | Hiển thị ngày tạo bệnh án |
| `appointment.type` | string | Loại khám |
| `appointment.statusDisplay` | string | Trạng thái khám |
| `invoice.status` | string (code) | Trạng thái thanh toán hóa đơn |
| `invoice.canPayOnline` | boolean | Nút thanh toán hóa đơn |
| `followUpAppointment.statusDisplay` | string | Trạng thái lịch tái khám |

---

## 2.6 Doctor - Danh sách lịch hẹn

Endpoint: `GET /api/doctor/appointments`

> Lưu ý: ở API doctor, `status` và `paymentStatus` đã là **label tiếng Việt** (không phải code).

| Field | Type | Bắt buộc | FE dùng để |
|---|---|---|---|
| `id`, `patientName`, `appointmentDate`, `appointmentTime` | mixed | Yes | Danh sách |
| `type` | string | Yes | `Khám bệnh` / `Tái khám` |
| `status` | string (label) | Yes | `Đã khám` / `Chưa khám` / `Hủy lịch` |
| `paymentStatus` | string (label) | Yes | `Đã thanh toán` / `Chưa thanh toán` ... |
| `consultationFee` | number | Yes | Phí khám |
| `canExamine` | boolean | Yes | Bật nút khám |
| `followUpNote`, `parentAppointmentId` | optional | No | Context tái khám |

---

## 2.7 Doctor - Chi tiết lịch hẹn

Endpoint: `GET /api/doctor/appointments/{id}`

| Field | Type | FE dùng để |
|---|---|---|
| `type` | string | Loại khám |
| `status` | string (label) | Trạng thái khám |
| `paymentStatus` | string (label) | Trạng thái thanh toán |
| `consultationFee` | number | Phí khám |
| `symptoms`, `note` | string | Nội dung khám |

---

## 2.8 Doctor - Hoàn tất khám

Endpoint: `POST /api/doctor/appointments/{appointmentId}/complete`

| Field | Type | FE dùng để |
|---|---|---|
| `status` | string (label) | Kết quả lịch hiện tại (`Đã khám`) |
| `invoice.status` | string (label) | Trạng thái hóa đơn sau khám |
| `followUpAppointment.status` | string (label) | Trạng thái lịch tái khám |
| `followUpAppointment.paymentStatus` | string (label) | Thanh toán lịch tái khám |

---

## 2.9 Doctor - Tạo lịch tái khám thủ công

Endpoint: `POST /api/doctor/medical-records/{recordId}/follow-up`

| Field | Type | FE dùng để |
|---|---|---|
| `appointmentDate`, `appointmentTime` | date/time | Hiển thị lịch tái khám |
| `type` | string | `Tái khám` |
| `status` | string (label) | Trạng thái khám |
| `paymentStatus` | string (label) | Trạng thái thanh toán |
| `consultationFee` | number | Phí tái khám (50%) |

---

## 2.10 Doctor - Hồ sơ bệnh án bệnh nhân

Endpoint: `GET /api/doctor/medical-records/patients/{patientId}`

| Field | Type | FE dùng để |
|---|---|---|
| `records[].recordId` | number | Mã bệnh án nội bộ |
| `records[].recordCreatedAt` (`records[].createdAt` alias) | datetime | Hiển thị ngày tạo bệnh án |
| `records[].examDate` | date | Ngày khám |
| `records[].type` | string | `Khám bệnh` / `Tái khám` |
| `records[].symptoms`, `records[].diagnosis`, `records[].doctorAdvice` | string | Nội dung bệnh án |

---

## 2.11 Admin - Lịch hẹn tổng

Endpoint: `GET /api/admin/appointments`

Response là `Appointment` entity giống patient list, dùng:
- `statusDisplay`
- `paymentStatusDisplay`
- `appointmentType`
- `appointmentDate`

---

## 3) FE rules bắt buộc (để hết lỗi status)

1. Không hiển thị raw code (`PENDING`, `PAID_ONLINE`, ...) nếu API đã trả field display/label.
2. Với `Appointment` entity, ưu tiên:
   - `statusDisplay`
   - `paymentStatusDisplay`
3. Nút thanh toán lịch khám ban đầu:
   - Ẩn/disable nếu `paymentStatus in [PAID, PAID_ONLINE]` hoặc `paymentStatusDisplay = "Đã thanh toán"`.
4. Nút thanh toán hóa đơn sau khám:
   - Chỉ bật khi `canPayOnline = true`.
5. Khi parse ngày giờ từ `appointmentDate` (timestamp):
   - Date: `yyyy-MM-dd`
   - Time: `HH:mm`

---

## 4) Change log (status contract)

| Date | Change |
|---|---|
| 2026-06-01 | VNPay success của lịch hẹn nâng `status` từ `PENDING/PENDING_PAYMENT` lên `CONFIRMED` (không còn treo pending). |
| 2026-06-01 | `Appointment` bổ sung label chuẩn: `statusDisplay`, `paymentStatusDisplay`. |
| 2026-06-01 | Label mặc định trạng thái khám thống nhất về `Chưa khám` (thay cho `Chờ khám` ở các API chính). |
| 2026-06-01 | Bổ sung `recordCreatedAt` cho API hồ sơ bệnh án patient và doctor để FE hiển thị ngày tạo ổn định. |
| 2026-06-01 | Thêm alias backward-compatible `createdAt` cho dữ liệu hồ sơ bệnh án để FE cũ không cần đổi key. |
| 2026-06-01 | Dashboard admin yêu cầu ROLE_ADMIN; gói dịch vụ bổ sung bộ đếm đã đặt/hoàn thành/thanh toán/chờ; feedback bổ sung trạng thái hiển thị và action aliases. |
| 2026-06-02 | Website feedback admin bo sung `canUnhide`, `visibleOnHomepage`; action response tach rieng `Da duyet feedback.`, `Da an feedback.`, `Da bo an feedback.`, `Da xoa feedback.` de FE show toast dung thao tac. |
| 2026-06-03 | Admin service package management bo sung `statusDisplay`, `itemCount`, `hasBookings`, `canDelete`, query filter `active/status/configured`, endpoint `summary`, `detail`, `toggle active`; FE co the lam man quan ly goi dich vu rieng thay vi chi co man booking. |
| 2026-06-01 | Hotfix thuốc: `/api/medicines` và `/api/doctor/medicines` bỏ filter `lower(...)` ở DB để tránh lỗi `lower(bytea)`; filter/search chuyển sang backend memory-safe, thêm migration tự sửa cột thuốc bị lệch kiểu `bytea` về text. |

---

## 5) Quy trình cập nhật FE/BE

Mỗi lần BE thêm/sửa field liên quan trạng thái hoặc payment:
1. Cập nhật file `API_FE_STATUS_CONTRACT.md`.
2. Thêm 1 dòng vào `Change log`.
3. Gửi FE changelog + endpoint bị ảnh hưởng + sample JSON mới.
