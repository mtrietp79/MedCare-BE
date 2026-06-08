package com.medcare.clinic_backend.dto;

public class AuthResponse {
    private Integer id;
    private String token;
    private String accessToken;
    private String tokenType = "Bearer ";
    private String username;
    private String displayName;
    private String role;
    private Boolean profileCompleted;
    private Boolean mustChangePassword;

    public AuthResponse(String accessToken) {
        this.token = accessToken;
        this.accessToken = accessToken;
    }

    public AuthResponse(Integer id, String accessToken, String username, String displayName, String role) {
        this.id = id;
        this.token = accessToken;
        this.accessToken = accessToken;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public AuthResponse(Integer id, String accessToken, String username, String displayName, String role, Boolean profileCompleted) {
        this(id, accessToken, username, displayName, role, profileCompleted, false);
    }

    public AuthResponse(
            Integer id,
            String accessToken,
            String username,
            String displayName,
            String role,
            Boolean profileCompleted,
            Boolean mustChangePassword
    ) {
        this.id = id;
        this.token = accessToken;
        this.accessToken = accessToken;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.profileCompleted = profileCompleted;
        this.mustChangePassword = mustChangePassword;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.token = accessToken;
        this.accessToken = accessToken;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        this.accessToken = token;
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

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
