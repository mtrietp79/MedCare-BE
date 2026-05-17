package com.medcare.clinic_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class SocialLoginRequest {

    @JsonAlias({"idToken", "accessToken", "credential", "authToken"})
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
