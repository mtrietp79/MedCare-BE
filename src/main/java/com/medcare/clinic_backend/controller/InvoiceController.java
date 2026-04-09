package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.entity.Invoice;
import com.medcare.clinic_backend.repository.InvoiceRepository;
import com.medcare.clinic_backend.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    // Lấy toàn bộ danh sách Hóa đơn
    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    // Lấy Hóa đơn theo ID của Hồ sơ bệnh án
    @GetMapping("/record/{recordId}")
    public Invoice getInvoiceByRecordId(@PathVariable Integer recordId) {
        return invoiceService.getInvoiceByRecordId(recordId);
    }

    // API cho Quầy Thu ngân: Xác nhận đã nhận tiền và chuyển trạng thái thành PAID
    @PutMapping("/{id}/pay")
    public Invoice payInvoice(@PathVariable Integer id) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        invoice.setStatus("PAID");
        return invoiceRepository.save(invoice);
    }
}