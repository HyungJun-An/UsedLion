package com.example.usedlion.dto;

import java.time.LocalDateTime; // ★ import 추가

public class UserInformation {
    private Long id;
    private String email;
    private String password;
    private String username; // name → username
    private String nickname; // ★ new
    private String provider;
    private String providerId;
    private String role;
    private LocalDateTime createdAt; // 타입 동일
    private String region; // Daum Postcode API
    private boolean is_profile_complete;

    // Getters and Setters
    public boolean isProfileComplete() {
        return this.is_profile_complete;
    }

    public boolean hasRequiredProfileFields() {
        return nickname != null && !nickname.isBlank()
                && region != null && !region.isBlank();
    }

    public void setProfileComplete(boolean profileComplete) {
        this.is_profile_complete = profileComplete;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

}
