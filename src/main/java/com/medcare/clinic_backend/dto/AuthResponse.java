package com.medcare.clinic_backend.dto;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer ";
    private String username;
    private String displayName;
    private String role;
    private Boolean profileCompleted;

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public AuthResponse(String accessToken, String username, String displayName, String role) {
        this.accessToken = accessToken;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public AuthResponse(String accessToken, String username, String displayName, String role, Boolean profileCompleted) {
        this.accessToken = accessToken;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.profileCompleted = profileCompleted;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}
