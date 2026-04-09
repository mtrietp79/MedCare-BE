package com.medcare.clinic_backend.service;

import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.entity.MedicalRecord;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.repository.PrescriptionDetailRepository;
import com.medcare.clinic_backend.repository.ServiceDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionRepo;

    @Autowired
    private ServiceDetailRepository serviceDetailRepo;

    // Logic Tự động tạo hóa đơn và tính tiền
    public Invoice createInvoiceFromRecord(MedicalRecord record) {

        // 1. Quét bảng Chi tiết đơn thuốc: Tính tổng (Số lượng * Đơn giá thuốc)
        double medicineFee = prescriptionRepo.findByMedicalRecordId(record.getId())
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicine().getPrice())
                .sum();

        // 2. Quét bảng Chi tiết dịch vụ: Tính tổng (Số lượng * Đơn giá dịch vụ)
        double serviceFee = serviceDetailRepo.findByMedicalRecordId(record.getId())
                .stream()
                .mapToDouble(detail -> detail.getQuantity() * detail.getMedicalService().getPrice())
                .sum();

        // 3. Tạo Hóa đơn mới
        Invoice invoice = new Invoice();
        invoice.setMedicalRecord(record);
        invoice.setMedicineFee(medicineFee);
        invoice.setServiceFee(serviceFee);
        invoice.setTotalAmount(medicineFee + serviceFee);
        invoice.setStatus("UNPAID");

        // Lưu vào Database
        return invoiceRepository.save(invoice);
    }

    // API hỗ trợ lấy Hóa đơn ra xem
    public Invoice getInvoiceByRecordId(Integer recordId) {
        return invoiceRepository.findByMedicalRecordId(recordId);
    }
}