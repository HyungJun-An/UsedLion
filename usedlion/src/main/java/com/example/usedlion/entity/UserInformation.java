package com.example.usedlion.entity;

import java.time.LocalDateTime;

import groovy.transform.builder.Builder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // 실제 DB 컬럼명과 매핑
    private Integer userId;
    private String email;
    private String password;
    private String username;
    private String nickname;
    private String provider;
    private String provider_id;
    private String role;
    private LocalDateTime created_at;
    private String region; // Daum Postcode API
    private boolean is_profile_complete;

}
