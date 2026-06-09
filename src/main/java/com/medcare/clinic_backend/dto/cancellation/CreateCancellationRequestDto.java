package com.medcare.clinic_backend.dto.cancellation;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CreateCancellationRequestDto {
    @JsonAlias({"cancel_reason", "reason", "ly_do_huy", "lyDoHuy"})
    private String cancelReason;

    @JsonAlias({"bank_name", "ten_ngan_hang", "tenNganHang"})
    private String bankName;

    @JsonAlias({"bank_account_number", "so_tai_khoan", "soTaiKhoan", "accountNumber"})
    private String bankAccountNumber;

    @JsonAlias({"bank_account_holder", "ten_chu_tai_khoan", "tenChuTaiKhoan", "accountHolder"})
    private String bankAccountHolder;

    @JsonAlias({"patient_note", "note", "ghi_chu", "ghiChu"})
    private String patientNote;
}
