package com.medcare.clinic_backend.controller;

import com.medcare.clinic_backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DashboardController controller = new DashboardController();
        ReflectionTestUtils.setField(controller, "dashboardService", dashboardService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void exportDashboardReport_shouldReturnExcelAttachment() throws Exception {
        byte[] reportBytes = new byte[]{1, 2, 3, 4};
        when(dashboardService.exportDashboardReport(2026)).thenReturn(reportBytes);

        mockMvc.perform(get("/api/admin/dashboard/report").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"medcare-dashboard-report-2026.xlsx\""
                ))
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .andExpect(content().bytes(reportBytes));

        verify(dashboardService).exportDashboardReport(2026);
    }
}
