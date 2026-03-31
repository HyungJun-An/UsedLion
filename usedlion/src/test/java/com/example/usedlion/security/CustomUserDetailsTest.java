package com.example.usedlion.security;

import com.example.usedlion.dto.UserInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        UserInformation user = new UserInformation();
        user.setEmail("test@test.com");
        user.setPassword("$2a$10$encodedPasswordHash");
        user.setRole("USER");
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("getUsername()은 email을 반환해야 한다")
    void getUsername_returnsEmail() {
        assertThat(userDetails.getUsername()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("getPassword()는 암호화된 비밀번호를 반환해야 한다")
    void getPassword_returnsEncodedPassword() {
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPasswordHash");
    }

    @Test
    @DisplayName("getAuthorities()는 ROLE_USER를 포함해야 한다")
    void getAuthorities_containsRoleUser() {
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("계정 상태 플래그는 모두 true여야 한다")
    void accountStatusFlags_allTrue() {
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("getUser()는 원본 UserInformation 객체를 반환해야 한다")
    void getUser_returnsOriginalUserInformation() {
        assertThat(userDetails.getUser().getEmail()).isEqualTo("test@test.com");
    }
}
