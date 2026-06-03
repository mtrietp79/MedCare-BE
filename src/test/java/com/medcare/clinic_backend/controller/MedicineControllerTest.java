package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.dto.AdminMedicineSummaryResponse;
import com.medcare.clinic_backend.dto.MedicineResponse;
import com.medcare.clinic_backend.exception.GlobalExceptionHandler;
import com.medcare.clinic_backend.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MedicineControllerTest {

    @Mock
    private MedicineService medicineService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MedicineController controller = new MedicineController();
        ReflectionTestUtils.setField(controller, "medicineService", medicineService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSummary_shouldReturnSummaryPayloadInsteadOfParsingSummaryAsId() throws Exception {
        when(medicineService.getAdminMedicineSummary())
                .thenReturn(new AdminMedicineSummaryResponse(2, 1, 3, 62));

        mockMvc.perform(get("/api/medicines/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockCount").value(2))
                .andExpect(jsonPath("$.outOfStockCount").value(1))
                .andExpect(jsonPath("$.expiredCount").value(3))
                .andExpect(jsonPath("$.total").value(62));

        verify(medicineService).getAdminMedicineSummary();
        verify(medicineService, never()).getMedicineResponseById(anyInt());
    }

    @Test
    void getById_shouldStillResolveNumericMedicineId() throws Exception {
        MedicineResponse response = new MedicineResponse();
        response.setId(12);
        response.setName("Paracetamol");

        when(medicineService.getMedicineResponseById(12)).thenReturn(response);

        mockMvc.perform(get("/api/medicines/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.name").value("Paracetamol"));

        verify(medicineService).getMedicineResponseById(12);
    }
}
