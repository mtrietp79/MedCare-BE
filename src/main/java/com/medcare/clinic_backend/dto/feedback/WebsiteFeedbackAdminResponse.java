package com.medcare.clinic_backend.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteFeedbackAdminResponse {
    private Integer id;
    private Integer patientId;
    private String fullName;
    private String email;
    private Integer rating;
    private String comment;
    private String status;
    private String statusDisplay;
    private Boolean canApprove;
    private Boolean canHide;
    private Boolean canUnhide;
    private Boolean canDelete;
    private Boolean visibleOnHomepage;
    private LocalDateTime createdAt;
}
